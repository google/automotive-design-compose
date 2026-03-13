/* eslint-disable @typescript-eslint/no-explicit-any */
import {
  AnimationData,
  SerializedNode,
  Variant,
  KeyframeTime,
  Timeline,
  Keyframe,
  Node,
  ArcData,
  AnimatedNodeProps,
  DecomposedTransform,
  AnimationNode,
  AnimatedNode
} from "../timeline/types";
import { rgbToHex, decomposeMatrix, invertMatrix, multiplyMatrix } from "../utils/common";
import { deserializeKeyframes } from "../timeline/serialization";
import { getAnimationSegment } from "../timeline/utils";
// Remove PlaybackController dependency

/**
 * Service responsible for transforming Figma variant data into animation-ready structures.
 * It handles calculating keyframe timings, building the timeline data model, and extracting node properties.
 */
export class DataMapper {
  // Using a static instance or injecting the controller if needed for decomposition helper.
  // Alternatively, move decomposeMatrix to a static helper class.


  /**
   * Calculates the timeline positions for each variant based on animation specs.
   * @param variants The list of variants.
   * @returns An object containing the keyframe times and the total duration.
   */
  static calculateKeyframeData(variants: Variant[]) {
    let currentTime = 0;
    const keyframeTimes: KeyframeTime[] = [];
    
    if (!variants || variants.length === 0) {
        return { keyframeTimes: [], totalTime: 1 };
    }

    variants.forEach((variant, index) => {
      keyframeTimes.push({
        time: currentTime,
        name: variant.name,
        index: index,
      });

      const animSourceVariant =
        index < variants.length - 1
          ? variants[index + 1]
          : variants[0];

      if (animSourceVariant && variants.length > 1) {
        const anim = animSourceVariant.animation;
        if (anim && anim.spec) {
          let delay = 0;
          if (anim.spec.initial_delay) {
            delay =
              (anim.spec.initial_delay.secs || 0) +
              (anim.spec.initial_delay.nanos || 0) / 1e9;
          }
          let duration = 0;
          if (anim.spec.animation && anim.spec.animation.Smooth) {
            const d = anim.spec.animation.Smooth.duration;
            duration = (d.secs || 0) + (d.nanos || 0) / 1e9;
          }
          currentTime += delay + duration;
        }
      }
    });
    const totalTime = currentTime > 0 ? currentTime : 1;
    // Always add a loop keyframe, even for single variants, so the timeline has a clear end point.
    // For single variants, this will simply create a loop back to the same (only) variant.
    keyframeTimes.push({
      time: totalTime,
      name: variants[0].name,
      index: 0,
      isLoop: true,
    });
    return { keyframeTimes, totalTime };
  }

  /**
   * Populates a timeline with locked keyframes derived from variant data.
   */
  private static populateVariantKeyframes(
      timeline: Timeline,
      keyframeTimes: KeyframeTime[],
      values: (number | string | boolean | undefined)[],
      totalTime: number,
      nodePresence: boolean[],
      propName: string,
      nodeId: string
  ) {
      keyframeTimes.forEach((kt: KeyframeTime) => {
        const vIndex = kt.index;
        let value = values[vIndex];
        let isMissing = false;

        if (!nodePresence[vIndex]) {
            isMissing = true;
            if (propName === "opacity") {
                value = 0;
            } else {
                // Look ahead for next valid value
                for (let i = vIndex + 1; i < values.length; i++) {
                    if (values[i] !== undefined) {
                        value = values[i];
                        break;
                    }
                }
                if (value === undefined) {
                        // Look behind
                        for (let i = vIndex - 1; i >= 0; i--) {
                            if (values[i] !== undefined) {
                                value = values[i];
                                break;
                            }
                        }
                }
                if (value === undefined) value = 0; // Fallback
            }
        } else if (value === undefined) {
            // Node present but property undefined (e.g. fill removed).
            // Default to 0 for numeric properties to ensure continuity (e.g. fade out).
            if (
                propName.endsWith("opacity") ||
                propName.endsWith("width") ||
                propName.endsWith("height") ||
                propName.endsWith("Radius") ||
                propName.endsWith("Weight")
            ) {
                value = 0;
            }
        }

        if (value !== undefined) {
            const keyframeId = kt.isLoop
            ? `${nodeId}-${propName}-loop`
            : `${nodeId}-${propName}-${vIndex}`;
            const keyframe: Keyframe = {
            id: keyframeId,
            position: kt.time / totalTime,
            value: value,
            locked: true,
            isMissing: isMissing,
            easing: "Inherit",
            };

            timeline.keyframes.push(keyframe);
        }
      });
  }

  /**
   * Transforms raw variant data into the internal `AnimationData` structure used by the timeline.
   * This involves creating timelines for changed properties and handling custom keyframes.
   * @param variants The list of variants with animation specs.
   * @param serializedVariants The serialized node data for each variant.
   * @returns The structured `AnimationData`.
   */
  static transformDataToAnimationData(
    variants: Variant[],
    serializedVariants: SerializedNode[],
  ): AnimationData {
    console.log("transformDataToAnimationData inputs:", {
      variantsLength: variants ? variants.length : 0,
      serializedVariantsLength: serializedVariants ? serializedVariants.length : 0,
    });

    if (
      !variants ||
      variants.length === 0 ||
      !serializedVariants ||
      serializedVariants.length === 0
    ) {
      console.warn("transformDataToAnimationData: variants or serializedVariants is empty.");
      return { nodes: [], duration: 1 };
    }

    const { keyframeTimes, totalTime } = this.calculateKeyframeData(variants);

    // Build a superset tree of nodes from ALL variants
    const nodesMap = new Map<string, Node>();

    const getOrCreateNode = (id: string, name: string, figmaId: string): Node => {
        if (!nodesMap.has(id)) {
            nodesMap.set(id, {
                id,
                figmaId,
                name,
                children: [],
                timelines: [],
            });
        }
        return nodesMap.get(id)!;
    };

    serializedVariants.forEach((variantRoot) => {
        const traverse = (node: SerializedNode, parentId: string | null) => {
            let effectiveId = node.name;
            if (parentId === null) effectiveId = "__ROOT__";

            const timelineNode = getOrCreateNode(effectiveId, node.name, node.id);

            if (parentId) {
                const parentNode = nodesMap.get(parentId);
                if (parentNode) {
                    // Check if already added to children to avoid duplicates
                    if (!parentNode.children.find((c) => c.id === effectiveId)) {
                        parentNode.children.push(timelineNode);
                    }
                }
            }

            if (node.children) {
                node.children.forEach((child) => traverse(child, effectiveId));
            }
        };
        traverse(variantRoot, null);
    });

    const figmaVariantRootNode = nodesMap.get("__ROOT__");
    if (!figmaVariantRootNode) {
         console.warn("transformDataToAnimationData: __ROOT__ node not found in nodesMap.");
         return { nodes: [], duration: 1 };
    }

    const variantChangeTimeline: Timeline = {
      id: "variant-change",
      property: "variant-change",
      keyframes: [],
    };
    keyframeTimes.forEach((kt: KeyframeTime) => {
      const keyframeId = kt.isLoop
        ? `variant-change-loop`
        : `variant-change-${kt.index}`;
      variantChangeTimeline.keyframes.push({
        id: keyframeId,
        position: kt.time / totalTime,
        value: kt.index,
        locked: true,
      });
    });
    figmaVariantRootNode.timelines.push(variantChangeTimeline);

    const _findNode = (nodes: Node[], id: string): Node | null => {
      for (const node of nodes) {
        if (node.id === id) return node;
        const found = _findNode(node.children, id);
        if (found) return found;
      }
      return null;
    };

    const flattenNodes = (nodes: Node[]): Node[] => {
      let flat: Node[] = [];
      for (const node of nodes) {
        flat.push(node);
        flat = flat.concat(flattenNodes(node.children));
      }
      return flat;
    };

    const allNodes = flattenNodes([figmaVariantRootNode]);

    allNodes.forEach((node) => {
      const properties: {
        [key: string]: (number | string | boolean | undefined)[];
      } = {};
      const nodePresence: boolean[] = [];

      serializedVariants.forEach((variant: SerializedNode, vIndex) => {
        const variantNodesMap = new Map();
        // SerializedNode is strictly not AnimationNode but structurally similar enough here.
        // We cast to avoid TS errors as we know the structure matches for what collectAnimationNodes needs.
        this.collectAnimationNodes(variant as unknown as AnimationNode, null, true, variantNodesMap);
        const figmaNode = variantNodesMap.get(node.id);

        if (figmaNode) {
          nodePresence[vIndex] = true;
          const props = [
            "x",
            "y",
            "rotation",
            "width",
            "height",
            "opacity",
            "topLeftRadius",
            "topRightRadius",
            "bottomLeftRadius",
            "bottomRightRadius",
            "arcData",
            "strokeWeight"
          ];
          props.forEach(p => {
              if (!properties[p]) properties[p] = [];
              properties[p][vIndex] = this.getNodePropertyValue(figmaNode, p); // eslint-disable-line @typescript-eslint/no-explicit-any
          });

          if (figmaNode.fills && Array.isArray(figmaNode.fills)) {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (figmaNode.fills as any[]).forEach((fill: unknown, i: number) => {
              // We need to check all potential properties for fills
              const fillProps = [
                  `fills.${i}.solid`,
                  `fills.${i}.solid.opacity`,
                  `fills.${i}.gradient.positions`,
                  `fills.${i}.gradient.stops`,
                  `fills.${i}.gradient.opacity`,
                  `fills.${i}.opacity`,
                  `fills.${i}.arc`
              ];
                            fillProps.forEach(p => {
                                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                const val = this.getNodePropertyValue(figmaNode, p);
                                if (val !== 0 && val !== undefined) { 
                                    if (!properties[p]) properties[p] = [];
                                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                    properties[p][vIndex] = val;
                                }
                            });            });
          }

          if (figmaNode.strokes && Array.isArray(figmaNode.strokes)) {
             // eslint-disable-next-line @typescript-eslint/no-explicit-any
             (figmaNode.strokes as any[]).forEach((stroke: unknown, i: number) => {
                const strokeProps = [
                    `strokes.${i}.solid`,
                    `strokes.${i}.opacity`,
                    `strokes.${i}.arc`
                ];
                 strokeProps.forEach(p => {
                  const val = this.getNodePropertyValue(figmaNode, p);
                  if (val !== 0 && val !== undefined) {
                      if (!properties[p]) properties[p] = [];
                      properties[p][vIndex] = val; // eslint-disable-line @typescript-eslint/no-explicit-any
                  }
              });
             });
          }
        } else {
            nodePresence[vIndex] = false;
        }
      });

      for (const [propName, values] of Object.entries(properties)) {
        const isAnimated = values.some((v) => v !== values[0]);
        if (isAnimated) {
          const timeline: Timeline = {
            id: `${node.id}-${propName}`,
            property: propName,
            keyframes: [],
          };

          this.populateVariantKeyframes(timeline, keyframeTimes, values, totalTime, nodePresence, propName, node.id);
          node.timelines.push(timeline);
        }
      }

      variants.forEach((variant) => {
        if (variant.animation?.customKeyframeData) {
          for (const timelineId in variant.animation.customKeyframeData) {
            if (timelineId.startsWith(`${node.id}-`)) {
              const propName = timelineId.substring(node.id.length + 1);
              if (!node.timelines.some((t) => t.property === propName)) {
                const timeline: Timeline = {
                  id: timelineId,
                  property: propName,
                  keyframes: [],
                  isCustom: true,
                };
                
                const values = properties[propName];
                if (values) {
                    this.populateVariantKeyframes(timeline, keyframeTimes, values, totalTime, nodePresence, propName, node.id);
                }
                
                node.timelines.push(timeline);
              }
            }
          }
        }
      });
    });

    allNodes.forEach((node) => {
      node.timelines.forEach((timeline) => {
        if (timeline.id === "variant-change") return;

        for (let i = 0; i < keyframeTimes.length - 1; i++) {
          const segment = getAnimationSegment(
            keyframeTimes[i].time / totalTime,
            keyframeTimes,
            totalTime,
            variants,
          );
          if (!segment) continue;

          const animSourceVariantIndex = keyframeTimes[i + 1].index;
          const animSourceVariant =
            variants[animSourceVariantIndex];

          if (
            animSourceVariant?.animation?.customKeyframeData &&
            animSourceVariant.animation.customKeyframeData[timeline.id]
          ) {
            const serializedCustomKeyframes =
              animSourceVariant.animation.customKeyframeData[timeline.id];
            const { keyframes: customKeyframes, targetEasing } = deserializeKeyframes(
              serializedCustomKeyframes,
            );

            if (targetEasing && targetEasing !== "Inherit") {
               const endKeyframeTime = keyframeTimes[i + 1].time / totalTime;
               // eslint-disable-next-line @typescript-eslint/no-explicit-any
               const endKeyframe = timeline.keyframes.find((kf) => Math.abs(kf.position - endKeyframeTime) < 0.0001);
               if (endKeyframe) {
                   // eslint-disable-next-line @typescript-eslint/no-explicit-any
                   endKeyframe.easing = targetEasing as any;
               }
            }

            customKeyframes.forEach((customKf) => {
              const absolutePosition =
                segment.activeStartPos +
                customKf.fraction * segment.activeDuration;

              timeline.keyframes.push({
                id: `${timeline.id}-custom-${Math.random()
                  .toString(36)
                  .substr(2, 9)}`,
                position: absolutePosition,
                value: customKf.value as any, // Cast to any
                locked: false,
                easing: customKf.easing,
              });
            });
          }
        }
        timeline.keyframes.sort((a, b) => a.position - b.position);
      });
    });

    return { nodes: [figmaVariantRootNode], duration: totalTime };
  }

  // Helper method for node property retrieval
  /**
   * Retrieves a specific property value from an animation node.
   * @param node The animation node.
   * @param propertyName The name of the property to retrieve (supports dot notation for nested props).
   * @returns The value of the property.
   */
  public static getNodePropertyValue(
    node: AnimationNode,
    propertyName: string,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ): any {
    if (!node) return 0;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const n = node as any;
    switch (propertyName) {
      case "x":
        return n.relativeLeft;
      case "y":
        return n.relativeTop;
      case "rotation":
        return n.decomposedTransform.angle;
      case "width":
        return node.width;
      case "height":
        return node.height;
      case "opacity":
        return node.opacity !== undefined ? node.opacity : 1;
      case "topLeftRadius":
        return node.topLeftRadius !== undefined
          ? node.topLeftRadius
          : typeof node.cornerRadius === "number"
            ? node.cornerRadius
            : 0;
      case "topRightRadius":
        return node.topRightRadius !== undefined
          ? node.topRightRadius
          : typeof node.cornerRadius === "number"
            ? node.cornerRadius
            : 0;
      case "bottomLeftRadius":
        return node.bottomLeftRadius !== undefined
          ? node.bottomLeftRadius
          : typeof node.cornerRadius === "number"
            ? node.cornerRadius
            : 0;
      case "bottomRightRadius":
        return node.bottomRightRadius !== undefined
          ? node.bottomRightRadius
          : typeof node.cornerRadius === "number"
            ? node.cornerRadius
            : 0;
      case "arcData":
        return node.arcData ? node.arcData : undefined;
      default: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const nodeAny = node as any;
        if (propertyName.startsWith("fills.")) {
          const parts = propertyName.split(".");
          const index = parseInt(parts[1], 10);
          const fill = nodeAny.fills && nodeAny.fills[index];
          if (fill) {
            const fillProp = parts.slice(2).join(".");
            if (fillProp === "solid") {
              if (fill.color) {
                return rgbToHex(
                  Math.round(fill.color.r * 255),
                  Math.round(fill.color.g * 255),
                  Math.round(fill.color.b * 255),
                );
              }
              return undefined;
            } else if (fillProp === "solid.opacity") {
              return fill.opacity !== undefined ? fill.opacity : 1;
            } else if (fillProp === "gradient.positions") {
              return fill.gradientHandlePositions;
            } else if (fillProp === "gradient.stops") {
              return (fill.gradientStops && Array.isArray(fill.gradientStops)) ? fill.gradientStops.map(
                  (stop: {
                    position: number;
                    color: { r: number; g: number; b: number };
                  }) => {
                    if (stop.color) {
                      return {
                        position: stop.position,
                        color: rgbToHex(
                          Math.round(stop.color.r * 255),
                          Math.round(stop.color.g * 255),
                          Math.round(stop.color.b * 255),
                        ),
                      };
                    }
                    return stop;
                  }) as unknown : undefined;
            } else if (fillProp === "gradient.opacity") {
              return fill.opacity !== undefined ? fill.opacity : 1;
            } else if (fillProp === "arc") {
              return fill.arcData;
            }
          }
        }
        if (propertyName.startsWith("strokes.")) {
          const parts = propertyName.split(".");
          const index = parseInt(parts[1], 10);
          const stroke = nodeAny.strokes && nodeAny.strokes[index];
          if (stroke) {
            const strokeProp = parts.slice(2).join(".");
            if (strokeProp === "solid") {
              if (stroke.color) {
                return rgbToHex(
                  Math.round(stroke.color.r * 255),
                  Math.round(stroke.color.g * 255),
                  Math.round(stroke.color.b * 255),
                );
              }
              return undefined;
            } else if (strokeProp === "opacity") {
              return stroke.opacity !== undefined ? stroke.opacity : 1;
            }
          }
        }
        return 0;
      }
    }
  }

  /**
   * Recursively flattening the node hierarchy into a map for easy access.
   * @param node The current node to process.
   * @param parentAbsTransform The absolute transform of the parent node.
   * @param isRoot Whether this is the root node.
   * @param map The map to store processed nodes.
   */
  public static collectAnimationNodes(
    node: AnimationNode,
    parentAbsTransform: AnimationNode | null, // Changed to AnimationNode to carry matrix info
    isRoot: boolean,
    map: Map<string, AnimationNode>,
  ) {
    if (node.visible === false) return;

    const currentAbsTransform = node.absoluteTransform || [[1, 0, 0], [0, 1, 0]];
    let relativeLeft = 0;
    let relativeTop = 0;
    let decomposedLocal: DecomposedTransform;

    if (isRoot || !parentAbsTransform || !parentAbsTransform.absoluteTransform) {
        const nodeAbsDecomposed = decomposeMatrix(currentAbsTransform);
        if (isRoot) {
            relativeLeft = 0;
            relativeTop = 0;
            decomposedLocal = nodeAbsDecomposed;
        } else {
            relativeLeft = nodeAbsDecomposed.translateX;
            relativeTop = nodeAbsDecomposed.translateY;
            decomposedLocal = nodeAbsDecomposed;
        }
    } else {
        const parentAbs = parentAbsTransform.absoluteTransform;
        const parentInv = invertMatrix(parentAbs);
        
        if (parentInv) {
            const relMatrix = multiplyMatrix(parentInv, currentAbsTransform);
            decomposedLocal = decomposeMatrix(relMatrix);
            relativeLeft = decomposedLocal.translateX;
            relativeTop = decomposedLocal.translateY;
        } else {
            // Fallback for non-invertible parent matrix
            const nodeAbsDecomposed = decomposeMatrix(currentAbsTransform);
            const parentAbsDecomposed = decomposeMatrix(parentAbs);
            relativeLeft = nodeAbsDecomposed.translateX - parentAbsDecomposed.translateX;
            relativeTop = nodeAbsDecomposed.translateY - parentAbsDecomposed.translateY;
            decomposedLocal = nodeAbsDecomposed;
        }
    }

    map.set(isRoot ? "__ROOT__" : node.name, {
      ...node,
      relativeLeft,
      relativeTop,
      decomposedTransform: decomposedLocal,
    });
    
    if (node.children)
      node.children.forEach((child: AnimationNode) =>
        this.collectAnimationNodes(child, { ...node, absoluteTransform: currentAbsTransform }, false, map),
      );
  }

  /**
   * Recursively collects properties for all nodes to be sent to Figma for preview.
   * @param node The current node.
   * @param parentAbsTransform The absolute transform of the parent.
   * @param isRoot Whether this is the root node.
   * @param animatedNodes The list to populate with animated node properties.
   */
  public static collectNodesForPreview(
    node: AnimationNode,
    parentAbsTransform: AnimationNode | null, // Changed type to store matrix info
    isRoot: boolean,
    animatedNodes: AnimatedNode[],
  ) {
    // Similar logic to collectAnimationNodes for consistent preview
    const currentAbsTransform = node.absoluteTransform || [[1, 0, 0], [0, 1, 0]];
    let relativeLeft = 0;
    let relativeTop = 0;
    let decomposedLocal: DecomposedTransform;

    if (isRoot || !parentAbsTransform || !parentAbsTransform.absoluteTransform) {
       const nodeAbsDecomposed = decomposeMatrix(currentAbsTransform);
       if (isRoot) {
           relativeLeft = 0;
           relativeTop = 0;
           decomposedLocal = nodeAbsDecomposed;
       } else {
           relativeLeft = nodeAbsDecomposed.translateX;
           relativeTop = nodeAbsDecomposed.translateY;
           decomposedLocal = nodeAbsDecomposed;
       }
    } else {
       const parentAbs = parentAbsTransform.absoluteTransform;
       const parentInv = invertMatrix(parentAbs);
       if (parentInv) {
           const relMatrix = multiplyMatrix(parentInv, currentAbsTransform);
           decomposedLocal = decomposeMatrix(relMatrix);
           relativeLeft = decomposedLocal.translateX;
           relativeTop = decomposedLocal.translateY;
       } else {
           const nodeAbsDecomposed = decomposeMatrix(currentAbsTransform);
           const parentAbsDecomposed = decomposeMatrix(parentAbs);
           relativeLeft = nodeAbsDecomposed.translateX - parentAbsDecomposed.translateX;
           relativeTop = nodeAbsDecomposed.translateY - parentAbsDecomposed.translateY;
           decomposedLocal = nodeAbsDecomposed;
       }
    }

    const props: AnimatedNodeProps = {
      x: relativeLeft,
      y: relativeTop,
      rotation: decomposedLocal.angle,
      width: node.width as number,
      height: node.height as number,
      opacity: node.opacity as number,
      visible: node.visible as boolean,
      topLeftRadius:
        node.topLeftRadius !== undefined
          ? (node.topLeftRadius as number)
          : typeof node.cornerRadius === "number"
            ? (node.cornerRadius as number)
            : 0,
      topRightRadius:
        node.topRightRadius !== undefined
          ? (node.topRightRadius as number)
          : typeof node.cornerRadius === "number"
            ? (node.cornerRadius as number)
            : 0,
      bottomLeftRadius:
        node.bottomLeftRadius !== undefined
          ? (node.bottomLeftRadius as number)
          : typeof node.cornerRadius === "number"
            ? (node.cornerRadius as number)
            : 0,
      bottomRightRadius:
        node.bottomRightRadius !== undefined
          ? (node.bottomRightRadius as number)
          : typeof node.cornerRadius === "number"
            ? (node.cornerRadius as number)
            : 0,
      strokeWeight: node.strokeWeight as number,
    };
    if (node.fills) {
      props.fills = node.fills;
    }
    if (node.strokes) {
      props.strokes = node.strokes;
    }
    if (node.arcData) {
      props.arcData = node.arcData as ArcData;
    }
    animatedNodes.push({
      nodeName: isRoot ? "__ROOT__" : node.name,
      originalNodeId: node.id,
      props: props,
    });

    if (node.children) {
      node.children.forEach((child: AnimationNode) =>
        this.collectNodesForPreview(
          child,
          { ...node, absoluteTransform: currentAbsTransform },
          false,
          animatedNodes,
        ),
      );
    }
  }
}
