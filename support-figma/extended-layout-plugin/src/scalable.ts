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

/*
const SHADER_PLUGIN_DATA_KEY = "shader";
const SHADER_FALLBACK_COLOR_PLUGIN_DATA_KEY = "shaderFallbackColor";
const SHADER_UNIFORMS_PLUGIN_DATA_KEY = "shaderUniforms";
// Private plugin data, used for clearing shader functionalities
const SHADER_IMAGE_HASH = "shaderImageHash";
*/

const SCALABLE_PLUGIN_DATA_KEY = "scalableui";

interface VariantData {
  id: string,
  name: string | null,
  isDefault: boolean,
}

interface Event {
  eventName: string,
  variantId: string,
  variantName: string,
}

interface Keyframe {
  frame: number,
  variantName: string,
}

interface KeyframeVariant {
  name: string,
  keyframes: Keyframe[],
}

interface ComponentSetData {
  id: string,
  name: string,
  role: string,
  defaultVariantId: string,
  eventList: Event[],
  keyframeVariants: KeyframeVariant[],
}

function newComponentSetData(id: string, name: string) {
  return {
    id: id,
    name: name,
    role: "",
    defaultVariantId: "",
    eventList: [],
    keyframeVariants: [],
  } as ComponentSetData;
}

export function onSelectionChanged() {
  let selection = figma.currentPage.selection;

  if (!selection || selection.length != 1 || !selection[0]) {
    deselectNode();
    return;
  }

  let node = selection[0];
  if (node.type == "COMPONENT_SET") {
    Utils.dcLog("### Component Set " + node.name);
    sendComponentSetData(node);

    /*
    // TEMPORARY REMOVE THIS

      let setNode = figma.currentPage.selection[0] as ComponentSetNode;
      for (const child of setNode.children) {
        const variantData = loadVariantData(child);
        Utils.dcLog("### Variant " + child.name + ": " + JSON.stringify(variantData));
        child.setSharedPluginData(
          Utils.SHARED_PLUGIN_NAMESPACE,
          SCALABLE_PLUGIN_DATA_KEY,
          JSON.stringify(variantData)
        );
      }
    */
  }
  else {
    deselectNode();
  }
}

function sendComponentSetData(node: ComponentSetNode) {
  const setData = loadComponentSetData(node);
  const variantList = [];
  for (let child of node.children) {
    const variantData = loadVariantData(child);
    variantList.push(variantData);
  }

  figma.ui.postMessage({
    msg: "scalable-select-component-set",
    nodeName: node.name,
    nodeType: node.type,
    nodeId: node.id,
    variantList: variantList,
    setData: setData,
  });
}

export async function createNewEvent(msg: any) {
  const node = figma.currentPage.selection[0] as ComponentSetNode;
  let setData = loadComponentSetData(node);
  if (!setData)
    setData = newComponentSetData(node.id, node.name);
  const event = {
    eventName: msg.event,
    variantId: msg.variantId,
    variantName: msg.variantName,
  } as Event;
  setData.eventList.push(event);
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY,
    JSON.stringify(setData)
  );
  sendComponentSetData(node);
}

export function removeEvent(msg: any) {
  Utils.dcLog("### Remove event " + msg.event);

  const node = figma.currentPage.selection[0] as ComponentSetNode;
  let setData = loadComponentSetData(node);
  if (setData != null)
    setData.eventList = setData.eventList.filter(event => event.eventName !== msg.event);
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY,
    JSON.stringify(setData)
  );
  sendComponentSetData(node);
}

export function createKeyframeVariant(msg: any) {
  Utils.dcLog("### Add Keyframe Variant " + msg.name + " with " + msg.keyframes.length + " keys");

  const node = figma.currentPage.selection[0] as ComponentSetNode;
  let setData = loadComponentSetData(node);
  if (!setData)
    setData = newComponentSetData(node.id, node.name);

  const keyframes = [];
  for (let i = 0; i < msg.keyframes.length; ++i) {
    const kf = msg.keyframes[i];
    keyframes.push({
        frame: kf.frame,
        variantName: kf.variant
      } as Keyframe
    );
  }

  const keyframeVariant = {
    name: msg.name,
    keyframes: keyframes
  } as KeyframeVariant;

  setData.keyframeVariants.push(keyframeVariant);

  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY,
    JSON.stringify(setData)
  );
  sendComponentSetData(node);
}

export function removeKeyframeVariant(msg: any) {
  Utils.dcLog("### Remove Keyframe Variant " + msg.keyframeVariant);

  const node = figma.currentPage.selection[0] as ComponentSetNode;
  let setData = loadComponentSetData(node);
  if (setData != null)
    setData.keyframeVariants = setData.keyframeVariants.filter(kfv => kfv.name != msg.keyframeVariant);
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY,
    JSON.stringify(setData)
  );
  sendComponentSetData(node);
}

export function roleChanged(msg: any) {
  Utils.dcLog("### Set role " + msg.role);

  const node = figma.currentPage.selection[0] as ComponentSetNode;
  let setData = loadComponentSetData(node);
  if (setData != null)
    setData.role = msg.role;
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY,
    JSON.stringify(setData)
  );
  sendComponentSetData(node);
}

export function addNode() {
  console.log("### Adding node");
  let node = figma.currentPage.selection[0] as ComponentSetNode;
  const setData = newComponentSetData(node.id, node.name);

  let first = true;
  for (const child of node.children) {
    const variantData = newVariantData(child.id, child.name, first);
    if (first)
      setData.defaultVariantId = child.id;
    first = false;
    child.setSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      SCALABLE_PLUGIN_DATA_KEY,
      JSON.stringify(variantData)
    );
  }
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY,
    JSON.stringify(setData)
  );
  sendComponentSetData(node);
}

function newVariantData(id: string, name: string, isDefault: boolean) {
  return {
    id: id,
    name: name,
    isDefault: isDefault,
  } as VariantData;
}

export function removeNode() {
  console.log("### Removing node");
  const node = figma.currentPage.selection[0];
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY, ""
  );

  const setNode = node as ComponentSetNode;
  if (setNode != null) {
    for (const child of setNode.children) {
      child.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SCALABLE_PLUGIN_DATA_KEY, ""
      );
    }
  }

  sendComponentSetData(node as ComponentSetNode);
}

function replaceNum(key: string, value: any) {
  if (key == "value") {
    return parseFloat(value);
  }
  return value;
}

export async function nodeChanged(msg: any) {
  const variantMap = hashToVariantMap(msg.variantList);
  for (let [id, variant] of variantMap) {
    if (variant.isDefault) {
      const setNode = figma.currentPage.selection[0] as ComponentSetNode;
      let setData = loadComponentSetData(setNode);
      if (setData != null)
        setData.defaultVariantId = id;
      setNode.setSharedPluginData(
        Utils.SHARED_PLUGIN_NAMESPACE,
        SCALABLE_PLUGIN_DATA_KEY,
        JSON.stringify(setData)
      );
    }
    Utils.dcLog("### Saving to " + id + ": " + JSON.stringify(variant, replaceNum));
    let node = await figma.getNodeByIdAsync(id) as BaseNode;
    node.setSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      SCALABLE_PLUGIN_DATA_KEY,
      JSON.stringify(variant, replaceNum)
    );
  }
}

function loadComponentSetData(node: ComponentSetNode) {
  let componentSetDataStr = node.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY
  );
  let setData: ComponentSetData | null = null;
  if (componentSetDataStr)
    setData = JSON.parse(componentSetDataStr) as ComponentSetData;
  Utils.dcLog("### Loaded set: " + componentSetDataStr);
  if (setData != null) {
    setData.id = node.id;
    setData.name = node.name;
    if (setData.eventList == null)
      setData.eventList = [];
    if (setData.keyframeVariants == null)
      setData.keyframeVariants = [];
  }
  return setData;
}

function loadVariantData(node: SceneNode) {
  // Try to load existing data on this node
  let variantDataStr = node.getSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY
  );
  let variantData: VariantData | null = null;
  if (variantDataStr) {
    variantData = JSON.parse(variantDataStr) as VariantData;
    variantData.id = node.id;
    variantData.name = node.name;
  } else {
    variantData = {
      id: node.id,
      name: node.name,
    } as VariantData;
  }
  return variantData;
}

function hashToVariantMap(variantList: any) {
  const variantMap: Map<string, VariantData> = new Map();
  for (const v of variantList) {
    const variantData = {
      id: v.id,
      name: v.name,
      isDefault: v.isDefault,
    } as VariantData;
    variantMap.set(v.id, variantData);
  }
  return variantMap;
}

function deselectNode() {
  figma.ui.postMessage({
    msg: "scalable-deselect",
  });
}
