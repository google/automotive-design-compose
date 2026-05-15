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

import { TimelineEditor } from "../timeline/TimelineEditor";
import { PlaybackController } from "../timeline/PlaybackController";
import { Keyframe, Timeline, Node, Variant } from "../timeline/types";
import { getAnimationSegment } from "../timeline/utils";
import { interpolateColor } from "../utils/common";
import { DataMapper } from "../services/DataMapper";
import { serializeKeyframes } from "../timeline/serialization";
import { PropertyElementBuilder, applyEasing } from "./properties/PropertyElementBuilder";

/**
 * Manages the properties panel, displaying details for selected keyframes and animation sections.
 * It allows editing keyframe values, easing, and positions, as well as inspecting animation segment properties.
 */
export class PropertiesPanel {
  private timelineEditor: TimelineEditor;
  private playbackController: PlaybackController;
  private currentVariants: Variant[] = [];
  
  // Elements
  private removeKeyframeButton: HTMLButtonElement;
  private removeAllKeyframesButton: HTMLButtonElement;
  private keyframeTimeInput: HTMLInputElement;
  private keyframeFractionInput: HTMLInputElement;
  private sectionEasingSelect: HTMLSelectElement;

  private selectedKeyframeInfo: {
    keyframeId: string;
    segment: {
      startPos: number;
      endPos: number;
      activeStartPos: number;
      activeDuration: number;
    } | null;
  } | null = null;

  private selectedSectionInfo: {
    timelineId: string;
    start: number;
    end: number;
  } | null = null;

  constructor(timelineEditor: TimelineEditor, playbackController: PlaybackController) {
    this.timelineEditor = timelineEditor;
    this.playbackController = playbackController;

    this.removeKeyframeButton = document.getElementById(
      "remove-keyframe-button",
    ) as HTMLButtonElement;
    this.removeAllKeyframesButton = document.getElementById(
      "remove-all-keyframes-button",
    ) as HTMLButtonElement;
    this.keyframeTimeInput = document.getElementById(
      "keyframe-time-input",
    ) as HTMLInputElement;
    this.keyframeFractionInput = document.getElementById(
      "keyframe-fraction-input",
    ) as HTMLInputElement;
    this.sectionEasingSelect = document.getElementById(
      "section-easing",
    ) as HTMLSelectElement;
    
    this.setupEventListeners();
  }

  /**
   * Updates the list of current variants.
   * @param variants The new list of variants.
   */
  public setCurrentVariants(variants: Variant[]) {
    this.currentVariants = variants;
  }

  private setupEventListeners() {
      const saveKeyframePosition = () => {
      if (!this.selectedKeyframeInfo) return;
      const data = this.timelineEditor.getData();
      if (!data) return;
      const findResult = this.playbackController.findKeyframeAndTimelineData(
        data.nodes,
        this.selectedKeyframeInfo.keyframeId,
      );
      if (findResult) {
        this.saveCustomKeyframeData(
          findResult.timeline.id,
          findResult.keyframe.id,
        );
      }
    };

    this.keyframeTimeInput.addEventListener("change", saveKeyframePosition);
    this.keyframeFractionInput.addEventListener("change", saveKeyframePosition);

     // Add event listeners for keyframe time/fraction changes
    this.keyframeTimeInput.addEventListener("input", () =>
      this.handleKeyframeTimeOrFractionChanged(true),
    );
    this.keyframeFractionInput.addEventListener("input", () =>
      this.handleKeyframeTimeOrFractionChanged(false),
    );

        this.removeKeyframeButton.onclick = () => {
      const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
      if (selectedKeyframeId) {
        const data = this.timelineEditor.getData();
        if (!data) return;
        const findResult = this.playbackController.findKeyframeAndTimelineData(
          data.nodes,
          selectedKeyframeId,
        );
        if (findResult) {
          const { timeline, keyframe } = findResult;
          const keyframePosition = keyframe.position; // Capture position before removal
          this.timelineEditor.emit("keyframe:remove", selectedKeyframeId);
          // Hide the panel after removing the keyframe
          document.getElementById("single-keyframe-properties")!.style.display =
            "none";
          this.saveCustomKeyframeData(timeline.id, undefined, keyframePosition); // Pass position
        }
      }
    };

    this.removeAllKeyframesButton.onclick = () => {
      const selectedKeyframeIds = this.timelineEditor.getSelectedKeyframeIds();
      if (selectedKeyframeIds.length > 0) {
        const data = this.timelineEditor.getData();
        if (!data) return;
        const keyframesToRemove = selectedKeyframeIds.map((id) => {
          const findResult =
            this.playbackController.findKeyframeAndTimelineData(data.nodes, id);
          return findResult;
        });

        keyframesToRemove.forEach((findResult) => {
          if (findResult) {
            const { timeline, keyframe } = findResult;
            if (keyframe.locked) return; // Skip locked keyframes
            const keyframePosition = keyframe.position; // Capture position before removal
            this.timelineEditor.emit("keyframe:remove", keyframe.id);
            this.saveCustomKeyframeData(
              timeline.id,
              undefined,
              keyframePosition,
            ); // Pass position
          }
        });

        // Hide the panel after removing the keyframes
        document.getElementById("multi-keyframes-list")!.style.display = "none";
      }
    };

        const keyframeValueInput = document.getElementById(
      "keyframe-property-value",
    ) as HTMLInputElement;
    keyframeValueInput.addEventListener("change", () => {
      const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
      if (!selectedKeyframeId) return;

      const data = this.timelineEditor.getData();
      if (!data) return;

      const findResult = this.playbackController.findKeyframeAndTimelineData(
        data.nodes,
        selectedKeyframeId,
      );
      if (findResult) {
        const { keyframe, timeline } = findResult;
        keyframe.value = parseFloat(keyframeValueInput.value);
        this.timelineEditor.setData(data);
        this.saveCustomKeyframeData(timeline.id, keyframe.id);
        this.playbackController.seek(this.playbackController.currentTime);
      }
    });

    const keyframeColorInput = document.getElementById(
      "keyframe-property-color-value",
    ) as HTMLInputElement;
    keyframeColorInput.addEventListener("change", () => {
      const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
      if (!selectedKeyframeId) return;

      const data = this.timelineEditor.getData();
      if (!data) return;

      const findResult = this.playbackController.findKeyframeAndTimelineData(
        data.nodes,
        selectedKeyframeId,
      );
      if (findResult) {
        const { keyframe, timeline } = findResult;
        keyframe.value = keyframeColorInput.value;
        this.timelineEditor.setData(data);
        this.saveCustomKeyframeData(timeline.id, keyframe.id);
        this.playbackController.seek(this.playbackController.currentTime);
      }
    });

    this.sectionEasingSelect.addEventListener("change", () => {
      if (!this.selectedSectionInfo) return;

      const data = this.timelineEditor.getData();
      if (!data) return;

      const timelineData = this.findTimelineData(
        this.selectedSectionInfo.timelineId,
      );
      if (!timelineData) return;

      const endingKeyframe = timelineData.keyframes.find(
        (kf) => Math.abs(kf.position - this.selectedSectionInfo!.end) < 0.001,
      );
      if (endingKeyframe) {
        endingKeyframe.easing =
          this.sectionEasingSelect.value === "Inherit"
            ? "Inherit"
            : this.sectionEasingSelect.value;
        this.timelineEditor.setData(data);
        this.saveCustomKeyframeData(timelineData.id);
        this.timelineEditor.setData(data);
        // Re-run the update to refresh the UI, including the gradient preview
        this.updateAnimationSectionProperties(
          this.selectedSectionInfo.timelineId,
          this.selectedSectionInfo.start,
          this.selectedSectionInfo.end,
        );
        this.playbackController.seek(this.playbackController.currentTime);
      }
    });
    
    // Event listeners for gradient editor
    const gradientEditor = document.getElementById(
      "keyframe-property-gradient-editor",
    ) as HTMLElement;
    gradientEditor.addEventListener("change", (event) => {
      const target = event.target as HTMLInputElement;
      if (target.closest(".gradient-stop")) {
        const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
        if (!selectedKeyframeId) return;

        const data = this.timelineEditor.getData();
        if (!data) return;

        const findResult = this.playbackController.findKeyframeAndTimelineData(
          data.nodes,
          selectedKeyframeId,
        );
        if (findResult) {
          const { keyframe, timeline } = findResult;
          const stops =
            typeof keyframe.value === "string"
              ? JSON.parse(keyframe.value)
              : keyframe.value;
          const stopElements = Array.from(
            gradientEditor.querySelectorAll(".gradient-stop"),
          );
          const index = stopElements.indexOf(
            target.closest(".gradient-stop") as HTMLElement,
          );

          if (index !== -1) {
            if (target.type === "color") {
              stops[index].color = target.value;
            } else if (target.type === "number") {
              stops[index].position = parseFloat(target.value);
            }
            keyframe.value = JSON.stringify(stops);
            this.timelineEditor.setData(data);
            this.saveCustomKeyframeData(timeline.id, keyframe.id);
            PropertyElementBuilder.buildGradientEditor(gradientEditor, keyframe); // Rebuild UI to update preview
            this.playbackController.seek(this.playbackController.currentTime);
          }
        }
      }
    });

    gradientEditor.addEventListener("click", (event) => {
      const target = event.target as HTMLElement;
      if (target.textContent === "Add Stop") {
        const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
        if (!selectedKeyframeId) return;

        const data = this.timelineEditor.getData();
        if (!data) return;

        const findResult = this.playbackController.findKeyframeAndTimelineData(
          data.nodes,
          selectedKeyframeId,
        );
        if (findResult) {
          const { keyframe, timeline } = findResult;
          const stops = JSON.parse(keyframe.value as string);
          stops.push({ position: 1, color: "#000000" }); // Default new stop
          stops.sort(
            (a: { position: number }, b: { position: number }) =>
              a.position - b.position,
          );
          keyframe.value = JSON.stringify(stops);
          this.timelineEditor.setData(data);
          this.saveCustomKeyframeData(timeline.id, keyframe.id);
          PropertyElementBuilder.buildGradientEditor(gradientEditor, keyframe); // Rebuild UI
          this.playbackController.seek(this.playbackController.currentTime);
        }
      } else if (target.textContent === "-") {
        const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
        if (!selectedKeyframeId) return;

        const data = this.timelineEditor.getData();
        if (!data) return;

        const findResult = this.playbackController.findKeyframeAndTimelineData(
          data.nodes,
          selectedKeyframeId,
        );
        if (findResult) {
          const { keyframe, timeline } = findResult;
          const stops =
            typeof keyframe.value === "string"
              ? JSON.parse(keyframe.value)
              : keyframe.value;
          const stopElements = Array.from(
            gradientEditor.querySelectorAll(".gradient-stop"),
          );
          const index = stopElements.indexOf(
            target.closest(".gradient-stop") as HTMLElement,
          );

          if (index !== -1 && stops.length > 2) {
            // Ensure at least two stops remain
            stops.splice(index, 1);
            keyframe.value = JSON.stringify(stops);
            this.timelineEditor.setData(data);
            this.saveCustomKeyframeData(timeline.id, keyframe.id);
            PropertyElementBuilder.buildGradientEditor(gradientEditor, keyframe); // Rebuild UI
            this.playbackController.seek(this.playbackController.currentTime);
          }
        }
      }
    });

    const arcEditor = document.getElementById(
      "keyframe-property-arc-editor",
    ) as HTMLElement;
    arcEditor.addEventListener("change", (event) => {
      const target = event.target as HTMLInputElement;
      if (target.type === "number") {
        const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
        if (!selectedKeyframeId) return;

        const data = this.timelineEditor.getData();
        if (!data) return;

        const findResult = this.playbackController.findKeyframeAndTimelineData(
          data.nodes,
          selectedKeyframeId,
        );
        if (findResult) {
          const { keyframe, timeline } = findResult;
          const startingAngle = parseFloat(
            (
              arcEditor.querySelector(
                'input[type="number"]',
              ) as HTMLInputElement
            ).value,
          );
          const endingAngle = parseFloat(
            (
              arcEditor.querySelectorAll(
                'input[type="number"]',
              )[1] as HTMLInputElement
            ).value,
          );
          const innerRadius = parseFloat(
            (
              arcEditor.querySelectorAll(
                'input[type="number"]',
              )[2] as HTMLInputElement
            ).value,
          );
          keyframe.value = JSON.stringify({
            startingAngle,
            endingAngle,
            innerRadius,
          });
          this.timelineEditor.setData(data);
          this.saveCustomKeyframeData(timeline.id, keyframe.id);
          this.playbackController.seek(this.playbackController.currentTime);
        }
      }
    });

    const cornerRadiusEditor = document.getElementById(
      "keyframe-property-corner-radius-editor",
    ) as HTMLElement;
    cornerRadiusEditor.addEventListener("change", (event) => {
      const target = event.target as HTMLInputElement;
      if (target.type === "number") {
        const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
        if (!selectedKeyframeId) return;

        const data = this.timelineEditor.getData();
        if (!data) return;

        const findResult = this.playbackController.findKeyframeAndTimelineData(
          data.nodes,
          selectedKeyframeId,
        );
        if (findResult) {
          const { keyframe, timeline } = findResult;
          const tl = parseFloat(
            (
              document.getElementById(
                "corner-radius-top-left",
              ) as HTMLInputElement
            ).value,
          );
          const tr = parseFloat(
            (
              document.getElementById(
                "corner-radius-top-right",
              ) as HTMLInputElement
            ).value,
          );
          const br = parseFloat(
            (
              document.getElementById(
                "corner-radius-bottom-right",
              ) as HTMLInputElement
            ).value,
          );
          const bl = parseFloat(
            (
              document.getElementById(
                "corner-radius-bottom-left",
              ) as HTMLInputElement
            ).value,
          );
          keyframe.value = [tl, tr, br, bl];
          this.timelineEditor.setData(data);
          this.saveCustomKeyframeData(timeline.id, keyframe.id);
          this.playbackController.seek(this.playbackController.currentTime);
        }
      }
    });
  }

  private handleKeyframeTimeOrFractionChanged(isTimeChanged: boolean) {
    if (!this.selectedKeyframeInfo || !this.selectedKeyframeInfo.segment)
      return;

    const data = this.timelineEditor.getData();
    if (!data) return;

    const findResult = this.playbackController.findKeyframeAndTimelineData(
      data.nodes,
      this.selectedKeyframeInfo.keyframeId,
    );
    if (!findResult) return;

    const { keyframe } = findResult;
    if (keyframe.locked) return; // Should be disabled, but double check

    const totalDuration = data.duration;
    const segment = this.selectedKeyframeInfo.segment;
    let newPosition: number;

    if (isTimeChanged) {
      const newTime = parseFloat(this.keyframeTimeInput.value);
      if (isNaN(newTime)) return;

      newPosition = newTime / totalDuration;

      // Clamp position to within the ACTIVE part of the segment
      newPosition = Math.max(
        segment.activeStartPos,
        Math.min(segment.endPos, newPosition),
      );

      let newFraction = 0;
      if (segment.activeDuration > 0.0001) {
        newFraction =
          (newPosition - segment.activeStartPos) / segment.activeDuration;
      }
      this.keyframeFractionInput.value = Math.max(
        0,
        Math.min(1, newFraction),
      ).toFixed(3);
      this.keyframeTimeInput.value = (newPosition * totalDuration).toFixed(2); // Update with clamped value
    } else {
      // Fraction changed
      let newFraction = parseFloat(this.keyframeFractionInput.value);
      if (isNaN(newFraction)) return;

      newFraction = Math.max(0, Math.min(1, newFraction)); // Clamp fraction

      newPosition =
        segment.activeStartPos + newFraction * segment.activeDuration;

      this.keyframeTimeInput.value = (newPosition * totalDuration).toFixed(2);
      this.keyframeFractionInput.value = newFraction.toFixed(3); // Update with clamped value
    }

    // Update the data model and the timeline UI
    keyframe.position = newPosition;
    this.timelineEditor.setData(data);
  }

  private findTimelineData(timelineId: string): Timeline | null {
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
    if (!this.timelineEditor.getData()) return null;
    return find(this.timelineEditor.getData()!.nodes);
  }

  /**
   * Hides the keyframe properties panel.
   */
  public resetKeyframePropertiesPanel() {
    document.getElementById("single-keyframe-properties")!.style.display =
      "none";
    document.getElementById("multi-keyframes-list")!.style.display = "none";
    document.getElementById("animation-section-properties")!.style.display =
      "none";
    this.removeKeyframeButton.disabled = true;
  }

  /**
   * Updates the panel to show properties for a selected animation section.
   * @param timelineId The ID of the timeline.
   * @param start The start position of the section (0-1).
   * @param end The end position of the section (0-1).
   */
  public updateAnimationSectionProperties(
    timelineId: string,
    start: number,
    end: number,
  ) {
    this.selectedSectionInfo = { timelineId, start, end };
    document.getElementById("keyframe-properties")!.style.display = "none";
    document.getElementById("animation-section-properties")!.style.display =
      "block";

    const data = this.timelineEditor.getData();
    if (!data) return;
    const totalDuration = data.duration;
    const startTime = start * totalDuration;
    const endTime = end * totalDuration;
    const duration = endTime - startTime;

    const startValue = this.playbackController.getInterpolatedValue(
      timelineId,
      startTime,
    );
    const endValue = this.playbackController.getInterpolatedValue(
      timelineId,
      endTime,
    );

    document.getElementById("section-start-time")!.textContent =
      `${startTime.toFixed(2)}`;
    document.getElementById("section-end-time")!.textContent =
      `${endTime.toFixed(2)}`;
    document.getElementById("section-duration")!.textContent =
      `${duration.toFixed(2)}s`;

    const timelineData = this.findTimelineData(timelineId);

    const startValueContainer = document.getElementById("section-start-value")!;
    const endValueContainer = document.getElementById("section-end-value")!;

    if (
      timelineData &&
      (timelineData.property.startsWith("fills.") ||
        timelineData.property.startsWith("strokes.")) &&
      timelineData.property.endsWith(".solid")
    ) {
      startValueContainer.innerHTML = `<input type="color" value="${startValue}" disabled /> <span>${startValue}</span>`;
      endValueContainer.innerHTML = `<input type="color" value="${endValue}" disabled /> <span>${endValue}</span>`;

      const gradientPreview = document.createElement("div");
      gradientPreview.style.width = "100%";
      gradientPreview.style.height = "20px";
      gradientPreview.style.border = "1px solid var(--figma-color-border)";
      gradientPreview.style.marginTop = "4px";
      const easing = this.sectionEasingSelect.value;
      const gradientSteps: string[] = [];
      const numSteps = 10;

      for (let i = 0; i <= numSteps; i++) {
        const linearProgress = i / numSteps;
        const easedProgress = applyEasing(linearProgress, easing);
        const interpolatedColor = interpolateColor(
          String(startValue),
          String(endValue),
          easedProgress,
        );
        gradientSteps.push(interpolatedColor);
      }

      gradientPreview.style.background = `linear-gradient(to right, ${gradientSteps.join(", ")})`;
      endValueContainer.appendChild(gradientPreview);
    } else if (
      timelineData &&
      (timelineData.property.startsWith("fills.") ||
        timelineData.property.startsWith("strokes.")) &&
      timelineData.property.endsWith(".gradient.stops")
    ) {
      PropertyElementBuilder.buildDetailedGradientPreview(
        startValueContainer,
        startValue,
        endValue,
        this.sectionEasingSelect.value,
      );
      endValueContainer.innerHTML = ""; // Clear the end value container
    } else if (
      timelineData &&
      timelineData.property.startsWith("cornerRadius")
    ) {
      startValueContainer.textContent = Array.isArray(startValue)
        ? startValue.join(", ")
        : String(startValue);
      endValueContainer.textContent = Array.isArray(endValue)
        ? endValue.join(", ")
        : String(endValue);
    } else if (timelineData && timelineData.property === "arcData") {
      try {
        const startArc =
          typeof startValue === "string" ? JSON.parse(startValue) : startValue;
        const endArc =
          typeof endValue === "string" ? JSON.parse(endValue) : endValue;

        if (startArc) {
          startValueContainer.innerHTML = `
            <div><b>Starting Angle:</b> ${startArc.startingAngle.toFixed(2)}</div>
            <div><b>Ending Angle:</b> ${startArc.endingAngle.toFixed(2)}</div>
            <div><b>Inner Radius:</b> ${startArc.innerRadius.toFixed(2)}</div>
          `;
        } else {
          startValueContainer.textContent = "No Arc Data";
        }

        if (endArc) {
          endValueContainer.innerHTML = `
            <div><b>Starting Angle:</b> ${endArc.startingAngle.toFixed(2)}</div>
            <div><b>Ending Angle:</b> ${endArc.endingAngle.toFixed(2)}</div>
            <div><b>Inner Radius:</b> ${endArc.innerRadius.toFixed(2)}</div>
          `;
        } else {
          endValueContainer.textContent = "No Arc Data";
        }
      } catch (e) {
        startValueContainer.textContent = "Invalid Arc Data";
        endValueContainer.textContent = "Invalid Arc Data";
      }
    } else if (
      timelineData &&
      timelineData.property.startsWith("fills.") &&
      timelineData.property.endsWith(".gradient.positions")
    ) {
      startValueContainer.textContent = String(startValue);
      endValueContainer.textContent = String(endValue);
    } else {
      startValueContainer.textContent =
        typeof startValue === "number"
          ? startValue.toFixed(2)
          : String(startValue);
      endValueContainer.textContent =
        typeof endValue === "number" ? endValue.toFixed(2) : String(endValue);
    }

    const keyframeTimes = this.playbackController.getKeyframeTimes();
    const segment = getAnimationSegment(
      start,
      keyframeTimes,
      totalDuration,
      this.currentVariants,
    );

    let sectionType = "Animation"; // Default
    if (segment) {
      const timelineData = this.findTimelineData(timelineId);
      if (timelineData) {
        // A section is ONLY a delay if it is the specific initial delay part of a segment.
        const isTheDelaySection =
          segment.delay > 0 &&
          Math.abs(start - segment.startPos) < 0.0001 &&
          Math.abs(end - segment.delayEndPos) < 0.0001;

        if (isTheDelaySection) {
          sectionType = "Delay";
        } else {
          // It's an animation section. Now determine if it's a "Variant animation" or a regular "Animation".
          const startKeyframe = timelineData.keyframes.find(
            (kf) => Math.abs(kf.position - start) < 0.0001,
          );
          const endKeyframe = timelineData.keyframes.find(
            (kf) => Math.abs(kf.position - end) < 0.0001,
          );

          const hasCustomKeyframesWithinSection = timelineData.keyframes.some(
            (kf) =>
              !kf.locked &&
              kf.position > start + 0.0001 && // Strictly after start
              kf.position < end - 0.0001, // Strictly before end
          );

          if (
            (startKeyframe && !startKeyframe.locked) ||
            (endKeyframe && !endKeyframe.locked) ||
            hasCustomKeyframesWithinSection
          ) {
            sectionType = "Animation";
          } else {
            sectionType = "Variant animation";
          }
        }
      }
    }

    document.getElementById("section-type")!.textContent = sectionType;

    // Get references to the easing label and select element
    const easingLabel = document.querySelector(
      'label[for="section-easing"]',
    ) as HTMLLabelElement;
    const easingSelect = document.getElementById(
      "section-easing",
    ) as HTMLSelectElement;

    if (sectionType === "Delay") {
      easingLabel.style.display = "none";
      easingSelect.style.display = "none";
      easingSelect.disabled = true;
    } else {
      easingLabel.style.display = "block";
      easingSelect.style.display = "block";
      easingSelect.disabled = false;
    }

    // Handle easing selector logic
    if (timelineId === "variant-change") {
      // For the main variant timeline, show the inherited easing and disable changes.
      if (segment) {
        const animSourceVariantIndex = keyframeTimes[segment.endIndex].index;
        const animSourceVariant = this.currentVariants[animSourceVariantIndex];
        const easing =
          animSourceVariant?.animation?.spec?.animation?.Smooth?.easing ||
          "Linear";
        this.sectionEasingSelect.value = easing;
      }
      this.sectionEasingSelect.disabled = true;
    } else {
      // For property timelines, use the existing logic
      const timelineData = this.findTimelineData(timelineId);
      if (timelineData) {
        const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(this.currentVariants);

        const isStartAVariantKeyframeBoundary = keyframeTimes.some(
          (kt) => Math.abs(kt.time / totalTime - start) < 0.0001,
        );
        const isEndAVariantKeyframeBoundary = keyframeTimes.some(
          (kt) => Math.abs(kt.time / totalTime - end) < 0.0001,
        );

        // Only consider UNLOCKED keyframes when checking for intermediate keyframes
        const hasIntermediateUnlockedKeyframes = timelineData.keyframes.some(
          (kf) =>
            !kf.locked && // Only consider unlocked keyframes
            kf.position > start + 0.0001 &&
            kf.position < end - 0.0001,
        );

        const canInherit =
          isStartAVariantKeyframeBoundary &&
          isEndAVariantKeyframeBoundary &&
          !hasIntermediateUnlockedKeyframes;
        const inheritOption = this.sectionEasingSelect.querySelector(
          'option[value="Inherit"]',
        ) as HTMLOptionElement;

        if (inheritOption) {
          inheritOption.hidden = !canInherit;
        }

        const endingKeyframe = timelineData.keyframes.find(
          (kf) => Math.abs(kf.position - end) < 0.001,
        );

        if (endingKeyframe?.easing && endingKeyframe.easing !== "Inherit") {
          this.sectionEasingSelect.value = endingKeyframe.easing;
        } else {
          // Default to Inherit if it's a pure section, otherwise Linear
          this.sectionEasingSelect.value = canInherit ? "Inherit" : "Linear";
        }

        // If for some reason "Inherit" is selected but not allowed, default to "Linear"
        if (!canInherit && this.sectionEasingSelect.value === "Inherit") {
          this.sectionEasingSelect.value = "Linear";
        }
      } else {
        this.sectionEasingSelect.value = "Inherit"; // Should not happen, but as a fallback
      }
    }
  }

  /**
   * Updates the panel to show a summary for multiple selected keyframes.
   * @param keyframes The list of selected keyframes.
   */
  public updateMultiKeyframeProperties(keyframes: Keyframe[]) {
    document.getElementById("keyframe-properties")!.style.display = "block";
    document.getElementById("keyframe-panel-title")!.textContent =
      "Selected Keyframes";
    document.getElementById("single-keyframe-properties")!.style.display =
      "none";
    document.getElementById("animation-section-properties")!.style.display =
      "none";
    const multiKeyframesList = document.getElementById("multi-keyframes-list")!;
    multiKeyframesList.style.display = "block";
    multiKeyframesList.innerHTML = "";
    this.removeKeyframeButton.disabled = true; // Disable remove button for multiple selections
    this.removeAllKeyframesButton.style.display = "block";
    const ul = document.createElement("ul");
    ul.style.listStyle = "none";
    ul.style.padding = "0";
    ul.style.margin = "0";

    keyframes.forEach((kf) => {
      const findResult = this.playbackController.findKeyframeAndTimelineData(
        this.timelineEditor.getData()!.nodes,
        kf.id,
      );
      if (findResult) {
        const { node, timeline } = findResult;
        const li = document.createElement("li");
        const keyframeElement = document.createElement("a");
        keyframeElement.href = "#";

        const totalDuration = this.timelineEditor.getData()!.duration;
        const keyframeTimeInSeconds = kf.position * totalDuration;
        const keyframeTimes = this.playbackController.getKeyframeTimes();
        const variantKeyframe = keyframeTimes.find(
          (kt: KeyframeTime) =>
            Math.abs(kt.time / totalDuration - kf.position) < 0.001,
        );

        let displayText = `${node.name} -> ${timeline.property} (ID: ${kf.id}, Time: ${keyframeTimeInSeconds.toFixed(2)}s`;
        if (variantKeyframe) {
          displayText += `, Variant: ${variantKeyframe.name}`;
        }
        displayText += ")";

        keyframeElement.textContent = displayText;
        keyframeElement.onclick = (e) => {
          e.preventDefault();
          // First, update the selection in the timeline view itself.
          // This will clear the multi-selection and select only this keyframe.
          this.timelineEditor.emit("keyframe:select_in_view", kf.id);
          // Then, update the properties panel to show the details for the newly selected keyframe.
          this.updateKeyframeProperties(kf);
        };
        li.appendChild(keyframeElement);
        ul.appendChild(li);
      }
    });
    multiKeyframesList.appendChild(ul);
  }

  /**
   * Updates the panel to show detailed properties for a single selected keyframe.
   * @param keyframe The selected keyframe.
   */
  public updateKeyframeProperties(keyframe: Keyframe) {
    this.selectedSectionInfo = null;
    document.getElementById("keyframe-properties")!.style.display = "block";
    document.getElementById("keyframe-panel-title")!.textContent =
      "Keyframe Properties";
    document.getElementById("single-keyframe-properties")!.style.display =
      "block";
    document.getElementById("multi-keyframes-list")!.style.display = "none";
    document.getElementById("animation-section-properties")!.style.display =
      "none";

    this.selectedKeyframeInfo = null; // Reset
    const data = this.timelineEditor.getData();
    if (!data) return;

    const findResult = this.playbackController.findKeyframeAndTimelineData(
      data.nodes,
      keyframe.id,
    );
    if (findResult) {
      const { node, timeline } = findResult;
      const nodeNameElement = document.getElementById("keyframe-node-name")!;
      
      if (timeline.id === "variant-change") {
          const variantIndex = keyframe.value as number;
          if (this.currentVariants[variantIndex]) {
              nodeNameElement.textContent = this.currentVariants[variantIndex].name;
          } else {
              nodeNameElement.textContent = node.name;
          }
      } else {
          nodeNameElement.textContent = node.name;
      }

      nodeNameElement.onclick = () => {
        parent.postMessage(
          { pluginMessage: { type: "select-node", nodeId: node.figmaId } },
          "*",
        );
      };

      document.getElementById("keyframe-id")!.textContent = keyframe.id;

      const totalDuration = this.timelineEditor.getData()!.duration;
      const keyframeTimeInSeconds = keyframe.position * totalDuration;
      this.keyframeTimeInput.value = keyframeTimeInSeconds.toFixed(2);

      const keyframeTimes = this.playbackController.getKeyframeTimes();
      const segment = getAnimationSegment(
        keyframe.position,
        keyframeTimes,
        totalDuration,
        this.currentVariants,
      );

      if (segment) {
        let fraction = 0;
        // If the keyframe is within the delay period, its fraction is 0.
        if (keyframe.position < segment.activeStartPos) {
          fraction = 0;
        } else {
          if (segment.activeDuration > 0.0001) {
            fraction =
              (keyframe.position - segment.activeStartPos) /
              segment.activeDuration;
          }
        }

        // Clamp fraction between 0 and 1 for safety.
        fraction = Math.max(0, Math.min(1, fraction));
        this.keyframeFractionInput.value = fraction.toFixed(3);

        this.selectedKeyframeInfo = {
          keyframeId: keyframe.id,
          segment: {
            startPos: segment.startPos,
            endPos: segment.endPos,
            activeStartPos: segment.activeStartPos,
            activeDuration: segment.activeDuration,
          },
        };
      } else {
        this.keyframeFractionInput.value = "N/A";
      }

      const keyframeTime = keyframeTimes.find(
        (kt: KeyframeTime) =>
          Math.abs(
            kt.time / this.timelineEditor.getData()!.duration -
              keyframe.position,
          ) < 0.001,
      );
      if (keyframeTime) {
        document.getElementById("keyframe-variant-name")!.textContent =
          keyframeTime.name;
      } else {
        document.getElementById("keyframe-variant-name")!.textContent = "-";
      }

      document.getElementById("keyframe-property-name")!.textContent =
        timeline.property;

      const valueInput = document.getElementById(
        "keyframe-property-value",
      ) as HTMLInputElement;

      const colorInput = document.getElementById(
        "keyframe-property-color-value",
      ) as HTMLInputElement;

      const gradientEditor = document.getElementById(
        "keyframe-property-gradient-editor",
      ) as HTMLElement;
      const arcEditor = document.getElementById(
        "keyframe-property-arc-editor",
      ) as HTMLElement;
      const cornerRadiusEditor = document.getElementById(
        "keyframe-property-corner-radius-editor",
      ) as HTMLElement;

      // Helper to create a boolean editor if it doesn't exist
      let booleanEditor = document.getElementById("keyframe-property-boolean-editor") as HTMLInputElement;
      if (!booleanEditor) {
          const container = document.createElement("div");
          container.id = "keyframe-property-boolean-editor";
          container.style.display = "none";
          const checkbox = document.createElement("input");
          checkbox.type = "checkbox";
          checkbox.id = "keyframe-property-boolean-value";
          container.appendChild(checkbox);
          // Insert it after the color input
          colorInput.parentNode?.insertBefore(container, colorInput.nextSibling);
          booleanEditor = container;
          
          checkbox.addEventListener("change", () => {
              const selectedKeyframeId = this.timelineEditor.getSelectedKeyframeId();
              if (findResult && selectedKeyframeId) {
                  findResult.keyframe.value = checkbox.checked;
                  this.timelineEditor.setData(data);
                  this.saveCustomKeyframeData(findResult.timeline.id, findResult.keyframe.id);
                  this.playbackController.seek(this.playbackController.currentTime);
              }
          });
      }
      const booleanCheckbox = booleanEditor.querySelector("input") as HTMLInputElement;

      // Hide all value editors by default
      valueInput.style.display = "none";
      colorInput.style.display = "none";
      gradientEditor.style.display = "none";
      arcEditor.style.display = "none";
      cornerRadiusEditor.style.display = "none";
      booleanEditor.style.display = "none";

      if (
        (timeline.property.startsWith("fills.") ||
          timeline.property.startsWith("strokes.")) &&
        timeline.property.endsWith(".solid")
      ) {
        colorInput.style.display = "block";
        colorInput.value = String(keyframe.value);
      } else if (
        timeline.property.startsWith("fills.") &&
        timeline.property.endsWith(".gradient.positions")
      ) {
        valueInput.style.display = "block";
        valueInput.type = "text";
        valueInput.value = String(keyframe.value);
      } else if (
        timeline.property.startsWith("fills.") &&
        timeline.property.endsWith(".gradient.stops")
      ) {
        gradientEditor.style.display = "block";
        this.buildGradientEditor(gradientEditor, keyframe);
      } else if (timeline.property === "arcData") {
        arcEditor.style.display = "block";
        this.buildArcEditor(arcEditor, keyframe);
      } else if (timeline.property.startsWith("cornerRadius")) {
        cornerRadiusEditor.style.display = "block";
        // Assuming the value is a single number or an array of 4 numbers
        const values = Array.isArray(keyframe.value)
          ? keyframe.value
          : [keyframe.value, keyframe.value, keyframe.value, keyframe.value];
        (
          document.getElementById("corner-radius-top-left") as HTMLInputElement
        ).value = String(values[0]);
        (
          document.getElementById("corner-radius-top-right") as HTMLInputElement
        ).value = String(values[1]);
        (
          document.getElementById(
            "corner-radius-bottom-right",
          ) as HTMLInputElement
        ).value = String(values[2]);
        (
          document.getElementById(
            "corner-radius-bottom-left",
          ) as HTMLInputElement
        ).value = String(values[3]);
      } else if (timeline.property === "visible") {
          booleanEditor.style.display = "block";
          booleanCheckbox.checked = Boolean(keyframe.value);
      } else {
        valueInput.style.display = "block";
        // Check if value is a number to determine input type
        if (typeof keyframe.value === 'number') {
            valueInput.type = "number";
        } else {
            valueInput.type = "text";
        }
        valueInput.value = String(keyframe.value);
      }

      (
        document.getElementById("keyframe-locked-status") as HTMLInputElement
      ).checked = keyframe.locked || false;

      // Enable/disable controls based on locked status
      // Only consider it locked if it is explicitly marked as locked OR it is the variant-change timeline.
      // EXCEPTION: If it is marked as 'missing', we allow editing the VALUE, but not the position or removing it.
      const isLocked = (keyframe.locked === true) || (timeline.id === "variant-change");
      const isMissing = keyframe.isMissing === true;
      
      // Position and removal are always locked for variant keyframes (including missing ones)
      this.removeKeyframeButton.disabled = isLocked;
      this.keyframeTimeInput.disabled = isLocked;
      this.keyframeFractionInput.disabled =
        isLocked || this.selectedKeyframeInfo?.segment === null;

      // Value inputs are unlocked if it's NOT locked, OR if it IS locked but IS MISSING.
      // Effectively: isValueLocked = isLocked && !isMissing
      const isValueLocked = isLocked && !isMissing;

      valueInput.disabled = isValueLocked;
      colorInput.disabled = isValueLocked;
      // Also update other editors if they exist/are shown
      if (booleanCheckbox) booleanCheckbox.disabled = isValueLocked;
      
      // We need to propagate this to gradient/arc/corner/etc editors if they check 'locked' property internally
      // The helper functions buildGradientEditor etc check keyframe.locked.
      // We should update them or just temporarily patch keyframe object? 
      // Patching is risky. Better to pass 'isValueLocked' to them or have them check isMissing.
      // Since those methods are private and take 'keyframe', I should update them to use the calculated lock state.
      // But for now, let's rely on re-rendering. 
      // Wait, buildGradientEditor uses `keyframe.locked`. I need to update it.
      
      // I will update buildGradientEditor, buildArcEditor calls below.
      // Or I can just modify the 'disabled' attribute of their inputs after calling them?
      // The inputs are created inside.
      
      // Let's update the calls to pass a "readOnly" flag or update the methods.
      // For now, I will just update the standard inputs above.
      // If the user uses gradient editor, I need to fix that too.
      
      // To do this cleanly, I'll wrap the 'disabled' logic update in a helper or update the build methods.
      // Since I can't easily change the signature in this replace block without replacing the whole class,
      // I will update the inputs AFTER building them if they are displayed.
      
      if (gradientEditor.style.display === "block") {
          const inputs = gradientEditor.querySelectorAll("input, button");
          inputs.forEach(el => (el as HTMLInputElement).disabled = isValueLocked);
      }
      if (arcEditor.style.display === "block") {
           const inputs = arcEditor.querySelectorAll("input");
           inputs.forEach(el => (el as HTMLInputElement).disabled = isValueLocked);
      }
      if (cornerRadiusEditor.style.display === "block") {
           const inputs = cornerRadiusEditor.querySelectorAll("input");
           inputs.forEach(el => (el as HTMLInputElement).disabled = isValueLocked);
      }

    }
  }

  private buildGradientEditor(container: HTMLElement, keyframe: Keyframe) {
    container.innerHTML = "";
    const stops =
      typeof keyframe.value === "string"
        ? JSON.parse(keyframe.value)
        : keyframe.value;

    // Create and add the gradient preview bar
    const preview = document.createElement("div");
    preview.className = "gradient-preview";
    preview.style.width = "100%";
    preview.style.height = "20px";
    preview.style.border = "1px solid var(--figma-color-border)";
    preview.style.marginBottom = "8px";
    const gradientCss = `linear-gradient(to right, ${stops
      .map(
        (s: { color: string; position: number }) =>
          `${s.color} ${s.position * 100}%`,
      )
      .join(", ")})`;
    preview.style.background = gradientCss;
    container.appendChild(preview);

    const editor = document.createElement("div");
    editor.className = "gradient-editor";

    const stopsContainer = document.createElement("div");
    stopsContainer.className = "gradient-stops";

    stops.forEach(
      (stop: { position: number; color: string }, _index: number) => {
        const stopEl = document.createElement("div");
        stopEl.className = "gradient-stop";

        const colorInput = document.createElement("input");
        colorInput.type = "color";
        colorInput.value = stop.color;
        colorInput.disabled = keyframe.locked;

        const positionInput = document.createElement("input");
        positionInput.type = "number";
        positionInput.step = "0.01";
        positionInput.min = "0";
        positionInput.max = "1";
        positionInput.value = stop.position.toFixed(2);
        positionInput.disabled = keyframe.locked;

        const removeBtn = document.createElement("button");
        removeBtn.textContent = "-";
        removeBtn.disabled = keyframe.locked;

        stopEl.appendChild(colorInput);
        stopEl.appendChild(positionInput);
        stopEl.appendChild(removeBtn);
        stopsContainer.appendChild(stopEl);
      },
    );

    editor.appendChild(stopsContainer);

    if (!keyframe.locked) {
      const addBtn = document.createElement("button");
      addBtn.textContent = "Add Stop";
      editor.appendChild(addBtn);
    }

    container.appendChild(editor);
  }

  private buildArcEditor(container: HTMLElement, keyframe: Keyframe) {
    container.innerHTML = "";
    const arc =
      typeof keyframe.value === "string"
        ? JSON.parse(keyframe.value)
        : keyframe.value;

    const editor = document.createElement("div");
    editor.className = "form-grid";
    editor.style.gridTemplateColumns = "100px 1fr";

    const startingAngleLabel = document.createElement("label");
    startingAngleLabel.textContent = "Starting Angle";
    const startingAngleInput = document.createElement("input");
    startingAngleInput.type = "number";
    startingAngleInput.step = "0.1";
    startingAngleInput.value = arc.startingAngle.toFixed(2);
    startingAngleInput.disabled = keyframe.locked;

    const endingAngleLabel = document.createElement("label");
    endingAngleLabel.textContent = "Ending Angle";
    const endingAngleInput = document.createElement("input");
    endingAngleInput.type = "number";
    endingAngleInput.step = "0.1";
    endingAngleInput.value = arc.endingAngle.toFixed(2);
    endingAngleInput.disabled = keyframe.locked;

    const innerRadiusLabel = document.createElement("label");
    innerRadiusLabel.textContent = "Inner Radius";
    const innerRadiusInput = document.createElement("input");
    innerRadiusInput.type = "number";
    innerRadiusInput.step = "0.1";
    innerRadiusInput.value = arc.innerRadius.toFixed(2);
    innerRadiusInput.disabled = keyframe.locked;

    editor.appendChild(startingAngleLabel);
    editor.appendChild(startingAngleInput);
    editor.appendChild(endingAngleLabel);
    editor.appendChild(endingAngleInput);
    editor.appendChild(innerRadiusLabel);
    editor.appendChild(innerRadiusInput);

    container.appendChild(editor);
  }

  private buildDetailedGradientPreview(
    container: HTMLElement,
    startValue: unknown,
    endValue: unknown,
    easing: string,
  ) {
    container.innerHTML = "";
    let startStops;
    let endStops;

    try {
      startStops =
        typeof startValue === "string" ? JSON.parse(startValue) : startValue;
      endStops = typeof endValue === "string" ? JSON.parse(endValue) : endValue;
      if (
        !Array.isArray(startStops) ||
        !Array.isArray(endStops) ||
        startStops.length !== endStops.length
      ) {
        container.textContent = "Gradient stops mismatch";
        return;
      }
    } catch (e) {
      container.textContent = "Invalid gradient data";
      return;
    }

    const previewContainer = document.createElement("div");
    previewContainer.style.display = "flex";
    previewContainer.style.gap = "10px";

    // Per-stop gradients
    const stopsContainer = document.createElement("div");
    stopsContainer.style.flexGrow = "1";
    startStops.forEach(
      (startStop: { position: number; color: string }, index: number) => {
        const endStop = endStops[index];
        const stopRow = document.createElement("div");
        stopRow.style.display = "flex";
        stopRow.style.alignItems = "center";
        stopRow.style.marginBottom = "4px";

        const gradient = document.createElement("div");
        gradient.style.width = "100px";
        gradient.style.height = "20px";
        gradient.style.border = "1px solid var(--figma-color-border)";

        const gradientSteps: string[] = [];
        const numSteps = 10;
        for (let i = 0; i <= numSteps; i++) {
          const linearProgress = i / numSteps;
          const easedProgress = this._applyEasing(linearProgress, easing);
          const interpolatedColor = interpolateColor(
            startStop.color,
            endStop.color,
            easedProgress,
          );
          gradientSteps.push(interpolatedColor);
        }
        gradient.style.background = `linear-gradient(to right, ${gradientSteps.join(
          ", ",
        )})`;
        stopRow.appendChild(gradient);

        const text = document.createElement("span");
        text.textContent = ` ${startStop.color} -> ${endStop.color}`;
        text.style.marginLeft = "8px";
        stopRow.appendChild(text);

        stopsContainer.appendChild(stopRow);
      },
    );
    previewContainer.appendChild(stopsContainer);

    // Overall vertical gradients
    const overallGradientContainer = document.createElement("div");
    overallGradientContainer.style.display = "flex";
    overallGradientContainer.style.gap = "5px";

    const startGradient = document.createElement("div");
    startGradient.style.width = "30px";
    startGradient.style.height = "100px";
    startGradient.style.border = "1px solid var(--figma-color-border)";
    const startGradientCss = `linear-gradient(to bottom, ${startStops
      .map(
        (s: { color: string; position: number }) =>
          `${s.color} ${s.position * 100}%`,
      )
      .join(", ")})`;
    startGradient.style.background = startGradientCss;
    overallGradientContainer.appendChild(startGradient);

    const endGradient = document.createElement("div");
    endGradient.style.width = "30px";
    endGradient.style.height = "100px";
    endGradient.style.border = "1px solid var(--figma-color-border)";
    const endGradientCss = `linear-gradient(to bottom, ${endStops
      .map(
        (s: { color: string; position: number }) =>
          `${s.color} ${s.position * 100}%`,
      )
      .join(", ")})`;
    endGradient.style.background = endGradientCss;
    overallGradientContainer.appendChild(endGradient);

    previewContainer.appendChild(overallGradientContainer);

    container.appendChild(previewContainer);
  }

  /**
   * Saves changes to a custom keyframe back to the Figma variant data.
   * @param timelineId The ID of the timeline.
   * @param changedKeyframeId Optional ID of the changed keyframe.
   * @param removedKeyframePosition Optional position of a removed keyframe.
   */
  public saveCustomKeyframeData(
    timelineId: string,
    changedKeyframeId?: string,
    removedKeyframePosition?: number,
  ) {
    const data = this.timelineEditor.getData();
    if (!data) return;

    const timelineData = this.findTimelineData(timelineId);
    if (!timelineData) return;

    const totalDuration = data.duration;
    const keyframeTimes = this.playbackController.getKeyframeTimes();

    let targetPosition: number;
    if (removedKeyframePosition !== undefined) {
      targetPosition = removedKeyframePosition;
    } else if (changedKeyframeId) {
      const changedKeyframe = timelineData.keyframes.find(
        (kf) => kf.id === changedKeyframeId,
      );
      if (!changedKeyframe) return;
      targetPosition = changedKeyframe.position;
    } else if (this.selectedSectionInfo) {
      targetPosition = this.selectedSectionInfo.start;
    } else {
      return; // No specific keyframe or section selected/changed
    }

    const segment = getAnimationSegment(
      targetPosition,
      keyframeTimes,
      totalDuration,
      this.currentVariants,
    );
    if (!segment) {
      console.warn("Could not find segment for keyframe.");
      return;
    }

    const endingVariantKeyframe = timelineData.keyframes.find(
      (kf) => kf.locked && Math.abs(kf.position - segment.endPos) < 0.001,
    );

    if (!endingVariantKeyframe) {
      console.warn("Ending variant keyframe not found in timeline data.");
      return;
    }

    const endingVariantName = keyframeTimes[segment.endIndex].name;

    // Collect all *unlocked* keyframes within this segment
    const customKeyframesInSegment = timelineData.keyframes
      .filter(
        (kf) =>
          !kf.locked &&
          kf.position >= segment.startPos &&
          kf.position <= segment.endPos,
      )
      .map((kf) => {
        let fraction = 0;
        if (segment.activeDuration > 0.0001) {
          fraction =
            (kf.position - segment.activeStartPos) / segment.activeDuration;
        }

        return {
          fraction: Math.max(0, Math.min(1, fraction)),
          value: kf.value,
          easing: kf.easing,
        };
      });

    const targetEasing = endingVariantKeyframe.easing || "Inherit";
    const serializedCustomKeyframes = serializeKeyframes(
      customKeyframesInSegment,
      targetEasing,
    );

    // Update the local state of currentVariants immediately.
    const endingVariantIndex = this.currentVariants.findIndex(
      (v) => v.name === endingVariantName,
    );
    if (endingVariantIndex !== -1) {
      const targetVariant = this.currentVariants[endingVariantIndex];
      if (!targetVariant.animation) {
        targetVariant.animation = { customKeyframeData: {} };
      }
      if (!targetVariant.animation.customKeyframeData) {
        targetVariant.animation.customKeyframeData = {};
      }
      targetVariant.animation.customKeyframeData[timelineId] =
        serializedCustomKeyframes;
    } else {
      console.error("Could not find ending variant in currentVariants");
    }

    // Update the data field of the ending variant keyframe
    if (!endingVariantKeyframe.data) {
      endingVariantKeyframe.data = {};
    }
    endingVariantKeyframe.data.keyframes = serializedCustomKeyframes; // Store as string
    // Send message to backend to save this data
    parent.postMessage(
      {
        pluginMessage: {
          type: "save-custom-keyframes",
          timelineId: timelineId,
          endingVariantName: endingVariantName,
          serializedKeyframes: serializedCustomKeyframes,
        },
      },
      "*",
    );
  }

  /**
   * Deletes a custom timeline and its keyframes from the variant data.
   * @param timelineId The ID of the timeline to delete.
   */
  public deleteCustomTimeline(timelineId: string) {
      // Remove from currentVariants locally to keep state consistent
      this.currentVariants.forEach(variant => {
          if (variant.animation && variant.animation.customKeyframeData) {
              delete variant.animation.customKeyframeData[timelineId];
          }
      });
      // Send message to backend
      parent.postMessage(
          {
            pluginMessage: {
              type: "delete-custom-timeline",
              timelineId: timelineId,
            },
          },
          "*",
      );
  }
}