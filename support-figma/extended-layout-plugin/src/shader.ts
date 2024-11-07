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

const SHADER_PLUGIN_DATA_KEY = "shader";

export const shaderMap: ReadonlyMap<string, string> = new Map([
  ["gradient_flow", __uiFiles__.gradient_flow],
  ["mesh2d", __uiFiles__.mesh2d],
  ["noisy_polka_dots", __uiFiles__.noisy_polka_dots],
  ["rainbow", __uiFiles__.rainbow],
  ["road", __uiFiles__.road],
  ["road_with_scene", __uiFiles__.road_with_scene],
  ["static_dots", __uiFiles__.static_dots],
  ["static_fence_mesh", __uiFiles__.static_fence_mesh],
  ["static_mesh2d", __uiFiles__.static_mesh2d],
]);

export function onSelectionChanged() {
  let selection = figma.currentPage.selection;

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.ui.postMessage({ msg: "shader-selection-cleared" });
    return;
  }

  let nodeWithFills: MinimalFillsMixin = selection[0] as MinimalFillsMixin;
  if (nodeWithFills) {
    figma.ui.postMessage({
      msg: "shader-selection",
      nodeId: selection[0].id,
      size: { width: selection[0].width, height: selection[0].height },
    });
  }
}

export async function insertImage(imageBytes: Uint8Array) {
  Utils.dcLog("Insert image", imageBytes);
  await figma.loadAllPagesAsync();

  let selection = figma.currentPage.selection;

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.notify(
      "No selections or multiple selections. Please select 1 node that can have an image fill..."
    );
    return;
  }

  let nodeWithFills: MinimalFillsMixin = selection[0] as MinimalFillsMixin;
  if (nodeWithFills) {
    // Set the image as the only fill for the node.
    const imageHash = figma.createImage(imageBytes).hash;
    nodeWithFills.fills = [
      {
        type: "IMAGE",
        imageHash: imageHash,
        scaleMode: "FILL",
      },
    ];
  } else {
    figma.notify(
      "Current selection doesn't support image fill. Please select 1 node that can have an image fill..."
    );
  }
}

export async function setShader(shader: string) {
  Utils.dcLog("Set shader: \n", shader);
  await figma.loadAllPagesAsync();
  let selection = figma.currentPage.selection;

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.notify(
      "No selections or multiple selections. Please select 1 node that can have an image fill..."
    );
    return;
  }
  let nodeWithFills: MinimalFillsMixin = selection[0] as MinimalFillsMixin;

  if (nodeWithFills) {
    if (shader) {
      selection[0].setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_PLUGIN_DATA_KEY,
        shader
      );
    }
  } else {
    figma.notify(
      "Current selection doesn't support image fill. Please select 1 node that can have an image fill..."
    );
  }
}
