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

enum DimType {
  DP = "dp",
  PERCENT = "percent",
}

interface Dimension {
  value: number,
  dim: DimType,
}

interface Bounds {
  left: Dimension,
  top: Dimension,
  right: Dimension,
  bottom: Dimension,
}

interface Bounds2 {
  left: number,
  top: number,
  width: number,
  height: number,
}

interface VariantData {
  id: string,
  name: string | null,
  isDefault: boolean,
  bounds: Bounds,
  bounds2: Bounds2 | null,
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
  eventList: Event[],
  keyframeVariants: KeyframeVariant[],
}

function dimToStr(dim: Dimension) {
  return dim.value + (dim.dim == DimType.DP ? ".dp" : "%");
}

function boundsToStr(bounds: Bounds) {
  return "l " + dimToStr(bounds.left) + " t " + dimToStr(bounds.top) + " r " + dimToStr(bounds.right) + " b " + dimToStr(bounds.bottom);
}

function logVariantData(vd: VariantData) {
  Utils.dcLog("  ### id " + vd.id + " default " + vd.isDefault + (vd.bounds ? (" bounds " + boundsToStr(vd.bounds)) : ""));
}

function dimType(dim: string) {
  if (dim == "dp")
    return DimType.DP;
  else
    return DimType.PERCENT;
}

function createDim(value: number, dimStr: string) {
  return {
    value: value,
    dim: dimType(dimStr),
  } as Dimension
}

function createBounds(v: any) {
  return {
    left: createDim(v.leftValue, v.leftDim),
    top: createDim(v.topValue, v.topDim),
    right: createDim(v.rightValue, v.rightDim),
    bottom: createDim(v.bottomValue, v.bottomDim),
  } as Bounds;
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
  }
  else {
    deselectNode();
  }

  /*
  if (node.type == "COMPONENT") {
    Utils.dcLog("### Variant " + node.name);
    //let nodeData = loadNodeData(node);
    const width = node.absoluteBoundingBox?.width;
    const height = node.absoluteBoundingBox?.height;
    figma.ui.postMessage({
      msg: "scalable-select-component",
      nodeName: node.name,
      nodeType: node.type,
      nodeId: node.id,
      nodeWidth: width,
      nodeHeight: height,
      //nodeData: nodeData,
    });
  }
  */
}

function sendComponentSetData(node: ComponentSetNode) {
  const setData = loadComponentSetData(node);
  const variantList = [];
  for (let child of node.children) {
    const variantData = loadVariantData(child);
    variantList.push(variantData);
    logVariantData(variantData);
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
    setData = {
      id: node.id,
      name: node.name,
      role: "",
      eventList: [],
      keyframeVariants: [],
    } as ComponentSetData;
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
  const setData = {
    id: node.id,
    name: node.name,
    role: "",
    eventList: [],
    keyframeVariants: [],
  } as ComponentSetData;
  node.setSharedPluginData(
    Utils.SHARED_PLUGIN_NAMESPACE,
    SCALABLE_PLUGIN_DATA_KEY,
    JSON.stringify(setData)
  );

  let first = true;
  for (const child of node.children) {
    const variantData = newVariantData(child.id, child.name, first);
    first = false;
    child.setSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      SCALABLE_PLUGIN_DATA_KEY,
      JSON.stringify(variantData)
    );
  }
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
  Utils.dcLog("### SET DATA:");
  Utils.dcLog(componentSetDataStr);
  if (setData != null) {
    setData.id = node.id;
    setData.name = node.name;
  } else {
    /*
    setData = {
      id: node.id,
      name: node.name,
      eventList: [],
    } as ComponentSetData;
    */
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
      isDefault: v.isDefault,
      bounds: createBounds(v),
    } as VariantData;
    Utils.dcLog("### Msg variant " + v.name + ", " + v.id + ": " + variantData);
    variantMap.set(v.id, variantData);
  }
  return variantMap;
}

function deselectNode() {
  figma.ui.postMessage({
    msg: "scalable-deselect",
  });
}

const sliderPosWide = 570;
const sliderPosThin = 390;
const sliderPosEmpty = 100;

const mapPosWide = 581;
const mapSizeWide = 619;

const panelSizeWide = 470;

const panelHeight = 649;

export async function sliderChanged(msg: any) {
  let slider = await figma.getNodeByIdAsync("7:20") as FrameNode;
  let map = await figma.getNodeByIdAsync("7:15") as FrameNode;
  let panelInstance = await figma.getNodeByIdAsync("7:11") as InstanceNode;
  let panel = await figma.getNodeByIdAsync("7:11") as FrameNode;
  let panelDragging = await figma.getNodeByIdAsync("38:20") as ComponentNode;

  const diff = (msg.value - 100) / 100 * 470;
  Utils.dcLog("### Slider Changed " + msg.nodeId + ": " + msg.value + " diff " + diff);

  slider.x = sliderPosWide + diff;
  map.x = mapPosWide + diff;
  map.resize(mapSizeWide - diff, map.height)
  panelInstance.swapComponent(panelDragging);
  panel.resize(panelSizeWide + diff, panel.height);
}

export async function sliderReleased(msg: any) {
  let panelWide = await figma.getNodeByIdAsync("2:5") as ComponentNode;
  let panelThin = await figma.getNodeByIdAsync("2:9") as ComponentNode;
  let panelEmpty = await figma.getNodeByIdAsync("2:13") as ComponentNode;
  let panelDragging = await figma.getNodeByIdAsync("38:20") as ComponentNode;

  let slider = await figma.getNodeByIdAsync("7:20") as FrameNode;
  let map = await figma.getNodeByIdAsync("7:15") as FrameNode;
  let panelInstance = await figma.getNodeByIdAsync("7:11") as InstanceNode;
  let panel = await figma.getNodeByIdAsync("7:11") as FrameNode;

  if (slider.x > (sliderPosWide + sliderPosThin) / 2) {
    panelInstance.swapComponent(panelWide);
    panelInstance.resize(panelSizeWide, panelHeight);
    map.resize(mapSizeWide, map.height);
    map.x = mapPosWide;
    slider.x = sliderPosWide;
  } else if (slider.x > (sliderPosThin + sliderPosEmpty) / 2) {
    panelInstance.swapComponent(panelThin);
    panelInstance.resize(panelSizeWide + sliderPosWide - sliderPosThin, panelHeight);
    map.resize(mapSizeWide + sliderPosWide - sliderPosThin, map.height);
    map.x = mapPosWide - sliderPosWide + sliderPosThin;
    slider.x = sliderPosThin;
  } else {
    panelInstance.swapComponent(panelEmpty);
    panelInstance.resize(panelSizeWide + sliderPosWide - sliderPosEmpty, panelHeight);
    map.resize(mapSizeWide + sliderPosWide - sliderPosEmpty, map.height);
    map.x = mapPosWide - sliderPosWide + sliderPosEmpty;
    slider.x = sliderPosEmpty;
  }
}