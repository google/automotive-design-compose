/* eslint-disable @typescript-eslint/no-explicit-any */
import { AnimationData, Node, Timeline, Keyframe, SerializedNode, Variant, KeyframeTime, AnimatedNode, AnimationNode } from "./types";
import { EventEmitter } from "./EventEmitter";
import { InterpolationService } from "../utils/InterpolationService";
import { DataMapper } from "../services/DataMapper";

// Easing functions
const Easing = {
  linear: (t: number) => t,
  easeInQuad: (t: number) => t * t,
  easeOutQuad: (t: number) => t * (2 - t),
  easeInOutQuad: (t: number) => (t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t),
  easeInCubic: (t: number) => t * t * t,
  easeOutCubic: (t: number) => --t * t * t + 1,
  easeInOutCubic: (t: number) =>
    t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1,
};

type EasingFunction = (t: number) => number;

interface PlaybackControllerOptions {
  animationData: AnimationData;
  serializedVariants: SerializedNode[];
  variants: Variant[];
}

/**
 * Controls the animation playback, timeline seek, and preview updates.
 * It manages the current time, play state, and coordinates with the data mapper and UI to render frames.
 */
export class PlaybackController extends EventEmitter {
  private animationFrameId: number | null = null;
  public isPlaying: boolean = false;
  public currentTime: number = 0;
  private totalTime: number = 1;
  public continuePlayback: boolean = false;
  private lastTimestamp: number | null = null;
  private currentKeyframeIndex: number = -1;

  public throttleUpdates: boolean = false;
  private lastUpdateTime: number = 0;
  private updateTimeout: number | null = null;
  private nextUpdateTime: number | null = null;
  private isFigmaProcessing: boolean = false;

  private animationData: AnimationData;
  private serializedVariants: SerializedNode[];
  private variants: Variant[];
  private keyframeTimes: KeyframeTime[] = [];

  constructor(options: PlaybackControllerOptions) {
    super();
    this.animationData = options.animationData;
    this.serializedVariants = options.serializedVariants;
    this.variants = options.variants;
    this.calculateKeyframeData();
  }

  /**
   * Enables or disables the throttling of preview updates.
   * When enabled, updates are limited to 60fps and wait for Figma to acknowledge completion.
   * @param enabled Whether throttling should be enabled.
   */
  public setThrottleUpdates(enabled: boolean) {
    this.throttleUpdates = enabled;
    if (!enabled) {
        // Reset state if disabling
        this.isFigmaProcessing = false;
        this.nextUpdateTime = null;
        if (this.updateTimeout !== null) {
            clearTimeout(this.updateTimeout);
            this.updateTimeout = null;
        }
    }
  }

  /**
   * Called when the Figma main thread acknowledges that a preview update has completed.
   * This triggers the processing of the next pending update if throttling is enabled.
   */
  public acknowledgePreviewUpdate() {
    this.isFigmaProcessing = false;
    if (this.throttleUpdates && this.nextUpdateTime !== null) {
        // Trigger processing of the latest pending time
        this.updatePreviewAtTime(this.nextUpdateTime);
    }
  }

  /**
   * Starts animation playback.
   */
  public play() {
    if (this.isPlaying) return;
    this.isPlaying = true;
    this.lastTimestamp = null; // Reset for deltaTime calculation
    this.emit("play");
    this.animationFrameId = requestAnimationFrame(this.tick.bind(this));
  }

  /**
   * Pauses animation playback.
   */
  public pause() {
    if (!this.isPlaying) return;
    this.isPlaying = false;
    this.lastTimestamp = null; // Reset
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
    this.emit("pause");
  }

  /**
   * Seeks to a specific time in the animation.
   * @param time The time in seconds to seek to.
   */
  public seek(time: number) {
    this.currentTime = Math.max(0, Math.min(this.totalTime, time));
    this.updatePreviewAtTime(this.currentTime);
    this.emit("timeupdate", this.currentTime);
  }

  /**
   * Sets whether playback should loop or stop at the end.
   * @param shouldContinue True to loop, false to stop.
   */
  public setContinue(shouldContinue: boolean) {
    this.continuePlayback = shouldContinue;
  }

  /**
   * Updates the controller with new animation data and variants.
   * Recalculates keyframe times.
   * @param options The new data options.
   */
  public updateData(options: PlaybackControllerOptions) {
    this.animationData = options.animationData;
    this.serializedVariants = options.serializedVariants;
    this.variants = options.variants;
    this.calculateKeyframeData();
  }

  /**
   * Updates only the animation data structure.
   * @param animationData The new animation data.
   */
  public updateAnimationData(animationData: AnimationData) {
    this.animationData = animationData;
  }

  /**
   * Returns a variant at a specific index.
   * @param index The index of the variant.
   */
  public getVariant(index: number) {
    return this.variants[index];
  }

  /**
   * Returns all current variants.
   */
  public getVariants() {
    return this.variants;
  }

  /**
   * Returns the calculated keyframe times.
   */
  public getKeyframeTimes() {
    return this.keyframeTimes;
  }

  private tick(timestamp: number) {
    if (!this.isPlaying) {
      this.lastTimestamp = null;
      return;
    }

    if (this.lastTimestamp === null) {
      this.lastTimestamp = timestamp;
      this.animationFrameId = requestAnimationFrame(this.tick.bind(this));
      return; // Skip the first frame to have a valid deltaTime
    }
    const deltaTime = (timestamp - this.lastTimestamp) / 1000; // deltaTime in seconds
    this.lastTimestamp = timestamp;

    const previousTime = this.currentTime;
    this.currentTime += deltaTime;

    // If not continuing, find the next stop point.
    if (!this.continuePlayback) {
      let stopTime = this.totalTime;
      let stopIndex =
        this.keyframeTimes.length > 0
          ? this.keyframeTimes[this.keyframeTimes.length - 1].index
          : 0;
      // Find the first keyframe time strictly after the time before this tick.
      for (const keyframe of this.keyframeTimes) {
        if (keyframe.time > previousTime) {
          stopTime = keyframe.time;
          stopIndex = keyframe.index;
          break;
        }
      }

      // If we've passed the stop time on this tick, stop exactly at it.
      if (this.currentTime >= stopTime) {
        if (stopTime === this.totalTime) {
          this.currentTime = 0; // Reset to beginning
        } else {
          this.currentTime = stopTime;
        }
        this.updatePreviewAtTime(this.currentTime);
        this.emit("timeupdate", this.currentTime);
        this.pause();
        this.emit("stop", stopIndex);
        return;
      }
    }

    // If we've passed the total time, loop if continuing.
    if (this.currentTime >= this.totalTime) {
      if (this.continuePlayback) {
        this.currentTime = this.currentTime % this.totalTime; // Loop smoothly
      }
      // The 'else' case for not continuing is handled above.
    }

    this.updatePreviewAtTime(this.currentTime);
    this.emit("timeupdate", this.currentTime);

    this.animationFrameId = requestAnimationFrame(this.tick.bind(this));
  }

  private calculateKeyframeData() {
    const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(this.variants);
    this.keyframeTimes = keyframeTimes;
    this.totalTime = totalTime;
  }

  private findNodeByName(nodes: Node[], name: string): Node | null {
    for (const node of nodes) {
      if (node.name === name) return node;
      if (node.children) {
        const found = this.findNodeByName(node.children, name);
        if (found) return found;
      }
    }
    return null;
  }

  private updatePreviewAtTime(time: number) {
    if (this.throttleUpdates) {
      this.nextUpdateTime = time;

      if (this.isFigmaProcessing) {
        return; // Wait for ACK
      }

      const now = Date.now();
      const timeSinceLastUpdate = now - this.lastUpdateTime;

      if (timeSinceLastUpdate >= 16) {
        this.isFigmaProcessing = true;
        this.performUpdatePreviewAtTime(this.nextUpdateTime); // Send the latest
        this.lastUpdateTime = now;
        this.nextUpdateTime = null;
      } else {
        if (this.updateTimeout === null) {
          this.updateTimeout = window.setTimeout(() => {
            // Check again if processing
            if (!this.isFigmaProcessing && this.nextUpdateTime !== null) {
                this.isFigmaProcessing = true;
                this.performUpdatePreviewAtTime(this.nextUpdateTime);
                this.lastUpdateTime = Date.now();
                this.nextUpdateTime = null;
            }
            this.updateTimeout = null;
          }, 16 - timeSinceLastUpdate);
        }
      }
    } else {
      this.performUpdatePreviewAtTime(time);
    }
  }

  private performUpdatePreviewAtTime(time: number) {
    let fromIndex = -1,
      toIndex = -1;
    let segmentStartTime = 0,
      segmentDuration = 1,
      segmentDelay = 0;

    if (this.keyframeTimes.length < 2) return;

    for (let i = 0; i < this.keyframeTimes.length - 1; i++) {
      const startKeyframe = this.keyframeTimes[i];
      const endKeyframe = this.keyframeTimes[i + 1];
      if (time >= startKeyframe.time && time <= endKeyframe.time) {
        fromIndex = startKeyframe.index;
        toIndex = endKeyframe.index;
        segmentStartTime = startKeyframe.time;
        const anim = this.variants[toIndex].animation;
        if (anim && anim.spec && anim.spec.initial_delay) {
          segmentDelay =
            (anim.spec.initial_delay.secs || 0) +
            (anim.spec.initial_delay.nanos || 0) / 1e9;
        }
        segmentDuration = endKeyframe.time - startKeyframe.time - segmentDelay;
        break;
      }
    }

    if (fromIndex === -1) {
      if (time > this.keyframeTimes[this.keyframeTimes.length - 1].time) {
        toIndex = this.keyframeTimes[this.keyframeTimes.length - 1].index;
        fromIndex = toIndex;
      } else {
        toIndex = this.keyframeTimes[0].index;
        fromIndex = toIndex;
      }
    }

    if (fromIndex !== -1 && fromIndex !== this.currentKeyframeIndex) {
      this.currentKeyframeIndex = fromIndex;
      this.emit("keyframe-changed", fromIndex);
    }

    let value = 0;
    if (time < segmentStartTime + segmentDelay) {
      value = 0;
    } else if (segmentDuration > 0) {
      value = (time - (segmentStartTime + segmentDelay)) / segmentDuration;
    } else {
      value = 0;
    }
    value = Math.max(0, Math.min(1, value));

    const animData = this.variants[toIndex].animation;
    let nodeEasing: EasingFunction = Easing.easeInOutQuad;
    if (
      animData &&
      animData.spec &&
      animData.spec.animation &&
      animData.spec.animation.Smooth
    ) {
      const easingStr = animData.spec.animation.Smooth.easing;
      switch (easingStr) {
        case "Linear":
          nodeEasing = Easing.linear;
          break;
        case "EaseIn":
          nodeEasing = Easing.easeInQuad;
          break;
        case "EaseOut":
          nodeEasing = Easing.easeOutQuad;
          break;
        case "EaseInOut":
          nodeEasing = Easing.easeInOutQuad;
          break;
      }
    }
    const easedValue = nodeEasing(value);

    const fromVariant = this.serializedVariants[fromIndex];
    const toVariant = this.serializedVariants[toIndex];

    const fromNodes = new Map();
    // Cast to AnimationNode (mostly compatible)
    DataMapper.collectAnimationNodes(fromVariant as unknown as AnimationNode, null, true, fromNodes);

    const toNodes = new Map();
    DataMapper.collectAnimationNodes(toVariant as unknown as AnimationNode, null, true, toNodes);

    const allNodeNames = new Set([...fromNodes.keys(), ...toNodes.keys()]);
    const animatedNodes: AnimatedNode[] = [];

    for (const nodeName of allNodeNames) {
      const fromNode = fromNodes.get(nodeName);
      const toNode = toNodes.get(nodeName);
      const timelineNode = this.findNodeByName(
        this.animationData.nodes,
        nodeName === "__ROOT__" ? this.animationData.nodes[0].name : nodeName,
      );

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const props: any = {};

      const propertiesToAnimate = new Set<string>();
      if (timelineNode) {
        timelineNode.timelines.forEach((t) =>
          propertiesToAnimate.add(t.property),
        );
      }
      // Also add the base properties, in case they don't have timelines but are animated between variants
      [
        "x",
        "y",
        "rotation",
        "width",
        "height",
        "opacity",
        "cornerRadius",
        "topLeftRadius",
        "topRightRadius",
        "bottomLeftRadius",
        "bottomRightRadius",
      ].forEach((p) => propertiesToAnimate.add(p));
      // Add all fill properties from the timeline
      if (timelineNode) {
        timelineNode.timelines.forEach((t) => {
           propertiesToAnimate.add(t.property);
        });
      }

      for (const propName of propertiesToAnimate) {
        if (propName === "variant-change") continue;

        const timelineId = timelineNode?.timelines.find(
          (t) => t.property === propName,
        )?.id;
        const timeline = timelineId
          ? this.findKeyframeData(this.animationData.nodes, timelineId)
              ?.timeline
          : null;

        // Always prioritize timeline interpolation if a timeline exists.
        // This ensures we use the correct values for "missing" keyframes (which are locked)
        // and any custom keyframes. The timeline is the source of truth.
        if (timeline) {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          props[propName] = this.getInterpolatedValue(timeline.id, time);
        } else {
          const startValue = this.getNodePropertyValue(fromNode, propName);
          const endValue = this.getNodePropertyValue(toNode, propName);

          if (fromNode && toNode) {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            props[propName] = InterpolationService.interpolate(startValue, endValue, easedValue, propName);
          } else if (toNode) {
            const effectiveStart = propName === "opacity" ? 0 : endValue;
            if (
              typeof effectiveStart === "number" &&
              typeof endValue === "number"
            ) {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              props[propName] =
                effectiveStart + (endValue - effectiveStart) * easedValue;
            } else {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              props[propName] = effectiveStart;
            }
          } else if (fromNode) {
            const effectiveEnd = propName === "opacity" ? 0 : startValue;
            if (
              typeof startValue === "number" &&
              typeof effectiveEnd === "number"
            ) {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              props[propName] =
                startValue + (effectiveEnd - startValue) * easedValue;
            } else {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              props[propName] = startValue;
            }
          }
        }
      }

      // Post-process to fix rotation origin if no custom spatial timelines exist
      if (fromNode && toNode) {
          const spatialProps = ['x', 'y', 'width', 'height', 'rotation'];
        const hasCustomSpatialTimeline = spatialProps.some(prop =>
          timelineNode?.timelines.some(t => t.property === prop && t.keyframes.some(kf => !kf.locked))
          );

          if (!hasCustomSpatialTimeline) {
              const startX = this.getNodePropertyValue(fromNode, 'x') as number;
              const startY = this.getNodePropertyValue(fromNode, 'y') as number;
              const startW = this.getNodePropertyValue(fromNode, 'width') as number;
              const startH = this.getNodePropertyValue(fromNode, 'height') as number;
              const startRot = this.getNodePropertyValue(fromNode, 'rotation') as number;

              const endX = this.getNodePropertyValue(toNode, 'x') as number;
              const endY = this.getNodePropertyValue(toNode, 'y') as number;
              const endW = this.getNodePropertyValue(toNode, 'width') as number;
              const endH = this.getNodePropertyValue(toNode, 'height') as number;
              const endRot = this.getNodePropertyValue(toNode, 'rotation') as number;

              const degreesToRadians = (deg: number) => deg * (Math.PI / 180);

              // Calculate Start Center
              // We negate the angle because updateFigmaPreview applies node.rotation = -props.rotation
              // implying the visual rotation is inverted relative to the DataMapper value.
            const startRotRad = degreesToRadians(-startRot);
              // Center relative to TopLeft(0,0) in local rotated space is (w/2, h/2)
              // Vector d = (w/2, h/2). Rotate d by rot. Add to TopLeft (x,y).
              // Standard rotation: x' = x*cos - y*sin, y' = x*sin + y*cos
              const startCx = startX + (startW / 2) * Math.cos(startRotRad) - (startH / 2) * Math.sin(startRotRad);
              const startCy = startY + (startW / 2) * Math.sin(startRotRad) + (startH / 2) * Math.cos(startRotRad);

              // Calculate End Center
              const endRotRad = degreesToRadians(-endRot);
              const endCx = endX + (endW / 2) * Math.cos(endRotRad) - (endH / 2) * Math.sin(endRotRad);
              const endCy = endY + (endW / 2) * Math.sin(endRotRad) + (endH / 2) * Math.cos(endRotRad);

              // Interpolate Center, Width, Height, Rotation
              const currentCx = startCx + (endCx - startCx) * easedValue;
              const currentCy = startCy + (endCy - startCy) * easedValue;
              const currentW = props.width !== undefined ? props.width : (startW + (endW - startW) * easedValue);
              const currentH = props.height !== undefined ? props.height : (startH + (endH - startH) * easedValue);
              const currentRot = props.rotation !== undefined ? props.rotation : (startRot + (endRot - startRot) * easedValue);

              // Calculate New TopLeft from Interpolated Center
              const currentRotRad = degreesToRadians(-currentRot);
              // To go back from Center to TopLeft: subtract rotated (w/2, h/2)
              const newX = currentCx - ((currentW / 2) * Math.cos(currentRotRad) - (currentH / 2) * Math.sin(currentRotRad));
              const newY = currentCy - ((currentW / 2) * Math.sin(currentRotRad) + (currentH / 2) * Math.cos(currentRotRad));

              props.x = newX;
              props.y = newY;
              // Ensure width/height/rotation are set if not already (though loop above should have set them)
              props.width = currentW;
              props.height = currentH;
              props.rotation = currentRot;
          }
      }



      if (nodeName) {
        animatedNodes.push({
          nodeName: nodeName,
          props: props,
        });
      }
    }

    parent.postMessage(
      {
        pluginMessage: {
          type: "update-figma-preview",
          animatedNodes: animatedNodes,
        },
      },
      "*",
    );
  }

  public getInterpolatedValue(
    timelineId: string,
    time: number,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ): any {
    const findResult = this.findKeyframeData(
      this.animationData.nodes,
      timelineId,
    );
    if (!findResult) return 0;
    const { node, timeline } = findResult;

    return InterpolationService.getInterpolatedValue(
      timeline,
      time,
      this.totalTime,
      this.keyframeTimes,
      this.serializedVariants,
      this.variants,
      node
    );
  }

  public getNodePropertyValue(
    node: AnimationNode,
    propertyName: string,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ): any {
    return DataMapper.getNodePropertyValue(node, propertyName);
  }

  public findKeyframeData(
    nodes: Node[],
    timelineId: string,
  ): { node: Node; timeline: Timeline } | null {
    for (const node of nodes) {
      if (node.timelines) {
        for (const timeline of node.timelines) {
          if (timeline.id === timelineId) {
            return { node, timeline };
          }
        }
      }
      if (node.children && node.children.length > 0) {
        const found = this.findKeyframeData(node.children, timelineId);
        if (found) return found;
      }
    }
    return null;
  }

  public findKeyframeAndTimelineData(
    nodes: Node[],
    keyframeId: string,
  ): { node: Node; timeline: Timeline; keyframe: Keyframe } | null {
    for (const node of nodes) {
      if (node.timelines) {
        for (const timeline of node.timelines) {
          const keyframe = timeline.keyframes.find(
            (kf) => kf.id === keyframeId,
          );
          if (keyframe) {
            return { node, timeline, keyframe };
          }
        }
      }
      if (node.children && node.children.length > 0) {
        const found = this.findKeyframeAndTimelineData(
          node.children,
          keyframeId,
        );
        if (found) return found;
      }
    }
    return null;
  }
}
