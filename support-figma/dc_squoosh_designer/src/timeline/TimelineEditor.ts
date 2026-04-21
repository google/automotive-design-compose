import "./styles.css";
import { AnimationData, Node, Timeline, Keyframe } from "./types";
import { NodeTreeView } from "./NodeTreeView";
import { TimelineGridView } from "./TimelineGridView";
import { getAnimationSegment } from "./utils";
import { TimelineHeaderView } from "./TimelineHeaderView";
import { EventEmitter } from "./EventEmitter";
import { PlaybackController } from "./PlaybackController";

/**
 * The main editor class that orchestrates the timeline UI and interaction.
 * It manages the node tree view, the timeline grid view, and the timeline header view.
 * It also handles user input for zooming, scrolling, and playhead manipulation.
 */
export class TimelineEditor {
  private container: HTMLElement;
  private nodeTreeView: NodeTreeView | null;
  private timelineGridView: TimelineGridView | null;
  private timelineHeaderView: TimelineHeaderView | null;
  private eventEmitter: EventEmitter;
  private playbackController: PlaybackController;
  private data: AnimationData | null = null;
  private zoom: number = 1;
  private wheelHandler: (event: WheelEvent) => void;
  private contextMenuHandler: (event: MouseEvent) => void;
  private mouseMoveHandler: (event: MouseEvent) => void;
  private mouseUpHandler: (event: MouseEvent) => void;
  private isSyncingScroll: boolean = false;
  private playheadPosition: number = 0;
  private isDraggingPlayhead: boolean = false;
  private isThrottlingPlayhead: boolean = false;
  private playheadDragPosition: number = 0;

  constructor(container: HTMLElement, playbackController: PlaybackController) {
    this.container = container;
    this.nodeTreeView = null;
    this.timelineGridView = null;
    this.timelineHeaderView = null;
    this.eventEmitter = new EventEmitter();
    this.playbackController = playbackController;
    this.wheelHandler = this.handleWheel.bind(this);
    this.contextMenuHandler = this.handleContextMenu.bind(this);
    this.mouseMoveHandler = this.handleMouseMove.bind(this);
    this.mouseUpHandler = this.handleMouseUp.bind(this);
    this.render();
    this.setupEventListeners();
    this.container.addEventListener("wheel", this.wheelHandler);
    this.container.addEventListener("contextmenu", this.contextMenuHandler);
  }

  private handleContextMenu(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.closest(".timeline-header")) {
      event.preventDefault();
      this.zoom = 1;
      if (this.timelineHeaderView) {
        this.timelineHeaderView.setZoom(this.zoom);
      }
      if (this.timelineGridView) {
        this.timelineGridView.setZoom(this.zoom);
      }
    }
  }

  private handleWheel(event: WheelEvent): void {
    const target = event.target as HTMLElement;
    const timelineHeader = target.closest(".timeline-header");
    if (!timelineHeader) {
      return;
    }

    event.preventDefault();

    const zoomSpeed = 0.02;
    const oldZoom = this.zoom;

    let newZoom = this.zoom - event.deltaY * zoomSpeed;
    newZoom = Math.max(1, Math.min(20, newZoom));
    this.zoom = newZoom;

    const timelineGrid = this.container.querySelector(
      ".timeline-grid",
    ) as HTMLElement;
    const mouseX = event.clientX - timelineGrid.getBoundingClientRect().left;
    const oldScrollLeft = timelineGrid.scrollLeft;

    const newScrollLeft =
      oldScrollLeft * (this.zoom / oldZoom) +
      mouseX * (this.zoom / oldZoom - 1);

    if (this.timelineHeaderView) {
      this.timelineHeaderView.setZoom(this.zoom);
    }
    if (this.timelineGridView) {
      this.timelineGridView.setZoom(this.zoom);
    }

    requestAnimationFrame(() => {
      timelineGrid.scrollLeft = newScrollLeft;
      this.setPlayheadPosition(this.playheadPosition);
    });
  }

  /**
   * Sets the animation data for the editor to display.
   * This method initializes the views and sets up the scrolling synchronization.
   * @param data The animation data to be visualized.
   */
  public setData(data: AnimationData): void {
    const lockFirstAndLast = (node: Node) => {
      node.timelines.forEach((timeline) => {
        // Only lock first and last keyframes if the timeline is NOT custom
        if (!timeline.isCustom) {
          if (timeline.keyframes.length > 0) {
            timeline.keyframes[0].locked = true;
          }
          if (timeline.keyframes.length > 1) {
            timeline.keyframes[timeline.keyframes.length - 1].locked = true;
          }
        }
      });
      if (node.children) {
        node.children.forEach(lockFirstAndLast);
      }
    };
    data.nodes.forEach(lockFirstAndLast);

    this.data = data;
    this.playbackController.updateAnimationData(this.data);

    const nodeTree = this.container.querySelector(".node-tree") as HTMLElement;
    if (this.nodeTreeView) {
      this.nodeTreeView.setData(data.nodes);
    } else if (nodeTree) {
      this.nodeTreeView = new NodeTreeView(
        nodeTree,
        data.nodes,
        this.eventEmitter,
      );
    }

    const timelineHeaderContainer = this.container.querySelector(
      ".timeline-header",
    ) as HTMLElement;
    if (this.timelineHeaderView) {
      this.timelineHeaderView.setData(data.duration);
    } else if (timelineHeaderContainer) {
      this.timelineHeaderView = new TimelineHeaderView(
        timelineHeaderContainer,
        this.eventEmitter,
        data.duration,
      );
    }

    const timelineGrid = this.container.querySelector(
      ".timeline-grid",
    ) as HTMLElement;
    if (this.timelineGridView) {
      this.timelineGridView.setData(data);
    } else if (timelineGrid && timelineHeaderContainer && nodeTree) {
      this.timelineGridView = new TimelineGridView(
        timelineGrid,
        data,
        this.eventEmitter,
        this.playbackController,
      );

      timelineGrid.addEventListener("scroll", () => {
        if (this.timelineHeaderView) {
          this.timelineHeaderView.setScrollLeft(timelineGrid.scrollLeft);
          const scrollbarWidth =
            timelineGrid.offsetWidth - timelineGrid.clientWidth;
          this.timelineHeaderView.setScrollbarWidth(scrollbarWidth);
        }
        this.setPlayheadPosition(this.playheadPosition);
      });

      timelineGrid.addEventListener("scroll", () => {
        if (!this.isSyncingScroll) {
          this.isSyncingScroll = true;
          nodeTree.scrollTop = timelineGrid.scrollTop;
          requestAnimationFrame(() => {
            this.isSyncingScroll = false;
          });
        }
      });

      nodeTree.addEventListener("scroll", () => {
        if (!this.isSyncingScroll) {
          this.isSyncingScroll = true;
          timelineGrid.scrollTop = nodeTree.scrollTop;
          requestAnimationFrame(() => {
            this.isSyncingScroll = false;
          });
        }
      });
    }
    this.updateDataOutput();
  }

  private updateDataOutput(): void {
    const dataOutput = document.getElementById("data-output");
    if (dataOutput) {
      dataOutput.innerHTML = JSON.stringify(this.data, null, 2);
    }
  }

  /**
   * Returns the current playhead position (0-1).
   */
  public getPlayheadPosition(): number {
    return this.playheadPosition;
  }

  /**
   * Returns the ID of the currently selected keyframe, if any.
   */
  public getSelectedKeyframeId(): string | null {
    if (this.timelineGridView) {
      return this.timelineGridView.getSelectedKeyframeId();
    }
    return null;
  }

  /**
   * Returns the IDs of all currently selected keyframes.
   */
  public getSelectedKeyframeIds(): string[] {
    if (this.timelineGridView) {
      return this.timelineGridView.getSelectedKeyframeIds();
    }
    return [];
  }

  /**
   * Registers an event listener.
   * @param eventName The name of the event to listen for.
   * @param callback The callback function to execute when the event is triggered.
   */
  public on(eventName: string, callback: (...args: unknown[]) => void): void {
    this.eventEmitter.on(eventName, callback);
  }

  /**
   * Removes an event listener.
   * @param eventName The name of the event.
   * @param callback The callback function to remove.
   */
  public off(eventName: string, callback: (...args: unknown[]) => void): void {
    this.eventEmitter.off(eventName, callback);
  }

  /**
   * Emits an event.
   * @param eventName The name of the event to emit.
   * @param args The arguments to pass to the event listeners.
   */
  public emit(eventName: string, ...args: unknown[]): void {
    this.eventEmitter.emit(eventName, ...args);
  }

  /**
   * Returns the current animation data.
   */
  public getData(): AnimationData | null {
    return this.data;
  }

  /**
   * Clears all selections in the timeline.
   */
  public clearAllSelections(): void {
    if (this.timelineGridView) {
      this.timelineGridView.clearAllSelections();
    }
  }

  /**
   * Cleans up the editor and removes all event listeners.
   */
  public destroy(): void {
    if (this.nodeTreeView) {
      this.nodeTreeView.destroy();
    }
    if (this.timelineGridView) {
      this.timelineGridView.destroy();
    }
    if (this.timelineHeaderView) {
      this.timelineHeaderView.destroy();
    }
    this.container.removeEventListener("wheel", this.wheelHandler);
    this.container.removeEventListener("contextmenu", this.contextMenuHandler);
    window.removeEventListener("mousemove", this.mouseMoveHandler);
    window.removeEventListener("mouseup", this.mouseUpHandler);
    this.container.innerHTML = "";
    this.nodeTreeView = null;
    this.timelineGridView = null;
    this.timelineHeaderView = null;
  }

  private findTimelineByProperty(
    nodeId: string,
    propertyName: string,
  ): Timeline | null {
    if (!this.data) return null;
    const find = (nodes: Node[]): Timeline | null => {
      for (const node of nodes) {
        if (node.id === nodeId) {
          const timeline = node.timelines.find(
            (t) => t.property === propertyName,
          );
          return timeline || null;
        }
        if (node.children && node.children.length > 0) {
          const found = find(node.children);
          if (found) return found;
        }
      }
      return null;
    };
    return find(this.data.nodes);
  }

  private setupEventListeners(): void {
    this.eventEmitter.on(
      "node:toggle",
      (nodeId: string, isCollapsed: boolean) => {
        if (this.timelineGridView) {
          this.timelineGridView.toggleNodeVisibility(nodeId, isCollapsed);
        }
      },
    );

    this.eventEmitter.on(
      "timeline:add",
      (nodeId: string, propertyName: string) => {
        if (!this.data) return;

        const findAndAddTimeline = (nodes: Node[]): boolean => {
          for (const node of nodes) {
            if (node.id === nodeId) {
              if (node.timelines.find((t) => t.property === propertyName)) {
                alert(
                  `Property "${propertyName}" already exists on this node.`,
                );
                return false;
              }
              const newTimeline: Timeline = {
                id: `timeline-${Date.now()}`,
                property: propertyName,
                keyframes: [
                  {
                    id: `kf-${Date.now()}-1`,
                    position: 0,
                    value: 0,
                    locked: false,
                    easing: "Linear",
                  },
                  {
                    id: `kf-${Date.now()}-2`,
                    position: 1,
                    value: 0,
                    locked: false,
                    easing: "Linear",
                  },
                ],
                isCustom: true,
              };
              node.timelines.push(newTimeline);
              return true;
            }
            if (node.children && node.children.length > 0) {
              if (findAndAddTimeline(node.children)) {
                return true;
              }
            }
          }
          return false;
        };

        if (findAndAddTimeline(this.data.nodes)) {
          // Re-render everything for simplicity
          this.setData(this.data);
          if (this.timelineGridView) {
            this.timelineGridView.setZoom(this.zoom);
          }
          this.updateDataOutput();
          const newTimeline = this.findTimelineByProperty(nodeId, propertyName);
          if (newTimeline && this.nodeTreeView) {
            this.nodeTreeView.setEditingTimeline(newTimeline.id);
          }
        }
      },
    );

    this.eventEmitter.on("scroll:pan", (delta: number) => {
      const timelineGrid = this.container.querySelector(
        ".timeline-grid",
      ) as HTMLElement;
      if (timelineGrid) {
        timelineGrid.scrollLeft += delta;
      }
    });

    this.eventEmitter.on("scroll:pan", (delta: number) => {
      const timelineGrid = this.container.querySelector(
        ".timeline-grid",
      ) as HTMLElement;
      if (timelineGrid) {
        timelineGrid.scrollLeft += delta;
      }
    });

    this.eventEmitter.on("playhead:set", (position: number) => {
      this.playheadPosition = position;

      this.setPlayheadPosition(position);
    });

    this.eventEmitter.on(
      "keyframe:add",
      (timelineId: string, position: number) => {
        if (!this.data) return;

        const findAndAddKeyframe = (nodes: Node[]): boolean => {
          for (const node of nodes) {
            for (const timeline of node.timelines) {
              if (timeline.id === timelineId) {
                const totalDuration = this.data.duration;
                const keyframeTimes =
                  this.playbackController.getKeyframeTimes();
                const segment = getAnimationSegment(
                  position,
                  keyframeTimes,
                  totalDuration,
                  this.playbackController.getVariants(),
                );

                if (segment && position > segment.delayEndPos) {
                  const newKeyframeId = `${timeline.id}-custom-${Date.now()}`;
                  const value = this.playbackController.getInterpolatedValue(
                    timeline.id,
                    position * totalDuration,
                  );
                  const newKeyframe: Keyframe = {
                    id: newKeyframeId,
                    position: position,
                    value: value,
                    locked: false,
                  };
                  timeline.keyframes.push(newKeyframe);
                  timeline.keyframes.sort((a, b) => a.position - b.position);
                  this.setData(this.data);
                  this.emit(
                    "custom-keyframe:added",
                    timeline.id,
                    newKeyframeId,
                  );
                  // Emit event to save the new keyframe data
                  this.emit("keyframe:saved", timeline.id, newKeyframeId); 
                }
                return true;
              }
            }

            if (node.children && node.children.length > 0) {
              if (findAndAddKeyframe(node.children)) return true;
            }
          }

          return false;
        };

        if (findAndAddKeyframe(this.data.nodes)) {
          this.setData(this.data);

          if (this.timelineGridView) {
            this.timelineGridView.setZoom(this.zoom);
          }
        }
      },
    );

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    this.eventEmitter.on("keyframe:remove", (_keyframeId: string) => {
      if (!this.data) return;

      const findAndRemoveKeyframe = (
        nodes: Node[],
        keyframeId: string,
      ): boolean => {
        for (const node of nodes) {
          for (const timeline of node.timelines) {
            const keyframeIndex = timeline.keyframes.findIndex(
              (kf) => kf.id === keyframeId,
            );
            if (keyframeIndex !== -1) {
              const keyframe = timeline.keyframes[keyframeIndex];
              if (keyframe.position === 0 || keyframe.position === 1) {
                return true; // Don't remove start or end keyframes
              }
              timeline.keyframes.splice(keyframeIndex, 1);
              return true;
            }
          }
          if (node.children && node.children.length > 0) {
            if (findAndRemoveKeyframe(node.children, keyframeId)) {
              return true;
            }
          }
        }
        return false;
      };

      if (findAndRemoveKeyframe(this.data.nodes, _keyframeId)) {
        if (this.timelineGridView) {
          this.timelineGridView.setZoom(this.zoom);
        }
        this.updateDataOutput();
      }
    });

    this.eventEmitter.on(
      "keyframe:move",
      (keyframeId: string, newPosition: number) => {
        if (!this.data) return;

        const findAndUpdateKeyframe = (nodes: Node[]): boolean => {
          for (const node of nodes) {
            for (const timeline of node.timelines) {
              const keyframeIndex = timeline.keyframes.findIndex(
                (kf) => kf.id === keyframeId,
              );
              if (keyframeIndex !== -1) {
                const keyframe = timeline.keyframes[keyframeIndex];
                if (keyframe.locked) return true;

                // 1. Find boundaries from locked keyframes
                const prevLockedKf = timeline.keyframes
                  .slice(0, keyframeIndex)
                  .reverse()
                  .find((kf) => kf.locked);
                const nextLockedKf = timeline.keyframes
                  .slice(keyframeIndex + 1)
                  .find((kf) => kf.locked);
                const minPos = prevLockedKf ? prevLockedKf.position : 0;
                const maxPos = nextLockedKf ? nextLockedKf.position : 1;

                let clampedPos = Math.max(
                  minPos,
                  Math.min(maxPos, newPosition),
                );
                const keyframeTimes =
                  this.playbackController.getKeyframeTimes();
                if (keyframeTimes && keyframeTimes.length > 1) {
                  for (let i = 0; i < keyframeTimes.length - 1; i++) {
                    const segmentStart = keyframeTimes[i];
                    const segmentEnd = keyframeTimes[i + 1];

                    const anim = this.playbackController.getVariant(
                      segmentEnd.index,
                    ).animation;
                    let duration = 0;
                    if (
                      anim &&
                      anim.spec &&
                      anim.spec.animation &&
                      anim.spec.animation.Smooth
                    ) {
                      const d = anim.spec.animation.Smooth.duration;
                      duration = (d.secs || 0) + (d.nanos || 0) / 1e9;
                    }

                    const delayDuration =
                      segmentEnd.time - segmentStart.time - duration;

                    if (delayDuration > 0.001) {
                      const delayStartPos =
                        segmentStart.time / this.data.duration;
                      const activeStartPos =
                        (segmentStart.time + delayDuration) /
                        this.data.duration;

                      if (
                        newPosition > delayStartPos &&
                        newPosition < activeStartPos
                      ) {
                        if (keyframe.position > activeStartPos) {
                          clampedPos = activeStartPos;
                        } else {
                          clampedPos = delayStartPos;
                        }
                      }
                    }
                  }
                }

                const finalPos = Math.max(minPos, Math.min(maxPos, clampedPos));

                if (finalPos !== keyframe.position) {
                  keyframe.position = finalPos;
                  timeline.keyframes.sort((a, b) => a.position - b.position);
                }

                // 3. Always emit the final position back to the view
                this.eventEmitter.emit(
                  "keyframe:position-corrected",
                  keyframeId,
                  finalPos,
                );
                return true;
              }
            }
            if (node.children && node.children.length > 0) {
              if (findAndUpdateKeyframe(node.children)) return true;
            }
          }
          return false;
        };

        if (findAndUpdateKeyframe(this.data.nodes)) {
          this.updateDataOutput();
        }
      },
    );

    this.eventEmitter.on("keyframe:toggle-lock", (keyframeId: string) => {
      if (!this.data) return;

      const findAndToggleKeyframeLock = (nodes: Node[]): boolean => {
        for (const node of nodes) {
          for (const timeline of node.timelines) {
            const keyframe = timeline.keyframes.find(
              (kf) => kf.id === keyframeId,
            );
            if (keyframe) {
              if (keyframe.position === 0 || keyframe.position === 1) {
                return true; // Don't unlock start or end keyframes
              }
              keyframe.locked = !keyframe.locked;
              return true;
            }
          }
          if (node.children && node.children.length > 0) {
            if (findAndToggleKeyframeLock(node.children)) {
              return true;
            }
          }
        }
        return false;
      };

      if (findAndToggleKeyframeLock(this.data.nodes)) {
        if (this.timelineGridView) {
          this.timelineGridView.setZoom(this.zoom);
        }
        this.updateDataOutput();
      }
    });

    this.eventEmitter.on(
      "keyframe:selected",
      (_keyframeId: string | null) => {},
    );

    const playheadHandle = this.container.querySelector(
      ".playhead-handle",
    ) as HTMLElement;
    if (playheadHandle) {
      playheadHandle.addEventListener("mousedown", (_event: MouseEvent) => {
        this.isDraggingPlayhead = true;
        document.body.classList.add("no-select");
      });
    }

    window.addEventListener("mousemove", this.mouseMoveHandler);
    window.addEventListener("mouseup", this.mouseUpHandler);
  }

  private handleMouseMove(event: MouseEvent): void {
    if (this.isDraggingPlayhead) {
      const timelineGrid = this.container.querySelector(
        ".timeline-grid",
      ) as HTMLElement;
      if (timelineGrid) {
        const rect = timelineGrid.getBoundingClientRect();
        const position =
          (event.clientX - rect.left + timelineGrid.scrollLeft) /
          timelineGrid.scrollWidth;
        this.playheadDragPosition = Math.max(0, Math.min(1, position));

        if (!this.isThrottlingPlayhead) {
          this.isThrottlingPlayhead = true;
          requestAnimationFrame(() => {
            this.emit("playhead:set", this.playheadDragPosition);
            this.isThrottlingPlayhead = false;
          });
        }
      }
    }
  }

  private handleMouseUp(): void {
    if (this.isDraggingPlayhead) {
      this.isDraggingPlayhead = false;
      document.body.classList.remove("no-select");
    }
  }

  private setPlayheadPosition(position: number): void {
    const playhead = this.container.querySelector(".playhead") as HTMLElement;
    const timelineGrid = this.container.querySelector(
      ".timeline-grid",
    ) as HTMLElement;
    if (playhead && timelineGrid) {
      // Calculate the absolute pixel position within the *total scrollable width*
      const absolutePlayheadX = position * timelineGrid.scrollWidth;
      // Calculate the position relative to the *visible area* of the timelineGrid
      const relativePlayheadX = absolutePlayheadX - timelineGrid.scrollLeft;
      playhead.style.transform = `translateX(${relativePlayheadX}px)`;
    }
  }

  private render(): void {
    this.container.innerHTML = `
      <div class="timeline-editor">
        <div class="node-tree-container">
          <div class="node-tree-header"></div>
          <div class="node-tree"></div>
        </div>
        <div class="timeline-content">
          <div class="timeline-header"></div>
          <div class="timeline-grid"></div>
          <div class="playhead"><div class="playhead-handle"></div></div>
        </div>
      </div>
    `;
  }
}
