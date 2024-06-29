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

import * as Utils from "./utils";

/** See: com.android.designcompose.codegen.CustomizationType */
export enum DesignCustomizationKind {
  Text = "Text",
  TextFunction = "TextFunction",
  TextStyle = "TextStyle",
  Image = "Image",
  ImageWithContext = "ImageWithContext",
  Visibility = "Visibility",
  VariantProperty = "VariantProperty",
  ComponentReplacement = "ComponentReplacement",
  ContentReplacement = "ContentReplacement",
  Modifier = "Modifier",
  // Not covering "placeholder"; we should remove it.
}

export interface DesignCustomization {
  kind: DesignCustomizationKind;
  name: string;
  node: string;
}

export interface DesignCustomizationVariantProperty
  extends DesignCustomization {
  kind: DesignCustomizationKind.VariantProperty;
  values: string[];
}

export interface DesignCustomizationContentReplacement
  extends DesignCustomization {
  kind: DesignCustomizationKind.ContentReplacement;
  content: string[]; // XXX: this won't work for children that have variants.
}

export interface DesignComponentSpec {
  name: string;
  node: string;
  isRoot?: boolean; // is this the root component? We must know this since we pass customizations down to children.
  customizations: DesignCustomization[];
}

export interface DesignDocSpec {
  name: string;
  version: string;
  components: DesignComponentSpec[];
}

type TopLevelComponentMaps = {
  topLevelComponents: Map<string, BaseNode[]>;
  topLevelComponentDefns: Map<string, DesignComponentSpec>;
  topLevelReactionComponents: Map<string, BaseNode[]>;
};

export function initTopLevelComponentMaps(
  json: DesignDocSpec
): TopLevelComponentMaps {
  // Recurse over all nodes in the doc, from the root down. We populate this
  // `topLevelComponents` map to identify duplicates and identify missing
  // top level components (a top-level component can appear anywhere in a doc;
  // it is "top-level" because of how it is used in the code).
  let topLevelComponents: Map<string, BaseNode[]> = new Map();
  let topLevelComponentDefns: Map<string, DesignComponentSpec> = new Map();
  // A map of the top level component node name to the aggregation of the reaction
  // destination nodes. For example, for top level component "#page1", if a descendent
  // navigates to "#page2" and then a descendent of "#page2" navigates to "#page3",
  // then it has an entry {"#page1" : ["#page2", "#page3"]}.
  let topLevelReactionComponents: Map<string, BaseNode[]> = new Map();
  for (const component of json.components) {
    topLevelComponentDefns.set(component.node, component);
  }
  return {
    topLevelComponents,
    topLevelComponentDefns,
    topLevelReactionComponents,
  };
}

export async function populateComponentMapsAsync(
  node: BaseNode,
  topLevelComponents: Map<string, BaseNode[]>,
  topLevelComponentDefns: Map<string, DesignComponentSpec>
) {
  if (!node) return;
  if (topLevelComponentDefns.has(node.name)) {
    // Store this ref in our list of topLevelComponents.
    let list = topLevelComponents.get(node.name);
    if (list) {
      list.push(node);
    } else {
      topLevelComponents.set(node.name, [node]);
    }
  }
  if ((node as any).children) {
    let parent: ChildrenMixin = node as ChildrenMixin;
    for (const child of parent.children) {
      await populateComponentMapsAsync(
        child,
        topLevelComponents,
        topLevelComponentDefns
      );
    }
  }
}

// NOTE: this returns the id to component map.
export async function mergeReactionsAsync(
  topLevelComponents: Map<string, BaseNode[]>,
  topLevelComponentDefns: Map<string, DesignComponentSpec>,
  topLevelReactionComponents: Map<string, BaseNode[]>
): Promise<Map<string, DesignComponentSpec>> {
  for (const [
    topLevelComponentNodeName,
    topLevelComponentNodes,
  ] of topLevelComponents) {
    for (const node of topLevelComponentNodes) {
      await populateReactionChainAsync(
        node,
        topLevelComponentNodeName,
        topLevelReactionComponents
      );
    }
  }

  let idToDesignComponentSpecMap: Map<string, DesignComponentSpec> = new Map();
  for (const [topLevelComponentNodeName, topLevelNodes] of topLevelComponents) {
    let list = topLevelComponentDefns.get(topLevelComponentNodeName);
    if (list) {
      for (const topLevelNode of topLevelNodes) {
        idToDesignComponentSpecMap.set(topLevelNode.id, list);
      }
    }
  }
  // Merge topLevelComponentDefns with topLevelReactionComponents
  for (const [
    topLevelComponentNodeName,
    reactionNodes,
  ] of topLevelReactionComponents) {
    let list = topLevelComponentDefns.get(topLevelComponentNodeName);
    if (list) {
      for (const reactionNode of reactionNodes) {
        idToDesignComponentSpecMap.set(reactionNode.id, list);
      }
    }
  }
  return idToDesignComponentSpecMap;
}

/**
 * Get the destination nodes on the reaction chain for the given node.
 * 
 * For a top level component, if any descendant has an action that does to another node,
 * we will use the same customization definitions from the top level component to check
 * text customizations.
 * Entry of the map: topLevelComponentNodeName.
 * Value of the map: array of the destination nodes of the reaction chain.
 */
async function populateReactionChainAsync(
  node: BaseNode,
  topLevelComponentNodeName: string,
  topLevelReactionComponents: Map<string, BaseNode[]>
) {
  // If this action goes to another node, put the destination node into the map.
  // Then recursively call #populateReactionChainAsync for find the next level 
  // destination nodes originated from descendents of this destination node.
  async function handleReactionNodeAsync(action?: Action) {
    if (action?.type == "NODE") {
      let destinationId = action?.destinationId;
      if (destinationId) {
        const destinationNode = await figma.getNodeByIdAsync(destinationId);
        if (destinationNode) {
          let list = topLevelReactionComponents.get(topLevelComponentNodeName);
          if (list) {
            if (!list.includes(destinationNode)) {
              list.push(destinationNode);
              await populateReactionChainAsync(
                destinationNode,
                topLevelComponentNodeName,
                topLevelReactionComponents
              );
            }
          } else {
            topLevelReactionComponents.set(topLevelComponentNodeName, [
              destinationNode,
            ]);
            await populateReactionChainAsync(
              destinationNode,
              topLevelComponentNodeName,
              topLevelReactionComponents
            );
          }
        }
      }
    }
  }

  var reactions = null;
  // If this node has reactions, save the destination node into the map.
  let maybeReactions = node as ReactionMixin;
  if (maybeReactions.reactions) {
    reactions = maybeReactions.reactions;
  }
  if (reactions != null && reactions.length > 0) {
    for (const it of reactions) {
      await handleReactionNodeAsync(it.action);
      for (const action of it.actions ? it.actions : []) {
        await handleReactionNodeAsync(action);
      }
    }
  }

  // Now examine the children until we visit all the nodes of the tree.
  if ((node as any).children) {
    let parent: ChildrenMixin = node as ChildrenMixin;
    for (const child of parent.children) {
      await populateReactionChainAsync(
        child,
        topLevelComponentNodeName,
        topLevelReactionComponents
      );
    }
  }
}

export function loadClippy(): DesignDocSpec | null {
  // Get the json plugin data from our root node
  let clippyData = figma.root.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    Utils.CLIPPY_JSON_KEY
  );
  if (!clippyData) return null;

  /*
  var reviver = function(key, value) {
    return value;
  };
  */

  try {
    return JSON.parse(clippyData);
  } catch (e) {
    console.log("Error parsing clippy JSON: " + e);
  }
  return null;
}

export async function findTextCustomizationNodesAsync(
  idToDesignComponentSpecMap: Map<string, DesignComponentSpec>,
  node: BaseNode,
  customizedNodeArray: BaseNode[],
  insideComponentDefn?: DesignComponentSpec
) {
  if (!node) return;
  if (idToDesignComponentSpecMap.has(node.id)) {
    insideComponentDefn = idToDesignComponentSpecMap.get(node.id);
  } else if (insideComponentDefn) {
    // Check to see if our name is one of the customizations, and if so, save the node if it is a text node.
    for (const customization of insideComponentDefn.customizations) {
      // We should also look for nodes that start with "#" but don't match a customization
      // or a top-level component name.
      if (node.name != customization.node) continue;
      if (
        customization.kind == DesignCustomizationKind.Text ||
        customization.kind == DesignCustomizationKind.TextFunction ||
        customization.kind == DesignCustomizationKind.ComponentReplacement
      ) {
        if (node.type == "TEXT") {
          customizedNodeArray.push(node);
        } else {
          console.log(
            "Customization type: ",
            node.id,
            node.name,
            customization.kind
          );
        }
      }
    }
  }
  // Now examine the children of this node. If this was a top level node then provide
  // context on the customizations possible, so that those can be validated.
  if ((node as any).children) {
    let parent: ChildrenMixin = node as ChildrenMixin;
    for (const child of parent.children) {
      await findTextCustomizationNodesAsync(
        idToDesignComponentSpecMap,
        child,
        customizedNodeArray,
        insideComponentDefn
      );
    }
  }
}
