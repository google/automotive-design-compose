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
import * as Localization from "./localization-module";
import * as DesignSpecs from "./design-spec-module";

// Warning component.
interface ClippyWarningRun {
  // The text for the warning text run.
  text: string;
  // Does this text run relate to a particular node?
  node: string;
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

function clippyWarningSeverityFromKind(
  kind: ClippyWarningKind
): ClippyWarningSeverity {
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
  kind: ClippyWarningKind;
  severity: ClippyWarningSeverity;
  runs: (string | ClippyWarningRun)[];
}

function createWarning(
  kind: ClippyWarningKind,
  runs: (string | ClippyWarningRun)[]
): ClippyWarning {
  let severity = clippyWarningSeverityFromKind(kind);
  return { kind, severity, runs };
}

// 1. Warn for unused keywords in a top level frame
function clippyCheckUnusedKeywords(
  topLevelComponents: Map<string, BaseNode[]>,
  topLevelComponentDefns: Map<string, DesignSpecs.DesignComponentSpec>,
  warnings: ClippyWarning[]
) {
  // Given a node and a list of keywords (node name customizations), check that the keywords
  // are somewhere in the node's tree hierarchy. Keep a count of nodes that match a keyword
  // so that we can verify missing or extra nodes.
  function findKeywordsRecurse(
    node: BaseNode,
    keywords: Set<String>,
    found: Map<String, number>
  ) {
    if (!node) return;
    if (keywords.has(node.name)) {
      const num = found.get(node.name);
      if (num) {
        found.set(node.name, num + 1);
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
    if (!component || component.isRoot) continue;
    let keywords: Set<string> = new Set();
    component.customizations.forEach((c) => {
      if (c.kind != DesignSpecs.DesignCustomizationKind.VariantProperty)
        keywords.add(c.node);
    });
    if (topLevelComponents.has(nodeName)) {
      let components = topLevelComponents.get(nodeName);
      if (!components) continue;
      let componentSet = components.find(
        (node) => node.type == "COMPONENT_SET"
      );
      components.forEach((node) => {
        // If there are multiple components with the same name and at least one is a COMPONENT_SET,
        // then only check the COMPONENT_SET nodes
        if (componentSet == null || node.type == "COMPONENT_SET") {
          let foundKeywords: Map<String, number> = new Map();
          findKeywordsRecurse(node, keywords, foundKeywords);
          let runs: (string | ClippyWarningRun)[] = [];
          keywords.forEach((keyword) => {
            if (!foundKeywords.has(keyword)) {
              runs.push(keyword);
            }
          });
          if (runs.length > 0) {
            warnings.push(
              createWarning(ClippyWarningKind.UNUSED_KEYWORD, [
                "Missing keywords from ",
                { text: `${node.name}:`, node: node.id } as ClippyWarningRun,
                ...runs,
              ])
            );
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
  topLevelComponentDefns: Map<string, DesignSpecs.DesignComponentSpec>,
  warnings: ClippyWarning[]
) {
  // See if there are any missing or duplicate top-level component names.
  for (const topLevelComponentName of topLevelComponentDefns.keys()) {
    if (topLevelComponents.has(topLevelComponentName)) {
      let components = topLevelComponents.get(topLevelComponentName);
      if (components && components.length > 1) {
        // 3. Warn for duplicated top level keywords
        // If there's one node that's a COMPONENT or COMPONENT_SET, and everything else is
        // a FRAME or INSTANCE then that's fine because the service scores the components higher.
        let componentCount = 0;
        components.forEach((node) => {
          if (node.type == "COMPONENT_SET" || node.type == "COMPONENT")
            componentCount++;
        });
        if (componentCount == 0 || componentCount > 1) {
          warnings.push(
            createWarning(ClippyWarningKind.DUPLICATE_TOP_LEVEL, [
              `Multiple nodes named ${topLevelComponentName}, expected just one:`,
              ...components
                .filter(
                  (figmaNode) =>
                    figmaNode.type == "COMPONENT_SET" ||
                    figmaNode.type == "COMPONENT"
                )
                .map((figmaNode) => {
                  return {
                    text: topLevelComponentName + " (" + figmaNode.type + ")",
                    node: figmaNode.id,
                  } as ClippyWarningRun;
                }),
            ])
          );
        }
      }
    } else {
      // 2. Warn for missing top level keywords
      warnings.push(
        createWarning(ClippyWarningKind.MISSING_TOP_LEVEL, [
          `No nodes found for ${topLevelComponentName}, but one was expected.`,
        ])
      );
    }
  }
}

// 4. Warn if a customization with variants does not have a matching node of type COMPONENT_SET
function clippyCheckVariants(
  topLevelComponentDefns: Map<string, DesignSpecs.DesignComponentSpec>,
  warnings: ClippyWarning[]
) {
  // Recurse through the document and store all COMPONENT_SET nodes into a map.
  function findComponentSets(
    node: BaseNode,
    componentSets: Map<string, ComponentSetNode>
  ) {
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
    if (!component || component.isRoot) continue;

    // `variants` maps a property name to variant names within a node
    let variants: Map<string, Set<string>> = new Map();
    component.customizations.forEach((c) => {
      if (c.kind == DesignSpecs.DesignCustomizationKind.VariantProperty) {
        let customizationVariant =
          c as DesignSpecs.DesignCustomizationVariantProperty;
        if (!variants.has(c.node)) {
          variants.set(c.node, new Set());
        }
        let variantNames = variants.get(c.node);
        if (variantNames) {
          for (const variantName of customizationVariant.values)
            variantNames.add(variantName);
        }
      }
    });

    // If this component has variants, check componentSets to ensure we have at least one
    // COMPONENT_SET with matching variant properties and names
    if (variants.size > 0) {
      variants.forEach((values: Set<string>, property: string) => {
        let propertyValidated = false;
        componentSets.forEach(
          (componentSet: ComponentSetNode, name: string) => {
            try {
              let groupProperties = componentSet.variantGroupProperties;
              let propertyNames = new Set(Object.keys(groupProperties));

              if (propertyNames.has(property)) {
                let propertyValues = new Set(groupProperties[property].values);
                let valuesFound = true;
                values.forEach((value) => {
                  if (!propertyValues.has(value)) valuesFound = false;
                });
                if (valuesFound) propertyValidated = true;
              }
            } catch (e) {
              warnings.push(
                createWarning(ClippyWarningKind.MISSING_COMPONENT_SET, [
                  { text: name, node: componentSet.id },
                  " has an exception. ",
                  `${e}`,
                ])
              );
            }
            return !propertyValidated;
          }
        );
        // Add a warning if this property was not validated
        if (!propertyValidated)
          warnings.push(
            createWarning(ClippyWarningKind.MISSING_COMPONENT_SET, [
              `${property} is a variant property with possible values [${Array.from(values.values()).join(",")}], but could not be found`,
            ])
          );
      });
    }
  }
}

// 5. Warn for node type mismatches (ClippyWarningKind.TYPE_MISMATCH):
function clipyCheckTypeMismatches(
  topLevelComponentDefns: Map<string, DesignSpecs.DesignComponentSpec>,
  node: BaseNode,
  warnings: ClippyWarning[],
  insideComponentDefn?: DesignSpecs.DesignComponentSpec
) {
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
      var kind = customization.kind;
      switch (kind) {
        case DesignSpecs.DesignCustomizationKind.Text:
        case DesignSpecs.DesignCustomizationKind.TextStyle:
        case DesignSpecs.DesignCustomizationKind.TextState:
          if (node.type != "TEXT") {
            warnings.push(
              createWarning(ClippyWarningKind.TYPE_MISMATCH, [
                `${customization.node} is a text customization, but`,
                { text: node.name, node: node.id },
                `is a ${node.type} instead of a TEXT node.`,
              ])
            );
          }
          break;
        case DesignSpecs.DesignCustomizationKind.ContentReplacement:
          if (node.type != "FRAME") {
            warnings.push(
              createWarning(ClippyWarningKind.TYPE_MISMATCH, [
                `${customization.node} is a customization that adds children to a FRAME, but`,
                { text: node.name, node: node.id },
                `is a ${node.type}.`,
              ])
            );
          }
          break;
        case DesignSpecs.DesignCustomizationKind.Image:
        case DesignSpecs.DesignCustomizationKind.ImageWithContext:
          // Not sure what the rules are for image. Frames and rectangles seem to work
          if (node.type != "FRAME" && node.type != "RECTANGLE") {
            warnings.push(
              createWarning(ClippyWarningKind.TYPE_MISMATCH, [
                `${customization.node} is a customization that changes the background fill to a FRAME, but`,
                { text: node.name, node: node.id },
                `is a ${node.type}.`,
              ])
            );
          }
          break;
        case DesignSpecs.DesignCustomizationKind.Visibility:
        case DesignSpecs.DesignCustomizationKind.VariantProperty:
        case DesignSpecs.DesignCustomizationKind.ComponentReplacement:
        case DesignSpecs.DesignCustomizationKind.Modifier:
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
      clipyCheckTypeMismatches(
        topLevelComponentDefns,
        child,
        warnings,
        insideComponentDefn
      );
    }
  }
}

// Parse the document look for various types of warnings
async function clippy(
  json: DesignSpecs.DesignDocSpec
): Promise<ClippyWarning[]> {
  // Generated warnings
  let warnings: ClippyWarning[] = [];

  let {
    topLevelComponents,
    topLevelComponentDefns,
    topLevelReactionComponents,
  } = DesignSpecs.initTopLevelComponentMaps(json);
  await DesignSpecs.populateComponentMapsAsync(
    figma.root,
    topLevelComponents,
    topLevelComponentDefns
  );

  // 1. Warn for unused keywords in a top level frame (ClippyWarningKind.UNUSED_KEYWORD)
  clippyCheckUnusedKeywords(
    topLevelComponents,
    topLevelComponentDefns,
    warnings
  );

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

async function clippyRefresh() {
  await figma.loadAllPagesAsync();

  // Get the json plugin data from our root node
  let clippyFile = figma.root.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    Utils.CLIPPY_JSON_FILE_KEY
  );
  let json = DesignSpecs.loadClippy();
  var errors = null;
  if (json != null) {
    errors = await clippy(json);
  }
  figma.ui.postMessage({ msg: "clippy", errors, clippyFile });
}

// If we were invoked with the "sync" command then run our sync logic and quit.
if (figma.command === "sync") {
  console.log("### Sync");
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
    node.setSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      Utils.REACTIONS_KEY,
      reactionData == null ? "" : JSON.stringify(reactionData)
    );

    // Pull out other prototyping properties that aren't available in the REST API.
    if (
      node.type == "FRAME" ||
      node.type == "COMPONENT" ||
      node.type == "INSTANCE"
    ) {
      node.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        "vsw-frame-extras",
        JSON.stringify({
          numberOfFixedChildren: node.numberOfFixedChildren,
          overlayPositionType: node.overlayPositionType,
          overlayBackground: node.overlayBackground,
          overlayBackgroundInteraction: node.overlayBackgroundInteraction,
          overflowDirection: node.overflowDirection,
          layoutMode: node.layoutMode,
        })
      );
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
  async function performSync() {
    await figma.loadAllPagesAsync();
    for (let page of figma.root.children) {
      for (let child of page.children) {
        syncReactions(child);
      }
    }

    // Close our plugin with a success message.
    figma.closePlugin("Synchronized Interaction Data with AAOS UX");
  }
  performSync();
} else if (figma.command === "localization") {
  figma.showUI(__html__, { width: 800, height: 600 });
  figma.ui.postMessage({
    msg: "localization",
  });
  figma.ui.onmessage = (msg) => {
    if (msg.msg === "generate-localization-data") {
      Localization.generateLocalizationData(msg.contents, msg.options);
    } else if (msg.msg === "update-localization-data") {
      Localization.updateStringRes(msg.item);
    } else if (msg.msg === "localization-exclude-node") {
      Localization.excludeTextNode(msg.node, msg.excluded);
    } else if (msg.msg === "show-node") {
      Utils.showNode(msg.node);
    } else if (msg.msg === "close-plugin") {
      figma.closePlugin();
    }
  };
} else if (figma.command === "clear-localization") {
  Localization.clearLocalizationData();
} else if (figma.command === "move-plugin-data") {
  function movePluginDataWithKey(node: BaseNode, key: string) {
    // Read the private plugin data, write to shared
    let data = node.getPluginData(key);
    node.setSharedPluginData(Utils.SHARED_PLUGIN_NAMESPACE, key, data);
  }
  // If we were invoked with the "move plugin data" command then move all the plugin data
  // from the private location to the shared location, then quit.
  function movePluginData(node: SceneNode) {
    movePluginDataWithKey(node, Utils.REACTIONS_KEY);
    movePluginDataWithKey(node, "vsw-extended-text-layout");
    movePluginDataWithKey(node, "vsw-frame-extras");

    // Recurse into any children.
    let maybeParent = node as ChildrenMixin;
    if (maybeParent.children) {
      for (let child of maybeParent.children) {
        movePluginData(child);
      }
    }
  }

  async function performMove() {
    await figma.loadAllPagesAsync();

    // Move the root level data first
    movePluginDataWithKey(figma.root, "clippy-json-file");
    movePluginDataWithKey(figma.root, "clippy-json");

    // We want to visit every node; the document and page nodes can't have any reaction
    // data.
    for (let page of figma.root.children) {
      for (let child of page.children) {
        movePluginData(child);
      }
    }

    // Close our plugin with a success message.
    figma.closePlugin("Moved plugin data to shared location");
  }
  performMove();
} else if (figma.command === "meters") {
  figma.showUI(__html__, { width: 400, height: 400 });
  figma.ui.postMessage({
    msg: "meters",
  });

  function clamp(num: number, min: number, max: number): number {
    return Math.min(Math.max(num, min), max);
  }

  function percentToValue(percent: number, min: number, max: number) {
    let range = max - min;
    return min + (percent / 100) * range;
  }

  function radiansToDegrees(radians: number) {
    return (radians * 180) / Math.PI;
  }

  function degreesToRadians(degrees: number) {
    return (degrees * Math.PI) / 180;
  }

  function transformMultiply(m: Transform, n: Transform): Transform {
    return [
      [
        m[0][0] * n[0][0] + m[0][1] * n[1][0],
        m[0][0] * n[0][1] + m[0][1] * n[1][1],
        m[0][0] * n[0][2] + m[0][1] * n[1][2] + m[0][2],
      ],
      [
        m[1][0] * n[0][0] + m[1][1] * n[1][0],
        m[1][0] * n[0][1] + m[1][1] * n[1][1],
        m[1][0] * n[0][2] + m[1][1] * n[1][2] + m[1][2],
      ],
    ];
  }

  function moveTransform(x: number, y: number): Transform {
    return [
      [1, 0, x],
      [0, 1, y],
    ];
  }

  function rotateTransform(angleRadians: number): Transform {
    return [
      [Math.cos(angleRadians), Math.sin(angleRadians), 0],
      [-Math.sin(angleRadians), Math.cos(angleRadians), 0],
    ];
  }

  function deltaTransformPoint(transform: Transform, point: Vector) {
    var x = point.x * transform[0][0] + point.y * transform[0][1];
    var y = point.x * transform[1][0] + point.y * transform[1][1];
    return { x, y };
  }

  // Hypotenuse of right triangle with sides x and y
  function hypot(x: number, y: number): number {
    return Math.sqrt(x * x + y * y);
  }

  function decomposeTransform(t: Transform) {
    let result: any = {};
    let row0x = t[0][0];
    let row0y = t[0][1];
    let row1x = t[1][0];
    let row1y = t[1][1];
    result.translateX = t[0][2];
    result.translateY = t[1][2];

    // Compute scaling factors.
    result.scaleX = hypot(row0x, row0y);
    result.scaleY = hypot(row1x, row1y);

    // If determinant is negative, one axis was flipped.
    const determinant = row0x * row1y - row0y * row1x;
    if (determinant < 0) {
      // Flip axis with minimum unit vector dot product.
      if (row0x < row1y) result.scaleX = -result.scaleX;
      else result.scaleY = -result.scaleY;
    }

    // Renormalize matrix to remove scale.
    if (result.scaleX != 1) {
      row0x *= 1 / result.scaleX;
      row0y *= 1 / result.scaleX;
    }
    if (result.scaleY != 1) {
      row1x *= 1 / result.scaleY;
      row1y *= 1 / result.scaleY;
    }

    // Compute rotation and renormalize matrix.
    result.angle = Math.atan2(row0y, row0x);

    if (result.angle != 0) {
      // Rotate(-angle) = [cos(angle), sin(angle), -sin(angle), cos(angle)]
      //                = [row0x, -row0y, row0y, row0x]
      // Thanks to the normalization above.
      const sn = -row0y;
      const cs = row0x;
      const m11 = row0x;
      const m12 = row0y;
      const m21 = row1x;
      const m22 = row1y;

      row0x = cs * m11 + sn * m21;
      row0y = cs * m12 + sn * m22;
      row1x = -sn * m11 + cs * m21;
      row1y = -sn * m12 + cs * m22;
    }

    result.m11 = row0x;
    result.m12 = row0y;
    result.m21 = row1x;
    result.m22 = row1y;

    // Convert into degrees because our rotation functions expect it.
    result.angle = radiansToDegrees(result.angle);

    // calculate delta transform point
    const px = deltaTransformPoint(t, { x: 0, y: 1 });
    const py = deltaTransformPoint(t, { x: 1, y: 0 });

    // calculate skew
    result.skewX = (180 / Math.PI) * Math.atan2(px.y, px.x) - 90;
    result.skewY = (180 / Math.PI) * Math.atan2(py.y, py.x);

    return result;
  }

  function getMeterData(node: SceneNode) {
    let meterDataStr = node.getSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      "vsw-meter-data"
    );
    return meterDataStr && meterDataStr.length ? JSON.parse(meterDataStr) : {};
  }

  function saveMeterData(meterData: any) {
    let node = figma.currentPage.selection[0];
    node.setSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      "vsw-meter-data",
      JSON.stringify(meterData)
    );
  }

  function onSelectionChangeMeters() {
    let selection = figma.currentPage.selection;

    // We don't support multiple selections.
    if (!selection || selection.length != 1 || !selection[0]) {
      figma.ui.postMessage({ msg: "meters-selection-cleared" });
      return;
    }

    let node = selection[0];
    let meterData = getMeterData(node);
    let ellipseAngle = 0;
    let rotation = 0;
    let progress = 0;

    // Get angle/arc data if this is an ellipse
    if (node.type == "ELLIPSE")
      ellipseAngle = radiansToDegrees(
        (node as EllipseNode).arcData.endingAngle
      );

    // Get rotation data if this is any type of node. Case to FrameNode in order to
    // access the `rotation` field, which works even for other types of nodes
    rotation = (node as FrameNode).rotation;

    if (node.type == "FRAME" || node.type == "RECTANGLE") {
      // Calculate current progress bar position based on node position and size
      if (meterData.progressMarkerData) {
        const { startX = 0, endX = 0 } = meterData.progressMarkerData;
        progress = ((node.x - startX) / (endX - startX)) * 100;
      } else if (meterData.progressBarData) {
        const { endX = 0 } = meterData.progressBarData;
        progress = (node.width / endX) * 100;
      }
    }

    // If we just selected a node with arc data, save the corner radius if it has changed
    saveArcCornerRadius();

    let parent = node.parent as FrameNode;

    figma.ui.postMessage({
      msg: "meters-selection",
      nodeType: node.type,
      parentType: parent.type,
      parentSize: { width: parent.width, height: parent.height },
      meterData,
      ellipseAngle,
      rotation,
      progress,
    });
  }

  function onDocumentChangedMeters(event: DocumentChangeEvent) {
    // Whenever the document has changed, save the corner radius if it has changed
    saveArcCornerRadius();
  }

  // Since the corner radius of an ellipse is not exposed in Figma's REST API but it is in the
  // plugin API, save it whenever we detect it has changed.
  function saveArcCornerRadius() {
    let selection = figma.currentPage.selection;
    if (selection && selection.length == 1 && selection[0]) {
      let node = selection[0];
      let meterData = getMeterData(node);
      if (meterData && meterData.arcData) {
        let eNode = node as EllipseNode;
        if (meterData.arcData.cornerRadius != eNode.cornerRadius) {
          meterData.arcData.cornerRadius = eNode.cornerRadius;
          saveMeterData(meterData);
        }
      }
    }
  }

  function arcChanged(msg: any) {
    let startAngle = msg.start;
    let endAngle = msg.end;
    let value = percentToValue(msg.value, startAngle, endAngle);
    if (msg.discrete) value = value - (value % msg.discreteValue);
    value = degreesToRadians(value);

    let eNode = figma.currentPage.selection[0] as EllipseNode;
    eNode.arcData = {
      startingAngle: degreesToRadians(startAngle),
      endingAngle: value,
      innerRadius: eNode.arcData.innerRadius,
    };

    let arcData = {
      enabled: msg.enabled,
      start: msg.start,
      end: msg.end,
      discrete: msg.discrete,
      discreteValue: msg.discreteValue,
      cornerRadius: eNode.cornerRadius,
    };

    let meterData: any = {};
    meterData.arcData = arcData;
    saveMeterData(meterData);
  }

  function rotationChanged(msg: any) {
    // Calculate rotation around center, not origin (top left), so we need to calculate
    // a transformation matrix to translates the center to the origin, rotates, then
    // translates back. Furthermore, the node may already be offset from its parent, so
    // we need to calculate that offset as well.
    let startAngle = msg.start;
    let endAngle = msg.end;
    let rotation = percentToValue(msg.value, startAngle, endAngle);
    if (msg.discrete) rotation = rotation - (rotation % msg.discreteValue);
    let a = degreesToRadians(rotation);

    // Calculate the x and y offset of the top left corner from its parent when the
    // rotation is 0. These values will be needed in a translate matrix after the
    // rotation is performed.
    let node = figma.currentPage.selection[0] as FrameNode;
    let r = Math.sqrt(node.width * node.width + node.height * node.height) / 2;
    let topLeftAngle = radiansToDegrees(Math.atan(node.height / -node.width));
    let angleFromTopLeft =
      degreesToRadians(node.rotation) + degreesToRadians(topLeftAngle);
    let cos = Math.abs(Math.cos(angleFromTopLeft));
    let sin = Math.abs(Math.sin(angleFromTopLeft));

    let xOffset = node.x - node.width / 2;
    if (
      node.rotation >= -90 - topLeftAngle &&
      node.rotation < 90 - topLeftAngle
    )
      xOffset += r * cos;
    else xOffset -= r * cos;

    let yOffset = node.y - node.height / 2;
    if (
      node.rotation <= -topLeftAngle &&
      node.rotation >= -topLeftAngle - 180
    ) {
      yOffset += r * sin;
    } else {
      yOffset -= r * sin;
    }

    // Calculate the transformation matrix:
    // 1. Translate left and up by half the width and height to center the origin
    // 2. Rotate
    // 3. Translate right and down by half the width and height plus xOffset and yOffset
    //    to restore the node's offset from its parent
    let x = node.width / 2;
    let y = node.height / 2;
    let moveFinal = moveTransform(x + xOffset, y + yOffset);
    let rotate = transformMultiply(moveFinal, rotateTransform(a));
    let totalTransform = transformMultiply(rotate, moveTransform(-x, -y));
    node.relativeTransform = totalTransform;

    let rotationData = {
      enabled: msg.enabled,
      start: msg.start,
      end: msg.end,
      discrete: msg.discrete,
      discreteValue: msg.discreteValue,
    };

    let meterData: any = {};
    meterData.rotationData = rotationData;
    saveMeterData(meterData);
  }

  function flipRect(r: Rect): Rect {
    return {
      x: r.x,
      y: r.y,
      width: r.height,
      height: r.width,
    };
  }

  function progressChanged(msg: any, progressMarker: boolean) {
    const node = figma.currentPage.selection[0] as FrameNode;

    // Use the parent of the progress bar to determine the extents to which the
    // progress bar moves.
    const parent = node.parent as FrameNode;

    // Calculate the progress bar value if discrete values are specified
    let value = msg.value;
    if (msg.discrete)
      value = clamp(value - (value % msg.discreteValue), 0, 100);

    let barData: any = {
      enabled: msg.enabled,
      discrete: msg.discrete,
      discreteValue: msg.discreteValue,
      vertical: msg.vertical,
    };

    let meterData: any = {};
    if (progressMarker) {
      // The progress marker means we don't resize the node; we just move it along the x axis
      // if horizontal or y axis if vertical
      if (msg.vertical) {
        const startY = parent.height - node.height / 2;
        const endY = -node.height / 2;
        const moveY = percentToValue(value, startY, endY);
        node.y = moveY;

        barData.startY = startY;
        barData.endY = endY;
      } else {
        const startX = -node.width / 2;
        const endX = parent.width - node.width / 2;
        const moveX = percentToValue(value, startX, endX);
        node.x = moveX;

        barData.startX = startX;
        barData.endX = endX;
      }

      meterData.progressMarkerData = barData;
    } else {
      // Normal progress bar mode means we resize the progress bar between a width of 0.01
      // and a max size based on the parent width if horizontal or height if vertical
      if (msg.vertical) {
        const height = percentToValue(value, 0.01, parent.height);
        node.resize(parent.width, height);
        node.y = parent.height - height;
        barData.endY = parent.height;
      } else {
        const width = percentToValue(value, 0.01, parent.width);
        node.resize(width, parent.height);
        node.y = 0;
        barData.endX = parent.width;
      }

      meterData.progressBarData = barData;
    }

    saveMeterData(meterData);
  }

  function vectorChanged(msg: any) {
    const node = figma.currentPage.selection[0] as VectorNode;

    // Collect vector paths into an array. The vector data here for a vector stroked path
    // (no fill) is different than the one provided by the REST API. This one returns the
    // path of the stroke itself, whereas the REST API returns the path of the outline of
    // the stroke. The stroke path is the one needed in order to render a partial curve
    // as a progress bar that follows a vector path.
    let paths = [];
    for (const path of node.vectorPaths) {
      paths.push({
        path: path.data,
        windingRule: path.windingRule,
      });
    }

    let vectorData: any = {
      enabled: msg.enabled,
      discrete: msg.discrete,
      discreteValue: msg.discreteValue,
      paths: paths,
    };

    let meterData: any = {
      progressVectorData: vectorData,
    };

    saveMeterData(meterData);
  }

  async function initMeters() {
    await figma.loadAllPagesAsync();
    onSelectionChangeMeters();
    figma.on("selectionchange", onSelectionChangeMeters);
    figma.on("documentchange", onDocumentChangedMeters);
    figma.ui.onmessage = (msg) => {
      if (msg.msg == "arc-changed") {
        arcChanged(msg);
      } else if (msg.msg == "rotation-changed") {
        rotationChanged(msg);
      } else if (msg.msg == "bar-changed") {
        progressChanged(msg, false);
      } else if (msg.msg == "vector-changed") {
        vectorChanged(msg);
      } else if (msg.msg == "marker-changed") {
        progressChanged(msg, true);
      }
    };
  }
  initMeters();
} else if (figma.command === "clippy") {
  figma.showUI(__html__, { width: 400, height: 600 });
  clippyRefresh();

  figma.ui.onmessage = (msg) => {
    // Listen for node-highlight messages; maybe we can have a "refresh button" to run
    // clippy again in the future, too?
    if (msg.msg === "show-node") Utils.showNode(msg.node);
  };
} else if (figma.command === "check-keywords") {
  function refresh() {
    // Get the json plugin data from our root node
    let file = figma.root.getSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      "clippy-json-file"
    );
    let clippyData = DesignSpecs.loadClippy();
    let name = clippyData ? clippyData.name : null;
    console.log("Refresh " + file);
    let version = clippyData ? clippyData.version : null;
    figma.ui.postMessage({ msg: "check-keywords", file, name, version });
  }

  // Once a file has been selected, upload its contents to our plugin data
  figma.ui.onmessage = (msg) => {
    if (msg.msg == "clippy-file-selected") {
      figma.root.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        "clippy-json-file",
        msg.fileName
      );
      figma.root.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        "clippy-json",
        msg.contents
      );
      figma.notify(
        "Plugin data from " +
          msg.fileName +
          " uploaded: " +
          msg.contents.length +
          " bytes"
      );
      refresh();
    }
  };

  figma.showUI(__html__, { width: 400, height: 300 });
  refresh();
}
