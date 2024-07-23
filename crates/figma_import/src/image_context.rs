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

use std::{
    collections::{HashMap, HashSet},
    io::{Cursor, Read},
    sync::Arc,
    time::Duration,
};

use crate::error::Error;
use crate::fetch::ProxyConfig;
use crate::figma_schema::{Paint, Transform};
use dc_bundle::legacy_definition::element::background::ImageKey;
use image::DynamicImage;
use serde::{Deserialize, Serialize};
use std::hash::Hash;

#[derive(PartialEq, Clone, Debug, Serialize, Deserialize)]
pub struct VectorImageId {
    stroke_hash: u64,
    fill_hash: u64,
    transforms: Vec<Option<Transform>>,
    paints: Vec<Paint>,
}

fn http_fetch_image(
    url: impl ToString,
    proxy_config: &ProxyConfig,
) -> Result<(DynamicImage, Vec<u8>), Error> {
    let url = url.to_string();

    let mut agent_builder = ureq::AgentBuilder::new();
    // Only HttpProxyConfig is supported.
    if let ProxyConfig::HttpProxyConfig(spec) = proxy_config {
        agent_builder = agent_builder.proxy(ureq::Proxy::new(spec)?);
    }
    let body = agent_builder.build().get(url.as_str()).timeout(Duration::from_secs(90)).call()?;

    let mut response_bytes: Vec<u8> = Vec::new();
    body.into_reader().read_to_end(&mut response_bytes)?;

    let img = image::io::Reader::new(Cursor::new(response_bytes.as_slice()))
        .with_guessed_format()?
        .decode()?;

    Ok((img, response_bytes))
}

fn lookup_or_fetch(
    client_images: &HashSet<String>,
    client_used_images: &mut HashSet<String>,
    referenced_images: &mut HashSet<String>,
    decoded_image_sizes: &mut HashMap<String, (u32, u32)>,
    network_bytes: &mut HashMap<String, Arc<serde_bytes::ByteBuf>>,
    url: Option<&Option<String>>,
    proxy_config: &ProxyConfig,
) -> bool {
    if let Some(Some(url)) = url {
        referenced_images.insert(url.clone());

        // If client_images already has this url, add it to client_used_images so that we know
        // that this updated document also uses the same image
        if client_images.contains(url) {
            client_used_images.insert(url.clone());
            return true;
        }
        if network_bytes.contains_key(url) {
            return true;
        } else {
            match http_fetch_image(url, proxy_config) {
                Ok((dynamic_image, fetched_bytes)) => {
                    decoded_image_sizes
                        .insert(url.clone(), (dynamic_image.width(), dynamic_image.height()));
                    network_bytes
                        .insert(url.clone(), Arc::new(serde_bytes::ByteBuf::from(fetched_bytes)));
                    return true;
                }
                Err(e) => {
                    println!("Unable to fetch Figma Image URL {}: {:#?}", url, e);
                }
            }
        }
    }
    false
}

/// EncodedImageMap contains a mapping from ImageKey to network bytes. It can create an
/// ImageMap and is intended to be used when we want to use Figma-defined components but do
/// not want to communicate with the Figma service.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct EncodedImageMap(HashMap<ImageKey, Arc<serde_bytes::ByteBuf>>);
impl EncodedImageMap {
    pub fn map(&self) -> HashMap<ImageKey, Arc<serde_bytes::ByteBuf>> {
        self.0.clone()
    }
}

/// ImageContext fetches images from Figma when requested, caches them (currently infinitely)
/// and also handles fetching image versions of vector content that we don't yet support.
///
/// ImageContext is used when we're talking to the Figma service. It can create an
/// EncodedImageMap which contains an ImageKey -> Network Bytes mapping.
pub struct ImageContext {
    // imageRef -> URL?
    images: HashMap<String, Option<String>>,
    // node ID
    vectors: HashSet<String>,
    // node ID -> URL
    node_urls: HashMap<String, Option<String>>,
    // URL -> Network Bytes
    network_bytes: HashMap<String, Arc<serde_bytes::ByteBuf>>,
    // URL -> (width, height)
    decoded_image_sizes: HashMap<String, (u32, u32)>,
    // Image node names to not download
    ignored_images: HashSet<String>,
    // URL -> Vector Hash
    image_hash: HashMap<String, VectorImageId>,
    // Images that a remote client has, which we will not bother to fetch again. This is
    // only populated when we're running the web server configuration.
    client_images: HashSet<String>,
    // Images that a remote client has that we are in fact reusing. We keep this data in
    // a separate hash to inform the client which images are still used, so that it can
    // purge unused ones.
    client_used_images: HashSet<String>,
    // Images that have been referenced since this ImageContext was created. We track these
    // so that a remote client knows which images from a previous run to keep.
    referenced_images: HashSet<String>,
    // Proxy configuration
    proxy_config: ProxyConfig,
}
impl ImageContext {
    /// Create a new ImageContext that knows about the given images and vector ID to URL mappings
    /// and that uses the api_key to fetch the image bytes.
    ///
    /// * `images`: the mapping from Figma's `imageRef` to image URL.
    pub fn new(
        images: HashMap<String, Option<String>>,
        proxy_config: &ProxyConfig,
    ) -> ImageContext {
        ImageContext {
            images,
            vectors: HashSet::new(),
            node_urls: HashMap::new(),
            network_bytes: HashMap::new(),
            decoded_image_sizes: HashMap::new(),
            ignored_images: HashSet::new(),
            image_hash: HashMap::new(),
            client_images: HashSet::new(),
            client_used_images: HashSet::new(),
            referenced_images: HashSet::new(),
            proxy_config: proxy_config.clone(),
        }
    }

    /// Fetch and decode the image associated with the given Figma imageRef.
    ///
    /// If this image has already been fetched and decoded then it is returned from cache
    /// and not fetched again.
    ///
    /// * `image_ref`: the Figma imageRef to fetch.
    pub fn image_fill(&mut self, image_ref: impl ToString, node_name: &String) -> Option<ImageKey> {
        if self.ignored_images.contains(node_name) {
            None
        } else {
            let url = self.images.get(&image_ref.to_string());
            if lookup_or_fetch(
                &self.client_images,
                &mut self.client_used_images,
                &mut self.referenced_images,
                &mut self.decoded_image_sizes,
                &mut self.network_bytes,
                url,
                &self.proxy_config,
            ) {
                url.unwrap_or(&None).as_ref().map(|url_string| ImageKey::new(url_string.clone()))
            } else {
                None
            }
        }
    }

    //Return a copy of the current vector map
    // TODO: we shouldn't have HashMap values which are Option<>,
    // The correct approach would be just just not have entries for those keys.
    pub fn cache(&self) -> HashMap<String, Option<ImageKey>> {
        let mut map = HashMap::new();
        let url_map = self.node_urls.clone();

        for (node, addr) in url_map {
            if let Some(url) = addr {
                map.insert(node, Some(ImageKey::new(url.clone())));
            }
        }

        map.clone()
    }
    /// Update the mapping of Figma imageRefs to URLs
    pub fn update_images(&mut self, images: HashMap<String, Option<String>>) {
        self.images = images;
    }

    /// Create a EncodedImageMap.
    pub fn encoded_image_map(&self) -> EncodedImageMap {
        let mut image_bytes = HashMap::new();
        for (k, v) in &self.network_bytes {
            image_bytes.insert(ImageKey::new(k.clone()), v.clone());
        }
        // Add empty entries for any referenced images which we don't have network bytes for.
        for k in &self.referenced_images {
            let key = ImageKey::new(k.clone());
            image_bytes.entry(key).or_insert_with(|| Arc::new(serde_bytes::ByteBuf::new()));
        }
        EncodedImageMap(image_bytes)
    }

    pub fn set_ignored_images(&mut self, images: Option<&HashSet<String>>) {
        if let Some(images) = images {
            self.ignored_images = images.clone();
        } else {
            self.ignored_images.clear();
        }
    }
}

// We can serialize an ImageContext and bring it back without any of the image
// content. This is used so that remote clients using the web service can remember
// the relevant image context and save the server a lot of extra fetches from
// Figma's API to get images that the client already has. It also saves network
// bytes sending the same image bytes over and over to the client.
//
// So this structure is the serialized ImageContext with no images, and can be
// used to resurrect an ImageContext with the appropriate state.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct ImageContextSession {
    // imageRef -> URL?
    images: HashMap<String, Option<String>>,
    // node ID
    vectors: HashSet<String>,
    // URL -> Vector Hash
    image_hash: HashMap<String, VectorImageId>,
    // Decoded image bounds.
    #[serde(default)]
    image_bounds: HashMap<String, (u32, u32)>,
    // Images that a remote client has, which we will not bother to fetch again. This is
    // only populated when we're running the web server configuration.
    client_images: HashSet<String>,
}

impl ImageContext {
    pub fn as_session(&self) -> ImageContextSession {
        // Don't put data into ImageContextSession that we didn't use. Fill in client_images with
        // images we retrieved from the current session as well as images from the previous session
        // that we used again, which are in self.client_used_images. Then fill out the rest of
        // ImageContextSession with only data that is in client_images.
        let mut client_images = self.client_used_images.clone();
        for (k, _) in &self.network_bytes {
            client_images.insert(k.clone());
        }
        let mut image_bounds = self.decoded_image_sizes.clone();
        for (k, &(width, height)) in &self.decoded_image_sizes {
            if client_images.contains(k) {
                image_bounds.insert(k.clone(), (width, height));
            }
        }

        ImageContextSession {
            images: self
                .images
                .clone()
                .into_iter()
                .filter(|(k, _)| client_images.contains(k))
                .collect(),
            vectors: self
                .vectors
                .clone()
                .into_iter()
                .filter(|k| client_images.contains(k))
                .collect(),
            image_hash: self
                .image_hash
                .clone()
                .into_iter()
                .filter(|(k, _)| client_images.contains(k))
                .collect(),
            image_bounds,
            client_images,
        }
    }

    pub fn add_session_info(&mut self, session: ImageContextSession) {
        for (k, v) in session.images {
            self.images.insert(k, v);
        }
        for (k, v) in session.image_bounds {
            self.decoded_image_sizes.insert(k, v);
        }
        for k in session.vectors {
            self.vectors.insert(k);
        }
        for (k, v) in session.image_hash {
            self.image_hash.insert(k, v);
        }
        for k in session.client_images {
            self.client_images.insert(k);
        }
    }
}
