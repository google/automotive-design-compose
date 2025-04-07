// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use crate::{
    component_context::ComponentContext,
    error::Error,
    extended_layout_schema::ExtendedAutoLayout,
    figma_schema,
    image_context::{ImageContext, ImageContextSession},
    proxy_config::ProxyConfig,
    transform_flexbox::create_component_flexbox,
    variable_utils::create_variable,
};
use dc_bundle::definition::EncodedImageMap;
use dc_bundle::definition::NodeQuery;
use dc_bundle::figma_doc::FigmaDocInfo;
use dc_bundle::variable::variable_map::NameIdMap;
use dc_bundle::variable::{Collection, Mode, Variable, VariableMap};
use dc_bundle::view::view_data::{Container, View_data_type};
use dc_bundle::view::{ComponentInfo, ComponentOverrides, View, ViewData};
use dc_bundle::view_style::ViewStyle;
use log::error;
use protobuf::MessageField;
use std::time::Duration;
use std::{
    collections::{HashMap, HashSet},
    iter::FromIterator,
};

const FIGMA_TOKEN_HEADER: &str = "X-Figma-Token";
const BASE_FILE_URL: &str = "https://api.figma.com/v1/files/";
const BASE_COMPONENT_URL: &str = "https://api.figma.com/v1/components/";
const BASE_PROJECT_URL: &str = "https://api.figma.com/v1/projects/";

fn http_fetch(api_key: &str, url: String, proxy_config: &ProxyConfig) -> Result<String, Error> {
    let mut agent_builder = ureq::AgentBuilder::new();
    let mut buffer = Vec::new();
    // Only HttpProxyConfig is supported.
    if let ProxyConfig::HttpProxyConfig(spec) = proxy_config {
        agent_builder = agent_builder.proxy(ureq::Proxy::new(spec)?);
    }

    agent_builder
        .build()
        .get(url.as_str())
        .set(FIGMA_TOKEN_HEADER, api_key)
        .timeout(Duration::from_secs(90))
        .call()?
        .into_reader()
        .read_to_end(&mut buffer)?;

    let body = String::from_utf8(buffer)?;

    Ok(body)
}

/// Document update requests return this value to indicate if an update was
/// made or not.
#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub enum UpdateStatus {
    Updated,
    NotUpdated,
}

/// Branches alwasy return head of file, i.e. no version returned
fn get_branches(document_root: &figma_schema::FileResponse) -> Vec<FigmaDocInfo> {
    let mut branches = vec![];
    if let Some(doc_branches) = &document_root.branches {
        for hash in doc_branches {
            if let (Some(Some(id)), Some(Some(name))) = (hash.get("key"), hash.get("name")) {
                let figma_doc =
                    FigmaDocInfo { name: name.clone(), id: id.clone(), ..Default::default() };
                branches.push(figma_doc);
            }
        }
    }
    branches
}

fn load_image_hash_to_res_map(
    document_root: &figma_schema::FileResponse,
) -> HashMap<String, String> {
    let root_node = &document_root.document;
    let shared_plugin_data = &root_node.shared_plugin_data;
    if shared_plugin_data.contains_key("designcompose") {
        let plugin_data = shared_plugin_data.get("designcompose");
        if let Some(vsw_data) = plugin_data {
            if let Some(image_hash_to_res_data) = vsw_data.get("image_hash_to_res") {
                let image_hash_to_res_map: Option<HashMap<String, String>> =
                    serde_json::from_str(image_hash_to_res_data.as_str()).ok();
                if let Some(map) = image_hash_to_res_map {
                    return map;
                }
            }
        }
    }
    return HashMap::new();
}

/// Document is used to access and maintain an entire Figma document, including
/// components and image resources. It can be updated if the source document
/// has changed since this structure was created.
pub struct Document {
    api_key: String,
    document_id: String,
    version_id: String,
    proxy_config: ProxyConfig,
    document_root: figma_schema::FileResponse,
    variables_response: Option<figma_schema::VariablesResponse>,
    image_context: ImageContext,
    variant_nodes: Vec<figma_schema::Node>,
    component_sets: HashMap<String, String>,
    pub branches: Vec<FigmaDocInfo>,
}
impl Document {
    pub fn root_node(&self) -> &figma_schema::Node {
        &self.document_root.document
    }
    /// Fetch a document from Figma and return a Document instance that can be used
    /// to extract toolkit nodes.
    pub fn new(
        api_key: &str,
        document_id: String,
        version_id: String,
        proxy_config: &ProxyConfig,
        image_session: Option<ImageContextSession>,
    ) -> Result<Document, Error> {
        // Fetch the document...
        let mut document_url = format!(
            "{}{}?plugin_data=shared&geometry=paths&branch_data=true",
            BASE_FILE_URL, document_id,
        );
        if !version_id.is_empty() {
            document_url.push_str("&version=");
            document_url.push_str(&version_id);
        }
        let document_root: figma_schema::FileResponse =
            serde_json::from_str(http_fetch(api_key, document_url, proxy_config)?.as_str())?;

        // ...and the mapping from imageRef to URL. It returns images from all versions.
        let image_ref_url = format!("{}{}/images", BASE_FILE_URL, document_id);
        let image_refs: figma_schema::ImageFillResponse =
            serde_json::from_str(http_fetch(api_key, image_ref_url, proxy_config)?.as_str())?;

        let image_hash_to_res_map = load_image_hash_to_res_map(&document_root);
        let mut image_context =
            ImageContext::new(image_refs.meta.images, image_hash_to_res_map, proxy_config);
        if let Some(session) = image_session {
            image_context.add_session_info(session);
        }

        let branches = get_branches(&document_root);
        let variables_response =
            match Self::fetch_variables(api_key, &document_id, proxy_config).map_err(Error::from) {
                Ok(it) => Some(it),
                Err(err) => {
                    error!("Failed to fetch variables {:?}", err);
                    None
                }
            };

        Ok(Document {
            api_key: api_key.to_string(),
            document_id,
            version_id,
            proxy_config: proxy_config.clone(),
            document_root,
            variables_response,
            image_context,
            variant_nodes: vec![],
            component_sets: HashMap::new(),
            branches,
        })
    }

    // Fetch and store all the variables, collections, and modes from the Figma document.
    fn fetch_variables(
        api_key: &str,
        document_id: &String,
        proxy_config: &ProxyConfig,
    ) -> Result<figma_schema::VariablesResponse, Error> {
        let variables_url = format!("{}{}/variables/local", BASE_FILE_URL, document_id);
        let var_fetch = http_fetch(api_key, variables_url, proxy_config)?;
        let var_response: figma_schema::VariablesResponse =
            serde_json::from_str(var_fetch.as_str())?;
        Ok(var_response)
    }

    /// Fetch a document from Figma only if it has changed since the given last
    /// modified time.
    pub fn new_if_changed(
        api_key: &str,
        document_id: String,
        requested_version_id: String,
        proxy_config: &ProxyConfig,
        last_modified: String,
        last_version: String,
        image_session: Option<ImageContextSession>,
    ) -> Result<Option<Document>, Error> {
        let mut document_head_url = format!("{}{}?depth=1", BASE_FILE_URL, document_id);
        if !requested_version_id.is_empty() {
            document_head_url.push_str("&version=");
            document_head_url.push_str(&requested_version_id);
        }
        let document_head: figma_schema::FileHeadResponse =
            serde_json::from_str(http_fetch(api_key, document_head_url, proxy_config)?.as_str())?;

        if document_head.last_modified == last_modified && document_head.version == last_version {
            return Ok(None);
        }

        Document::new(api_key, document_id, requested_version_id, proxy_config, image_session)
            .map(Some)
    }

    /// Ask Figma if an updated document is available, and then fetch the updated document
    /// if so.
    pub fn update(&mut self, proxy_config: &ProxyConfig) -> Result<UpdateStatus, Error> {
        self.proxy_config = proxy_config.clone();

        // Fetch just the top level of the document. (depth=0 causes an internal server error).
        let mut document_head_url = format!("{}{}?depth=1", BASE_FILE_URL, self.document_id);
        if !self.version_id.is_empty() {
            document_head_url.push_str("&version=");
            document_head_url.push_str(&self.version_id);
        }
        let document_head: figma_schema::FileHeadResponse = serde_json::from_str(
            http_fetch(self.api_key.as_str(), document_head_url, &self.proxy_config)?.as_str(),
        )?;

        // Now compare the version and modification times and bail out if they're the same.
        // Figma docs include a "version" field, but that doesn't always change when the document
        // changes (but the mtime always seems to change). The version does change (and mtime does
        // not) when a branch is created.
        if document_head.last_modified == self.document_root.last_modified
            && document_head.version == self.document_root.version
        {
            return Ok(UpdateStatus::NotUpdated);
        }

        // Fetch the updated document in its entirety and replace our document root...
        let mut document_url = format!(
            "{}{}?plugin_data=shared&geometry=paths&branch_data=true",
            BASE_FILE_URL, self.document_id,
        );
        if !self.version_id.is_empty() {
            document_url.push_str("&version=");
            document_url.push_str(&self.version_id);
        }
        let document_root: figma_schema::FileResponse = serde_json::from_str(
            http_fetch(self.api_key.as_str(), document_url, &self.proxy_config)?.as_str(),
        )?;

        // ...and the mapping from imageRef to URL. It returns images from all versions.
        let image_ref_url = format!("{}{}/images", BASE_FILE_URL, self.document_id);
        let image_refs: figma_schema::ImageFillResponse = serde_json::from_str(
            http_fetch(self.api_key.as_str(), image_ref_url, &self.proxy_config)?.as_str(),
        )?;

        self.branches = get_branches(&document_root);
        self.document_root = document_root;
        self.image_context.update_images(image_refs.meta.images);

        Ok(UpdateStatus::Updated)
    }

    /// Return the last modified time of this document. This seems to update whenever the doc
    /// is changed (but the version number does not).
    pub fn last_modified(&self) -> &String {
        &self.document_root.last_modified
    }

    /// Find all nodes whose data is of type NodeData::Instance, which means could be a
    /// component with variants. Find its parent, and if it is of type NodeData::ComponentSet,
    /// then add all of its children to self.variant_nodes. Also fill out node_doc_hash,
    /// which hashes node ids to the document id they come from.
    fn fetch_component_variants(
        &self,
        node: &figma_schema::Node,
        node_doc_hash: &mut HashMap<String, String>,
        variant_nodes: &mut Vec<figma_schema::Node>,
        id_index: &HashMap<String, &figma_schema::Node>,
        component_hash: &HashMap<String, figma_schema::Component>,
        parent_tree: &mut Vec<String>,
        error_list: &mut Vec<String>,
        error_hash: &mut HashSet<String>,
        skip_hidden: bool,
    ) -> Result<(), Error> {
        // Ignore hidden nodes
        if !node.visible && skip_hidden {
            return Ok(());
        }
        fn add_node_doc_hash(
            node: &figma_schema::Node,
            node_doc_hash: &mut HashMap<String, String>,
            doc_id: &String,
        ) {
            // Add the node id, doc id to the hash and recurse on all children
            node_doc_hash.insert(node.id.clone(), doc_id.clone());
            for child in &node.children {
                add_node_doc_hash(child, node_doc_hash, doc_id);
            }
        }

        if let figma_schema::NodeData::Instance { frame: _, component_id } = &node.data {
            // If the component_id is in id_index, we know it's in this document so we don't
            // need to do anything. If it isn't, it's in a different doc, so proceed to
            // download data for it
            if !id_index.contains_key(component_id) {
                // Find the component info for the component_id
                let component = component_hash.get(component_id);
                if let Some(component) = component {
                    // Fetch the component from the figma api given its key
                    let file_key = component.key.clone();
                    // If we already retrieved this component instance but got an error, don't try again
                    if error_hash.contains(&file_key) {
                        return Ok(());
                    }
                    let component_url = format!("{}{}", BASE_COMPONENT_URL, file_key);
                    let component_http_response = match http_fetch(
                        self.api_key.as_str(),
                        component_url.clone(),
                        &self.proxy_config,
                    ) {
                        Ok(str) => str,
                        Err(e) => {
                            let fetch_error = if let Error::NetworkError(ureq_error) = &e {
                                if let ureq::Error::Status(code, _response) = ureq_error {
                                    format!("HTTP {} at {}", code, component_url)
                                } else {
                                    ureq_error.to_string()
                                }
                            } else {
                                e.to_string()
                            };
                            let error_string = format!(
                                "Fetch component error {}: {} -> {}",
                                fetch_error,
                                parent_tree.join(" -> "),
                                node.name
                            );
                            error_hash.insert(file_key);
                            error_list.push(error_string);
                            return Ok(());
                        }
                    };

                    // Deserialize into a ComponentKeyResponse
                    let component_key_response: figma_schema::ComponentKeyResponse =
                        serde_json::from_str(component_http_response.as_str())?;
                    // If this variant points to a file_key different than this document, fetch it
                    let maybe_parent_node_id = component_key_response.parent_id();
                    if let Some(parent_node_id) = maybe_parent_node_id {
                        let variant_document_id = component_key_response.meta.file_key;
                        if variant_document_id != self.document_id {
                            let nodes_url = format!(
                                "{}{}/nodes?ids={}",
                                BASE_FILE_URL, variant_document_id, parent_node_id
                            );
                            let http_str =
                                http_fetch(self.api_key.as_str(), nodes_url, &self.proxy_config)?;
                            let nodes_response: figma_schema::NodesResponse =
                                serde_json::from_str(http_str.as_str())?;
                            // The response is a list of nodes, but we only requested one so this loop
                            // should only go through one time
                            for (node_id, node_response_data) in nodes_response.nodes {
                                if node_id != parent_node_id {
                                    continue; // We only care about parent_node_id
                                }
                                // If the parent is a COMPONENT_SET, then we want to get the parent's children
                                // and add them to our list of nodes
                                if let figma_schema::NodeData::ComponentSet { frame: _ } =
                                    node_response_data.document.data
                                {
                                    for node in node_response_data.document.children {
                                        add_node_doc_hash(
                                            &node,
                                            node_doc_hash,
                                            &variant_document_id,
                                        );
                                        // Recurse on all children
                                        for child in &node.children {
                                            parent_tree.push(node.name.clone());
                                            self.fetch_component_variants(
                                                child,
                                                node_doc_hash,
                                                variant_nodes,
                                                id_index,
                                                &node_response_data.components,
                                                parent_tree,
                                                error_list,
                                                error_hash,
                                                skip_hidden,
                                            )?;
                                            parent_tree.pop();
                                        }
                                        variant_nodes.push(node);
                                    }
                                }
                            }
                        }
                    } else {
                        let error_string = format!(
                            "Fetch component unable to find component parent for: {} -> {}",
                            parent_tree.join(" -> "),
                            node.name
                        );
                        error_list.push(error_string);
                        return Ok(());
                    }
                }
            }
        }
        // Recurse on all children
        for child in &node.children {
            parent_tree.push(node.name.clone());
            self.fetch_component_variants(
                child,
                node_doc_hash,
                variant_nodes,
                id_index,
                component_hash,
                parent_tree,
                error_list,
                error_hash,
                skip_hidden,
            )?;
            parent_tree.pop();
        }
        Ok(())
    }

    /// Find all of the Component Instance views and see which style and text properties are
    /// overridden in the instance compared to the reference component. If we then render a
    /// different variant of the component (due to an interaction) we can apply these delta
    /// styles to get the correct output.
    fn compute_component_overrides(&self, nodes: &mut HashMap<NodeQuery, View>) {
        // XXX: Would be nice to avoid cloning here. Do we need to? We need to mutate the
        //      instance views in place. And we can't hold a ref and a mutable ref to nodes
        //      at the same time.
        let reference_components = nodes.clone();

        // This function finds all of the Component Instances (views with a populated
        // component_info field) in the given view tree, and looks up which component
        // they are an instance of. If the component is found, then the "action" function
        // is run with a mutable reference to the Component Instance view and a reference
        // to the component.
        //
        // These two pieces of information (the instance and the component) can then be
        // used to figure out which properties of the component have been customized in
        // the instance. Then we can be sure to apply those customized properties to other
        // variants (where Figma just gives us the variant definition, but not filled out
        // instance with overrides applied).
        fn for_each_component_instance(
            reference_components: &HashMap<NodeQuery, View>,
            view: &mut View,
            parent_component_info: Option<&mut ComponentInfo>,
            parent_reference_component: Option<&View>,
            action: &impl Fn(
                MessageField<ViewStyle>,
                MessageField<ViewData>,
                String,
                String,
                &mut ComponentInfo,
                Option<&View>,
                bool,
            ),
        ) {
            match (view.component_info.as_mut(), parent_component_info) {
                (Some(info), _) => {
                    // This is the root node of a component instance.
                    // Compute its style and data overrides and write to its component info whose
                    // key is the component_set_name.

                    // See if we can find the target component. If not then don't look up
                    // references. Try searching by id, name, and variant
                    let queries = [
                        NodeQuery::NodeId(info.id.clone()),
                        NodeQuery::NodeName(info.name.clone()),
                        NodeQuery::NodeVariant(info.name.clone(), info.component_set_name.clone()),
                    ];

                    let reference_component_option =
                        queries.iter().find_map(|query| reference_components.get(query));

                    if reference_component_option.is_some() {
                        action(
                            view.style.clone(),
                            view.data.clone(),
                            view.id.clone(),
                            info.component_set_name.clone(),
                            info,
                            reference_component_option,
                            true,
                        );
                    }

                    if let Some(data) = view.data.as_mut() {
                        if let Some(View_data_type::Container { 0: Container { children, .. } }) =
                            data.view_data_type.as_mut()
                        {
                            for child in children {
                                for_each_component_instance(
                                    reference_components,
                                    child,
                                    Some(info),
                                    reference_component_option,
                                    action,
                                );
                            }
                        }
                    }
                }
                (None, Some(parent_info)) => {
                    // This matches a descendent view of a component instance.
                    // The style and data overrides are written to hash map keyed by the view name
                    // in the component info of the instance.
                    action(
                        view.style.clone(),
                        view.data.clone(),
                        view.id.clone(),
                        view.name.clone(),
                        parent_info,
                        parent_reference_component,
                        false,
                    );
                    if let Some(data) = view.data.as_mut() {
                        if let Some(View_data_type::Container { 0: Container { children, .. } }) =
                            data.view_data_type.as_mut()
                        {
                            for child in children {
                                for_each_component_instance(
                                    reference_components,
                                    child,
                                    Some(parent_info),
                                    parent_reference_component,
                                    action,
                                );
                            }
                        }
                    }
                }
                (None, None) => {
                    // This matches the nodes from the root node of the view tree until it
                    // meets a component instance.
                    if let Some(data) = view.data.as_mut() {
                        if let Some(View_data_type::Container { 0: Container { children, .. } }) =
                            data.view_data_type.as_mut()
                        {
                            for child in children {
                                for_each_component_instance(
                                    reference_components,
                                    child,
                                    None,
                                    None,
                                    action,
                                );
                            }
                        }
                    }
                }
            }
        }

        for view in nodes.values_mut() {
            for_each_component_instance(
                &reference_components,
                view,
                None,
                None,
                &|view_style,
                  view_data,
                  view_id,
                  // overrides_table_key will either be the component_set_name or view_name.
                  // This only works if the view name is identical.
                  overrides_table_key,
                  component_info,
                  component,
                  is_component_root| {
                    if let Some(reference_component) = component {
                        let template_view_option = if is_component_root {
                            Some(reference_component)
                        } else {
                            reference_component.find_view_by_id(&view_id)
                        };
                        if let Some(template_view) = template_view_option {
                            let override_view_style = if view_style == template_view.style {
                                MessageField::none()
                            } else if let Some(view_style_ref) = view_style.as_ref() {
                                let diff: Option<ViewStyle> =
                                    Some(template_view.style().difference(view_style_ref));
                                diff.into()
                            } else {
                                error!("ViewStyle is required.");
                                MessageField::none()
                            };
                            let override_view_data =
                                if let Some(reference_view_data) = template_view.data.as_ref() {
                                    if let Some(data) = view_data.as_ref() {
                                        reference_view_data.difference(data).into()
                                    } else {
                                        MessageField::none()
                                    }
                                } else {
                                    MessageField::none()
                                };

                            if override_view_style.is_some() || override_view_data.is_some() {
                                component_info.overrides_table.insert(
                                    overrides_table_key,
                                    ComponentOverrides {
                                        style: override_view_style,
                                        view_data: override_view_data,
                                        ..Default::default()
                                    },
                                );
                            }
                        }
                    }
                },
            );
        }
    }

    /// Convert the nodes with the given names to a structure that's closer to a toolkit
    /// View. This method doesn't use the toolkit itself.
    pub fn nodes(
        &mut self,
        node_names: &Vec<NodeQuery>,
        ignored_images: &Vec<(NodeQuery, Vec<String>)>,
        error_list: &mut Vec<String>,
        skip_hidden: bool,
    ) -> Result<HashMap<NodeQuery, View>, Error> {
        // First we gather all of nodes that we're going to convert and find all of the
        // child nodes that can't be rendered. Then we ask Figma to do a batch render on
        // them. Finally we convert and return the set of toolkit nodes.
        fn index_node<'a>(
            node: &'a figma_schema::Node,
            parent_node: Option<&'a figma_schema::Node>,
            name_index: &mut HashMap<String, &'a figma_schema::Node>,
            id_index: &mut HashMap<String, &'a figma_schema::Node>,
            variant_index: &mut HashMap<(String, String), &'a figma_schema::Node>,
            component_set_name_index: &mut HashMap<String, &'a figma_schema::Node>,
            component_id_index: &mut HashMap<String, &'a figma_schema::Node>,
            skip_hidden: bool,
        ) {
            // Ignore hidden nodes
            if !node.visible && skip_hidden {
                return;
            }

            // If we have a parent that is a component set, add to the variant index
            let mut is_variant = false;
            if let Some(parent) = parent_node {
                if let figma_schema::NodeData::ComponentSet { .. } = parent.data {
                    is_variant = true;
                    variant_index.insert((node.name.clone(), parent.name.clone()), node);
                }
            }

            if !is_variant {
                // If there's already a node with the same name, then only replace it if
                // we're a component.
                if name_index.get(&node.name).is_some() {
                    if let figma_schema::NodeData::Component { .. } = node.data {
                        name_index.insert(node.name.clone(), node);
                    }
                } else {
                    name_index.insert(node.name.clone(), node);
                }
            }
            id_index.insert(node.id.clone(), node);
            for child in &node.children {
                index_node(
                    child,
                    Some(node),
                    name_index,
                    id_index,
                    variant_index,
                    component_set_name_index,
                    component_id_index,
                    skip_hidden,
                );
            }
            if let figma_schema::NodeData::ComponentSet { .. } = node.data {
                component_set_name_index.insert(node.name.clone(), node);
                for child in &node.children {
                    component_id_index.insert(child.id.clone(), node);
                }
            }
        }

        // For each node in nodes or an ancestor of a node in nodes, if it is a component
        // instance, add the component set associated with the instance into node_name_hash.
        fn find_component_sets(
            nodes: &Vec<&figma_schema::Node>,
            component_set_index: &HashMap<String, &figma_schema::Node>,
            node_name_hash: &mut HashSet<NodeQuery>,
        ) {
            for node in nodes {
                if let figma_schema::NodeData::Instance { frame: _, component_id } = &node.data {
                    let component_set = component_set_index.get(component_id);
                    if let Some(cs) = component_set {
                        node_name_hash.insert(NodeQuery::NodeComponentSet(cs.name.clone()));

                        // Recurse on the children of the component set since variants can change
                        // to any of them at runtime
                        let cs_children: Vec<&figma_schema::Node> = cs.children.iter().collect();
                        find_component_sets(&cs_children, component_set_index, node_name_hash);
                    }
                }

                let children: Vec<&figma_schema::Node> = node.children.iter().collect();
                find_component_sets(&children, component_set_index, node_name_hash);
            }
        }

        // For each node that is a component set, add all of its children (which are variants)
        // into the node name hash so that we retrieve those nodes. Also add them to the
        // component_set_hash, which hashes a component set node name to all of its children.
        fn add_variant_node_names(
            node: &figma_schema::Node,
            node_name_hash: &mut HashSet<NodeQuery>,
            component_set_hash: &mut HashMap<String, Vec<String>>,
        ) {
            if let figma_schema::NodeData::ComponentSet { .. } = &node.data {
                // Add the component set's children to node_name_has if either the component set itself
                // is in the node_name_hash, or one of the child variants is in node_name_hash
                let mut add_children = node_name_hash
                    .contains(&NodeQuery::NodeName(node.name.clone()))
                    || node_name_hash.contains(&NodeQuery::NodeComponentSet(node.name.clone()));
                if !add_children {
                    'outer: for child in &node.children {
                        let child_name_parts = child.name.split(',');
                        for variant_name in child_name_parts {
                            let variant_name = variant_name.to_string();
                            let variant_parts: Vec<&str> = variant_name.split('=').collect();
                            if variant_parts.len() == 2 {
                                // Format is property_name=value, so check if the property_name is in node_name_hash
                                if node_name_hash
                                    .contains(&NodeQuery::NodeName(variant_parts[0].to_string()))
                                {
                                    add_children = true;
                                    break 'outer;
                                }
                            }
                        }
                    }
                }
                if add_children {
                    for child in &node.children {
                        let child_node_query =
                            NodeQuery::NodeVariant(child.name.clone(), node.name.clone());
                        if !node_name_hash.contains(&child_node_query) {
                            node_name_hash.insert(child_node_query);
                        }
                    }
                }
                let mut variant_list: Vec<String> = vec![];
                for child in &node.children {
                    variant_list.push(child.name.clone());
                }
                component_set_hash.insert(node.name.clone(), variant_list);
            }
            // Recurse on all children
            for child in &node.children {
                add_variant_node_names(child, node_name_hash, component_set_hash);
            }
        }

        // Search the node hierarchy for nodes that have plugin data that specifies an overflow
        // node. Add these nodes to node_name_hash so that we fetch them.
        fn add_overflow_nodes(node: &figma_schema::Node, node_name_hash: &mut HashSet<NodeQuery>) {
            if node.shared_plugin_data.contains_key("designcompose") {
                let plugin_data = node.shared_plugin_data.get("designcompose");
                if let Some(vsw_data) = plugin_data {
                    if let Some(plugin_layout) = vsw_data.get("vsw-extended-auto-layout") {
                        let extended_auto_layout: Option<ExtendedAutoLayout> =
                            serde_json::from_str(plugin_layout.as_str()).ok();
                        if let Some(extended_auto_layout) = extended_auto_layout {
                            if extended_auto_layout.limit_content {
                                let lcd = extended_auto_layout.limit_content_data;
                                node_name_hash.insert(NodeQuery::NodeId(lcd.overflow_node_id));
                            }
                        }
                    }
                }
            }

            // Recurse on all children
            for child in &node.children {
                add_overflow_nodes(child, node_name_hash);
            }
        }

        let mut name_index = HashMap::new();
        let mut id_index = HashMap::new();
        let mut variant_index = HashMap::new();
        let mut component_set_name_index = HashMap::new();
        // If a component belongs to a component set (there are "variants" in the Figma UI)
        // then we want to know which component set it belongs to so that if the component
        // set has bindings then we can use them for the instance.
        //
        // (For example, we might encounter an instance of a PRNDL component; the component
        // itself has no bindings, but the component set binds to the "vehicle gear" signal
        // and selects a different variant/component based on the gear; in the Figma JSON we
        // will find a component instance, like "Gear=P", and then need to find the component
        // set "PRNDL", and then look at its bindings. If we find the component set and find
        // the bindings then we know we should replace the instance with special node that
        // knows to subscribe to the Gear signal).
        //
        // This map goes from "component id" -> "component set node", so there will be one
        // entry for each component in a component set.
        let mut component_id_index = HashMap::new();
        index_node(
            &self.document_root.document,
            None,
            &mut name_index,
            &mut id_index,
            &mut variant_index,
            &mut component_set_name_index,
            &mut component_id_index,
            skip_hidden,
        );

        // Fetch component variant nodes
        // doc_nodes is a hash that that hashes document_id -> list of node IDs from that
        // document that we need, so that we can retrieve images from the correct document
        let mut node_doc_hash: HashMap<String, String> = HashMap::new();
        let mut variant_nodes: Vec<figma_schema::Node> = vec![];
        let mut parent_tree: Vec<String> = vec![];
        let mut error_hash: HashSet<String> = HashSet::new();
        self.fetch_component_variants(
            &self.document_root.document,
            &mut node_doc_hash,
            &mut variant_nodes,
            &id_index,
            &self.document_root.components,
            &mut parent_tree,
            error_list,
            &mut error_hash,
            skip_hidden,
        )?;
        self.variant_nodes = variant_nodes;

        // Index the variant nodes that we pulled from other documents
        for node in &self.variant_nodes {
            index_node(
                node,
                None, // TODO this is untested -- we may need to get the parent of node and pass it in here
                &mut name_index,
                &mut id_index,
                &mut variant_index,
                &mut component_set_name_index,
                &mut component_id_index,
                skip_hidden,
            );
        }

        // Convert node_names into a HashSet. Then, for any node names that are a component set,
        // add all of its children (which are variants) into the node name hash.
        // Also add all component sets to component_set_hash, mapping the component set name to
        // a list of all their variant children.
        let mut node_name_hash: HashSet<NodeQuery> = HashSet::from_iter(node_names.iter().cloned());
        // Convert node_names into a list of nodes. We'll use this list in find_component_sets to
        // add any component sets whose instances are in the tree of any nodes in query_nodes. This
        // ensures that we have access to other component variants.
        let query_nodes: Vec<&figma_schema::Node> = node_names
            .iter()
            .map(|name| {
                // Look up the node if it's defined.
                let maybe_node = match &name {
                    NodeQuery::NodeId(id) => id_index.get(id),
                    NodeQuery::NodeName(node_name) => name_index.get(node_name),
                    NodeQuery::NodeComponentSet(node_name) => name_index.get(node_name),
                    NodeQuery::NodeVariant(node_name, parent_name) => {
                        variant_index.get(&(node_name.clone(), parent_name.clone()))
                    }
                };
                maybe_node
            })
            .filter(|maybe_node| maybe_node.is_some())
            .map(|node| *node.unwrap()) // safe to unwrap
            .collect();
        find_component_sets(&query_nodes, &component_id_index, &mut node_name_hash);
        let mut component_set_hash: HashMap<String, Vec<String>> = HashMap::new();
        add_variant_node_names(
            &self.document_root.document,
            &mut node_name_hash,
            &mut component_set_hash,
        );

        // Some nodes may have additional plugin data that specifies an overflow node. Look for these
        // and add them to node_name_hash
        add_overflow_nodes(&self.document_root.document, &mut node_name_hash);

        // Find the initial list of nodes we want to convert. This list excludes nodes that are hidden
        let mut nodes: Vec<(NodeQuery, &figma_schema::Node)> = node_name_hash
            .into_iter()
            .map(|name| {
                // Look up the node if it's defined.
                let maybe_node = match &name {
                    NodeQuery::NodeId(id) => id_index.get(id),
                    NodeQuery::NodeName(node_name) => name_index.get(node_name),
                    NodeQuery::NodeComponentSet(node_name) => {
                        component_set_name_index.get(node_name)
                    }
                    NodeQuery::NodeVariant(node_name, parent_name) => {
                        variant_index.get(&(node_name.clone(), parent_name.clone()))
                    }
                };

                // Yield up both the query and the node so that we can return the
                // query with the converted node.
                (name, maybe_node)
            })
            .filter(|(_, maybe_node)| maybe_node.is_some())
            .map(|(k, node)| (k, *node.unwrap())) // safe to unwrap
            .collect();

        // We use ComponentContext to track all of the nodes that need to be converted to
        // satisfy reactions.
        let mut component_context = ComponentContext::new(&nodes);
        let mut views = HashMap::new();

        loop {
            // XXX: Very silly cloning here; why doesn't DocumentKey do this once?

            // Add the ignored images into a hash map for easier access
            let mut ignored_image_hash: HashMap<NodeQuery, HashSet<String>> = HashMap::new();
            for (node_name, node_images) in ignored_images {
                let mut img_set: HashSet<String> = HashSet::new();
                for img in node_images {
                    img_set.insert(img.to_string());
                }
                ignored_image_hash.insert(node_name.clone(), img_set);

                // If node_name is a component set, all its variant children use the same set of ignored images
                if let NodeQuery::NodeName(name) = node_name {
                    let variant_list = component_set_hash.get(name);
                    if let Some(variant_list) = variant_list {
                        for variant_name in variant_list {
                            let mut img_set: HashSet<String> = HashSet::new();
                            for img in node_images {
                                img_set.insert(img.to_string());
                            }
                            ignored_image_hash.insert(NodeQuery::name(variant_name), img_set);
                        }
                    }
                }
            }
            // Now we can transform from the Figma schema to the toolkit schema.
            for (query, node) in nodes {
                // Set the set of images to ignore for this node
                let ignored_image_set = ignored_image_hash.get(&NodeQuery::name(node.name.clone()));
                self.image_context.set_ignored_images(ignored_image_set);

                views.insert(
                    query,
                    create_component_flexbox(
                        node,
                        &self.document_root.components,
                        &self.document_root.component_sets,
                        &mut component_context,
                        &mut self.image_context,
                        skip_hidden,
                    )?,
                );
            }

            // If we have no more nodes then we can break out.
            nodes = component_context.referenced_list(&id_index);

            // We use a hashset inside of ComponentContext to ensure that we don't request the same
            // nodes over and over.
            if nodes.is_empty() {
                break;
            }
        }

        // Populate the override data for components.
        self.compute_component_overrides(&mut views);

        // Update our mapping from instance to component set.
        self.component_sets = component_id_index
            .iter()
            .map(|(component_id, component_set_node)| {
                (component_id.clone(), component_set_node.id.clone())
            })
            .collect();

        // If there are nodes that were referenced that we weren't able to convert then we can report them
        // here. Ideally we'd log this so that a customer could see it and figure out what's going on and
        // why an action isn't working.
        let missing_nodes = component_context.missing_nodes();
        if !missing_nodes.is_empty() {
            println!("Figma: Unable to find nodes referenced by interactions: {:?}", missing_nodes);
            println!("       These interactions won't work.");
        }
        Ok(views)
    }

    // Parse through all the variables collected and store them into hash tables for easy access
    pub fn build_variable_map(&self) -> VariableMap {
        let mut collections_by_id: HashMap<String, Collection> = HashMap::new();
        let mut collection_ids_by_name: HashMap<String, String> = HashMap::new();
        if let Some(variables_response) = self.variables_response.clone() {
            for (_, c) in variables_response.meta.variable_collections.iter() {
                let mut mode_name_hash: HashMap<String, String> = HashMap::new();
                let mut mode_id_hash: HashMap<String, Mode> = HashMap::new();
                for m in &c.modes {
                    let mode =
                        Mode { id: m.mode_id.clone(), name: m.name.clone(), ..Default::default() };
                    mode_name_hash.insert(mode.name.clone(), mode.id.clone());
                    mode_id_hash.insert(mode.id.clone(), mode);
                }
                let collection = Collection {
                    id: c.id.clone(),
                    name: c.name.clone(),
                    default_mode_id: c.default_mode_id.clone(),
                    mode_name_hash,
                    mode_id_hash,
                    ..Default::default()
                };
                collections_by_id.insert(collection.id.clone(), collection);
                collection_ids_by_name.insert(c.name.clone(), c.id.clone());
            }
        }

        let mut variables_by_id: HashMap<String, Variable> = HashMap::new();
        let mut variable_name_id_maps_by_cid: HashMap<String, NameIdMap> = HashMap::new();
        if let Some(variables_response) = self.variables_response.clone() {
            for (id, v) in variables_response.meta.variables.iter() {
                let var = create_variable(v);
                let maybe_name_map =
                    variable_name_id_maps_by_cid.get_mut(&var.variable_collection_id);
                if let Some(name_map) = maybe_name_map {
                    name_map.m.insert(var.name.clone(), id.clone());
                } else {
                    let mut name_to_id = HashMap::new();
                    name_to_id.insert(var.name.clone(), id.clone());
                    variable_name_id_maps_by_cid.insert(
                        var.variable_collection_id.clone(),
                        NameIdMap { m: name_to_id, ..Default::default() },
                    );
                }

                variables_by_id.insert(id.clone(), var);
            }
        }

        let var_map = VariableMap {
            collections_by_id,
            collection_ids_by_name,
            variables_by_id,
            variable_name_id_maps_by_cid,
            ..Default::default()
        };

        var_map
    }

    /// Return the EncodedImageMap containing the mapping from ImageKey references in Nodes returned by this document
    /// to encoded image bytes. EncodedImageMap can be serialized and deserialized, and transformed into an ImageMap.
    pub fn encoded_image_map(&self) -> EncodedImageMap {
        self.image_context.encoded_image_map()
    }

    /// Return the ImageContextSession so that subsequent requests can avoid fetching the same images. We also return
    /// the list of all images referenced by this document, so that a client knows which images to copy from the old
    /// document (if it requested an incremental update).
    pub fn image_session(&self) -> ImageContextSession {
        self.image_context.as_session()
    }

    /// Return the mapping from Component node ID to Component Set node ID.
    pub fn component_sets(&self) -> &HashMap<String, String> {
        &self.component_sets
    }

    /// Get all the Figma documents in the given project ID
    pub fn get_projects(&self, project_id: &str) -> Result<Vec<FigmaDocInfo>, Error> {
        let url = format!("{}{}/files", BASE_PROJECT_URL, project_id);
        let project_files: figma_schema::ProjectFilesResponse = serde_json::from_str(
            http_fetch(self.api_key.as_str(), url, &self.proxy_config)?.as_str(),
        )?;

        let mut figma_docs: Vec<FigmaDocInfo> = vec![];
        for file_hash in &project_files.files {
            if let Some(doc_id) = file_hash.get("key") {
                if let Some(name) = file_hash.get("name") {
                    let figma_doc = FigmaDocInfo {
                        name: name.clone(),
                        id: doc_id.clone(),
                        ..Default::default()
                    };
                    // Getting project files return head version of the files.
                    figma_docs.push(figma_doc);
                }
            }
        }

        Ok(figma_docs)
    }

    /// Get the name of the document
    pub fn get_name(&self) -> String {
        self.document_root.name.clone()
    }

    /// Get the document ID of the document
    pub fn get_document_id(&self) -> String {
        self.document_id.clone()
    }

    /// Get the version string in the document's file response
    pub fn get_version(&self) -> String {
        self.document_root.version.clone()
    }

    pub fn cache(&self) -> HashMap<String, String> {
        self.image_context.cache()
    }
}
