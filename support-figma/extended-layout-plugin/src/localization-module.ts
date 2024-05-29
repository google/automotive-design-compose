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

const SHARED_PLUGIN_NAMESPACE = "designcompose"
const STRING_RES_PLUGIN_DATA_KEY = "vsw-string-res"

interface StringResource {
  name: string;
  translatable: boolean;
  text: string;
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
      localizeNode(child, output);
    }
  }

  // Convert to an array of key-value pairs
  const outputArray = Array.from(output);
  figma.ui.postMessage({ msg: 'localization-output', output: outputArray});
}

export async function updateStringResName(strRes: StringResource) {
  console.log("### Localization tool: user renamed, update string res name.");
  await figma.loadAllPagesAsync();

  debugStringRes(strRes);
  for (let nodeId of strRes.textNodes) {
    const node = await figma.getNodeByIdAsync(nodeId);
    if (node?.type == "TEXT") {
      saveToPluginData(node, strRes.name);
    } else {
      console.error(`### ${nodeId} is not a text node to update the string res name.`);
    }
  }
}

export async function deleteStringResName(strRes: StringResource) {
  console.log("### Localization tool: user removed, delete string res name.");
  if (!strRes.name) {
    console.error("### Expecting the resource name is set to empty");
  }
  strRes.name = "";
  updateStringResName(strRes);
}

async function localizeNode(node: SceneNode, output: Map<string, StringResource>) {
  if (node.type == "TEXT") {
    // Nodes with name starting with # is for local customization.
    if (node.name.startsWith('#')) {
      console.log("### Localization tool: ignore node " + node.name);
    } else {
      localizeTextNode(node, output);
    }
  } else {
    // Recurse into any children.
    let maybeParent = node as ChildrenMixin;
    if (maybeParent.children) {
      for (let child of maybeParent.children) {
        localizeNode(child, output);
      }
    }
  }
}

function localizeTextNode(node: TextNode, map: Map<string, StringResource>): string {
  // First find and tag. It will override the existing string resource name from the string resource entry read from file.
  const containedValue = [...map.values()].filter(value => value.text === node.characters && value.translatable);
  if (containedValue.length > 0) {
    let resName = containedValue[0].name;
    if (!containedValue[0].textNodes) {
      containedValue[0].textNodes = []
    }
    containedValue[0].textNodes.push(node.id);
    console.log("### found and tag: " + containedValue[0].name);
    saveToPluginData(node, containedValue[0].name);
    return resName;
  }

  var preferredName = node.getSharedPluginData(SHARED_PLUGIN_NAMESPACE, STRING_RES_PLUGIN_DATA_KEY);
  if (!preferredName || endsWithNumbers(preferredName)) {
    preferredName = fromNode(node)
  }

  var index = 0;
  var stringResName = preferredName;

  // Otherwise find a string resource name that doesn't duplicate.
  while (map.has(stringResName)) {
      index += 1;
      stringResName = preferredName + '_' + index;
  }

  saveToPluginData(node, stringResName);

  var stringRes = {name: stringResName, translatable: true, text: node.characters, textNodes: [node.id]};
  map.set(stringResName, stringRes);
  return stringResName;
}

function fromNode(node: TextNode): string {
  if (countWords(node.characters) > 4) {
    return 'label_'.concat(toSnakeCase(node.name));
  }
  return 'label_'.concat(toSnakeCase(node.characters));
}

function toSnakeCase(characters: string): string {
  var snakeCase = characters.replace(/\W/g, '_').replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`);
  if (snakeCase.startsWith('_')) {
    snakeCase = snakeCase.substring(1);
  }
  if (snakeCase.endsWith("_")) {
    return snakeCase.substring(0, snakeCase.length - 1);
  }
  return snakeCase;
}

function countWords(characters: string): number {
  // Remove leading/trailing whitespace and split into words
  const words = characters.trim().split(/\s+/);

  // Filter out empty strings (in case of multiple spaces)
  return words.filter(word => word !== '').length;
}

function debugStringRes(stringRes: StringResource) {
  console.log(`### ${stringRes.name} ${stringRes.text} ${stringRes.translatable} ${stringRes.textNodes}`)
}

function saveToPluginData(node: TextNode, stringResName: string) {
  node.setSharedPluginData(SHARED_PLUGIN_NAMESPACE, STRING_RES_PLUGIN_DATA_KEY, stringResName);
  console.log(`### Save string res name ${node.name}-${node.id} with ${stringResName}`);
}

function endsWithNumbers(stringResName: string): boolean {
  var patt = new RegExp("_\\d+$");
  return patt.test(stringResName);
}
