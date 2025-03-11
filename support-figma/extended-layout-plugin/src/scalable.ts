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
  layer: number,
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

export async function onSelectionChanged() {
  let selection = figma.currentPage.selection;

  if (!selection || selection.length != 1 || !selection[0]) {
    deselectNode();
    return;
  }

  let node = selection[0];
  if (node.type == "COMPONENT_SET") {
    Utils.dcLog("### Component Set " + node.name);
    sendComponentSetData(node);
  } else {
    let eventSet: Set<string> = new Set();
    await getAllEvents(node, eventSet);
    if (eventSet.size > 0) {
      sendStageData(eventSet);
    } else {
      deselectNode();
    }
  }
}

async function sendStageData(eventSet: Set<string>) {
  const eventList = Array.from(eventSet);
  figma.ui.postMessage({
    msg: "scalable-select-stage",
    eventList: eventList,
  });
}

async function getAllEvents(node: SceneNode, eventSet: Set<string>) {
  if (node.type == "INSTANCE") {
    const instanceNode = node as InstanceNode;
    const setData = await getComponentSetDataFromInstance(instanceNode);
    if (setData != null && setData.eventList != null) {
      for (const event of setData.eventList) {
        eventSet.add(event.eventName);
      }
    }
  }

  const parentNode = node as ChildrenMixin;
  if (parentNode != null && parentNode.children != null) {
    for (const child of parentNode.children)
      await getAllEvents(child, eventSet);
  }
}

async function getComponentSetDataFromInstance(node: InstanceNode) {
  const mainComponent = await node.getMainComponentAsync();
  const componentSet = mainComponent?.parent;

  if (componentSet != null) {
    const scalableData = componentSet.getSharedPluginData(
      Utils.SHARED_PLUGIN_NAMESPACE,
      SCALABLE_PLUGIN_DATA_KEY
    );
    let setData: ComponentSetData | null = null;
    if (scalableData) {
      setData = JSON.parse(scalableData) as ComponentSetData;
      return setData;
    }
  }
  return null;
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

export function sendEvent(msg: any) {
  Utils.dcLog("### Send Event " + msg.event);
  simulateEvent(figma.currentPage.selection[0], msg.event);
  //testAnim();
}

interface Bounds {
  left: number,
  top: number,
  width: number,
  height: number,
}

interface AnimData {
  bounds: Bounds,
  visible: boolean,
  alpha: number,
}

async function testAnim() {
  const mapFromInstance = await figma.getNodeByIdAsync("223:42");
  const mapToComponent = await figma.getNodeByIdAsync("218:71");

  if (mapFromInstance != null && mapToComponent != null) {
    const fromInstance = mapFromInstance as InstanceNode;
    const fromComponent = await fromInstance.getMainComponentAsync() as ComponentNode;
    const toComponent = mapToComponent as ComponentNode;
    const fromChild = fromComponent.findChild((child: SceneNode) => child.name == "main") as FrameNode;
    const toChild = toComponent.findChild((child: SceneNode) => child.name == "main") as FrameNode;
    if (fromChild != null && toChild != null) {
      startAnim(fromChild, toChild, fromInstance, toComponent);
    }
  }
}

function startAnim(fromChild: FrameNode, toChild: FrameNode, fromInstance: InstanceNode, toComponent: ComponentNode) {
  const fromBounds = {
    left: fromChild.x,
    top: fromChild.y,
    width: Math.max(fromChild.width, 0.01), // Figma width/height must be at least 0.01
    height: Math.max(fromChild.height, 0.01),
  } as Bounds;
  const fromAnimData = {
    bounds: fromBounds,
    visible: fromChild.visible,
    alpha: fromChild.opacity,
  }
  const toBounds = {
    left: toChild.x,
    top: toChild.y,
    width: Math.max(toChild.width, 0.01),
    height: Math.max(toChild.height, 0.01),
  } as Bounds;
  const toAnimData = {
    bounds: toBounds,
    visible: toChild.visible,
    alpha: toChild.opacity,
  }

  const duration = 500;
  let startTime = Date.now();
  let index = 0;
  let intervalId = setInterval(() => {
    const timeNow = Date.now();
    ++index;
    const linearProgress = (timeNow - startTime) / duration;
    const value = easeOutCubic(linearProgress);
    animateFrame(fromChild, fromAnimData, toAnimData, value);
    if (timeNow > startTime + duration) {
      clearInterval(intervalId);
      endAnim(fromChild, fromAnimData, fromInstance, toComponent)
    }
  });
  //endAnim(fromChild, fromAnimData, fromInstance, toComponent);
}

function endAnim(node: FrameNode, fromAnimData: AnimData, fromInstance: InstanceNode, toComponent: ComponentNode) {
  // Set the instance to the new component
  fromInstance.mainComponent = toComponent;

  // Undo changes to the component
  const width = Math.max(fromAnimData.bounds.width, 0.01);
  const height = Math.max(fromAnimData.bounds.height, 0.01);
  node.resize(width, height);
  node.x = fromAnimData.bounds.left;
  node.y = fromAnimData.bounds.top;

  node.visible = fromAnimData.visible;
  node.opacity = fromAnimData.alpha;
}

function easeOutCubic(x: number): number {
  return 1 - Math.pow(1 - x, 3);
}

function animateFrame(node: FrameNode, from: AnimData, to: AnimData, value: number) {
  const left = to.bounds.left * value + from.bounds.left * (1 - value);
  const top = to.bounds.top * value + from.bounds.top * (1 - value);
  const width = Math.max(to.bounds.width * value + from.bounds.width * (1 - value), 0.01);
  const height = Math.max(to.bounds.height * value + from.bounds.height * (1 - value), 0.01);
  node.resize(width, height);
  node.x = left;
  node.y = top;

  const visible = to.visible;
  const alpha = Math.max(Math.min(to.alpha * value + from.alpha * (1 - value), 0), 1);
  node.visible = visible;
  node.opacity = alpha;
}

async function simulateEvent(node: SceneNode, eventName: string) {
  if (node.type == "INSTANCE") {
    const instanceNode = node as InstanceNode;
    const setData = await getComponentSetDataFromInstance(instanceNode);
    if (setData != null && setData.eventList != null) {
      for (const event of setData.eventList) {
        if (event.eventName == eventName) {
          Utils.dcLog("### Execute " + node.name + " -> " + event.variantName);
          const toNode = await figma.getNodeByIdAsync(event.variantId);

          if (instanceNode != null && toNode != null) {
            const fromComponent = await instanceNode.getMainComponentAsync() as ComponentNode;
            const toComponent = toNode as ComponentNode;
            const fromChild = fromComponent.findChild((child: SceneNode) => child.name == "main") as FrameNode;
            const toChild = toComponent.findChild((child: SceneNode) => child.name == "main") as FrameNode;
            if (fromChild != null && toChild != null) {
              startAnim(fromChild, toChild, instanceNode, toComponent);
            }
          }
        }
      }
    }
  }

  const parentNode = node as ChildrenMixin;
  if (parentNode != null && parentNode.children != null) {
    for (const child of parentNode.children)
      await simulateEvent(child, eventName);
  }
}

export function resetState(msg: any) {
  Utils.dcLog("### Reset State");
  resetInstance(figma.currentPage.selection[0]);
}

async function resetInstance(node: SceneNode) {
  if (node.type == "INSTANCE") {
    const instanceNode = node as InstanceNode;
    const setData = await getComponentSetDataFromInstance(instanceNode);
    if (setData != null && setData.defaultVariantId != null) {
      const toNode = await figma.getNodeByIdAsync(setData.defaultVariantId);
      if (toNode?.type == "COMPONENT") {
        instanceNode.mainComponent = toNode;
      }
    }
  }

  const parentNode = node as ChildrenMixin;
  if (parentNode != null && parentNode.children != null) {
    for (const child of parentNode.children)
      await resetInstance(child);
  }
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
    layer: 0,
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
      layer: v.layer,
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
