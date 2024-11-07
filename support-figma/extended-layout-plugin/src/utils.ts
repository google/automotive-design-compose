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

export const SHARED_PLUGIN_NAMESPACE = "designcompose";
export const REACTIONS_KEY = "vsw-reactions";
export const CLIPPY_JSON_FILE_KEY = "clippy-json-file";
export const CLIPPY_JSON_KEY = "clippy-json";
export const CONSOLE_TAG = "#DC";

export async function showNode(nodeId: string) {
  // Listen for node-highlight messages; maybe we can have a "refresh button" to run
  // clippy again in the future, too?
  var highlightNode = await figma.getNodeByIdAsync(nodeId);
  if (highlightNode) {
    figma.viewport.scrollAndZoomIntoView([highlightNode]);
    figma.currentPage.selection = [highlightNode as any]; // XXX support multiple pages!
  } else {
    dcError("Can't find node", nodeId);
  }
}

export function dcLog(message?: any, ...optionalParams: any[]) {
  log(CONSOLE_TAG, message, optionalParams);
}

export function dcError(message?: any, ...optionalParams: any[]) {
  error(CONSOLE_TAG, message, optionalParams);
}

export function log(tag: string, message?: any, ...optionalParams: any[]) {
  console.log.call(console, `${tag} ${message}`, ...optionalParams);
}

export function error(tag: string, message?: any, ...optionalParams: any[]) {
  console.error.call(console, `${tag} ${message}`, ...optionalParams);
}

export function toSnakeCase(characters: string): string {
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
