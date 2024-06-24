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
export const CONSOLE_TAG = "#DC";

export async function showNode(nodeId: string) {
  // Listen for node-highlight messages; maybe we can have a "refresh button" to run
  // clippy again in the future, too?
  var highlightNode = await figma.getNodeByIdAsync(nodeId);
  if (highlightNode) {
    figma.viewport.scrollAndZoomIntoView([highlightNode]);
    figma.currentPage.selection = [highlightNode as any]; // XXX support multiple pages!
  } else {
    error(CONSOLE_TAG, "Can't find node", nodeId);
  }
}

export function log(tag: string, message?: any, ...optionalParams: any[]) {
  console.log.call(console, `${tag} ${message}`, ...optionalParams)
}

export function error(tag: string, message?: any, ...optionalParams: any[]) {
  console.error.call(console, `${tag} ${message}`, ...optionalParams)
}
