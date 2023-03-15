/**
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


// Warning component.
interface ClippyWarningRun {
  // The text for the warning text run.
  text: string,
  // Does this text run relate to a particular node?
  node: string
}

// Warning type
enum ClippyWarningKind {
  // A top-level keyword (e.g.: "#stage") is used more than once in the
  // document. High severity.
  DUPLICATE_TOP_LEVEL = "duplicate_top_level",
  // A top-level keyword (e.g.: "#stage") is missing in the document.
  // High severity.
  MISSING_TOP_LEVEL = "missing_top_level",
  // Something starts with a "#" but doesn't identify any keyword we know about. Low
  // severity.
  UNKNOWN_KEYWORD = "unknown_keyword",
  // A customization keyword is available inside of a component (e.g.: "#subtitle") but
  // was unused. Low severity.
  UNUSED_KEYWORD = "unused_keyword",
  // A customization keyword was applied to a node of the wrong type (e.g.: "#subtitle"
  // is not a text node). High severity.
  TYPE_MISMATCH = "type_mismatch",
  // A keyword for a variant property has been applied elsewhere. High severity.
  MISAPPLIED_KEYWORD = "misapplied_keyword",
  // An interaction isn't supported (e.g.: mouse hover). High severity.
  UNSUPPORTED_INTERACTION = "unsupported_interaction",
  // Conflicting interactions are tightly nested (e.g.: nested tap interactions of similar
  // bounds). High severity.
  TIGHTLY_NESTED_INTERACTION = "tightly_nested_interaction",
  // A customization of type VariantProperty does not have a matching node of type
  // COMPONENT_SET. High severity.
  MISSING_COMPONENT_SET = "missing_component_set",
}

// Warning severity
enum ClippyWarningSeverity {
  LOW = 1,
  HIGH = 2,
}

const SHARED_PLUGIN_NAMESPACE = "designcompose"

function clippyWarningSeverityFromKind(kind: ClippyWarningKind): ClippyWarningSeverity {
  switch (kind) {
    case ClippyWarningKind.DUPLICATE_TOP_LEVEL:
      return ClippyWarningSeverity.HIGH;
    case ClippyWarningKind.MISSING_TOP_LEVEL:
      return ClippyWarningSeverity.LOW;
    case ClippyWarningKind.UNKNOWN_KEYWORD:
      return ClippyWarningSeverity.LOW;
    case ClippyWarningKind.UNUSED_KEYWORD:
      return ClippyWarningSeverity.LOW;
    case ClippyWarningKind.TYPE_MISMATCH:
      return ClippyWarningSeverity.HIGH;
    case ClippyWarningKind.MISAPPLIED_KEYWORD:
      return ClippyWarningSeverity.HIGH;
    case ClippyWarningKind.UNSUPPORTED_INTERACTION:
      return ClippyWarningSeverity.HIGH;
    case ClippyWarningKind.TIGHTLY_NESTED_INTERACTION:
      return ClippyWarningSeverity.HIGH;
    case ClippyWarningKind.MISSING_COMPONENT_SET:
      return ClippyWarningSeverity.HIGH;
  }
}

// Warning to be presented in the UI.
interface ClippyWarning {
  kind: ClippyWarningKind,
  severity: ClippyWarningSeverity,
  runs: (string|ClippyWarningRun)[]
}

function createWarning(kind: ClippyWarningKind, runs: (string|ClippyWarningRun)[]): ClippyWarning {
  let severity = clippyWarningSeverityFromKind(kind);
  return { kind, severity, runs }
}

enum DesignCustomizationKind {
  Text = "text",
  TextStyle = "text_style",
  Image = "image",
  ImageWithContext = "image_with_context",
  Visibility = "visibility",
  VariantProperty = "variant_property",
  ComponentReplacement = "component_replacement",
  ContentReplacement = "content_replacement",
  Modifier = "modifier",
  // Not covering "placeholder"; we should remove it.
}

interface DesignCustomization {
  kind: DesignCustomizationKind,
  name: string,
  node: string,
}

interface DesignCustomizationVariantProperty extends DesignCustomization {
  kind: DesignCustomizationKind.VariantProperty,
  values: string[]
}

interface DesignCustomizationContentReplacement extends DesignCustomization {
  kind: DesignCustomizationKind.ContentReplacement,
  content: string[] // XXX: this won't work for children that have variants.
}

interface DesignComponentSpec {
  name: string,
  node: string,
  isRoot?: boolean, // is this the root component? We must know this since we pass customizations down to children.
  customizations: DesignCustomization[]
}

interface DesignDocSpec {
  name: string,
  version: string,
  components: DesignComponentSpec[]
}

// 1. Warn for unused keywords in a top level frame
function clippyCheckUnusedKeywords(
  topLevelComponents: Map<string, BaseNode[]>,
  topLevelComponentDefns: Map<string, DesignComponentSpec>,
  warnings: ClippyWarning[]) {
  // Given a node and a list of keywords (node name customizations), check that the keywords
  // are somewhere in the node's tree hierarchy. Keep a count of nodes that match a keyword
  // so that we can verify missing or extra nodes.
  function findKeywordsRecurse(node: BaseNode, keywords: Set<String>, found: Map<String, number>) {
    if (!node) return;
    if (keywords.has(node.name)) {
      if (found.has(node.name)) {
        found.set(node.name, found.get(node.name) + 1);
      } else {
        found.set(node.name, 1);
      }
    }
    if ((node as any).children) {
      let parent: ChildrenMixin = node as ChildrenMixin;
      for (const child of parent.children) {
        findKeywordsRecurse(child, keywords, found);
      }
    }
  }
  
  // 1. Warn for unused keywords in a top level frame
  for (const nodeName of topLevelComponentDefns.keys()) {
    let component = topLevelComponentDefns.get(nodeName);
    // Skip root frames because customizations within a root frame are often passed down to
    // customized children, so they don't show up in the root node of Figma.
    if (component.isRoot)
      continue;
    let keywords: Set<string> = new Set();
    component.customizations.forEach( c => {
      if (DesignCustomizationKind[c.kind] != DesignCustomizationKind.VariantProperty)
        keywords.add(c.node);
    });
    if (topLevelComponents.has(nodeName)) {
      let components = topLevelComponents.get(nodeName);
      let componentSet = components.find(node => node.type == "COMPONENT_SET");
      components.forEach(node => {
        // If there are multiple components with the same name and at least one is a COMPONENT_SET,
        // then only check the COMPONENT_SET nodes
        if (componentSet == null || node.type == "COMPONENT_SET") {
          let foundKeywords: Map<String, number> = new Map();
          findKeywordsRecurse(node, keywords, foundKeywords);
          let runs: (string|ClippyWarningRun)[] = [];
          keywords.forEach(keyword => {
            if (!foundKeywords.has(keyword)) {
              runs.push(keyword);
            }
          });
          if (runs.length > 0) {
            warnings.push(createWarning(
              ClippyWarningKind.UNUSED_KEYWORD,
              [
                "Missing keywords from ",
                { text: `${node.name}:`, node: node.id } as ClippyWarningRun,
                ...runs
              ]
            ));
          }
        }
      });
    }
  }
}

// 2. Warn for missing top level keywords
// 3. Warn for duplicated top level keywords
function clippyCheckKeywords(
  topLevelComponents: Map<string, BaseNode[]>,
  topLevelComponentDefns: Map<string, DesignComponentSpec>,
  warnings: ClippyWarning[]) {

  // See if there are any missing or duplicate top-level component names.
  for (const topLevelComponentName of topLevelComponentDefns.keys()) {
    if (topLevelComponents.has(topLevelComponentName)) {
      let components = topLevelComponents.get(topLevelComponentName);
      if (components.length > 1) {
        // 3. Warn for duplicated top level keywords
        // If there's one node that's a COMPONENT or COMPONENT_SET, and everything else is
        // a FRAME or INSTANCE then that's fine because the service scores the components higher.
        let componentCount = 0;
        components.forEach(node => {
          if (node.type == "COMPONENT_SET" || node.type == "COMPONENT")
           componentCount++
        });
        if (componentCount == 0 || componentCount > 1) {
          warnings.push(createWarning(
            ClippyWarningKind.DUPLICATE_TOP_LEVEL,
            [
              `Multiple nodes named ${topLevelComponentName}, expected just one:`,
              ...components.filter(figmaNode => figmaNode.type == "COMPONENT_SET" || figmaNode.type == "COMPONENT")
              .map(figmaNode => { 
                return { text: topLevelComponentName + " (" + figmaNode.type + ")", node: figmaNode.id } as ClippyWarningRun;
              })
            ]
          ));
        }
      }
    } else {
      // 2. Warn for missing top level keywords
      warnings.push(createWarning(
        ClippyWarningKind.MISSING_TOP_LEVEL,
        [
          `No nodes found for ${topLevelComponentName}, but one was expected.`
        ]
      ));
    }
  }
}

// 4. Warn if a customization with variants does not have a matching node of type COMPONENT_SET
function clippyCheckVariants(
  topLevelComponentDefns: Map<string, DesignComponentSpec>,
  warnings: ClippyWarning[]) {
  // Recurse through the document and store all COMPONENT_SET nodes into a map.
  function findComponentSets(node: BaseNode, componentSets: Map<string, ComponentSetNode>) {
    if (!node) return;
    if (node.type == "COMPONENT_SET") {
      componentSets.set(node.name, node as ComponentSetNode);
    } else {
      if ((node as any).children) {
        let parent: ChildrenMixin = node as ChildrenMixin;
        for (const child of parent.children) {
          findComponentSets(child, componentSets);
        }
      }
    }
  }
  let componentSets: Map<string, ComponentSetNode> = new Map();
  findComponentSets(figma.root, componentSets);

  // Go through our design nodes and look for customizations of type VariantProperty
  for (const nodeName of topLevelComponentDefns.keys()) {
    let component = topLevelComponentDefns.get(nodeName);
    // Skip root frames because customizations within a root frame are often passed down to
    // customized children, so they don't show up in the root node of Figma.
    if (component.isRoot)
      continue;

    // `variants` maps a property name to variant names within a node
    let variants: Map<string, Set<string>> = new Map();
    component.customizations.forEach( c => {
      if (DesignCustomizationKind[c.kind] == DesignCustomizationKind.VariantProperty) {
        let customizationVariant = c as DesignCustomizationVariantProperty
        if (!variants.has(c.node)) {
          variants.set(c.node, new Set());
        }
        let variantNames = variants.get(c.node);
        for (const variantName of customizationVariant.values)
          variantNames.add(variantName);
      }
    });

    // If this component has variants, check componentSets to ensure we have at least one
    // COMPONENT_SET with matching variant properties and names
    if (variants.size > 0) {
      variants.forEach((values: Set<string>, property: string) => {
        let propertyValidated = false;
        componentSets.forEach((componentSet: ComponentSetNode, name: string) => {
          try {
            let groupProperties = componentSet.variantGroupProperties
            let propertyNames = new Set(Object.keys(groupProperties));

            if (propertyNames.has(property)) {
              let propertyValues = new Set(groupProperties[property].values);
              let valuesFound = true;
              values.forEach( value => {
                if (!propertyValues.has(value))
                  valuesFound = false
              });
              if (valuesFound)
                propertyValidated = true;
            }
          } catch(e) {
            warnings.push(createWarning(
              ClippyWarningKind.MISSING_COMPONENT_SET,
              [
                { text: name, node: componentSet.id },
                " has an exception. ",
                `${e}`
              ]
            ));            
          }
          return !propertyValidated;
        });
        // Add a warning if this property was not validated
        if (!propertyValidated)
          warnings.push(createWarning(
            ClippyWarningKind.MISSING_COMPONENT_SET,
            [
              `${property} is a variant property with possible values [${Array.from(values.values()).join(",")}], but could not be found`
            ]
          ));
      });
    }
  }
}

// 5. Warn for node type mismatches (ClippyWarningKind.TYPE_MISMATCH):
function clipyCheckTypeMismatches(
  topLevelComponentDefns: Map<string, DesignComponentSpec>,
  node: BaseNode,
  warnings: ClippyWarning[],
  insideComponentDefn?: DesignComponentSpec) {
  if (!node) return;
  if (topLevelComponentDefns.has(node.name)) {
    insideComponentDefn = topLevelComponentDefns.get(node.name);
  } else if (insideComponentDefn) {
    // Check to see if our name is one of the customizations, and if so, validate
    // that we have the correct type.
    for (const customization of insideComponentDefn.customizations) {
      // We should also look for nodes that start with "#" but don't match a customization
      // or a top-level component name.
      if (node.name != customization.node) continue;

      // 5. Warn for node type mismatches:
      //   - If text customization is not on a text node
      //   - If content replacement customization is not on a frame node
      //   - If image customization is not on a frame node
      var kind = DesignCustomizationKind[customization.kind];
      switch (kind) {
        case DesignCustomizationKind.Text:
        case DesignCustomizationKind.TextStyle:
          if (node.type != "TEXT") {
            warnings.push(createWarning(
              ClippyWarningKind.TYPE_MISMATCH,
              [
                `${customization.node} is a text customization, but`,
                { text: node.name, node: node.id },
                `is a ${node.type} instead of a TEXT node.`
              ]
            ));
          }
          break;
        case DesignCustomizationKind.ContentReplacement:
          if (node.type != "FRAME") {
            warnings.push(createWarning(
              ClippyWarningKind.TYPE_MISMATCH,
              [
                `${customization.node} is a customization that adds children to a FRAME, but`,
                { text: node.name, node: node.id },
                `is a ${node.type}.`
              ]
            ));
          }
          break;
        case DesignCustomizationKind.Image:
        case DesignCustomizationKind.ImageWithContext:
          // Not sure what the rules are for image. Frames and rectangles seem to work
          if (node.type != "FRAME" && node.type != "RECTANGLE") {
            warnings.push(createWarning(
              ClippyWarningKind.TYPE_MISMATCH,
              [
                `${customization.node} is a customization that changes the background fill to a FRAME, but`,
                { text: node.name, node: node.id },
                `is a ${node.type}.`
              ]
            ));
          }
          break;
        case DesignCustomizationKind.Visibility:
        case DesignCustomizationKind.VariantProperty:
        case DesignCustomizationKind.ComponentReplacement:
        case DesignCustomizationKind.Modifier:
          break;
        default:
          console.log("Unknown customization type " + customization.kind);
      }
    }
  }
  // Now examine the children of this node. If this was a top level node then provide
  // context on the customizations possible, so that those can be validated.
  if ((node as any).children) {
    let parent: ChildrenMixin = node as ChildrenMixin;
    for (const child of parent.children) {
      clipyCheckTypeMismatches(topLevelComponentDefns, child, warnings, insideComponentDefn);
    }
  }
}

function showNode(nodeId: string) {
  // Listen for node-highlight messages; maybe we can have a "refresh button" to run
  // clippy again in the future, too?
  var highlightNode = figma.getNodeById(nodeId);
  if (highlightNode) {
    figma.viewport.scrollAndZoomIntoView([highlightNode]);
    figma.currentPage.selection = [highlightNode as any]; // XXX support multiple pages!
  } else {
    console.log(`Error: Can't find node ${nodeId}`);
  }
}
  
// Parse the document look for various types of warnings
function clippy(json: DesignDocSpec): ClippyWarning[] {
  // Generated warnings
  let warnings: ClippyWarning[] = [];

  // Recurse over all nodes in the doc, from the root down. We populate this
  // `topLevelComponents` map to identify duplicates and identify missing
  // top level components (a top-level component can appear anywhere in a doc;
  // it is "top-level" because of how it is used in the code).
  let topLevelComponents: Map<string, BaseNode[]> = new Map();
  let topLevelComponentDefns: Map<string, DesignComponentSpec> = new Map();
  for (const component of json.components) {
    topLevelComponentDefns.set(component.node, component);
  }

  function populateComponentMaps(node: BaseNode) {
    if (!node) return;
    if (topLevelComponentDefns.has(node.name)) {
      // Store this ref in our list of topLevelComponents.
      if (topLevelComponents.has(node.name)) {
        topLevelComponents.get(node.name).push(node);
      } else {
        topLevelComponents.set(node.name, [node]);
      }
    }

    if ((node as any).children) {
      let parent: ChildrenMixin = node as ChildrenMixin;
      for (const child of parent.children) {
        populateComponentMaps(child);
      }
    }
  }
  populateComponentMaps(figma.root);

  // 1. Warn for unused keywords in a top level frame (ClippyWarningKind.UNUSED_KEYWORD)
  clippyCheckUnusedKeywords(topLevelComponents, topLevelComponentDefns, warnings);

  // 2. Warn for missing top level keywords (ClippyWarningKind.MISSING_TOP_LEVEL)
  // 3. Warn for duplicated top level keywords (ClippyWarningKind.DUPLICATE_TOP_LEVEL)
  clippyCheckKeywords(topLevelComponents, topLevelComponentDefns, warnings);

  // 4. Warn if a customization with variants does not have a matching node of type COMPONENT_SET (ClippyWarningKind.MISSING_COMPONENT_SET)
  clippyCheckVariants(topLevelComponentDefns, warnings);

  // 5. Warn for node type mismatches (ClippyWarningKind.TYPE_MISMATCH):
  //   - If text customization is not on a text node
  //   - If content replacement customization is not on a frame node
  //   - If image customization is not on a frame node
  clipyCheckTypeMismatches(topLevelComponentDefns, figma.root, warnings);

  return warnings;
}

function clippyRefresh() {
  // Get the json plugin data from our root node
  let clippyFile = figma.root.getSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'clippy-json-file');
  let clippyData = figma.root.getSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'clippy-json');
  var reviver = function(key, value) {
    return value;
  };

  var errors = null;
  try {
    // Parse the string into a JSON tree
    let json = JSON.parse(clippyData, reviver);

    // Check for errors
    errors = clippy(json);
  } catch(e) {
    console.log("Could not run clippy: " + e);
  }

  figma.ui.postMessage({ msg: 'clippy', errors, clippyFile });
}

function loadClippy(): DesignDocSpec {
  // Get the json plugin data from our root node
  let clippyData = figma.root.getSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'clippy-json');
  if (!clippyData)
    return null;

  var reviver = function(key, value) {
    return value;
  };

  try {
    return JSON.parse(clippyData, reviver);
  } catch(e) {
    console.log("Error parsing clippy JSON: " + e);
  }
}

// If we were invoked with the "sync" command then run our sync logic and quit.
if (figma.command === "sync") {
  // Copy the reactions data to our plugin data, or clear out our plugin data if there
  // are no reactions.
  function syncReactions(node: SceneNode) {
    // Look up the reaction data from the node.
    let reactionData = null;
    let maybeReactions = node as ReactionMixin;
    if (maybeReactions.reactions) {
      reactionData = maybeReactions.reactions;
    }

    // Set the data into the plugin data, or clear it.
    node.setSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'vsw-reactions', reactionData == null ? "" : JSON.stringify(reactionData));

    // Pull out other prototyping properties that aren't available in the REST API.
    if (node.type == "FRAME" || node.type == "COMPONENT" || node.type == "INSTANCE") {
      node.setSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'vsw-frame-extras', JSON.stringify({
        numberOfFixedChildren: node.numberOfFixedChildren,
        overlayPositionType: node.overlayPositionType,
        overlayBackground: node.overlayBackground,
        overlayBackgroundInteraction: node.overlayBackgroundInteraction,
        overflowDirection: node.overflowDirection,
        layoutMode: node.layoutMode,
      }));
    }

    // Recurse into any children.
    let maybeParent = node as ChildrenMixin;
    if (maybeParent.children) {
      for (let child of maybeParent.children) {
        syncReactions(child);
      }
    }
  }

  // We want to visit every node; the document and page nodes can't have any reaction
  // data.
  for (let page of figma.root.children) {
    for (let child of page.children) {
      syncReactions(child);
    }
  }

  // Close our plugin with a success message.
  figma.closePlugin("Synchronized Interaction Data with AAOS UX");
}
else if (figma.command === "move-plugin-data") {
  function movePluginDataWithKey(node: BaseNode, key: string) {
    // Read the private plugin data, write to shared
    let data = node.getPluginData(key);
    node.setSharedPluginData(SHARED_PLUGIN_NAMESPACE, key, data);
  }
  // If we were invoked with the "move plugin data" command then move all the plugin data
  // from the private location to the shared location, then quit.
  function movePluginData(node: SceneNode) {
    movePluginDataWithKey(node, 'vsw-reactions');
    movePluginDataWithKey(node, 'vsw-extended-text-layout');
    movePluginDataWithKey(node, 'vsw-frame-extras');

    // Recurse into any children.
    let maybeParent = node as ChildrenMixin;
    if (maybeParent.children) {
      for (let child of maybeParent.children) {
        movePluginData(child);
      }
    }
  }

  // Move the root level data first
  movePluginDataWithKey(figma.root, 'clippy-json-file');
  movePluginDataWithKey(figma.root, 'clippy-json');

  // We want to visit every node; the document and page nodes can't have any reaction
  // data.
  for (let page of figma.root.children) {
    for (let child of page.children) {
      movePluginData(child);
    }
  }

  // Close our plugin with a success message.
  figma.closePlugin("Moved plugin data to shared location");
} else if (figma.command == "extended-layout-text") {
  // Edit layout properties that Figma doesn't yet provide (but that we need for applications).
  let pluginMode = "TEXT";
  // Show the plugin UI, and then send a message regarding the extended properties
  // of the currently selected node, and its type.
  figma.showUI(__html__, { width: 360, height: 480 });

  // Recursively parse the Figma document looking for nodes that match nodes in contentNodes.
  // If any of the nodes added are COMPONENT_SET nodes, find and add their variant children instead.
  // If any of the nodes are COMPONENT or FRAME nodes, just fill in their node IDs
  function findContentNodes(node: BaseNode, contentNodes: Map<string, string>) {
    if (!node) return;
    if (contentNodes.has(node.name)) {
      if (node.type == 'COMPONENT_SET') {
        // Delete the COMPONENT_SET in contentNodes and replace with all the variant children
        contentNodes.delete(node.name);
        if ((node as any).children) {
          let parent: ChildrenMixin = node as ChildrenMixin;
          for (const child of parent.children) {
            if (child.type == "COMPONENT")
              contentNodes.set(child.name, child.id);
          }
        }
        return;
      }
      else if (node.type == "FRAME" || node.type == "COMPONENT") {
        // Update contentNodes with the node ID if there isn't one already set
        let nodeId = contentNodes.get(node.name);
        if (nodeId.length == 0)
          contentNodes.set(node.name, node.id);
      }
    }

    if ((node as any).children) {
      let parent: ChildrenMixin = node as ChildrenMixin;
      for (const child of parent.children) {
        findContentNodes(child, contentNodes);
      }
    }
  }

  // Returns a map of replacement content nodes with the content types they hold formatted as:
  // <container frame node name> -> { content node name -> content node id }
  function loadNodeContentData(): Map<string, Map<string,string>> {
    let clippyData = loadClippy();
    if (!clippyData) {
      return null;
    }

    // Parse the JSON to find content replacement nodes that have content data.
    let contentData: Map<string, Map<string, string>> = new Map();
    for (const component of clippyData.components) {
      for (const c of component.customizations) {
        let contentNode = c as DesignCustomizationContentReplacement;
        if (contentNode != null && contentNode.content != null) {
          let contentNodes: Map<string, string> = new Map();
          for (const node of contentNode.content) {
            contentNodes.set(node, "");
          }
          // Look through the Figma document and fill in the node IDs
          findContentNodes(figma.root, contentNodes);
          contentData.set(c.node, contentNodes);
        }
      }
    }

    return contentData;
  }

  // Load accompanying JSON file to get content data about the nodes 
  let nodeContentData = loadNodeContentData();

  function onSelectionChange() {
    let selection = figma.currentPage.selection;

    // We don't support multiple selections.
    if (!selection || selection.length != 1 || !selection[0]) {
      figma.ui.postMessage({ msg: 'selection-cleared', pluginMode });
      return;
    }
    let node = selection[0];
    let extendedLayoutData = node.type == 'TEXT' ? node.getSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'vsw-extended-text-layout') : null;
    let extendedLayout = (extendedLayoutData && extendedLayoutData.length) ? JSON.parse(extendedLayoutData) : {};
    let layoutMode = 'NONE';
    let overflowDirection = 'NONE';
    let content = [];
    if (nodeContentData && nodeContentData.has(node.name)) {
      for (const [nodeName, nodeId] of nodeContentData.get(node.name).entries()) {
        content.push({ nodeName: nodeName, nodeId: nodeId });
      }
    }

    if (node.type == 'TEXT' || node.type == 'FRAME') {
      figma.ui.postMessage({ 
        msg: 'selection',
        extendedLayout,
        nodeType: node.type,
        nodeWidth: node.width,
        layoutMode: layoutMode,
        overflowDirection: overflowDirection,
        nodeName: node.name,
        nodeId: node.id,
        content: content,
        hasKeywordData: nodeContentData != null,
        pluginMode: pluginMode,
      });
    } else {
      figma.ui.postMessage({ msg: 'selection-cleared', pluginMode: pluginMode });
    }
  }

  onSelectionChange();

  // Update the UI whenever the selection changes.
  figma.on('selectionchange', onSelectionChange);

  // Listen for save messages from the UI.
  figma.ui.onmessage = msg => {
    if (msg.msg === 'save-extended-layout') {
      let extendedLayout = JSON.stringify(msg.extendedLayout);
      let selection = figma.currentPage.selection;
      if (selection && selection.length == 1 && selection[0]) {
        selection[0].setSharedPluginData(SHARED_PLUGIN_NAMESPACE, msg.key, extendedLayout);
      }    
    }
    if (msg.msg == 'show-node')
      showNode(msg.node);
  };
} else if (figma.command === "clippy") {
  figma.showUI(__html__, { width: 400, height: 600 });
  clippyRefresh();

  figma.ui.onmessage = msg => {
    // Listen for node-highlight messages; maybe we can have a "refresh button" to run
    // clippy again in the future, too?
    if (msg.msg === 'show-node')
      showNode(msg.node);
  };
} else if (figma.command == "check-keywords") {
  function refresh() {
    // Get the json plugin data from our root node
    let file = figma.root.getSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'clippy-json-file');
    let clippyData = loadClippy();
    let name = clippyData ? clippyData.name : null;
    console.log("Refresh " + file);
    let version = clippyData ? clippyData.version : null;
    figma.ui.postMessage({ msg: 'check-keywords', file, name, version });
  }

    // Once a file has been selected, upload its contents to our plugin data
  figma.ui.onmessage = msg => {
    if (msg.msg == 'clippy-file-selected') {
      figma.root.setSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'clippy-json-file', msg.fileName);
      figma.root.setSharedPluginData(SHARED_PLUGIN_NAMESPACE, 'clippy-json', msg.contents);
      figma.notify("Plugin data from " + msg.fileName + " uploaded: " + msg.contents.length + " bytes");
      refresh();
    }
  }

  figma.showUI(__html__, { width: 400, height: 300 });
  refresh();
}
