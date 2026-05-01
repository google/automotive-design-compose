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

export async function resolveComponentContext(node: SceneNode) {
  let componentSet: ComponentSetNode | null = null;
  let singleComponent: ComponentNode | null = null;

  if (node.type === "INSTANCE") {
    const mainComponent = await node.getMainComponentAsync();
    if (mainComponent?.parent?.type === "COMPONENT_SET") {
      componentSet = mainComponent.parent;
    } else if (mainComponent) {
      singleComponent = mainComponent;
    }
  } else if (node.type === "COMPONENT_SET") {
    componentSet = node;
  } else if (node.type === "COMPONENT") {
    if (node.parent?.type === "COMPONENT_SET") {
      componentSet = node.parent;
    } else {
      singleComponent = node;
    }
  }
  return { componentSet, singleComponent };
}

export function isDescendantOf(node: SceneNode, ancestorId: string): boolean {
  let current: BaseNode | null = node;
  while (current) {
    if (current.id === ancestorId) {
      return true;
    }
    current = current.parent;
  }
  return false;
}
