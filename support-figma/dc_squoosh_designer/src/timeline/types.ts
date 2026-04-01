/**
 * Represents a single keyframe on a timeline, defining a specific state of a property at a point in time.
 */
export interface Keyframe {
  /** A unique identifier for the keyframe. */
  id: string;
  /** The normalized position of the keyframe on the timeline, ranging from 0 to 1. */
  position: number;
  /** The value of the animated property at this keyframe. */
  value: number | string | boolean | object | undefined;
  locked?: boolean;
  easing?:
    | "Inherit"
    | "Linear"
    | "Instant"
    | "EaseIn"
    | "EaseOut"
    | "EaseInOut"
    | "EaseInCubic"
    | "EaseOutCubic"
    | "EaseInOutCubic";
  /** If true, this keyframe represents a state where the element is missing in the variant. */
  isMissing?: boolean;
  /** Optional data store for custom information, such as nested keyframes for complex animations. */
  data?: {
    keyframes?: {
      fraction: number;
      value: number | string | boolean | undefined;
      easing?: Keyframe["easing"];
    }[];
  };
}

/**
 * Represents a timeline for a single animatable property of a node.
 */
export interface Timeline {
  /** A unique identifier for the timeline. */
  id: string;
  /** The name of the animated property (e.g., "x", "opacity"). */
  property: string;
  /** An array of keyframes that define the animation for this property. */
  keyframes: Keyframe[];
  /** If true, this timeline was added by the user. */
  isCustom?: boolean;
}

/**
 * Represents a node in the animation hierarchy, corresponding to a layer in Figma.
 */
export interface Node {
  /** A unique identifier for the node within the animation data. */
  id: string;
  /** The corresponding Figma node ID. */
  figmaId: string;
  /** The name of the node, typically matching the Figma layer name. */
  name: string;
  /** An array of timelines for the animatable properties of this node. */
  timelines: Timeline[];
  /** An array of child nodes, representing the layer hierarchy. */
  children: Node[];
}

/**
 * Represents the entire animation data structure for the timeline editor.
 */
export interface AnimationData {
  /** The root nodes of the animation hierarchy. */
  nodes: Node[];
  /** The total duration of the animation in seconds. */
  duration: number;
}

/**
 * Represents a key moment in the animation, typically corresponding to a variant change.
 */
export interface KeyframeTime {
  /** The time in seconds at which this keyframe occurs. */
  time: number;
  /** The name of the variant associated with this keyframe. */
  name: string;
  /** The index of the variant in the sequence. */
  index: number;
  /** If true, this keyframe represents the loop-back point to the beginning of the animation. */
  isLoop?: boolean;
}

/**
 * Defines the animatable properties of a node for preview purposes.
 */
export interface AnimatedNodeProps {
  /** The x-coordinate of the node. */
  x?: number;
  /** The y-coordinate of the node. */
  y?: number;
  /** The rotation of the node in degrees. */
  rotation?: number;
  /** The width of the node. */
  width?: number;
  /** The height of the node. */
  height?: number;
  /** The opacity of the node, from 0 to 1. */
  opacity?: number;
  /** The visibility of the node. */
  visible?: boolean;
  /** The corner radius for all corners of the node. */
  cornerRadius?: number;
  /** The top-left corner radius. */
  topLeftRadius?: number;
  /** The top-right corner radius. */
  topRightRadius?: number;
  /** The bottom-left corner radius. */
  bottomLeftRadius?: number;
  /** The bottom-right corner radius. */
  bottomRightRadius?: number;
  /** The fill properties of the node. */
  fills?: readonly Paint[] | unknown;
  /** The stroke properties of the node. */
  strokes?: readonly Paint[] | unknown;
  /** The stroke weight of the node. */
  strokeWeight?: number;
  /** The arc properties for ellipse nodes. */
  arcData?: ArcData;
}

/**
 * Represents a node with its animated properties for updating the Figma preview.
 */
export interface AnimatedNode {
  /** The name of the node to be animated. */
  nodeName: string;
  /** The original unique ID of the node. */
  originalNodeId?: string;
  /** The set of properties to apply to the node. */
  props: AnimatedNodeProps;
}

/**
 * Represents a simplified node structure used within the animation data logic.
 */
export interface AnimationNode {
  /** The unique ID of the node (from Figma). */
  id?: string;
  /** The name of the node. */
  name: string;
  /** The visibility of the node. */
  visible: boolean;
  /** The absolute transformation matrix of the node. */
  absoluteTransform: number[][];
  /** An array of child nodes. */
  children?: AnimationNode[];
  /** Allows for other properties not explicitly defined. */
  [key: string]: unknown;
}

export interface DecomposedTransform {
  translateX: number;
  translateY: number;
  angle: number;
  scaleX: number;
  scaleY: number;
}

/**
 * Represents a Figma Paint object, defining a fill or stroke.
 */
export interface Paint {
  /** The type of paint (e.g., "SOLID", "GRADIENT_LINEAR"). */
  type: string;
  /** The visibility of the paint. */
  visible?: boolean;
  /** The opacity of the paint, from 0 to 1. */
  opacity?: number;
  /** The color of the paint. */
  color?: { r: number; g: number; b: number };
  /** The blend mode of the paint. */
  blendMode?: string;
  /** The positions of gradient handles for gradient paints. */
  gradientHandlePositions?: { x: number; y: number }[];
  /** The color stops for gradient paints. */
  gradientStops?: {
    color: { r: number; g: number; b: number; a: number };
    position: number;
  }[];
  arcData?: ArcData;
}

/**
 * Represents the data for an arc on an ellipse node in Figma.
 */
export interface ArcData {
  /** The starting angle of the arc in radians. */
  startingAngle: number;
  /** The ending angle of the arc in radians. */
  endingAngle: number;
  /** The inner radius of the arc, creating a donut shape. */
  innerRadius: number;
}

/**
 * Represents a vector path in Figma.
 */
export interface VectorPath {
  /** The winding rule for the vector path. */
  windingRule: "NONZERO" | "EVENODD";
  /** The SVG path data string. */
  data: string;
}

/**
 * Represents a serialized Figma node, containing a subset of its properties.
 */
export interface SerializedNode {
  /** The unique ID of the Figma node. */
  id: string;
  /** The name of the Figma node. */
  name: string;
  /** The type of the Figma node (e.g., "FRAME", "RECTANGLE"). */
  type: string;
  /** The x-coordinate of the node relative to its parent. */
  x: number;
  /** The y-coordinate of the node relative to its parent. */
  y: number;
  /** The absolute transformation matrix of the node. */
  absoluteTransform?: [[number, number, number], [number, number, number]];
  /** The width of the node. */
  width?: number;
  /** The height of the node. */
  height?: number;
  /** The opacity of the node. */
  opacity?: number;
  /** The visibility of the node. */
  visible?: boolean;
  /** The fill properties of the node. */
  fills?: readonly Paint[] | typeof figma.mixed;
  /** The stroke properties of the node. */
  strokes?: readonly Paint[] | typeof figma.mixed;
  /** The stroke weight of the node. */
  strokeWeight?: number | typeof figma.mixed;
  /** The corner radius for all corners of the node. */
  cornerRadius?: number | typeof figma.mixed;
  /** The top-left corner radius. */
  topLeftRadius?: number | typeof figma.mixed;
  /** The top-right corner radius. */
  topRightRadius?: number | typeof figma.mixed;
  /** The bottom-left corner radius. */
  bottomLeftRadius?: number | typeof figma.mixed;
  /** The bottom-right corner radius. */
  bottomRightRadius?: number | typeof figma.mixed;
  /** The geometry of the node's fill. */
  fillGeometry?: readonly VectorPath[] | typeof figma.mixed;
  /** The arc properties for ellipse nodes. */
  arcData?: ArcData;
  /** An array of child nodes. */
  children?: SerializedNode[];
}

/**
 * Represents the animation data stored in a Figma variant's plugin data.
 */
export interface VariantAnimation {
  /** The specification for the animation, including delay, duration, and easing. */
  spec?: {
    initial_delay?: {
      secs?: number;
      nanos?: number;
    };
    animation?: {
      Smooth?: {
        duration: {
          secs?: number;
          nanos?: number;
        };
        easing: string;
      };
    };
    interrupt_type?: string;
  };
  /** A map of timeline IDs to serialized custom keyframe data. */
  customKeyframeData?: { [key: string]: string };
}

/**
 * Represents a single variant in a component set, along with its associated animation data.
 */
export interface Variant {
  /** The name of the variant. */
  name: string;
  /** The animation data for the variant, or null if none is defined. */
  animation: VariantAnimation | null;
}
