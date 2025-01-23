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

export function onSelectionChanged() {
  let selection = figma.currentPage.selection;

  if (selection && selection.length == 1 && selection[0]) {
    let node = selection[0];
    figma.ui.postMessage({
      msg: "scalable-selection",
      nodeId: node.id,
    });
  }
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