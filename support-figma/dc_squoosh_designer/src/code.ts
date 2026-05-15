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

// Figplug-UI-Kit: Main thread
// This file holds the main code for the plugin. It has access to the Figma document.
// You can access browser APIs in the <script> tag inside "ui.html" which has a
// full browser environment (see https://www.figma.com/plugin-docs/how-plugins-run).

import { hexToRgb, EPSILON } from "./utils/common";
import { AnimatedNode, SerializedNode, Variant } from "./timeline/types";
import { runSandboxTests } from "./timeline/sandbox_tests";
import { serializeKeyframes, deserializeKeyframes } from "./timeline/serialization";


// Config
const PERSIST_WINDOW_SIZE = true; // Set to false to disable saving window size
const PREFERRED_WIDTH = 320;
const PREFERRED_HEIGHT = 480;
const MIN_WIDTH = 280;
const MIN_HEIGHT = 200;
const MAX_WIDTH = 900;
const MAX_HEIGHT = 800;

import { resolveComponentContext, isDescendantOf } from "./plugin/context";
import { loadFontsForNode } from "./plugin/fonts";
import { serializeNode } from "./plugin/serialize";
import { compareNodes } from "./plugin/compare";
import { cloneChildren, updateFigmaPreview, tagOriginalNodeId } from "./plugin/preview";
let hasSavedSize = false;
let windowSize = { width: PREFERRED_WIDTH, height: PREFERRED_HEIGHT };

let animationNodeId: string | null = null;
let isSelectingPreviewFrame = false;
const isUpdatingPreview = false;

async function updateSelection() {
    if (isSelectingPreviewFrame) {
      const selection = figma.currentPage.selection;
      if (selection.length === 1) {
        animationNodeId = selection[0].id;
        isSelectingPreviewFrame = false;
        figma.ui.postMessage({ type: "selection-mode-ended" });
        figma.ui.postMessage({
          type: "preview-frame-selected",
          name: selection[0].name,
        });
        figma.notify(`Preview frame set to "${selection[0].name}"`);
      }
      return;
    }

    const selection = figma.currentPage.selection;

    // Check if selection is inside the preview frame
    if (animationNodeId && selection.length > 0) {
        const selectedNode = selection[0];
        if (isDescendantOf(selectedNode, animationNodeId)) {
            // Optionally, we could send a message to the UI to highlight the corresponding node in the timeline
            // But for now, we just prevent deselecting/changing the current component context.

            // If the selected node corresponds to an original node, we might want to notify the UI?
            if ("getPluginData" in selectedNode) {
                const originalNodeId = selectedNode.getPluginData("originalNodeId");
                if (originalNodeId) {
                     // We can still notify the UI about which node was clicked in the preview
                     // so it can highlight the corresponding timeline track, WITHOUT resetting the whole timeline view.
                     figma.ui.postMessage({ type: "preview-node-selected", originalNodeId });
                }
            }
            return;
        }
    }

    // Check if selection is a preview node (old logic, can likely be merged or removed if the above covers it)
    // keeping it for safety if animationNodeId is somehow lost or managed differently?
    // Actually, the block above relies on animationNodeId being set.
    // The old block below relies on `originalNodeId` plugin data being present.
    if (selection.length === 1) {
        const node = selection[0];
        if ("getPluginData" in node) {
            const originalNodeId = node.getPluginData("originalNodeId");
            if (originalNodeId) {
              // If we are here, it means we probably didn't catch it with the ancestor check
                 // (maybe animationNodeId is unset but the node still has data?)
                 // In this case, we ALSO want to avoid resetting the timeline.
                 figma.ui.postMessage({ type: "preview-node-selected", originalNodeId });
                 return;
            }
        }
    }

    if (selection.length !== 1) {
      const message = "Please select a single layer.";
      figma.notify(message);
      figma.ui.postMessage({ type: "clear-timeline" });
      return;
    }

    const node = selection[0];
    const { componentSet, singleComponent } = await resolveComponentContext(node);
    let selectedVariantName: string | null = null;

    if (componentSet) {
        if (node.type === "INSTANCE") {
            const main = await (node as InstanceNode).getMainComponentAsync();
            selectedVariantName = main ? main.name : null;
        } else if (node.type === "COMPONENT") {
            selectedVariantName = node.name;
        }
    } else if (singleComponent) {
        selectedVariantName = singleComponent.name;
    }

    if (!componentSet && !singleComponent) {
      let message = `Selected layer "${node.name}" of type "${node.type}" is not part of a component set with variants.`;
      if (node.type === "INSTANCE") {
        message = `Selected instance "${node.name}" is not a variant within a component set.`;
      } else if (node.type === "COMPONENT") {
        message = `Selected component "${node.name}" is not a variant within a component set.`;
      }
      figma.notify(message);
      figma.ui.postMessage({ type: "clear-timeline" });
      return;
    }

    const variants: Variant[] = [];
    const serializedVariants: SerializedNode[] = [];

    let variantsNodeList: SceneNode[] = [];
    let componentName = "";

    if (componentSet) {
        // Reverse the children array to match Figma's UI order
        variantsNodeList = [...componentSet.children].reverse();
        componentName = componentSet.name;
    } else if (singleComponent) {
        variantsNodeList = [singleComponent];
        componentName = singleComponent.name;
    }

    let upgradedVariantsCount = 0;

    for (const variantNode of variantsNodeList) {
      if (variantNode.type === "COMPONENT") {
        const variantName = variantNode.name;
        const animationDataString = variantNode.getSharedPluginData(
          "designcompose",
          "squoosh",
        );
        const animationData = animationDataString ? JSON.parse(animationDataString) : null;
        let upgraded = false;

        if (animationData && animationData.customKeyframeData) {
          for (const [key, value] of Object.entries(animationData.customKeyframeData)) {
            if (typeof value === "string" && !value.startsWith("{") && value.indexOf("|") !== -1) {
              const deserialized = deserializeKeyframes(value);
              animationData.customKeyframeData[key] = serializeKeyframes(
                deserialized.keyframes,
                deserialized.targetEasing
              );
              upgraded = true;
            }
          }
        }

        if (upgraded) {
          variantNode.setSharedPluginData(
            "designcompose",
            "squoosh",
            JSON.stringify(animationData)
          );
          upgradedVariantsCount++;
        }

        variants.push({
          name: variantName,
          animation: animationData,
        });
        serializedVariants.push(serializeNode(variantNode));
      } else {
        console.warn(
          `Skipping unexpected node type in component set children: ${variantNode.type}`,
        );
      }
    }

    if (upgradedVariantsCount > 0) {
      figma.notify(`Upgraded legacy animation data to JSON format for ${upgradedVariantsCount} variant(s).`);
    }

    figma.ui.postMessage({
      type: "update",
      componentName: componentName,
      variants: variants,
      serializedVariants: serializedVariants,
      selectedVariantName: selectedVariantName,
    });
  }

(async () => {
  try {
    if (PERSIST_WINDOW_SIZE) {
      try {
        const savedSize = await figma.clientStorage.getAsync("windowSize");
        if (savedSize && savedSize.width && savedSize.height) {
          hasSavedSize = true;
          windowSize = {
            width: Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, savedSize.width)),
            height: Math.max(
              MIN_HEIGHT,
              Math.min(MAX_HEIGHT, savedSize.height),
            ),
          };
        }
      } catch (err) {
        console.error("Error getting window size:", err);
      }
    }

    figma.showUI(__html__, {
      width: windowSize.width,
      height: windowSize.height,
      themeColors: true,
    });

    figma.ui.onmessage = (msg) => {
      // Log all messages except for the spammy 'update-figma-preview'
      if (msg.type !== "update-figma-preview") {
      }

      if (msg.type === "ready") {
        figma.ui.postMessage({
          type: "init",
          hasSavedSize,
          width: windowSize.width,
          height: windowSize.height,
          persistenceEnabled: PERSIST_WINDOW_SIZE,
        });
      }

      if (msg.type === "ping") {
        (async () => {
            const selection = figma.currentPage.selection;
            if (selection.length > 0) {
                const node = selection[0];
                let componentSet: ComponentSetNode | null = null;

                if (node.type === "INSTANCE") {
                    const main = await node.getMainComponentAsync();
                    if (main && main.parent && main.parent.type === "COMPONENT_SET") {
                        componentSet = main.parent;
                    }
                } else if (node.type === "COMPONENT" && node.parent && node.parent.type === "COMPONENT_SET") {
                     componentSet = node.parent;
                } else if (node.type === "COMPONENT_SET") {
                    componentSet = node;
                }

                if (componentSet) {
                    // Log the first child's data for sampling
                    const firstVariant = componentSet.children[0];
                    const data = firstVariant.getSharedPluginData("designcompose", "squoosh");
                    // Also dump timeline data if available
                    // We don't have easy access to the 'internal' AnimationData structure here as it's built in UI
                    // But we can dump the customKeyframeData which is part of the spec.
                } else {
                }
            } else {
            }
        })();

        figma.ui.postMessage({ type: "pong" });
      }

      if (msg.type === "select-node") {
        (async () => {
          const node = await figma.getNodeByIdAsync(msg.nodeId);
          if (node) {
            figma.currentPage.selection = [node as SceneNode];
          }
        })();
      }

      if (msg.type === "resize") {
        figma.ui.resize(msg.width, msg.height);
      }

      if (msg.type === "run-sandbox-tests") {
        (async () => {
            const results = await runSandboxTests();
            figma.ui.postMessage({ type: "sandbox-tests-complete", results });
        })();
      }

      if (msg.type === "persist-size" && PERSIST_WINDOW_SIZE) {
        try {
          figma.clientStorage.setAsync("windowSize", {
            width: msg.width,
            height: msg.height,
          });
        } catch (err) {
          console.error("Error saving window size:", err);
        }
      }

      if (msg.type === "save-data") {
        (async () => {
          const { frameName, data } = msg;
          const selection = figma.currentPage.selection;
          if (selection.length !== 1) return;

          const node = selection[0];
          const { componentSet, singleComponent } = await resolveComponentContext(node);

          let variant: SceneNode | undefined;
          if (componentSet) {
            variant = componentSet.children.find((child) => child.name === frameName);
          } else if (singleComponent && singleComponent.name === frameName) {
            variant = singleComponent;
          }

          if (variant) {
            variant.setSharedPluginData("designcompose", "squoosh", data);
          }
        })();
      }
      if (msg.type === "save-custom-keyframes") {
        (async () => {
          const { timelineId, endingVariantName, serializedKeyframes } = msg;
          const selection = figma.currentPage.selection;
          if (selection.length !== 1) return;

          const node = selection[0];
          const { componentSet, singleComponent } = await resolveComponentContext(node);

          let variant: SceneNode | undefined;
          if (componentSet) {
            variant = componentSet.children.find((child) => child.name === endingVariantName);
          } else if (singleComponent && singleComponent.name === endingVariantName) {
            variant = singleComponent;
          }

          if (variant) {
            interface AnimationObject {
              customKeyframeData?: { [key: string]: string };
            }
            const existingAnimationData = variant.getSharedPluginData(
              "designcompose",
              "squoosh",
            );
            const animationObject: AnimationObject = existingAnimationData
              ? (JSON.parse(existingAnimationData) as AnimationObject)
              : {};

            if (!animationObject.customKeyframeData) {
              animationObject.customKeyframeData = {};
            }
            animationObject.customKeyframeData[timelineId] =
              serializedKeyframes;

            const dataToSave = JSON.stringify(animationObject);
            variant.setSharedPluginData(
              "designcompose",
              "squoosh",
              dataToSave,
            );
          }
        })();
      }
      if (msg.type === "delete-custom-timeline") {
        (async () => {
          const { timelineId } = msg;
          const selection = figma.currentPage.selection;
          if (selection.length !== 1) return;

          const node = selection[0];
          const { componentSet, singleComponent } = await resolveComponentContext(node);

          let variants: SceneNode[] = [];
          if (componentSet) {
            variants = [...componentSet.children];
          } else if (singleComponent) {
            variants = [singleComponent];
          }

          for (const variant of variants) {
            if (variant.type !== "COMPONENT") continue;
            const existingAnimationData = variant.getSharedPluginData(
              "designcompose",
              "squoosh",
            );
            if (existingAnimationData) {
              interface AnimationObject {
                customKeyframeData?: { [key: string]: string };
              }
              const animationObject: AnimationObject = JSON.parse(
                existingAnimationData,
              );

              if (
                animationObject.customKeyframeData &&
                animationObject.customKeyframeData[timelineId]
              ) {
                delete animationObject.customKeyframeData[timelineId];
                const dataToSave = JSON.stringify(animationObject);
                variant.setSharedPluginData(
                  "designcompose",
                  "squoosh",
                  dataToSave,
                );
              }
            }
          }
        })();
      }
      if (msg.type === "update-node-transform") {
        (async () => {
          const { nodeId, transformDelta, newSize } = msg;
          const node = await figma.getNodeByIdAsync(nodeId);

          if (
            node &&
            "relativeTransform" in node &&
            "rotation" in node &&
            "resize" in node
          ) {
            const transform = node.relativeTransform;
            const newTransform: [
              [number, number, number],
              [number, number, number],
            ] = [
              [transform[0][0], transform[0][1], transform[0][2]],
              [transform[1][0], transform[1][1], transform[1][2]],
            ];
            newTransform[0][2] += transformDelta.dx;
            newTransform[1][2] += transformDelta.dy;
            node.relativeTransform = newTransform;

            node.rotation += -transformDelta.dAngle;
            node.resize(newSize.width, newSize.height);
          } else {
            console.error("Node not found or does not support transformation", {
              nodeId,
              node,
            });
          }
          figma.commitUndo();
        })();
      }
      if (msg.type === "prepare-animation") {
        (async () => {
          const selection = figma.currentPage.selection;
          if (selection.length !== 1) return;

          const node = selection[0];
          const { componentSet, singleComponent } = await resolveComponentContext(node);

          if (!componentSet && !singleComponent) return;

          let primaryTarget: SceneNode | null = null;
          if (node.type === "INSTANCE" || node.type === "COMPONENT") {
             primaryTarget = node;
          } else if (componentSet && componentSet.children.length > 0) {
             primaryTarget = componentSet.children[0];
          } else if (singleComponent) {
             primaryTarget = singleComponent;
          }

          if (primaryTarget) {
              await loadFontsForNode(primaryTarget);
          }

          let frame: FrameNode | ComponentNode | InstanceNode | null = null;
          if (animationNodeId) {
            frame = (await figma.getNodeByIdAsync(animationNodeId)) as
              | FrameNode
              | ComponentNode
              | InstanceNode;
          }

          if (!frame) {
            let defaultFrame = figma.currentPage.findOne(
              (n) => n.name === "PreviewFrame" && n.type === "FRAME",
            ) as FrameNode;
            if (!defaultFrame) {
              defaultFrame = figma.createFrame();
              defaultFrame.name = "PreviewFrame";
              if (primaryTarget && "width" in primaryTarget) {
                  defaultFrame.x = primaryTarget.x + primaryTarget.width + 20;
                  defaultFrame.y = primaryTarget.y;
              }
            }
            frame = defaultFrame;
            animationNodeId = frame.id;
            figma.ui.postMessage({
              type: "preview-frame-selected",
              name: frame.name,
            });
          }

          if (!frame || !("children" in frame)) {
            figma.notify(
              "The selected preview frame is not a valid container (e.g., a frame or component).",
            );
            return;
          }

          frame.children.forEach((child) => child.remove());

          if (primaryTarget && "width" in primaryTarget) {
            frame.resize(primaryTarget.width, primaryTarget.height);
          }

          if ("fills" in frame && primaryTarget && "fills" in primaryTarget) {
            frame.fills = primaryTarget.fills;
          }

          if (componentSet) {
              const mergeChildren = (source: SceneNode, target: FrameNode | ComponentNode | InstanceNode | GroupNode) => {
                  if (!("children" in source) || !("children" in target)) return;

                  // We need to iterate source children and see if they exist in target
                  for (const sourceChild of source.children) {
                      let targetChild = target.findChild((c) => c.name === sourceChild.name);

                      if (!targetChild) {
                          // Clone and add if missing
                          if ("clone" in sourceChild) {
                              targetChild = (sourceChild as any).clone();
                              if (targetChild) {
                                  tagOriginalNodeId(targetChild, sourceChild);
                                  target.appendChild(targetChild);
                              }
                          }
                      }

                      // Recurse to ensure descendants from other variants are merged into this branch
                      // (e.g. if targetChild existed or was just cloned, we still check if sourceChild has MORE descendants)
                      // Note: sourceChild.clone() already brought its current descendants.
                      // But if a *subsequent* variant has *additional* children inside this structure, we need to merge them in.
                      // However, 'source' here IS the variant node.
                      // So we are iterating the variant's structure.
                      mergeChildren(sourceChild, targetChild as FrameNode | ComponentNode | InstanceNode | GroupNode);
                  }
              };

              for (const variant of componentSet.children) {
                  mergeChildren(variant as SceneNode, frame);
              }
          } else if (singleComponent) {
              await cloneChildren(singleComponent.children, frame);
          }
          figma.ui.postMessage({ type: "animation-ready" });
        })();
      }
      if (msg.type === "animate-figma-nodes") {
        (async () => {
          if (!animationNodeId) return;
          const { animatedNodes } = msg;
          await updateFigmaPreview(animationNodeId, animatedNodes);
        })();
      }
      if (msg.type === "update-figma-preview") {
        (async () => {
          if (animationNodeId) {
            const { animatedNodes } = msg;
            await updateFigmaPreview(animationNodeId, animatedNodes);
          }
          figma.ui.postMessage({ type: "preview-update-complete" });
        })();
      }
      if (msg.type === "debug-compare-nodes") {
        (async () => {
          if (!animationNodeId) {
            console.error("Debug Compare: animationNodeId not set.");
            return;
          }
          const previewFrame = await figma.getNodeByIdAsync(animationNodeId);
          if (
            !previewFrame ||
            (previewFrame.type !== "FRAME" &&
              previewFrame.type !== "COMPONENT" &&
              previewFrame.type !== "INSTANCE")
          ) {
            console.error(
              "Debug Compare: PreviewFrame not found or not a frame.",
            );
            return;
          }

          const toVariantData = msg.toVariant;
          const targetVariantNode = await figma.getNodeByIdAsync(
            toVariantData.id,
          );

          if (!targetVariantNode || targetVariantNode.type !== "COMPONENT") {
            console.error(
              "Debug Compare: Target variant node not found or not a component.",
            );
            return;
          }
          await compareNodes(previewFrame, targetVariantNode);
        })();
      }
      if (msg.type === "clear-preview") {
        (async () => {
          if (!animationNodeId) return;
          const frame = await figma.getNodeByIdAsync(animationNodeId);
          if (frame && "children" in frame) {
            frame.children.forEach((child) => child.remove());
          }
          animationNodeId = null;
          figma.ui.postMessage({ type: "preview-frame-cleared" });
        })();
      }
      if (msg.type === "reset-data") {
        (async () => {
          const selection = figma.currentPage.selection;
          if (selection.length !== 1) return;

          const node = selection[0];
          const { componentSet, singleComponent } = await resolveComponentContext(node);

          let variants: SceneNode[] = [];
          if (componentSet) {
            variants = [...componentSet.children];
          } else if (singleComponent) {
            variants = [singleComponent];
          }

          if (variants.length > 0) {
            const defaultAnimation = {
              spec: {
                initial_delay: { secs: 0, nanos: 0 },
                animation: {
                  Smooth: {
                    duration: { secs: 0, nanos: 300000000 },
                    easing: "Linear",
                  },
                },
                interrupt_type: "None",
              },
            };
            for (const variant of variants) {
              if (variant.type === "COMPONENT") {
                variant.setSharedPluginData(
                  "designcompose",
                  "squoosh",
                  JSON.stringify(defaultAnimation),
                );
              }
            }
            figma.notify("All animation data reset to default.");
            updateSelection(); // Reload UI
          }
        })();
      }
      if (msg.type === "select-preview-frame") {
        isSelectingPreviewFrame = !isSelectingPreviewFrame;
        if (isSelectingPreviewFrame) {
          figma.ui.postMessage({ type: "selection-mode-started" });
        } else {
          figma.ui.postMessage({ type: "selection-mode-ended" });
          // If selection is cancelled, and we had a node before, tell the UI
          (async () => {
            if (animationNodeId) {
              const node = await figma.getNodeByIdAsync(animationNodeId);
              if (node) {
                figma.ui.postMessage({
                  type: "preview-frame-selected",
                  name: node.name,
                });
              } else {
                // The node was deleted, so clear it
                animationNodeId = null;
                figma.ui.postMessage({ type: "preview-frame-cleared" });
              }
            }
          })();
        }
      }
    };

    figma.on("selectionchange", () => {
      updateSelection();
    });

    await figma.loadAllPagesAsync();

    figma.on("documentchange", (event) => {
        if (isUpdatingPreview) return; // Ignore programmatic changes

        for (const change of event.documentChanges) {
            if (change.type === "PROPERTY_CHANGE") {
                const node = change.node;

                // Check if node is selected
                const isSelected = figma.currentPage.selection.some(n => n.id === node.id);
                if (!isSelected) continue;

                if ("getPluginData" in node) {
                    const originalNodeId = node.getPluginData("originalNodeId") || null;
                    const serialized = serializeNode(node);
                  figma.ui.postMessage({
                    type: "preview-node-changed",
                    originalNodeId,
                    nodeProps: serialized
                    });
                }
            }
        }
    });

    // Initial update
    updateSelection();
  } catch (e) {
    console.error("Error in main thread:", e);
  }
})();
