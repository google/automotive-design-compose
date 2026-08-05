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

import { Timeline, Keyframe, Variant } from "./types";

/**
 * Describes a segment of the timeline between two variant keyframes.
 */
export interface AnimationSegment {
  startIndex: number;
  endIndex: number;
  startTime: number;
  endTime: number;
  startPos: number;
  endPos: number;
  delay: number;
  delayProportion: number;
  delayEndPos: number;
  activeDuration: number;
  activeStartPos: number;
}

/**
 * Information about a keyframe time.
 */
interface KeyframeTime {
  time: number;
  name: string;
  index: number;
  isLoop?: boolean;
}

/**
 * Finds the animation segment for a given position in the timeline.
 *
 * @param position The position to find the segment for (0-1).
 * @param keyframeTimes A list of all keyframe times.
 * @param totalDuration The total duration of the animation.
 * @param variants A list of all variants.
 * @returns The animation segment, or null if not found.
 */
export function getAnimationSegment(
  position: number,
  keyframeTimes: KeyframeTime[],
  totalDuration: number,
  variants: Variant[],
): AnimationSegment | null {
  if (keyframeTimes.length < 2) {
    return null;
  }

  let segmentStartIndex = -1;
  let segmentEndIndex = -1;

  // A position is in a segment if it's >= start and < end. [start, end)
  for (let i = 0; i < keyframeTimes.length - 1; i++) {
    const startPos = keyframeTimes[i].time / totalDuration;
    const endPos = keyframeTimes[i + 1].time / totalDuration;

    if (position >= startPos - 0.0001 && position < endPos - 0.0001) {
      segmentStartIndex = i;
      segmentEndIndex = i + 1;
      break;
    }
  }

  // Handle the case where the position is exactly at the end of the timeline
  if (
    segmentStartIndex === -1 &&
    Math.abs(position - 1.0) < 0.0001 &&
    keyframeTimes.length > 1
  ) {
    segmentStartIndex = keyframeTimes.length - 2;
    segmentEndIndex = keyframeTimes.length - 1;
  }

  if (segmentStartIndex === -1) {
    return null;
  }

  const segmentStartPos = keyframeTimes[segmentStartIndex].time / totalDuration;
  const animSourceVariantIndex = keyframeTimes[segmentEndIndex].index;
  const animSourceVariant = variants[animSourceVariantIndex];

  let delay = 0;
  let duration = 0;
  const spec = resolveVariantSpec(
    animSourceVariant?.animation,
    animSourceVariant?.name,
  );
  if (spec?.initial_delay) {
    const d = spec.initial_delay;
    delay = (d.secs || 0) + (d.nanos || 0) / 1e9;
  }

  if (spec?.animation?.Smooth) {
    const d = spec.animation.Smooth.duration;
    duration = (d.secs || 0) + (d.nanos || 0) / 1e9;
  }

  const delayProportion = delay / totalDuration;
  const delayEndPos = segmentStartPos + delayProportion;
  const activeAnimationStartPos = segmentStartPos + delayProportion;
  const activeAnimationDuration = duration / totalDuration;

  return {
    startIndex: segmentStartIndex,
    endIndex: segmentEndIndex,
    startTime: keyframeTimes[segmentStartIndex].time,
    endTime: keyframeTimes[segmentEndIndex].time,
    startPos: segmentStartPos,
    endPos: keyframeTimes[segmentEndIndex].time / totalDuration,
    delay: delay,
    delayProportion: delayProportion,
    delayEndPos: delayEndPos,
    activeDuration: activeAnimationDuration,
    activeStartPos: activeAnimationStartPos,
  };
}

/**
 * Finds the next keyframe at or after a given position.
 *
 * @param position The position to find the keyframe for (0-1).
 * @param timeline The timeline data.
 * @param lockedOnly If true, only locked keyframes will be considered.
 * @returns The next keyframe, or null if not found.
 */
export function findNextKeyframe(
  position: number,
  timeline: Timeline,
  lockedOnly: boolean = false,
): Keyframe | null {
  let bestKeyframe: Keyframe | null = null;
  let bestDistance = Infinity;

  for (const kf of timeline.keyframes) {
    if (lockedOnly && !kf.locked) {
      continue;
    }
    const distance = kf.position - position;
    if (distance >= -0.0001 && distance < bestDistance) {
      bestKeyframe = kf;
      bestDistance = distance;
    }
  }

  return bestKeyframe;
}

let globalActiveTransitionsMap:
  | { [variantName: string]: number }
  | Map<string, number>
  | undefined = undefined;

export function setGlobalActiveTransitions(
  map?: { [variantName: string]: number } | Map<string, number>,
): void {
  globalActiveTransitionsMap = map;
}

export function getGlobalActiveTransitions():
  | { [variantName: string]: number }
  | Map<string, number>
  | undefined {
  return globalActiveTransitionsMap;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function resolveVariantSpec(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  anim: any,
  variantName?: string,
  activeTransitionsMap?:
    | { [variantName: string]: number }
    | Map<string, number>,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
): any {
  if (!anim) return undefined;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let spec: any = undefined;
  const mapToUse = activeTransitionsMap || globalActiveTransitionsMap;

  if (anim.transitions && anim.transitions.length > 0) {
    let selectedIdx: number | undefined = undefined;
    if (mapToUse && variantName) {
      if (mapToUse instanceof Map) {
        selectedIdx = mapToUse.get(variantName);
      } else {
        selectedIdx = mapToUse[variantName];
      }
    }

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let matchingTrans: any = undefined;
    if (
      typeof selectedIdx === "number" &&
      selectedIdx >= 0 &&
      selectedIdx < anim.transitions.length
    ) {
      matchingTrans = anim.transitions[selectedIdx];
    } else {
      matchingTrans =
        anim.transitions.find(
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          (t: any) => (t.to === variantName || t.to === "*" || !t.to) && t.spec,
        ) ||
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        anim.transitions.find((t: any) => t.spec);
    }
    if (matchingTrans && matchingTrans.spec) {
      spec = matchingTrans.spec;
    }
  }

  if (!spec && anim.spec) {
    spec = anim.spec;
  }
  if (!spec && anim.default_spec) {
    spec = anim.default_spec;
  }

  return spec;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function resolveVariantCustomKeyframeData(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  anim: any,
  variantName?: string,
  activeTransitionsMap?:
    | { [variantName: string]: number }
    | Map<string, number>,
): { [key: string]: string } | undefined {
  if (!anim) return undefined;

  const mapToUse = activeTransitionsMap || globalActiveTransitionsMap;

  if (anim.transitions && anim.transitions.length > 0) {
    let selectedIdx: number | undefined = undefined;
    if (mapToUse && variantName) {
      if (mapToUse instanceof Map) {
        selectedIdx = mapToUse.get(variantName);
      } else {
        selectedIdx = mapToUse[variantName];
      }
    }

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let matchingTrans: any = undefined;
    if (
      typeof selectedIdx === "number" &&
      selectedIdx >= 0 &&
      selectedIdx < anim.transitions.length
    ) {
      matchingTrans = anim.transitions[selectedIdx];
    } else {
      matchingTrans =
        anim.transitions.find(
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          (t: any) =>
            (t.to === variantName || t.to === "*" || !t.to) &&
            t.customKeyframeData,
        ) || anim.transitions[0];
    }
    if (matchingTrans && matchingTrans.customKeyframeData) {
      return matchingTrans.customKeyframeData;
    }
  }

  return anim.customKeyframeData;
}
