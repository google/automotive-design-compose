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
 * Serializes an array of custom keyframes into a JSON string suitable for storage.
 * @param keyframes The array of keyframe data objects.
 * @param targetEasing The easing of the target variant keyframe.
 * @returns The serialized JSON string.
 */
export function serializeKeyframes(
  keyframes: {
    fraction: number;
    value: unknown;
    easing?: Keyframe["easing"];
  }[],
  targetEasing: string = "Inherit"
): string {
  // We use standard JSON serialization now. 
  // Figma's API limits plugin data to strings, but native JSON parsing is highly optimized.
  return JSON.stringify({
    targetEasing,
    keyframes: keyframes.map(k => ({
      fraction: k.fraction,
      value: k.value,
      easing: k.easing || "Inherit"
    }))
  });
}

function decodeBase64(encoded: string): string {
  if (typeof atob !== "undefined") {
    return atob(encoded);
  } else if (typeof figma !== "undefined" && figma.base64Decode) {
    const uint8Array = figma.base64Decode(encoded);
    let str = "";
    for (let i = 0; i < uint8Array.length; i++) {
      str += String.fromCharCode(uint8Array[i]);
    }
    return str;
  }
  throw new Error("No base64 decoder available");
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

  // Attempt to parse NEW JSON format first
  if (serializedData.startsWith("{")) {
    try {
      const parsed = JSON.parse(serializedData);
      if (parsed && Array.isArray(parsed.keyframes)) {
        return parsed;
      }
    } catch (e) {
      // Fall through to legacy parsing
      console.warn("Failed to parse JSON custom keyframes, falling back to legacy format.", e);
    }
  }

  // LEGACY parsing...
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
      const decodedValue = decodeBase64(encodedValue);
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
