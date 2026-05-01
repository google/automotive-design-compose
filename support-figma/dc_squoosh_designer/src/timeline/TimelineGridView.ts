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

import { AnimationData, Node, Keyframe } from "./types";
import { EventEmitter } from "./EventEmitter";
import { getAnimationSegment } from "./utils";

/**
 * Renders the timeline grid, including keyframes, tracks, and current time indicator.
 * Handles interactions like dragging keyframes, selecting ranges, and scrolling.
 */
export class TimelineGridView {
  private container: HTMLElement;
  private data: AnimationData;
  private eventEmitter: EventEmitter;
  private draggedKeyframe: HTMLElement | null = null;
  private timelineRect: DOMRect | null = null;
  private selectedKeyframeIds: Set<string> = new Set();
  private selectionRange: { start: number; end: number } | null = null;
  private currentTimeline: HTMLElement | null = null;
  private clickHandler: (event: MouseEvent) => void;
  private mouseDownHandler: (event: MouseEvent) => void;
  private mouseMoveHandler: (event: MouseEvent) => void;
  private mouseUpHandler: (event: MouseEvent) => void;
  private dblClickHandler: (event: MouseEvent) => void;
  private contextMenuHandler: (event: MouseEvent) => void;
  private zoom: number = 1;
  private wasDragging: boolean = false;
  private selectedTimelineId: string | null = null;

  private selectedTimeline: HTMLElement | null = null;

  private playbackController: PlaybackController;
  constructor(
    container: HTMLElement,
    data: AnimationData,
    eventEmitter: EventEmitter,
    playbackController: PlaybackController,
  ) {
    this.container = container;
    this.data = data;
    this.eventEmitter = eventEmitter;
    this.playbackController = playbackController;
    this.clickHandler = this.handleClick.bind(this);
    this.mouseDownHandler = this.handleMouseDown.bind(this);
    this.mouseMoveHandler = this.handleMouseMove.bind(this);
    this.mouseUpHandler = this.handleMouseUp.bind(this);
    this.dblClickHandler = this.handleDblClick.bind(this);
    this.contextMenuHandler = this.handleContextMenu.bind(this);
    this.render();
    this.addEventListeners();

    this.eventEmitter.on(
      "keyframe:position-corrected",
      (keyframeId: string, correctedPosition: number) => {
        const keyframeElement = this.container.querySelector(
          `[data-keyframe-id="${keyframeId}"]`,
        ) as HTMLElement;
        if (keyframeElement) {
          keyframeElement.style.left = `${correctedPosition * 100}%`;
        }
      },
    );

    this.eventEmitter.on("keyframe:select_in_view", (keyframeId: string) => {
      this.clearAllSelections(true);
      this.selectedKeyframeIds.add(keyframeId);
      this.eventEmitter.emit("keyframe:selected", keyframeId);
      this.updateKeyframeSelections();

      const keyframeEl = this.container.querySelector(
        `[data-keyframe-id="${keyframeId}"]`,
      ) as HTMLElement;
      if (keyframeEl) {
        const timelineEl = keyframeEl.closest(".timeline") as HTMLElement;
        if (timelineEl) {
          timelineEl.classList.add("selected");
          this.selectedTimeline = timelineEl;
          this.selectedTimelineId = timelineEl.dataset.timelineId || null;
        }
      }
    });
  }

  private addEventListeners(): void {
    this.container.addEventListener("click", this.clickHandler);
    this.container.addEventListener("mousedown", this.mouseDownHandler);
    this.container.addEventListener("mousemove", this.mouseMoveHandler);
    this.container.addEventListener("mouseup", this.mouseUpHandler);
    this.container.addEventListener("dblclick", this.dblClickHandler);
    this.container.addEventListener("contextmenu", this.contextMenuHandler);
  }

  private handleContextMenu(event: MouseEvent): void {
    event.preventDefault();
    const target = event.target as HTMLElement;
    if (target.classList.contains("keyframe")) {
      const keyframeId = target.dataset.keyframeId;
      if (keyframeId) {
        this.eventEmitter.emit("keyframe:toggle-lock", keyframeId);
      }
    }
  }

  private handleDblClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.classList.contains("keyframe")) {
      const keyframeId = target.dataset.keyframeId;
      if (keyframeId) {
        const keyframe = this.findKeyframe(keyframeId);
        if (keyframe && !keyframe.locked) {
          const timeline = this.findTimelineByKeyframe(keyframeId);
          if (timeline) {
            this.eventEmitter.emit("keyframe:remove", keyframeId);
            this.eventEmitter.emit(
              "keyframe:save-on-remove",
              timeline.id,
              keyframe.position,
            );
          }
        }
      }
    } else if (
      target.classList.contains("timeline-track") ||
      target.classList.contains("keyframe-container")
    ) {
      const timeline = target.closest(".timeline") as HTMLElement;
      const timelineId = timeline.dataset.timelineId;
      if (timelineId && timelineId !== "variant-change") {
        const rect = target.getBoundingClientRect();
        const position = (event.clientX - rect.left) / rect.width;
        const clampedPosition = Math.max(0, Math.min(1, position));
        this.eventEmitter.emit("keyframe:add", timelineId, clampedPosition);
      }
    }
  }

  private handleClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const isKeyframe = target.classList.contains("keyframe");

    if (this.wasDragging) {
      this.wasDragging = false;
      return;
    }

    const timeline = target.closest(".timeline") as HTMLElement;
    if (timeline && this.selectedTimeline !== timeline) {
      if (this.selectedTimeline) {
        this.selectedTimeline.classList.remove("selected");
      }
      timeline.classList.add("selected");
      this.selectedTimeline = timeline;
    }

    if (!isKeyframe) {
      if (!event.shiftKey) {
        this.clearAllSelections();
      }

      const clickedTimeline = target.closest(".timeline") as HTMLElement;
      if (clickedTimeline) {
        const timelineId = clickedTimeline.dataset.timelineId;
        if (!timelineId) return;

        if (this.selectedTimelineId !== timelineId) {
          this.selectedTimelineId = timelineId;
        }

        const timelineData = this.findTimeline(timelineId);
        if (!timelineData) return;

        const rect = clickedTimeline.getBoundingClientRect();
        const clickPosition = (event.clientX - rect.left) / rect.width;

        this.selectionRange = this.calculateSelectionRange(
          clickedTimeline,
          clickPosition,
        );

        if (this.selectionRange) {
          this.eventEmitter.emit(
            "section:selected",
            timelineId,
            this.selectionRange.start,
            this.selectionRange.end,
          );
        }

        this.updateSelection(clickedTimeline);
      }
    }
  }

  /**
   * Calculates the start and end positions for a selection range based on a click position
   * and existing keyframes/delays within a timeline.
   * @param clickedTimeline The HTML element of the timeline that was clicked.
   * @param clickPosition The normalized click position (0-1) within the timeline.
   * @returns An object containing the start and end positions of the calculated selection range.
   */
  private calculateSelectionRange(
    clickedTimeline: HTMLElement,
    clickPosition: number,
  ): { start: number; end: number } {
    const timelineId = clickedTimeline.dataset.timelineId;
    if (!timelineId) return { start: 0, end: 1 };

    const timelineData = this.findTimeline(timelineId);
    if (!timelineData) return { start: 0, end: 1 };

    const boundaryPoints = new Set<number>();
    boundaryPoints.add(0);
    boundaryPoints.add(1);

    timelineData.keyframes.forEach((kf) => boundaryPoints.add(kf.position));

    const keyframeTimes = this.playbackController.getKeyframeTimes();
    if (keyframeTimes && keyframeTimes.length > 1) {
      for (let i = 0; i < keyframeTimes.length - 1; i++) {
        const startKeyframe = keyframeTimes[i];
        const endKeyframe = keyframeTimes[i + 1];
        const segmentStartTime = startKeyframe.time;
        const segmentEndTime = endKeyframe.time;
        const segmentDuration = segmentEndTime - segmentStartTime;

        const toIndex = endKeyframe.index;
        const anim = this.playbackController.getVariant(toIndex).animation;
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

        const segmentDelay = segmentDuration - duration;

        if (segmentDelay > 0.001) {
          const delayStartPos = segmentStartTime / this.data.duration;
          const delayEndPos =
            (segmentStartTime + segmentDelay) / this.data.duration;
          boundaryPoints.add(delayStartPos);
          boundaryPoints.add(delayEndPos);
        }
      }
    }

    const sortedBoundaries = Array.from(boundaryPoints).sort((a, b) => a - b);

    let start = 0;
    let end = 1;
    for (let i = 0; i < sortedBoundaries.length - 1; i++) {
      const segmentStart = sortedBoundaries[i];
      const segmentEnd = sortedBoundaries[i + 1];
      if (clickPosition >= segmentStart && clickPosition < segmentEnd) {
        start = segmentStart;
        end = segmentEnd;
        break;
      }
    }
    if (clickPosition >= sortedBoundaries[sortedBoundaries.length - 1]) {
      start = sortedBoundaries[sortedBoundaries.length - 2];
      end = sortedBoundaries[sortedBoundaries.length - 1];
    }

    return { start, end };
  }

  private handleMouseDown(event: MouseEvent): void {
    this.wasDragging = false;
    const target = event.target as HTMLElement;
    if (target.classList.contains("keyframe")) {
      this.handleKeyframeMouseDown(event, target);
    } else if (
      target.classList.contains("timeline-track") ||
      target.classList.contains("keyframe-container")
    ) {
      this.handleTimelineMouseDown(event, target);
    }
  }

  /**
   * Handles the mouse down event on a timeline track.
   * This method is responsible for initiating range selection on the timeline.
   * @param event The mouse down event.
   * @param target The timeline track element that was clicked.
   */
  private handleTimelineMouseDown(
    event: MouseEvent,
    target: HTMLElement,
  ): void {
    if (!event.shiftKey) {
      this.clearAllSelections();
    }
    this.currentTimeline = target.closest(".timeline");
    if (this.currentTimeline) {
      const timelineId = this.currentTimeline.dataset.timelineId;
      if (timelineId && this.selectedTimelineId !== timelineId) {
        this.selectedTimelineId = timelineId;
      }
      if (this.selectedTimeline) {
        this.selectedTimeline.classList.remove("selected");
      }
      this.currentTimeline.classList.add("selected");
      this.selectedTimeline = this.currentTimeline;
    }
    this.timelineRect = this.currentTimeline?.getBoundingClientRect() || null;
    if (this.timelineRect) {
      const position =
        (event.clientX - this.timelineRect.left) / this.timelineRect.width;
      this.selectionRange = { start: position, end: position };
      const selectionDiv = this.currentTimeline.querySelector(
        ".selection",
      ) as HTMLElement;
      if (selectionDiv) {
        selectionDiv.classList.add("dragging");
      }
    }
  }

  /**
   * Handles the mouse down event on a keyframe.
   * This method is responsible for selecting and initiating dragging of keyframes.
   * @param event The mouse down event.
   * @param target The keyframe element that was clicked.
   */
  private handleKeyframeMouseDown(
    event: MouseEvent,
    target: HTMLElement,
  ): void {
    const keyframeId = target.dataset.keyframeId;
    if (!keyframeId) return;
    const keyframe = this.findKeyframe(keyframeId);
    if (keyframe && !keyframe.locked) {
      this.draggedKeyframe = target;
      this.timelineRect =
        target.closest(".timeline")?.getBoundingClientRect() || null;
      document.body.classList.add("dragging");
    }

    // Handle keyframe selection
    if (!this.selectedKeyframeIds.has(keyframeId)) {
      this.clearAllSelections(true); // Clear all selections before selecting a new keyframe
      this.selectedKeyframeIds.add(keyframeId);
      this.eventEmitter.emit("keyframe:selected", keyframeId);
    } else if (event.shiftKey) {
      this.selectedKeyframeIds.delete(keyframeId);
      this.eventEmitter.emit("keyframe:deselected", keyframeId);
    }
    this.updateKeyframeSelections();
  }

  private findKeyframe(keyframeId: string) {
    const find = (nodes: Node[]): Keyframe | null => {
      for (const node of nodes) {
        for (const timeline of node.timelines) {
          const keyframe = timeline.keyframes.find(
            (kf) => kf.id === keyframeId,
          );
          if (keyframe) {
            return keyframe;
          }
        }
        const found = find(node.children);
        if (found) {
          return found;
        }
      }
      return null;
    };
    return find(this.data.nodes);
  }

  private findTimelineByKeyframe(keyframeId: string): Timeline | null {
    const find = (nodes: Node[]): Timeline | null => {
      for (const node of nodes) {
        for (const timeline of node.timelines) {
          const keyframe = timeline.keyframes.find(
            (kf) => kf.id === keyframeId,
          );
          if (keyframe) {
            return timeline;
          }
        }
        const found = find(node.children);
        if (found) {
          return found;
        }
      }
      return null;
    };
    return find(this.data.nodes);
  }

  /**
   * Clears all selected keyframes and timelines.
   * @param silent If true, suppresses the "selection:cleared" event.
   */
  public clearAllSelections(silent: boolean = false): void {
    this.container.querySelectorAll(".timeline").forEach((timelineEl) => {
      const selectionDiv = timelineEl.querySelector(
        ".selection",
      ) as HTMLElement;
      if (selectionDiv) {
        selectionDiv.style.width = "0%";
        selectionDiv.style.left = "0%";
      }
      timelineEl.classList.remove("selected");
    });
    this.selectedKeyframeIds.clear();
    this.selectedTimeline = null;
    this.selectedTimelineId = null;
    this.selectionRange = null;
    this.updateKeyframeSelections();
    if (!silent) {
      this.eventEmitter.emit("selection:cleared");
    }
  }

  private findTimeline(timelineId: string): Timeline | null {
    const find = (nodes: Node[]): Timeline | null => {
      for (const node of nodes) {
        for (const timeline of node.timelines) {
          if (timeline.id === timelineId) {
            return timeline;
          }
        }
        if (node.children && node.children.length > 0) {
          const found = find(node.children);
          if (found) {
            return found;
          }
        }
      }
      return null;
    };
    return find(this.data.nodes);
  }

  private handleMouseMove(event: MouseEvent): void {
    if (this.draggedKeyframe && this.timelineRect) {
      this.handleKeyframeDrag(event);
    } else if (
      this.timelineRect &&
      this.selectionRange &&
      this.currentTimeline
    ) {
      this.handleRangeSelectionDrag(event);
    }
  }

  /**
   * Handles the dragging of a keyframe.
   * @param event The mouse move event.
   */
  private handleKeyframeDrag(event: MouseEvent): void {
    this.wasDragging = true;
    const keyframeId = this.draggedKeyframe!.dataset.keyframeId;
    if (!keyframeId) return;

    const keyframe = this.findKeyframe(keyframeId);
    if (
      !keyframe ||
      keyframe.position === 0 ||
      keyframe.position === 1 ||
      keyframe.locked
    ) {
      return;
    }

    const newPosition =
      (event.clientX - this.timelineRect!.left) / this.timelineRect!.width;
    let clampedPos = Math.max(0, Math.min(1, newPosition));

    const keyframeTimes = this.playbackController.getKeyframeTimes();
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

        const delayDuration = segmentEnd.time - segmentStart.time - duration;

        if (delayDuration > 0.001) {
          const delayStartPos = segmentStart.time / this.data.duration;
          const activeStartPos =
            (segmentStart.time + delayDuration) / this.data.duration;

          if (newPosition > delayStartPos && newPosition < activeStartPos) {
            const currentPos =
              parseFloat(this.draggedKeyframe!.style.left) / 100;
            if (currentPos > activeStartPos) {
              clampedPos = activeStartPos;
            } else {
              clampedPos = delayStartPos;
            }
          }
        }
      }
    }

    this.draggedKeyframe!.style.left = `${clampedPos * 100}%`;
    this.eventEmitter.emit("keyframe:move", keyframeId, clampedPos);
  }

  /**
   * Handles the dragging for range selection.
   * @param event The mouse move event.
   */
  private handleRangeSelectionDrag(event: MouseEvent): void {
    this.wasDragging = true;
    const position =
      (event.clientX - this.timelineRect!.left) / this.timelineRect!.width;
    this.selectionRange!.end = position;
    this.updateSelection(this.currentTimeline!);
  }

  /**
   * Returns the ID of the single selected keyframe, if any.
   */
  public getSelectedKeyframeId(): string | null {
    if (this.selectedKeyframeIds.size > 0) {
      return this.selectedKeyframeIds.values().next().value;
    }
    return null;
  }

  /**
   * Returns a list of all selected keyframe IDs.
   */
  public getSelectedKeyframeIds(): string[] {
    return Array.from(this.selectedKeyframeIds);
  }

  private handleMouseUp(event: MouseEvent): void {
    if (this.draggedKeyframe) {
      if (this.wasDragging) {
        const keyframeId = this.draggedKeyframe.dataset.keyframeId;
        const finalPosition = parseFloat(this.draggedKeyframe.style.left) / 100;
        if (keyframeId) {
          this.eventEmitter.emit(
            "keyframe:move_end",
            keyframeId,
            finalPosition,
          );
        }
      }
      this.draggedKeyframe = null;
      this.timelineRect = null;
      document.body.classList.remove("dragging");
      this.updateKeyframeSelections();
    } else if (this.selectionRange) {
      if (this.currentTimeline) {
        const selectionDiv = this.currentTimeline.querySelector(
          ".selection",
        ) as HTMLElement;
        if (selectionDiv) {
          selectionDiv.classList.remove("dragging");
        }

        const timelineId = this.currentTimeline.dataset.timelineId;
        if (timelineId) {
          const timelineData = this.findTimeline(timelineId);
          if (timelineData) {
            const selectionStart = Math.min(
              this.selectionRange.start,
              this.selectionRange.end,
            );
            const selectionEnd = Math.max(
              this.selectionRange.start,
              this.selectionRange.end,
            );
            const selectedKeyframes = timelineData.keyframes.filter(
              (kf) =>
                kf.position >= selectionStart && kf.position <= selectionEnd,
            );

            if (selectedKeyframes.length > 0) {
              if (!event.shiftKey) {
                this.clearAllSelections();
              }
              this.eventEmitter.emit("keyframes:selected", selectedKeyframes);
              selectedKeyframes.forEach((kf) =>
                this.selectedKeyframeIds.add(kf.id),
              );
              this.updateKeyframeSelections();
            } else {
              // No keyframes selected, check if selection is within a single section
              const boundaryPoints = new Set<number>();
              boundaryPoints.add(0);
              boundaryPoints.add(1);
              timelineData.keyframes.forEach((kf) =>
                boundaryPoints.add(kf.position),
              );

              const keyframeTimes = this.playbackController.getKeyframeTimes();
              if (keyframeTimes && keyframeTimes.length > 1) {
                for (let i = 0; i < keyframeTimes.length - 1; i++) {
                  const startKeyframe = keyframeTimes[i];
                  const endKeyframe = keyframeTimes[i + 1];
                  const segmentStartTime = startKeyframe.time;
                  const segmentEndTime = endKeyframe.time;
                  const segmentDuration = segmentEndTime - segmentStartTime;

                  const toIndex = endKeyframe.index;
                  const anim =
                    this.playbackController.getVariant(toIndex).animation;
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

                  const segmentDelay = segmentDuration - duration;

                  if (segmentDelay > 0.001) {
                    const delayStartPos = segmentStartTime / this.data.duration;
                    const delayEndPos =
                      (segmentStartTime + segmentDelay) / this.data.duration;
                    boundaryPoints.add(delayStartPos);
                    boundaryPoints.add(delayEndPos);
                  }
                }
              }

              const sortedBoundaries = Array.from(boundaryPoints).sort(
                (a, b) => a - b,
              );
              let containingSection: { start: number; end: number } | null =
                null;
              for (let i = 0; i < sortedBoundaries.length - 1; i++) {
                const sectionStart = sortedBoundaries[i];
                const sectionEnd = sortedBoundaries[i + 1];
                if (
                  selectionStart >= sectionStart &&
                  selectionEnd <= sectionEnd
                ) {
                  containingSection = { start: sectionStart, end: sectionEnd };
                  break;
                }
              }

              if (containingSection) {
                this.eventEmitter.emit(
                  "section:selected",
                  timelineId,
                  containingSection.start,
                  containingSection.end,
                );
                this.selectionRange = containingSection;
                this.updateSelection(this.currentTimeline);
              } else {
                this.clearAllSelections();
              }
            }
          }
        }
      }
      this.currentTimeline = null;
      if (!this.wasDragging) {
        this.selectionRange = null;
      }
    }
  }

  /**
   * Updates the zoom level and re-renders the grid.
   * @param zoom The new zoom level.
   */
  public setZoom(zoom: number): void {
    this.zoom = zoom;
    this.container.innerHTML = `<div class="grid-content-wrapper" style="width: ${100 * this.zoom}%">${this.renderNodes(this.data.nodes)}</div>`;

    if (this.selectedTimelineId) {
      const timelineEl = this.container.querySelector(
        `[data-timeline-id="${this.selectedTimelineId}"]`,
      ) as HTMLElement;
      if (timelineEl) {
        this.selectedTimeline = timelineEl;
        timelineEl.classList.add("selected");
        if (this.selectionRange) {
          this.updateSelection(timelineEl);
        }
      }
    }

    this.updateKeyframeSelections();
  }

  /**
   * Updates the animation data and re-renders the grid.
   * @param data The new animation data.
   */
  public setData(data: AnimationData): void {
    this.data = data;
    this.render();
    
    // Restore selections
    this.updateKeyframeSelections();

    if (this.selectedTimelineId) {
        const timelineEl = this.container.querySelector(
            `[data-timeline-id="${this.selectedTimelineId}"]`,
        ) as HTMLElement;
        
        if (timelineEl) {
            this.selectedTimeline = timelineEl;
            timelineEl.classList.add("selected");
            if (this.selectionRange) {
                this.updateSelection(timelineEl);
            }
        } else {
            // Timeline might have been deleted
            this.selectedTimeline = null;
            this.selectedTimelineId = null;
            this.selectionRange = null;
        }
    }
  }

  private render(): void {
    this.container.innerHTML = `<div class="grid-content-wrapper" style="width: ${100 * this.zoom}%">${this.renderNodes(this.data.nodes)}</div>`;
  }

  private renderNodes(nodes: Node[]): string {
    return `${nodes.map((node) => this.renderNode(node)).join("")}`;
  }

  private renderNode(node: Node): string {
    const totalDuration = this.data.duration;
    const keyframeTimes = this.playbackController.getKeyframeTimes();
    return `
      <div class="timeline-node" data-node-id="${node.id}">
        <div class="node-name-placeholder">${node.name}</div>
        ${node.timelines
          .map((timeline) => {
            const keyframes = timeline.keyframes;
            let delayBars = "";
            if (keyframeTimes && keyframeTimes.length > 1) {
              for (let i = 0; i < keyframeTimes.length - 1; i++) {
                const segment = getAnimationSegment(
                  keyframeTimes[i].time / totalDuration,
                  keyframeTimes,
                  totalDuration,
                  this.playbackController.getVariants(),
                );
                if (segment && segment.delay > 0) {
                  delayBars += `<div class="delay-bar" style="left: ${
                    segment.startPos * 100
                  }%; width: ${
                    (segment.delayEndPos - segment.startPos) * 100
                  }%"></div>`;
                }
              }
            }

            let specialUI = "";
            if (timeline.property.endsWith(".arc")) {
              specialUI = `<div class="arc-editor"></div>`;
            }

            return `
            <div class="timeline" data-timeline-id="${timeline.id}">
              <div class="timeline-track">
                <div class="keyframe-container">
                  ${delayBars}
                  <div class="selection"></div>
                  ${keyframes
                    .map(
                      (keyframe) => `
                    <div class="keyframe ${keyframe.locked ? "locked" : ""} ${keyframe.isMissing ? "missing" : ""}" style="left: ${keyframe.position * 100}%" data-keyframe-id="${keyframe.id}"></div>
                  `,
                    )
                    .join("")}
                  ${specialUI}
                </div>
              </div>
            </div>
          `;
          })
          .join("")}
        ${node.children.length > 0 ? this.renderNodes(node.children) : ""}
      </div>
    `;
  }

  private updateSelection(timeline: HTMLElement): void {
    if (!this.selectionRange) {
      return;
    }
    const selectionDiv = timeline.querySelector(".selection") as HTMLElement;
    if (selectionDiv) {
      const start = Math.min(
        this.selectionRange.start,
        this.selectionRange.end,
      );
      const end = Math.max(this.selectionRange.start, this.selectionRange.end);
      selectionDiv.style.left = `${start * 100}%`;
      selectionDiv.style.width = `${(end - start) * 100}%`;
    }
  }

  private updateKeyframeSelections(): void {
    this.container.querySelectorAll(".keyframe").forEach((el) => {
      el.classList.remove("selected");
    });
    this.selectedKeyframeIds.forEach((id) => {
      const el = this.container.querySelector(`[data-keyframe-id="${id}"]`);
      if (el) {
        el.classList.add("selected");
      }
    });
  }

  /**
   * Toggles the visibility of a node in the grid (expands/collapses).
   * @param nodeId The ID of the node to toggle.
   * @param isCollapsed Whether the node should be collapsed.
   */
  public toggleNodeVisibility(nodeId: string, isCollapsed: boolean): void {
    const nodeElement = this.container.querySelector(
      `[data-node-id="${nodeId}"]`,
    );
    if (nodeElement) {
      if (isCollapsed) {
        nodeElement.classList.add("collapsed");
      } else {
        nodeElement.classList.remove("collapsed");
      }
    }
  }

  /**
   * Scrolls the grid to bring a specific node into view.
   * @param nodeId The ID of the node to scroll to.
   */
  public scrollToNode(nodeId: string): void {
    const nodeElement = this.container.querySelector(
      `[data-node-id="${nodeId}"]`,
    );
    if (nodeElement) {
      nodeElement.scrollIntoView({ behavior: "smooth" });
    }
  }

  /**
   * Cleans up event listeners and DOM elements.
   */
  public destroy(): void {
    this.container.removeEventListener("click", this.clickHandler);
    this.container.removeEventListener("mousedown", this.mouseDownHandler);
    this.container.removeEventListener("mousemove", this.mouseMoveHandler);
    this.container.removeEventListener("mouseup", this.mouseUpHandler);
    this.container.removeEventListener("dblclick", this.dblClickHandler);
    this.container.removeEventListener("contextmenu", this.contextMenuHandler);
    this.container.innerHTML = "";
  }
}
