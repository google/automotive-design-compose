import { Keyframe, ArcData } from "./types";

// Serialization format: "fraction,value,easing;fraction,value,easing;..."
// Value will be JSON.stringify-ed if it's an object, and then base64 encoded.

/**
 * Serializes an `ArcData` object into a comma-separated string.
 * @param arc The arc data to serialize.
 * @returns The serialized string "start,end,inner".
 */
export function serializeArc(arc: ArcData): string {
  return `${arc.startingAngle},${arc.endingAngle},${arc.innerRadius}`;
}

/**
 * Deserializes a comma-separated string back into an `ArcData` object.
 * @param serializedArc The string to deserialize.
 * @returns The parsed `ArcData` object.
 */
export function deserializeArc(serializedArc: string): ArcData {
  const parts = serializedArc.split(",").map(parseFloat);
  return {
    startingAngle: parts[0],
    endingAngle: parts[1],
    innerRadius: parts[2],
  };
}

/**
 * Serializes an array of custom keyframes into a string format suitable for storage.
 * Handles base64 encoding of values to ensure safety.
 * @param keyframes The array of keyframe data objects.
 * @param targetEasing The easing of the target variant keyframe.
 * @returns The serialized string.
 */
export function serializeKeyframes(
  keyframes: {
    fraction: number;
    value: unknown;
    easing?: Keyframe["easing"];
  }[],
  targetEasing: string = "Inherit"
): string {
  const kfsString = keyframes
    .map((kf) => {
      const valueStr =
        typeof kf.value === "object"
          ? JSON.stringify(kf.value)
          : String(kf.value);
      const encodedValue = btoa(valueStr);
      return `${kf.fraction},${encodedValue},${kf.easing || "Linear"}`;
    })
    .join(";");
  return `${targetEasing}|${kfsString}`;
}

/**
 * Deserializes a string back into an array of keyframes and the target easing.
 * Handles base64 decoding and legacy format fallbacks.
 * @param serializedData The serialized string.
 * @returns An object containing the keyframes array and the target easing.
 */
export function deserializeKeyframes(
  serializedData: string,
): { keyframes: { fraction: number; value: unknown; easing?: Keyframe["easing"] }[], targetEasing: string } {
  if (!serializedData) {
    return { keyframes: [], targetEasing: "Inherit" };
  }

  let targetEasing = "Inherit";
  let kfsData = serializedData;

  if (serializedData.indexOf("|") !== -1) {
      const parts = serializedData.split("|");
      targetEasing = parts[0];
      kfsData = parts[1];
  }

  if (!kfsData) {
      return { keyframes: [], targetEasing };
  }

  const keyframes = kfsData.split(";").map((part) => {
    const [fractionStr, encodedValue, easingStr] = part.split(",");
    const fraction = parseFloat(fractionStr);
    let value: unknown;
    try {
      const decodedValue = atob(encodedValue);
      try {
        value = JSON.parse(decodedValue);
      } catch (e) {
        if (
          decodedValue.includes(",") &&
          decodedValue.split(",").length === 3
        ) {
          value = deserializeArc(decodedValue);
        } else {
          value = decodedValue;
        }
      }
    } catch (e) {
      // Fallback for old data that was not base64 encoded
      try {
        value = JSON.parse(encodedValue);
      } catch (e) {
        value = encodedValue;
      }
    }
    const easing = easingStr as Keyframe["easing"];
    return { fraction, value, easing };
  });
  
  return { keyframes, targetEasing };
}
