/**
 * Copyright 2026 Google LLC
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

import { SerializedNode } from "../timeline/types";

export function serializeNode(node: SceneNode): SerializedNode {
  const result: SerializedNode = {
    id: node.id,
    name: node.name,
    type: node.type,
    x: node.x,
    y: node.y,
  };

  if ("absoluteTransform" in node) {
    result.absoluteTransform = node.absoluteTransform;
  }

  if ("opacity" in node) {
    result.opacity = node.opacity;
  }
  if ("visible" in node) {
    result.visible = node.visible;
  }

  if ("width" in node && "height" in node) {
    result.width = node.width;
    result.height = node.height;
  }

  if ("fills" in node) {
    result.fills = node.fills as readonly Paint[];
  }
  if ("strokes" in node) {
    result.strokes = node.strokes as readonly Paint[];
  }
  if ("strokeWeight" in node && node.strokeWeight !== figma.mixed) {
    result.strokeWeight = node.strokeWeight;
  }

  if ("cornerRadius" in node && node.cornerRadius !== figma.mixed) {
    result.cornerRadius = node.cornerRadius;
  }
  if ("topLeftRadius" in node) {
    result.topLeftRadius = node.topLeftRadius;
  }
  if ("topRightRadius" in node) {
    result.topRightRadius = node.topRightRadius;
  }
  if ("bottomLeftRadius" in node) {
    result.bottomLeftRadius = node.bottomLeftRadius;
  }
  if ("bottomRightRadius" in node) {
    result.bottomRightRadius = node.bottomRightRadius;
  }

  if ("fillGeometry" in node) {
    result.fillGeometry = node.fillGeometry as readonly VectorPath[];
  }

  if (node.type === "ELLIPSE" && "arcData" in node) {
    result.arcData = node.arcData;
  }

  if ("children" in node && node.type !== "INSTANCE") {
    result.children = node.children.map(serializeNode);
  }

  return result;
}
