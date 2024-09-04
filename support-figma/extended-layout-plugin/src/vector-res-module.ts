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
import { loadImageDrawableResNames } from "./image-res-module";
import * as Utils from "./utils";

const IMAGE_REPLACEMENT_RES_NAME = "image_replacement_res_name";

// Node id to vector root(frame node)
const vectorFrames = new Map<string, SceneNode>();
// Res name to VectorRes map.
const vectorResMap = new Map<string, VectorRes>();
// Image res names that vector exported images can not use
var existingImageDrawableResNames : Array<string>;

// ldpi: 0.75x, mdpi: 1x, hdpi: 1.5x, xhdpi: 2x, xxhdpi: 3x, xxxhdpi: 4x according to:
// https://developer.android.com/training/multiscreen/screendensities
// Ignores the 0.5x export option.
interface VectorRes {
  resName: string;
  nodeId: string;
  ldpiBytes: Uint8Array;
  mdpiBytes: Uint8Array;
  hdpiBytes: Uint8Array;
  xhdpiBytes: Uint8Array;
  xxhdpiBytes: Uint8Array;
}

export async function exportAllVectorsAsync(){
  await figma.loadAllPagesAsync();

  vectorFrames.clear();
  vectorResMap.clear();

  const vectorNodes = figma.root.findAll((node) => node.type === "VECTOR");

  for (const vectorNode of vectorNodes) {
    const frameAncestor = findFrameAncestor(vectorNode);
    if (frameAncestor && isVectorRoot(frameAncestor)) {
      vectorFrames.set(frameAncestor.id, frameAncestor);
    }
  }

  existingImageDrawableResNames = loadImageDrawableResNames();
  for (const [frameNodeId, frameNode] of vectorFrames) {
    const cachedResName = getResName(frameNode);
    var resName = cachedResName
      ? cachedResName
      : `ic_${Utils.toSnakeCase(frameNode.name)}`;
    var index = 0;
    while (vectorResMap.has(resName) || existingImageDrawableResNames.includes(resName)) {
      index += 1;
      resName = resName + "_" + index;
    }

    let vectorRes = {
      resName: resName,
      nodeId: frameNodeId,
      ldpiBytes: await frameNode.exportAsync({
        format: "PNG",
        colorProfile: "SRGB",
        constraint: { type: "SCALE", value: 0.75 },
      }),
      mdpiBytes: await frameNode.exportAsync({
        format: "PNG",
        colorProfile: "SRGB",
        constraint: { type: "SCALE", value: 1 },
      }),
      hdpiBytes: await frameNode.exportAsync({
        format: "PNG",
        colorProfile: "SRGB",
        constraint: { type: "SCALE", value: 1.5 },
      }),
      xhdpiBytes: await frameNode.exportAsync({
        format: "PNG",
        colorProfile: "SRGB",
        constraint: { type: "SCALE", value: 2 },
      }),
      xxhdpiBytes: await frameNode.exportAsync({
        format: "PNG",
        colorProfile: "SRGB",
        constraint: { type: "SCALE", value: 3 },
      }),
      xxxhdpiBytes: await frameNode.exportAsync({
        format: "PNG",
        colorProfile: "SRGB",
        constraint: { type: "SCALE", value: 4 },
      }),
    };
    vectorResMap.set(resName, vectorRes);
    setResName(frameNode, resName);
  }

  figma.showUI(__html__, { width: 600, height: 600 });
  figma.ui.postMessage({
    msg: "vector-export",
    vectorResArray: Array.from(vectorResMap),
    existingImageDrawableResNames: existingImageDrawableResNames,
  });
}

function getResName(node: SceneNode): string {
  return node.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    IMAGE_REPLACEMENT_RES_NAME
  );
}

function setResName(node: SceneNode, resName: string) {
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    IMAGE_REPLACEMENT_RES_NAME,
    resName
  );
}

function findFrameAncestor(node: PageNode | SceneNode): FrameNode | undefined {
  if (node.parent?.type === "FRAME") {
    return node.parent!!;
  }

  if (node.parent?.type === "GROUP") {
    return findFrameAncestor(node.parent!!);
  }

  return undefined;
}

function isVectorRoot(frameNode: FrameNode): boolean {
  for (const childNode of frameNode.children) {
    if (!isVectorOrTextOrVectorGroup(childNode)) {
      return false;
    }
  }
  return true;
}

// Returns true if the node is a vector node or a group node with vector as descendants.
function isVectorOrTextOrVectorGroup(node: SceneNode): boolean {
  if (node.type === "VECTOR") {
    return true;
  }
  if (node.type === "TEXT") {
    return true;
  }
  if (node.type === "GROUP") {
    return isVectorGroup(node);
  }
  return false;
}

function isVectorGroup(groupNode: GroupNode): boolean {
  for (const childNode of groupNode.children) {
    if (childNode.type === "GROUP") {
      if (!isVectorGroup(childNode)) {
        return false;
      }
    } else if (childNode.type !== "VECTOR" && childNode.type != "TEXT") {
      return false;
    }
  }
  return true;
}
