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
const SHADER_UNIFORMS_PLUGIN_DATA_KEY = "shaderUniforms";
// Private plugin data, used for clearing shader functionalities
const SHADER_IMAGE_HASH = "shaderImageHash";

export interface ShaderUniform {
  uniformName: string,
  uniformType: string,
  uniformValue: number|Float32Array|RGBA
}

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
    selection[0].setPluginData(SHADER_IMAGE_HASH, imageHash);
  } else {
    figma.notify(
      "Current selection doesn't support image fill. Please select 1 node that can have an image fill..."
    );
  }
}

export async function setShader(shader: string, shaderFallbackColor: string, shaderUniforms: Array<ShaderUniform>) {
  await figma.loadAllPagesAsync();
  let selection = figma.currentPage.selection;

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.notify(
      "No selections or multiple selections. Please select 1 node that can have an image fill..."
    );
    return;
  }
  setShaderToNode(selection[0], shader, shaderFallbackColor, shaderUniforms);
}

function setShaderToNode(
  node: SceneNode,
  shader: string|undefined|null,
  shaderFallbackColor: string|undefined|null,
  shaderUniforms: Array<ShaderUniform>|undefined|null,
) {
  let nodeWithFills: MinimalFillsMixin = node as MinimalFillsMixin;

  if (nodeWithFills) {
    if (shader) {
      node.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_PLUGIN_DATA_KEY,
        shader
      );
    } else {
      // Clears the shader
      node.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_PLUGIN_DATA_KEY,
        ""
      );
    }
    if (shaderFallbackColor) {
      const rgbaPresent = figma.util.rgba(shaderFallbackColor);
      node.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_FALLBACK_COLOR_PLUGIN_DATA_KEY,
        rgbaPresent ? JSON.stringify(rgbaPresent) : ""
      );
      if (!rgbaPresent) {
        // Not expecting this to happen but shows an error message if anything unexpected raises up.
        figma.notify(
          "Invalid shader fallback color! Not using any shader fallback color."
        );
      }
    } else {
      // Clears the fallback color
      node.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_FALLBACK_COLOR_PLUGIN_DATA_KEY,
        ""
      );
    }
    if (shaderUniforms) {
      for (const shaderUniform of shaderUniforms) {
        console.log(JSON.stringify(shaderUniform));
      }

      console.log(JSON.stringify(shaderUniforms));

      node.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_UNIFORMS_PLUGIN_DATA_KEY,
        JSON.stringify(shaderUniforms)
      );
    } else {
      // Clears the float uniforms
      node.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SHADER_UNIFORMS_PLUGIN_DATA_KEY,
        ""
      );
      console.log("clear shader float uniforms");
    }
  } else {
    figma.notify(
      "Current selection doesn't support image fill. Please select 1 node that can have an image fill..."
    );
  }
}

////////////////// Clear //////////////////
export async function clearShader() {
  await figma.loadAllPagesAsync();
  let selection = figma.currentPage.selection;

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.notify(
      "No selections or multiple selections. Please select 1 node that can have an image fill..."
    );
    return;
  }

  clearShaderFromNode(selection[0]);

  figma.notify("Shader has been removed from the current node.");
}

function clearShaderFromNode(node: SceneNode) {
  let shaderCode = node.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SHADER_PLUGIN_DATA_KEY
  );
  if (shaderCode && shaderCode.length != 0) {
    let nodeWithFills: MinimalFillsMixin = node as MinimalFillsMixin;

    if (nodeWithFills) {
      const shaderImageHash = node.getPluginData(SHADER_IMAGE_HASH);
      let fills = nodeWithFills.fills as ReadonlyArray<Paint>;
      if (fills) {
        let filteredFills = fills.filter(
          (paint) => paint.type != "IMAGE" || paint.imageHash != shaderImageHash
        );
        if (node.type == "TEXT" && filteredFills.length == 0) {
          nodeWithFills.fills = [
            {
              type: "SOLID",
              color: figma.util.rgb("#FF46A2"),
            },
          ];
        } else {
          nodeWithFills.fills = filteredFills;
        }
      }
    }
    // Clear the saved shader image hash.
    node.setPluginData(SHADER_IMAGE_HASH, "");
  }
  setShaderToNode(node, "", "", null);
}

export async function clearAll() {
  Utils.dcLog("Clear shader data from plugin data.");

  await figma.loadAllPagesAsync();

  for (let page of figma.root.children) {
    for (let child of page.children) {
      await clearNodeRecursivelyAsync(child);
    }
  }
  figma.closePlugin("All shaders have been removed.");
}

async function clearNodeRecursivelyAsync(node: SceneNode) {
  clearShaderFromNode(node);

  // Recurse into any children.
  let maybeParent = node as ChildrenMixin;
  if (maybeParent.children) {
    for (let child of maybeParent.children) {
      await clearNodeRecursivelyAsync(child);
    }
  }
}
