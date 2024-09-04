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

const IMAGE_HASH_TO_RES_KEY = "image_hash_to_res";

// Hash to image bytes map
const imageHashToBytesMap = new Map<string, Uint8Array>();
// Hash to nodes array map
const imageHashToNodesMap = new Map<string, Array<string>>();
// Hash to res name map
const imageHashToResMap = new Map<string, string>();
// Res name to image hash map, this will be used to look up a unique res name.
const resToImageHashMap = new Map<string, string>();
// Hash to excluded state map
const exportedImageHashArray = new Array<string>();

export async function exportAllImagesAsync() {
  await figma.loadAllPagesAsync();

  imageHashToBytesMap.clear();
  imageHashToNodesMap.clear();
  imageHashToResMap.clear();
  resToImageHashMap.clear();
  exportedImageHashArray.length = 0;

  let cachedHashToResMap = loadExportedImages();
  for (const [key, _] of cachedHashToResMap) {
    exportedImageHashArray.push(key);
  }
  let cachedHashToResMapExcluded = loadNonExportedImages();
  cachedHashToResMapExcluded.forEach((resName, imageHash) =>
    cachedHashToResMap.set(imageHash, resName)
  );

  for (let page of figma.root.children) {
    for (let child of page.children) {
      await exportNodeImagesAsync(child, cachedHashToResMap);
    }
  }
  save();

  figma.showUI(__html__, { width: 600, height: 600 });
  figma.ui.postMessage({
    msg: "image-export",
    imageBytesArray: Array.from(imageHashToBytesMap),
    imageNodesArray: Array.from(imageHashToNodesMap),
    imageResNameArray: Array.from(imageHashToResMap),
    exportedImageHashArray: exportedImageHashArray,
  });
}

export function updateResName(imageHash: string, resName: string) {
  if (!resToImageHashMap.has(resName)) {
    const oldName = imageHashToResMap.get(imageHash);
    if (oldName) {
      resToImageHashMap.delete(oldName);
    }
    imageHashToResMap.set(imageHash, resName);
    resToImageHashMap.set(resName, imageHash);
    save();
    figma.notify("Res name successfully updated....");
  } else {
    figma.notify("Res name already exists. Reverting....");
    figma.ui.postMessage({
      msg: "image-export",
      imageBytesArray: Array.from(imageHashToBytesMap),
      imageNodesArray: Array.from(imageHashToNodesMap),
      imageResNameArray: Array.from(imageHashToResMap),
      exportedImageHashArray: exportedImageHashArray,
    });
  }
}

async function exportNodeImagesAsync(
  node: SceneNode,
  cachedHashToResMap: Map<string, string>
) {
  let resName = `ic_${Utils.toSnakeCase(node.name)}`;

  let nodeWithFills: MinimalFillsMixin = node as MinimalFillsMixin;
  if (nodeWithFills) {
    let fills = nodeWithFills.fills as ReadonlyArray<Paint>;
    if (fills) {
      for (const paint of fills) {
        if (paint.type == "IMAGE") {
          await processImagePaint(paint, node, resName, cachedHashToResMap);
        }
      }
    }
  }

  let nodeWithStrokes: MinimalStrokesMixin = node as MinimalStrokesMixin;
  if (nodeWithStrokes) {
    let strokes = nodeWithStrokes.strokes as readonly Paint[];
    if (strokes) {
      for (const paint of strokes) {
        if (paint.type == "IMAGE") {
          await processImagePaint(paint, node, resName, cachedHashToResMap);
        }
      }
    }
  }
  // Recurse into any children.
  let maybeParent = node as ChildrenMixin;
  if (maybeParent.children) {
    for (let child of maybeParent.children) {
      await exportNodeImagesAsync(child, cachedHashToResMap);
    }
  }
}

async function processImagePaint(
  paint: ImagePaint,
  node: SceneNode,
  preferredResName: string,
  cachedHashToResMap: Map<string, string>
) {
  if (paint.imageHash) {
    const imageBytes = await figma
      .getImageByHash(paint.imageHash)
      ?.getBytesAsync();
    if (imageBytes) {
      imageHashToBytesMap.set(paint.imageHash, imageBytes);
      if (imageHashToNodesMap.has(paint.imageHash)) {
        imageHashToNodesMap.get(paint.imageHash)?.push(node.id);
      } else {
        imageHashToNodesMap.set(paint.imageHash, [node.id]);
      }

      if (imageHashToResMap.has(paint.imageHash)) {
        return;
      }

      const cachedResName = cachedHashToResMap
        ? cachedHashToResMap.get(paint.imageHash)
        : undefined;
      var resName = cachedResName ? cachedResName : preferredResName;
      var index = 0;
      while (resToImageHashMap.has(resName)) {
        index += 1;
        resName = resName + "_" + index;
      }
      imageHashToResMap.set(paint.imageHash, resName);
      resToImageHashMap.set(resName, paint.imageHash);
    }
  }
}

export function clear() {
  figma.root.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    IMAGE_HASH_TO_RES_KEY,
    ""
  );
  figma.root.setPluginData(IMAGE_HASH_TO_RES_KEY, "");
  figma.closePlugin("Image res data has been cleared.");
}

export function excludeAnImage(imageHash: string) {
  const index = exportedImageHashArray.indexOf(imageHash);
  if (index != -1) {
    exportedImageHashArray.splice(index, 1);
    save();
    figma.notify(`Image ${imageHash} now will not be exported...`);
  } else {
    figma.notify(
      `Image ${imageHash} is already ignored from exporting, no action...`
    );
  }
}

export function includeAnImage(imageHash: string) {
  if (!exportedImageHashArray.includes(imageHash)) {
    exportedImageHashArray.push(imageHash);
    save();
    figma.notify(`Image ${imageHash} now will be exported...`);
  } else {
    figma.notify(`Image ${imageHash} is already being exported, no action...`);
  }
}

function save() {
  const exportedImagesJson: { [key: string]: string } = {};
  const nonExportedImagesJson: { [key: string]: string } = {};
  imageHashToResMap.forEach((resName, hash) => {
    if (exportedImageHashArray.includes(hash)) {
      exportedImagesJson[hash] = resName;
    } else {
      nonExportedImagesJson[hash] = resName;
    }
  });

  figma.root.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    IMAGE_HASH_TO_RES_KEY,
    JSON.stringify(exportedImagesJson)
  );
  figma.root.setPluginData(
    IMAGE_HASH_TO_RES_KEY,
    JSON.stringify(nonExportedImagesJson)
  );
}

function loadExportedImages(): Map<string, string> {
  const imageHashToResNameData = figma.root.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    IMAGE_HASH_TO_RES_KEY
  );

  return parseCachedImageHashToResMap(imageHashToResNameData);
}

// Private plugin data saves those images excluded from importing
function loadNonExportedImages(): Map<string, string> {
  const imageHashToResNameData = figma.root.getPluginData(
    IMAGE_HASH_TO_RES_KEY
  );
  return parseCachedImageHashToResMap(imageHashToResNameData);
}

function parseCachedImageHashToResMap(imageHashToResNameData?: string) {
  let imageHashToResPairs: { [key: string]: string } = imageHashToResNameData
    ? JSON.parse(imageHashToResNameData)
    : {};

  let cachedImageHashToResMap = new Map<string, string>(
    Object.entries(imageHashToResPairs)
  );
  return cachedImageHashToResMap;
}

export function loadImageDrawableResNames(): Array<string> {
  let imageResNames = new Array<string>();
  let cachedHashToResMap = loadExportedImages();
  cachedHashToResMap.forEach((resName, _) => imageResNames.push(resName));
  let cachedHashToResMapExcluded = loadNonExportedImages();
  cachedHashToResMapExcluded.forEach((resName, _) => imageResNames.push(resName));
  return imageResNames;
}
