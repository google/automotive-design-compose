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

const STRING_RES_PLUGIN_DATA_KEY = "vsw-string-res";
const CONSOLE_TAG = `${Utils.CONSOLE_TAG}-LOCALIZATION`;

interface StringResource {
  name: string;
  translatable: boolean;
  // It can be a simple string or a string array.
  text: string | string[];
  textNodes: string[];
}

export async function generateLocalizationData(uploadedStrings: string[]) {
  Utils.log(CONSOLE_TAG, "Generate string res name");

  await figma.loadAllPagesAsync();

  // String resource name to StringResource map.
  let stringResourceMap = new Map<string, StringResource>();

  // strings.xml files does not allow duplicates so no checks for duplicates here.
  for (let uploadedString of uploadedStrings) {
    let strRes = uploadedString as unknown as StringResource;
    stringResourceMap.set(strRes.name, strRes);
  }

  for (let page of figma.root.children) {
    for (let child of page.children) {
      await localizeNodeAsync(child, stringResourceMap);
    }
  }

  // Convert to an array of key-value pairs
  const stringReourceArray = Array.from(stringResourceMap);
  figma.ui.postMessage({ msg: "localization-output", output: stringReourceArray });
}

export async function updateStringResName(strRes: StringResource) {
  Utils.log(CONSOLE_TAG, "User renamed, update string res name");
  await figma.loadAllPagesAsync();

  for (let nodeId of strRes.textNodes) {
    const node = await figma.getNodeByIdAsync(nodeId);
    if (node?.type == "TEXT") {
      saveToPluginData(node, strRes.name);
    } else {
      Utils.error(
        CONSOLE_TAG,
        "%s not a text node to update string res name",
        nodeId
      );
    }
  }
}

async function localizeNodeAsync(
  node: SceneNode,
  stringResourceMap: Map<string, StringResource>
) {
  if (node.type == "TEXT") {
    // Nodes with name starting with # is for local customization.
    if (node.name.startsWith("#")) {
      Utils.log(CONSOLE_TAG, "Ignore node:", node.name);
    } else {
      await localizeTextNodeAsync(node, stringResourceMap);
    }
  } else {
    // Recurse into any children.
    let maybeParent = node as ChildrenMixin;
    if (maybeParent.children) {
      for (let child of maybeParent.children) {
        await localizeNodeAsync(child, stringResourceMap);
      }
    }
  }
}

async function localizeTextNodeAsync(
  node: TextNode,
  stringResourceMap: Map<string, StringResource>
): Promise<string> {
  let normalizedText = normalizeTextNode(node);
  // First find and tag. It will override the existing string resource name from the string resource entry read from file.
  const containedValue = [...stringResourceMap.values()].filter(
    (value) => textMatches(value, normalizedText) && value.translatable
  );
  if (containedValue.length > 0) {
    let resName = containedValue[0].name;
    if (!containedValue[0].textNodes) {
      containedValue[0].textNodes = [];
    }
    containedValue[0].textNodes.push(node.id);
    Utils.log(CONSOLE_TAG, "Found and tag:", containedValue[0].name);
    saveToPluginData(node, containedValue[0].name);
    return resName;
  }

  var preferredName = node.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    STRING_RES_PLUGIN_DATA_KEY
  );
  if (!preferredName) {
    preferredName = fromNode(node);
  } else if (stringResourceMap.has(preferredName) && endsWithNumbers(preferredName)) {
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

  saveToPluginData(node, stringResName);

  var stringRes = {
    name: stringResName,
    translatable: true,
    text: normalizedText,
    textNodes: [node.id],
  };
  stringResourceMap.set(stringResName, stringRes);
  return stringResName;
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

function saveToPluginData(node: TextNode, stringResName: string) {
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    STRING_RES_PLUGIN_DATA_KEY,
    stringResName
  );
  Utils.log(
    CONSOLE_TAG,
    "Save string res name %s-%s with %s",
    node.name,
    node.id,
    stringResName
  );
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
  Utils.log(CONSOLE_TAG, "Normalize node:", node.id);
  let stringArray: string[] = [];

  // All styles.
  try {
    let segments = node.getStyledTextSegments([
      "fontSize",
      "fontWeight",
      "letterSpacing",
      "lineHeight",
      "fontName",
      "textDecoration",
      "textCase",
      "fills",
      "textStyleId",
      "fillStyleId",
      // Comment out the following due to "Error: in getStyledTextSegments: Unknown list option"
      // "listOptions",
      "indentation",
      "hyperlink",
      "openTypeFeatures",
      "boundVariables",
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
  figma.closePlugin("String resource name has been cleared from text nodes.");
}

async function clearNodeAsync(node: SceneNode) {
  if (node.type == "TEXT") {
    saveToPluginData(node, "");
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
