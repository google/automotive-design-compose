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

const SHARED_PLUGIN_NAMESPACE = "designcompose";
const STRING_RES_PLUGIN_DATA_KEY = "vsw-string-res";

interface StringResource {
  name: string;
  translatable: boolean;
  // It can be a simple string or a string array.
  text: string | string[];
  textNodes: string[];
}

export async function generateLocalizationData(uploadedStrings: string[]) {
  console.log("### Localization tool: generate string res name.");

  await figma.loadAllPagesAsync();

  let output = new Map<string, StringResource>();

  // strings.xml files does not allow duplicates so no checks for duplicates here.
  for (let uploadedString of uploadedStrings) {
    let strRes = uploadedString as unknown as StringResource;
    output.set(strRes.name, strRes);
  }

  for (let page of figma.root.children) {
    for (let child of page.children) {
      await localizeNodeAsync(child, output);
    }
  }

  // Convert to an array of key-value pairs
  const outputArray = Array.from(output);
  figma.ui.postMessage({ msg: "localization-output", output: outputArray });
}

export async function updateStringResName(strRes: StringResource) {
  console.log("### Localization tool: user renamed, update string res name.");
  await figma.loadAllPagesAsync();

  for (let nodeId of strRes.textNodes) {
    const node = await figma.getNodeByIdAsync(nodeId);
    if (node?.type == "TEXT") {
      saveToPluginData(node, strRes.name);
    } else {
      console.error(
        `### ${nodeId} is not a text node to update the string res name.`
      );
    }
  }
}

export async function deleteStringResName(strRes: StringResource) {
  console.log("### Localization tool: user removed, delete string res name.");
  if (!strRes.name) {
    console.error("### Expecting the resource name is set to empty");
  }
  strRes.name = "";
  await updateStringResName(strRes);
}

async function localizeNodeAsync(
  node: SceneNode,
  output: Map<string, StringResource>
) {
  if (node.type == "TEXT") {
    // Nodes with name starting with # is for local customization.
    if (node.name.startsWith("#")) {
      console.log("### Localization tool: ignore node " + node.name);
    } else {
      await localizeTextNodeAsync(node, output);
    }
  } else {
    // Recurse into any children.
    let maybeParent = node as ChildrenMixin;
    if (maybeParent.children) {
      for (let child of maybeParent.children) {
        await localizeNodeAsync(child, output);
      }
    }
  }
}

async function localizeTextNodeAsync(
  node: TextNode,
  map: Map<string, StringResource>
): Promise<string> {
  let normalizedText = normalizeTextNode(node);
  // First find and tag. It will override the existing string resource name from the string resource entry read from file.
  const containedValue = [...map.values()].filter(
    (value) => textMatches(value, normalizedText) && value.translatable
  );
  if (containedValue.length > 0) {
    let resName = containedValue[0].name;
    if (!containedValue[0].textNodes) {
      containedValue[0].textNodes = [];
    }
    containedValue[0].textNodes.push(node.id);
    console.log("### found and tag: " + containedValue[0].name);
    saveToPluginData(node, containedValue[0].name);
    return resName;
  }

  var preferredName = node.getSharedPluginData(
    SHARED_PLUGIN_NAMESPACE,
    STRING_RES_PLUGIN_DATA_KEY
  );
  if (!preferredName || endsWithNumbers(preferredName)) {
    preferredName = fromNode(node);
  }

  var index = 0;
  var stringResName = preferredName;

  // Otherwise find a string resource name that doesn't duplicate.
  while (map.has(stringResName)) {
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
  map.set(stringResName, stringRes);
  return stringResName;
}

function fromNode(node: TextNode): string {
  if (countWords(node.characters) > 4) {
    return "tnode_".concat(toSnakeCase(node.name));
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

function debugStringRes(stringRes: StringResource) {
  console.log(
    `### ${stringRes.name} ${stringRes.text} ${stringRes.translatable} ${stringRes.textNodes}`
  );
}

function saveToPluginData(node: TextNode, stringResName: string) {
  node.setSharedPluginData(
    SHARED_PLUGIN_NAMESPACE,
    STRING_RES_PLUGIN_DATA_KEY,
    stringResName
  );
  console.log(
    `### Save string res name ${node.name}-${node.id} with ${stringResName}`
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
  console.log(`### normalize node ${node.id} ${node.characters}`);
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
    console.error("### An error occurred:", error);
  }
  if (stringArray.length > 1) {
    return stringArray;
  } else {
    return normalizeString(node.characters);
  }
}

function normalizeString(characters: string): string {
  return characters
    .replace(/(\r\n|\r|\n)/g, "\\n")
    .replace(/\u2028/g, "\\u2028")
    .replace(/&/g, "&amp;")
    .replace(/>/g, "&gt;")
    .replace(/</g, "&lt;")
    .replace(/'/g, "\\'")
    .replace(/"/g, '\\"')
    .normalize();
}
