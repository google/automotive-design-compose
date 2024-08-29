/**
 * Copyright 2024 Google LLC
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
import * as DesignSpecs from "./design-spec-module";

const STRING_RES_PLUGIN_DATA_KEY = "vsw-string-res";
const STRING_RES_EXTRAS_PLUGIN_DATA_KEY = "vsw-string-res-extras";
const EXPLICIT_EXCLUSION_PLUGIN_DATA_KEY = "vsw-string-explicit-exclusion";
const CONSOLE_TAG = `${Utils.CONSOLE_TAG}-LOCALIZATION`;
const OPTION_EXCLUDE_HASHTAG_NAME = "excludeHashTagName";
const OPTION_READ_CUSTOMIZATION = "readJsonCustomization";
const OPTION_GROUP_SAME_TEXT = "groupSameText";

interface StringResource {
  name: string;
  translatable: boolean;
  // It can be a simple string or a string array.
  text: string | string[];
  textNodes: NodeIdWithExtra[];
  extras?: StringResourceExtras;
  textLength: number;
}

interface StringResourceExtras {
  description: string;
  charlimit: number | "NONE";
}

interface NodeIdWithExtra {
  nodeId: string;
  isExcluded: boolean;
}

export async function generateLocalizationData(
  uploadedStrings: string[],
  options: string[]
) {
  Utils.log(CONSOLE_TAG, "Generate string res name");

  await figma.loadAllPagesAsync();
  let clippyTextNodes = await loadClippyTextNodesAsync(
    options.includes(OPTION_READ_CUSTOMIZATION)
  );

  // String resource name to StringResource map.
  let stringResourceMap = new Map<string, StringResource>();

  // strings.xml files does not allow duplicates so no checks for duplicates here.
  for (let uploadedString of uploadedStrings) {
    let strRes = uploadedString as unknown as StringResource;
    stringResourceMap.set(strRes.name, strRes);
  }

  if (clippyTextNodes.topLevelComponentIds.length === 0) {
    // No clippy top level components found in customization file. Localize all the nodes recursively from root.
    for (let page of figma.root.children) {
      for (let child of page.children) {
        await localizeNodeAsync(
          child,
          stringResourceMap,
          options,
          clippyTextNodes["customizedTextNodeArray"]
        );
      }
    }
  } else {
    // Localize the top level components found in customization file.
    for (let nodeId of clippyTextNodes.topLevelComponentIds) {
      let node = await figma.getNodeByIdAsync(nodeId);
      if (node) {
        await localizeNodeAsync(
          node,
          stringResourceMap,
          options,
          clippyTextNodes["customizedTextNodeArray"]
        );
      }
    }
    // Localize all the local components in the design file.
    const localComponents = figma.root.findAll(
      (node) => node.type === "COMPONENT"
    );
    for (let localComponent of localComponents) {
      if (!clippyTextNodes.topLevelComponentIds.includes(localComponent.id)) {
        await localizeNodeAsync(
          localComponent,
          stringResourceMap,
          options,
          clippyTextNodes["customizedTextNodeArray"]
        );
      }
    }
  }

  // Convert to an array of key-value pairs
  const stringResourceArray = Array.from(stringResourceMap);
  figma.ui.postMessage({
    msg: "localization-output",
    output: stringResourceArray,
  });
}

export async function updateStringRes(strRes: StringResource) {
  await figma.loadAllPagesAsync();

  for (let textNode of strRes.textNodes) {
    const nodeId = textNode["nodeId"];
    const node = await figma.getNodeByIdAsync(nodeId);
    if (node?.type == "TEXT") {
      const isNodeExcluded = textNode["isExcluded"];
      saveResName(node, strRes.name, isNodeExcluded);
      saveExtras(node, strRes.extras);
      setExplicitExcluded(node, isNodeExcluded);
      figma.notify(`Updated localization data of text node ${nodeId}...`);
    } else {
      figma.notify(
        `Ignore update string res request because node ${nodeId} is not a text node.`
      );
    }
  }
}

export async function ungroupTextNode(
  nodeId: string,
  stringResourceArray: Array<[string, StringResource]>
) {
  await figma.loadAllPagesAsync();

  const node = await figma.getNodeByIdAsync(nodeId);
  if (node?.type == "TEXT") {
    const stringResourceMap = new Map<string, StringResource>();
    for (const [key, stringRes] of stringResourceArray) {
      stringResourceMap.set(key, stringRes);
    }

    const isNodeExcluded = isExplicitlyExcluded(node);
    const currentResName = getResName(node);

    if (stringResourceMap.has(currentResName)) {
      const strRes = stringResourceMap.get(currentResName)!!;
      strRes.textNodes = strRes.textNodes.filter((it) => it.nodeId !== nodeId);

      var preferredName = currentResName;
      if (!preferredName) {
        preferredName = fromNode(node);
      } else if (
        stringResourceMap.has(preferredName) &&
        endsWithNumbers(preferredName)
      ) {
        // We need to find a new name so reset preferred name to default.
        preferredName = fromNode(node);
      }

      var index = 0;
      var stringResName = preferredName;

      // Otherwise find a string resource name that doesn't duplicate.
      while (stringResourceMap.has(stringResName)) {
        index += 1;
        stringResName = preferredName + "_" + index;
      }

      saveResName(node, stringResName, isNodeExcluded);
      const newStrRes = {
        name: stringResName,
        translatable: true,
        text: strRes.text,
        textNodes: [{ nodeId: node.id, isExcluded: isNodeExcluded }],
        extras: strRes.extras,
        textLength: strRes.textLength,
      };
      stringResourceMap.set(stringResName, newStrRes);

      // Convert to an array of key-value pairs
      const stringResourceArray = Array.from(stringResourceMap);
      figma.ui.postMessage({
        msg: "localization-ungroup-node-callback",
        output: stringResourceArray,
      });
    } else {
      Utils.error(
        CONSOLE_TAG,
        "This is not expected to ungroup a node that is not in the group",
        nodeId
      );
    }
  } else {
    figma.notify(
      `Ignore ungroup node from localization request because node ${nodeId} is not a text node.`
    );
  }
}

export async function excludeTextNode(nodeId: string, excluded: boolean) {
  await figma.loadAllPagesAsync();
  const node = await figma.getNodeByIdAsync(nodeId);
  if (node?.type == "TEXT") {
    let stringResName = getResName(node);
    setExplicitExcluded(node, excluded);
    saveResName(node, stringResName, excluded);

    if (excluded) {
      figma.notify(`Remove text node ${nodeId} from localization...`);
    } else {
      figma.notify(`Add text node ${nodeId} to localization...`);
    }
    figma.ui.postMessage({
      msg: "localization-exclude-node-callback",
    });
  } else {
    figma.notify(
      `Ignore exclude node from localization request because node ${nodeId} is not a text node.`
    );
  }
}

async function localizeNodeAsync(
  node: BaseNode,
  stringResourceMap: Map<string, StringResource>,
  options: string[],
  clippyTextNodes: BaseNode[]
) {
  if (node.type == "TEXT") {
    // Nodes with name starting with # is for local customization.
    if (
      options.includes(OPTION_EXCLUDE_HASHTAG_NAME) &&
      node.name.startsWith("#")
    ) {
      Utils.log(CONSOLE_TAG, "Ignore node:", node.name);
    } else if (clippyTextNodes.includes(node)) {
      Utils.log(CONSOLE_TAG, "Ignore client side customization:", node.name);
    } else {
      await localizeTextNodeAsync(node, stringResourceMap, options);
    }
  } else {
    // Recurse into any children.
    let maybeParent = node as ChildrenMixin;
    if (maybeParent.children) {
      for (let child of maybeParent.children) {
        await localizeNodeAsync(
          child,
          stringResourceMap,
          options,
          clippyTextNodes
        );
      }
    }
  }
}

async function localizeTextNodeAsync(
  node: TextNode,
  stringResourceMap: Map<string, StringResource>,
  options: string[]
) {
  let normalizedText = normalizeTextNode(node);
  const isNodeExcluded = isExplicitlyExcluded(node);
  if (options.includes(OPTION_GROUP_SAME_TEXT)) {
    // First find and tag. It will override the existing string resource name from the string resource entry read from file.
    const containedValue = [...stringResourceMap.values()].filter(
      (value) => textMatches(value, normalizedText) && value.translatable
    );
    if (containedValue.length > 0) {
      if (!containedValue[0].textNodes) {
        containedValue[0].textNodes = [];
      }
      containedValue[0].textNodes.push({
        nodeId: node.id,
        isExcluded: isNodeExcluded,
      });
      Utils.log(CONSOLE_TAG, "Found and tag:", containedValue[0].name);
      saveResName(node, containedValue[0].name, isNodeExcluded);
      saveExtras(node, containedValue[0].extras);
      // Set the text length to set a proper char limit range.
      containedValue[0].textLength = node.characters.length;
      return;
    }
  }

  var preferredName = getResName(node);
  if (!preferredName) {
    preferredName = fromNode(node);
  } else if (
    stringResourceMap.has(preferredName) &&
    endsWithNumbers(preferredName)
  ) {
    // We need to find a new name so reset preferred name to default.
    preferredName = fromNode(node);
  }

  var index = 0;
  var stringResName = preferredName;

  // Otherwise find a string resource name that doesn't duplicate.
  while (stringResourceMap.has(stringResName)) {
    index += 1;
    stringResName = preferredName + "_" + index;
  }

  saveResName(node, stringResName, isNodeExcluded);

  var stringRes = {
    name: stringResName,
    translatable: true,
    text: normalizedText,
    textNodes: [{ nodeId: node.id, isExcluded: isNodeExcluded }],
    extras: getSavedExtras(node),
    textLength: node.characters.length,
  };
  stringResourceMap.set(stringResName, stringRes);
}

function fromNode(node: TextNode): string {
  if (countWords(node.characters) > 4) {
    return "desc_".concat(toSnakeCase(node.name));
  }
  return "label_".concat(toSnakeCase(node.characters));
}

function toSnakeCase(characters: string): string {
  if (isAllCaps(characters)) {
    var snakeCase = characters.replace(/\W/g, "_").toLowerCase();
  } else {
    var snakeCase = characters
      .replace(/([A-Z]|\s+)/g, (match, group1) =>
        group1 === " " ? "_" : "_" + group1.toLowerCase()
      )
      .replace(/\W/g, "_")
      .toLowerCase();
  }
  snakeCase = snakeCase.replace(/_{2,}/g, "_");
  if (snakeCase.startsWith("_")) {
    snakeCase = snakeCase.substring(1);
  }
  if (snakeCase.endsWith("_")) {
    return snakeCase.substring(0, snakeCase.length - 1);
  }
  return snakeCase;
}

function isAllCaps(characters: string): boolean {
  return characters.toUpperCase() === characters;
}

function countWords(characters: string): number {
  // Remove leading/trailing whitespace and split into words
  const words = characters.trim().split(/\W+/);

  // Filter out empty strings (in case of multiple spaces)
  return words.filter((word) => word !== "").length;
}

function saveResName(
  node: TextNode,
  stringResName: string,
  isExcluded: boolean
) {
  // If node is excluded, save the res name to private plugin data, otherwise, save it to shared plugin data.
  // DesignCompose reads the res name from the shared plugin data to look up string resource at runtime.
  if (isExcluded) {
    node.setPluginData(STRING_RES_PLUGIN_DATA_KEY, stringResName);
    node.setSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      STRING_RES_PLUGIN_DATA_KEY,
      ""
    );
    Utils.log(
      CONSOLE_TAG,
      "Save string res name %s-%s with %s to plugin data",
      node.name,
      node.id,
      stringResName
    );
  } else {
    node.setSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      STRING_RES_PLUGIN_DATA_KEY,
      stringResName
    );
    node.setPluginData(STRING_RES_PLUGIN_DATA_KEY, "");
    Utils.log(
      CONSOLE_TAG,
      "Save string res name %s-%s with %s to shared plugin data",
      node.name,
      node.id,
      stringResName
    );
  }
}

function getResName(node: TextNode) {
  const isNodeExcluded = isExplicitlyExcluded(node);
  if (isNodeExcluded) {
    return node.getPluginData(STRING_RES_PLUGIN_DATA_KEY);
  } else {
    return node.getSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      STRING_RES_PLUGIN_DATA_KEY
    );
  }
}

function saveExtras(node: TextNode, extras?: StringResourceExtras) {
  if (extras) {
    node.setPluginData(
      STRING_RES_EXTRAS_PLUGIN_DATA_KEY,
      JSON.stringify(extras)
    );
  } else {
    Utils.log(CONSOLE_TAG, "Clear extras from", node.id);
    node.setPluginData(STRING_RES_EXTRAS_PLUGIN_DATA_KEY, "");
  }
}

function setExplicitExcluded(node: TextNode, excluded: boolean) {
  if (excluded) {
    node.setPluginData(EXPLICIT_EXCLUSION_PLUGIN_DATA_KEY, "true");
  } else {
    // Clear the data
    node.setPluginData(EXPLICIT_EXCLUSION_PLUGIN_DATA_KEY, "");
  }
}

function isExplicitlyExcluded(node: TextNode): boolean {
  return node.getPluginData(EXPLICIT_EXCLUSION_PLUGIN_DATA_KEY) === "true";
}

function endsWithNumbers(stringResName: string): boolean {
  var pattern = new RegExp("_\\d+$");
  return pattern.test(stringResName);
}

function textMatches(
  strRes: StringResource,
  textRuns: string | string[]
): boolean {
  if (typeof strRes.text === typeof textRuns) {
    if (typeof strRes.text === "string") {
      return strRes.text === textRuns;
    } else {
      return (
        strRes.text.length === textRuns.length &&
        strRes.text.every((val, index) => val === textRuns[index])
      );
    }
  }
  return false;
}

function normalizeTextNode(node: TextNode): string | string[] {
  let stringArray: string[] = [];

  // All styles.
  try {
    let segments = node.getStyledTextSegments([
      "boundVariables",
      "fontSize",
      "fontWeight",
      "fontName",
      "fills",
      "fillStyleId",
      "hyperlink",
      "indentation",
      "letterSpacing",
      "lineHeight",
      "listOptions",
      "openTypeFeatures",
      "textDecoration",
      "textStyleId",
      "textCase",
    ]);
    segments.forEach((it) => stringArray.push(normalizeString(it.characters)));
  } catch (error) {
    Utils.error(CONSOLE_TAG, "An error occured:", error);
  }
  if (stringArray.length > 1) {
    return stringArray;
  } else {
    return normalizeString(node.characters);
  }
}

function normalizeString(characters: string): string {
  return (
    characters
      // Preserve line separators
      .replace(/(\r\n|\r|\n)/g, "\\n")
      // Preserve whitespaces at the beginning and the end
      .replace(/^\s+/g, (match) => match.replace(/\s/g, "\\u0020"))
      .replace(/\s+$/g, (match) => match.replace(/\s/g, "\\u0020"))
      // Preserver tabs at the beginning and the end
      // Use '\t' for precise translations
      .replace(/\t/g, "\\t")
      // Replace the following dash characters with unicode for precise translations
      // - hyphen-minus
      .replace(/\u002d/g, "\\u002d")
      // - hyphen
      .replace(/\u2010/g, "\\u2010")
      // - non-breaking hyphen
      .replace(/\u2011/g, "\\u2011")
      // - figure dash
      .replace(/\u2012/g, "\\u2012")
      // - n dash
      .replace(/\u2013/g, "\\u2013")
      // - m dash
      .replace(/\u2014/g, "\\u2014")
      // - minus sign
      .replace(/\u2212/g, "\\u2212")
      // Replace LSEP with unicode for precise translations
      .replace(/\u2028/g, "\\u2028")
      // Special character & needs to be escaped.
      // Use unicode because html convert &amp; to "&" and make string unmatch.
      .replace(/&/g, "\\u0026")
      // Special character ' needs to be escaped.
      .replace(/'/g, "\\'")
      // Special character " needs to be escaped.
      .replace(/"/g, '\\"')
      // Special character < needs to be escaped.
      // Use unicode because html convert &lt; to "<" and make string unmatch.
      .replace(/</g, "\\u003c")
      // Special character > needs to be escaped.
      // Use unicode because html convert &gt; to ">" and make string unmatch.
      .replace(/>/g, "\\u003e")
      // Convert to normalization form for comparison.
      .normalize()
  );
}

////////////////// Clear //////////////////
export async function clearLocalizationData() {
  Utils.log(CONSOLE_TAG, "Clear string res name from plugin data.");

  await figma.loadAllPagesAsync();

  for (let page of figma.root.children) {
    for (let child of page.children) {
      await clearNodeAsync(child);
    }
  }
  figma.closePlugin("Localization data has been cleared from text nodes.");
}

async function clearNodeAsync(node: SceneNode) {
  if (node.type == "TEXT") {
    saveResName(node, "", false);
    saveExtras(node, undefined);
    setExplicitExcluded(node, false);
  } else {
    // Recurse into any children.
    let maybeParent = node as ChildrenMixin;
    if (maybeParent.children) {
      for (let child of maybeParent.children) {
        await clearNodeAsync(child);
      }
    }
  }
}

////////////////// Extras //////////////////
function estimateCharlimit(text: string): number | "NONE" {
  if (text.length > 40) return "NONE";
  return Math.ceil(text.length * 0.3) * 5;
}

function getSavedExtras(node: TextNode): StringResourceExtras {
  const savedExtras = node.getPluginData(STRING_RES_EXTRAS_PLUGIN_DATA_KEY);
  let extras = savedExtras ? JSON.parse(savedExtras) : {};
  if (!extras.charlimit) {
    extras["charlimit"] = estimateCharlimit(node.characters);
  }
  return extras;
}

////////////// Customization /////////////////

type ClippyCustomizationData = {
  topLevelComponentIds: string[];
  customizedTextNodeArray: BaseNode[];
};

// Parse the document look for text customizations
async function loadClippyTextNodesAsync(
  shouldUseClippy: boolean
): Promise<ClippyCustomizationData> {
  if (shouldUseClippy) {
    let designDocSpec = DesignSpecs.loadClippy();
    if (designDocSpec != null) {
      // Check for text customizations
      let {
        topLevelComponents,
        topLevelComponentDefns,
        topLevelReactionComponents,
      } = DesignSpecs.initTopLevelComponentMaps(designDocSpec);
      await DesignSpecs.populateComponentMapsAsync(
        figma.root,
        topLevelComponents,
        topLevelComponentDefns
      );

      let idToDesignComponentSpecMap = await DesignSpecs.mergeReactionsAsync(
        topLevelComponents,
        topLevelComponentDefns,
        topLevelReactionComponents
      );

      let customizedNodeArray: BaseNode[] = [];
      await DesignSpecs.findTextCustomizationNodesAsync(
        idToDesignComponentSpecMap,
        figma.root,
        customizedNodeArray
      );
      return {
        topLevelComponentIds: [...idToDesignComponentSpecMap.keys()],
        customizedTextNodeArray: customizedNodeArray,
      };
    } else {
      figma.notify(
        'Keywords document not found or corrupted. Run the "Check/update keywords" plugin to upload a new keyword document.',
        { timeout: 8000 }
      );
    }
  }
  return { topLevelComponentIds: [], customizedTextNodeArray: [] };
}
