import { Node, Timeline, Keyframe, Variant, KeyframeTime, SerializedNode, AnimationNode } from "../timeline/types";
import { getAnimationSegment } from "../timeline/utils";
import { rgbToHex, interpolateColor, decomposeMatrix } from "./common";

// Easing functions
export const Easing = {
  linear: (t: number) => t,
  easeInQuad: (t: number) => t * t,
  easeOutQuad: (t: number) => t * (2 - t),
  easeInOutQuad: (t: number) => (t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t),
  easeInCubic: (t: number) => t * t * t,
  easeOutCubic: (t: number) => --t * t * t + 1,
  easeInOutCubic: (t: number) =>
    t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1,
  instant: (_t: number) => 0,
};

export type EasingType =
  | "Linear"
  | "EaseIn"
  | "EaseOut"
  | "EaseInOut"
  | "EaseInCubic"
  | "EaseOutCubic"
  | "EaseInOutCubic"
  | "Instant";

export type EasingFunction = (t: number) => number;

// Helper functions for interpolation
function interpolateGradientStops(startValue: unknown, endValue: unknown, easedProgress: number): Array<{ position: number; color: string }> | undefined {
    try {
      const stops1 = typeof startValue === "string" ? JSON.parse(startValue) : startValue;
      const stops2 = typeof endValue === "string" ? JSON.parse(endValue) : endValue;
      if (stops1 && stops2 && Array.isArray(stops1) && Array.isArray(stops2) && stops1.length === stops2.length) {
        return stops1.map((stop1: { position: number; color: string }, index: number) => {
          const stop2 = stops2[index] as { position: number; color: string };
          return {
            position: stop1.position + (stop2.position - stop1.position) * easedProgress,
            color: interpolateColor(stop1.color, stop2.color, easedProgress),
          };
        });
      }
    } catch (e) {
      console.error("Error interpolating gradient stops", e);
    }
    return undefined;
}

function interpolateGradientPositions(startValue: unknown, endValue: unknown, easedProgress: number): Array<{ x: number; y: number }> | undefined {
    try {
      const pos1 = typeof startValue === "string" ? JSON.parse(startValue) : startValue;
      const pos2 = typeof endValue === "string" ? JSON.parse(endValue) : endValue;
      if (pos1 && pos2 && Array.isArray(pos1) && Array.isArray(pos2) && pos1.length === pos2.length) {
        return pos1.map((p1: { x: number; y: number }, index: number) => {
          const p2 = pos2[index] as { x: number; y: number };
          return {
            x: p1.x + (p2.x - p1.x) * easedProgress,
            y: p1.y + (p2.y - p1.y) * easedProgress,
          };
        });
      }
    } catch (e) {
      console.error("Error interpolating gradient positions", e);
    }
    return undefined;
}

function interpolateArcData(startValue: unknown, endValue: unknown, easedProgress: number): ArcData | undefined {
    try {
      const arc1 = typeof startValue === "string" ? JSON.parse(startValue) : startValue;
      const arc2 = typeof endValue === "string" ? JSON.parse(endValue) : endValue;
      if (arc1 && arc2) {
        return {
          startingAngle: arc1.startingAngle + (arc2.startingAngle - arc1.startingAngle) * easedProgress,
          endingAngle: arc1.endingAngle + (arc2.endingAngle - arc1.endingAngle) * easedProgress,
          innerRadius: arc1.innerRadius + (arc2.innerRadius - arc1.innerRadius) * easedProgress,
        };
      }
    } catch (e) {
      console.error("Error interpolating arc", e);
    }
    return undefined;
}


/**
 * Service providing interpolation logic for various property types.
 * It handles standard easing functions and specific interpolation strategies for gradients, colors, and transforms.
 */
export class InterpolationService {
  /**
   * Calculates the interpolated value of a property at a specific time.
   * It determines the active keyframes and applies the appropriate easing and interpolation.
   * @param timeline The timeline for the property.
   * @param time The current time in seconds.
   * @param totalTime The total duration of the animation.
   * @param keyframeTimes The list of keyframe times.
   * @param serializedVariants The serialized node data for all variants.
   * @param variants The list of variants.
   * @param node The current node.
   * @returns The interpolated value.
   */
  static getInterpolatedValue(
    timeline: Timeline,
    time: number,
    totalTime: number,
    keyframeTimes: KeyframeTime[],
    serializedVariants: SerializedNode[],
    variants: Variant[],
    node: Node
  ): number | string | boolean | object | undefined {
    const propertyName = timeline.property;
    const sortedKeyframes = [...timeline.keyframes].sort(
      (a, b) => a.position - b.position,
    );

    const position = time / totalTime;

    // Check if time falls exactly on a keyframe
    for (const kf of sortedKeyframes) {
      if (Math.abs(kf.position - position) < 0.00001) {
        return this.resolveKeyframeValue(kf, time, keyframeTimes, serializedVariants, node, propertyName);
      }
    }

    let kf1: Keyframe | null = null;
    let kf2: Keyframe | null = null;

    for (let i = 0; i < sortedKeyframes.length; i++) {
      if (position <= sortedKeyframes[i].position) {
        kf2 = sortedKeyframes[i];
        kf1 = sortedKeyframes[i - 1] || null;
        break;
      }
    }
    if (!kf2 && sortedKeyframes.length > 0) {
      kf1 = sortedKeyframes[sortedKeyframes.length - 1];
      kf2 = sortedKeyframes[sortedKeyframes.length - 1];
    }
    if (!kf1 && sortedKeyframes.length > 0) {
      kf1 = sortedKeyframes[0];
      kf2 = sortedKeyframes[0];
    }

    if (!kf1 || !kf2) return 0;

    const kf1Time = kf1.position * totalTime;
    const kf2Time = kf2.position * totalTime;

    const startValue = this.resolveKeyframeValue(kf1, kf1Time, keyframeTimes, serializedVariants, node, propertyName);
    const endValue = this.resolveKeyframeValue(kf2, kf2Time, keyframeTimes, serializedVariants, node, propertyName);

    let easingFunction: EasingFunction = Easing.linear;
    
    // Handle easing type, defaulting to Linear if undefined or Inherit
    let easingToUse: EasingType = "Linear"; // Default to Linear
    if (kf2.easing && kf2.easing !== "Inherit") {
        easingToUse = kf2.easing;
    } else {
        const segment = getAnimationSegment(kf1Time / totalTime, keyframeTimes, totalTime, variants);
        if (segment && segment.endIndex < keyframeTimes.length) {
            const variantIndex = keyframeTimes[segment.endIndex].index;
            if (variantIndex < variants.length) {
                const targetVariant = variants[variantIndex];
                if (targetVariant?.animation?.spec?.animation?.Smooth?.easing) {
                    easingToUse = targetVariant.animation.spec.animation.Smooth.easing as EasingType;
                }
            }
        }
    }

    switch (easingToUse) {
      case "EaseIn":
        easingFunction = Easing.easeInQuad;
        break;
      case "EaseOut":
        easingFunction = Easing.easeOutQuad;
        break;
      case "EaseInOut":
        easingFunction = Easing.easeInOutQuad;
        break;
      case "EaseInCubic":
        easingFunction = Easing.easeInCubic;
        break;
      case "EaseOutCubic":
        easingFunction = Easing.easeOutCubic;
        break;
      case "EaseInOutCubic":
        easingFunction = Easing.easeInOutCubic;
        break;
      case "Instant":
        easingFunction = Easing.instant;
        break;
    }

    let progress = 0;
    if (kf1Time !== kf2Time) {
      let segmentStartTime = 0;
      let segmentDelay = 0;
      const segment = getAnimationSegment(
        kf1Time / totalTime,
        keyframeTimes,
        totalTime,
        variants,
      );
      if (segment) {
        segmentDelay = segment.delay;
        segmentStartTime = segment.startTime;
      }

      const activeSegmentStart = segmentStartTime + segmentDelay;

      const effectiveKf1Time = Math.max(kf1Time, activeSegmentStart);
      const effectiveKf2Time = Math.max(kf2Time, activeSegmentStart);
      const effectiveTime = Math.max(time, activeSegmentStart);

      const activeDuration = effectiveKf2Time - effectiveKf1Time;

      if (activeDuration > 0) {
        progress = (effectiveTime - effectiveKf1Time) / activeDuration;
      } else {
        progress = effectiveTime >= effectiveKf2Time ? 1 : 0;
      }
    }
    progress = Math.max(0, Math.min(1, progress));

    const easedProgress = easingFunction(progress);

    return this.interpolate(startValue, endValue, easedProgress, propertyName);
  }

  private static resolveKeyframeValue(
    kf: Keyframe,
    time: number,
    keyframeTimes: KeyframeTime[],
    serializedVariants: SerializedNode[],
    node: Node,
    propertyName: string
  ): number | string | boolean | object | undefined {
    if (kf.locked) {
      // If the keyframe represents a missing state, use the pre-calculated fallback value
      // instead of querying the variant (which would return default/0).
      if (kf.isMissing) {
          return kf.value;
      }

      const variantIndex = keyframeTimes.find(
        (kt) => Math.abs(kt.time - time) < 0.001,
      )?.index;
      if (variantIndex !== undefined) {
        const variant = serializedVariants[variantIndex];
        const nodesMap = new Map();
        this.collectAnimationNodes(variant as unknown as AnimationNode, null, true, nodesMap);
        return this.getNodePropertyValue(
          nodesMap.get(node.name),
          propertyName,
        );
      }
    }
    return kf.value;
  }

  /**
   * Generic interpolation function that handles different data types.
   * Supports numbers, arrays of numbers, colors, gradient structures, and arc data.
   * @param startValue The start value.
   * @param endValue The end value.
   * @param easedProgress The progress value after easing (0-1).
   * @param propertyName The name of the property being interpolated (used for type hinting).
   * @returns The interpolated value.
   */
  public static interpolate(
    startValue: unknown,
    endValue: unknown,
    easedProgress: number,
    propertyName: string
  ): number | string | boolean | object | undefined {
    if (propertyName.endsWith(".gradient.positions")) {
        return interpolateGradientPositions(startValue, endValue, easedProgress);
    }
    if (propertyName.endsWith(".gradient.stops")) {
        return interpolateGradientStops(startValue, endValue, easedProgress);
    }
    if (propertyName === "arcData") {
        return interpolateArcData(startValue, endValue, easedProgress);
    }

    if (typeof startValue === "number" && typeof endValue === "number") {
      return startValue + (endValue - startValue) * easedProgress;
    } else if (
      Array.isArray(startValue) &&
      Array.isArray(endValue) &&
      startValue.length === endValue.length &&
      typeof startValue[0] === "number"
    ) {
      return startValue.map((v, i) => (v as number) + ((endValue[i] as number) - (v as number)) * easedProgress);
    } else if (typeof startValue === "boolean" && typeof endValue === "boolean") {
        // For boolean properties, treat as an instant change at 50% of the progress
        return easedProgress >= 0.5 ? endValue : startValue;
    } else if (
      typeof startValue === "string" &&
      typeof endValue === "string"
    ) {
      if (propertyName.endsWith(".solid")) {
        return interpolateColor(startValue, endValue, easedProgress);
      }
    }

    // Ensure return type compatibility by checking if it matches expected primitive types, otherwise return undefined or cast if sure.
    // Since this is a fallback, simply returning startValue (which is unknown) might violate the return type if not cast.
    return startValue as number | string | boolean | object | undefined; 
  }

  // Helper method for node property retrieval (duplicated from PlaybackController for now, needs refactoring to common)
  private static getNodePropertyValue(
    node: AnimationNode,
    propertyName: string,
  ): number | string | boolean | object | undefined {
    if (!node) return 0;
    // Use explicit interface or type assertion if needed for specific properties not in AnimationNode
    // But AnimationNode has [key: string]: unknown, so we can access properties but need to check/cast results.
    
    switch (propertyName) {
      case "x":
        // We added relativeLeft to the map in collectAnimationNodes, but it's not in the interface strictly speaking as a required prop
        // But we know it's there in our processed nodes.
        return (node as any).relativeLeft; 
      case "y":
        return (node as any).relativeTop;
      case "rotation":
        return (node as any).decomposedTransform?.angle;
      case "width":
        return node.width as number;
      case "height":
        return node.height as number;
      case "opacity":
        return node.opacity !== undefined ? node.opacity as number : 1;
      case "topLeftRadius":
        return node.topLeftRadius !== undefined
          ? (node.topLeftRadius as number)
          : typeof node.cornerRadius === "number"
            ? (node.cornerRadius as number)
            : 0;
      case "topRightRadius":
        return node.topRightRadius !== undefined
          ? (node.topRightRadius as number)
          : typeof node.cornerRadius === "number"
            ? (node.cornerRadius as number)
            : 0;
      case "bottomLeftRadius":
        return node.bottomLeftRadius !== undefined
          ? (node.bottomLeftRadius as number)
          : typeof node.cornerRadius === "number"
            ? (node.cornerRadius as number)
            : 0;
      case "bottomRightRadius":
        return node.bottomRightRadius !== undefined
          ? (node.bottomRightRadius as number)
          : typeof node.cornerRadius === "number"
            ? (node.cornerRadius as number)
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
                  }) : [];
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

  private static collectAnimationNodes(
    node: AnimationNode,
    parentAbsTransform: AnimationNode | null,
    isRoot: boolean,
    map: Map<string, AnimationNode>,
  ) {
    if (node.visible === false) return;

    const nodeAbsTransform = decomposeMatrix(node.absoluteTransform);
    let relativeLeft, relativeTop;
    if (isRoot) {
      relativeLeft = 0;
      relativeTop = 0;
    } else {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const parentTrans = (parentAbsTransform as any) || { translateX: 0, translateY: 0 };
      relativeLeft =
        nodeAbsTransform.translateX - parentTrans.translateX;
      relativeTop = nodeAbsTransform.translateY - parentTrans.translateY;
    }
    map.set(isRoot ? "__ROOT__" : node.name, {
      ...node,
      relativeLeft,
      relativeTop,
      decomposedTransform: nodeAbsTransform,
    });
    if (node.children)
      node.children.forEach((child: AnimationNode) =>
        this.collectAnimationNodes(child, nodeAbsTransform as unknown as AnimationNode, false, map),
      );
  }

}
