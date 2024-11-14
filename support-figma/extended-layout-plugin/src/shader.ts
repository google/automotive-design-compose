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
const SHADER_FALLBACK_COLOR_PLUGIN_DATA_KEY = "shaderFallbackColor";

export const shaderMap: ReadonlyMap<string, string> = new Map([
  ["cloudy_sky", __uiFiles__.cloudy_sky],
  ["discrete_ocean", __uiFiles__.discrete_ocean],
  ["fibonacci_sphere", __uiFiles__.fibonacci_sphere],
  ["gradient", __uiFiles__.gradient],
  ["julia", __uiFiles__.julia],
  ["star", __uiFiles__.star],
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

export async function setShader(shader: string, shaderFallbackColor: string) {
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
    } else {
      // Clears the shader
      selection[0].setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_PLUGIN_DATA_KEY,
        ""
      );
    }
    if (shaderFallbackColor) {
      const rgbaPresent = figma.util.rgba(shaderFallbackColor);
      selection[0].setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_FALLBACK_COLOR_PLUGIN_DATA_KEY,
        rgbaPresent ? JSON.stringify(rgbaPresent) : ""
      );
      if (!rgbaPresent) {
        // Not expecting this to happen but shows an error message if anything unexpected raises up.
        figma.notify("Invalid shader fallback color! Not using any shader fallback color.");
      }
    } else {
      // Clears the fallback color
      selection[0].setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_FALLBACK_COLOR_PLUGIN_DATA_KEY,
        ""
      );
    }
  } else {
    figma.notify(
      "Current selection doesn't support image fill. Please select 1 node that can have an image fill..."
    );
  }
}
