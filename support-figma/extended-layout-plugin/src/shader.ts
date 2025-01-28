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
const STROKE_SHADER_PLUGIN_DATA_KEY = "strokeShader";
// Private plugin data, used for clearing shader functionalities
const SHADER_IMAGE_HASH = "shaderImageHash";
const STROKE_SHADER_IMAGE_HASH = "strokeShaderImageHash";

interface ShaderData {
  shader: string;
  shaderFallbackColor: RGBA | null;
  shaderUniforms: ShaderUniform[] | null;
}

interface ShaderUniform {
  uniformName: string;
  uniformType: string;
  uniformValue: number | Float32Array | RGBA;
  extras: ShaderExtras | null;
}

interface ShaderExtras {
  min: Number;
  max: Number;
}

export const shaderMap: ReadonlyMap<string, string> = new Map([
  ["cloudy_sky", __uiFiles__.cloudy_sky],
  ["discrete_ocean", __uiFiles__.discrete_ocean],
  ["fibonacci_sphere", __uiFiles__.fibonacci_sphere],
  ["gradient", __uiFiles__.gradient],
  ["julia", __uiFiles__.julia],
  ["star", __uiFiles__.star],
]);

// Listens to the current selection size change and update the shader plugin ui.
const broadcastSizeChangeCallback = (event: NodeChangeEvent) => {
  let selection = figma.currentPage.selection;
  if (!selection || selection.length != 1 || !selection[0]) {
    return;
  }
  for (const nodeChange of event.nodeChanges) {
    if (
      nodeChange.node == selection[0] &&
      nodeChange.type == "PROPERTY_CHANGE"
    ) {
      for (const property of nodeChange.properties) {
        if (property == "width" || property == "height") {
          figma.ui.postMessage({
            msg: "shader-selection",
            nodeId: selection[0].id,
            size: {
              width: selection[0].width,
              height: selection[0].height,
            },
          });
          return;
        }
      }
    }
  }
};

export function onSelectionChanged() {
  let selection = figma.currentPage.selection;

  figma.currentPage.off("nodechange", broadcastSizeChangeCallback);

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.ui.postMessage({ msg: "shader-selection-cleared" });
    return;
  }

  let shaderData = getShaderData(
    selection[0].getSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      SHADER_PLUGIN_DATA_KEY
    )
  );
  let strokeShaderData = getShaderData(
    selection[0].getSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      STROKE_SHADER_PLUGIN_DATA_KEY
    )
  );
  figma.ui.postMessage({
    msg: "shader-selection",
    nodeId: selection[0].id,
    size: { width: selection[0].width, height: selection[0].height },
    shader: shaderData ? parse(shaderData.shader) : "",
    shaderUniforms: shaderData ? shaderData.shaderUniforms : null,
    strokeShader: strokeShaderData ? parse(strokeShaderData.shader) : "",
    strokeShaderUniforms: strokeShaderData
      ? strokeShaderData.shaderUniforms
      : null,
  });
  figma.currentPage.on("nodechange", broadcastSizeChangeCallback);
}

export async function insertImage(
  imageBytes: Uint8Array,
  asBackground: boolean
) {
  await figma.loadAllPagesAsync();

  let selection = figma.currentPage.selection;

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.notify(
      "No selections or multiple selections. Please select 1 node which can have background or strokes"
    );
    return;
  }
  if (asBackground) {
    await insertBackgroundImage(selection[0], imageBytes);
  } else {
    await insertStrokeImage(selection[0], imageBytes);
  }
}

async function insertBackgroundImage(node: SceneNode, imageBytes: Uint8Array) {
  let nodeWithFills: MinimalFillsMixin = node as MinimalFillsMixin;
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
    node.setPluginData(SHADER_IMAGE_HASH, imageHash);
  } else {
    figma.notify(
      "Current selection doesn't support image fill. Please select 1 node that can have an image fill..."
    );
  }
}

async function insertStrokeImage(node: SceneNode, imageBytes: Uint8Array) {
  let nodeWithStrokes: MinimalStrokesMixin = node as MinimalStrokesMixin;
  if (nodeWithStrokes) {
    // Set the image as the only fill for the node.
    const imageHash = figma.createImage(imageBytes).hash;
    nodeWithStrokes.strokes = [
      {
        type: "IMAGE",
        imageHash: imageHash,
        scaleMode: "FILL",
      },
    ];
    node.setPluginData(STROKE_SHADER_IMAGE_HASH, imageHash);
  } else {
    figma.notify(
      "Current selection doesn't support image stroke. Please select 1 node that can have an image stroke..."
    );
  }
}

export async function setShader(
  shader: string,
  shaderFallbackColor: string,
  shaderUniforms: Array<ShaderUniform>,
  asBackground: boolean
) {
  await figma.loadAllPagesAsync();
  let selection = figma.currentPage.selection;

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.notify(
      "No selections or multiple selections. Please select 1 node that can have an image fill..."
    );
    return;
  }
  setShaderToNode(
    selection[0],
    shader,
    shaderFallbackColor,
    shaderUniforms,
    asBackground
  );

  // Shader has updated. Using the selection callback to notify the html UI about the change.
  onSelectionChanged();
}

function setShaderToNode(
  node: SceneNode,
  shader: string | undefined | null,
  shaderFallbackColor: string | undefined | null,
  shaderUniforms: Array<ShaderUniform> | undefined | null,
  asBackground: boolean
) {
  const rgbaPresent = shaderFallbackColor
    ? figma.util.rgba(shaderFallbackColor)
    : null;
  let shaderData: ShaderData | null = shader
    ? {
        shader: shader,
        shaderFallbackColor: rgbaPresent ? rgbaPresent : null,
        shaderUniforms: shaderUniforms ? shaderUniforms : null,
      }
    : null;

  if (asBackground) {
    let nodeWithFills: MinimalFillsMixin = node as MinimalFillsMixin;

    if (nodeWithFills) {
      if (shaderData) {
        node.setSharedPluginData(
          Utils.SHARED_PLUGIN_NAMESPACE,
          SHADER_PLUGIN_DATA_KEY,
          JSON.stringify(shaderData)
        );
      } else {
        // Clears the shader
        node.setSharedPluginData(
          Utils.SHARED_PLUGIN_NAMESPACE,
          SHADER_PLUGIN_DATA_KEY,
          ""
        );
      }
    } else {
      figma.notify(
        "Current selection doesn't support image fill. Please select 1 node that can have background..."
      );
    }
  } else {
    let nodeWithStrokes: MinimalStrokesMixin = node as MinimalStrokesMixin;

    if (nodeWithStrokes) {
      if (shaderData) {
        node.setSharedPluginData(
          Utils.SHARED_PLUGIN_NAMESPACE,
          STROKE_SHADER_PLUGIN_DATA_KEY,
          JSON.stringify(shaderData)
        );
      } else {
        // Clears the shader
        node.setSharedPluginData(
          Utils.SHARED_PLUGIN_NAMESPACE,
          STROKE_SHADER_PLUGIN_DATA_KEY,
          ""
        );
      }
    } else {
      figma.notify(
        "Current selection doesn't support image stroke. Please select 1 node that can have strokes..."
      );
    }
  }
}

////////////////// Clear //////////////////
export async function clearShader(asBackground: boolean) {
  await figma.loadAllPagesAsync();
  let selection = figma.currentPage.selection;

  // We don't support multiple selections.
  if (!selection || selection.length != 1 || !selection[0]) {
    figma.notify(
      "No selections or multiple selections. Please select 1 node that can have an image fill..."
    );
    return;
  }

  if (asBackground) {
    clearShaderBackgroundFromNode(selection[0]);
  } else {
    clearShaderStrokeFromNode(selection[0]);
  }
  // Shader has been cleared. Using the selection callback to notify the html UI about the change.
  onSelectionChanged();

  figma.notify("Shader has been removed from the current node.");
}

function clearShaderBackgroundFromNode(node: SceneNode) {
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
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SHADER_PLUGIN_DATA_KEY,
    ""
  );
}

function clearShaderStrokeFromNode(node: SceneNode) {
  let shaderCode = node.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    STROKE_SHADER_PLUGIN_DATA_KEY
  );
  if (shaderCode && shaderCode.length != 0) {
    let nodeWithStrokes: MinimalStrokesMixin = node as MinimalStrokesMixin;

    if (nodeWithStrokes) {
      const shaderImageHash = node.getPluginData(STROKE_SHADER_IMAGE_HASH);
      let strokes = nodeWithStrokes.strokes as ReadonlyArray<Paint>;
      if (strokes) {
        let filteredStrokes = strokes.filter(
          (paint) => paint.type != "IMAGE" || paint.imageHash != shaderImageHash
        );
        nodeWithStrokes.strokes = filteredStrokes;
      }
    }
    // Clear the saved shader image hash.
    node.setPluginData(STROKE_SHADER_IMAGE_HASH, "");
  }
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    STROKE_SHADER_PLUGIN_DATA_KEY,
    ""
  );
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
  clearShaderBackgroundFromNode(node);
  clearShaderStrokeFromNode(node);

  // Recurse into any children.
  let maybeParent = node as ChildrenMixin;
  if (maybeParent.children) {
    for (let child of maybeParent.children) {
      await clearNodeRecursivelyAsync(child);
    }
  }
}

function parse(shader: string): string {
  if (!shader) {
    return "";
  }

  let extraUniformsEnd = "////// End of user supplied inputs ////// \n";
  let endIndex = shader.indexOf(extraUniformsEnd);

  return shader.substring(endIndex + extraUniformsEnd.length);
}

function getShaderData(shaderData: string): ShaderData | null {
  if (!shaderData) {
    return null;
  }

  try {
    return JSON.parse(shaderData);
  } catch (e) {
    console.log("Error parsing shader uniforms JSON: " + e);
  }
  return null;
}
