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

import { EventEmitter } from "../timeline/EventEmitter";
import { PlaybackController } from "../timeline/PlaybackController";
import { Variant, KeyframeTime } from "../timeline/types";
import { DataMapper } from "../services/DataMapper";

/**
 * Settings for the animation configuration.
 */
export interface AnimationSettings {
  initialDelay: number;
  duration: number;
  easing: string;
  interruptType: string;
}

/**
 * Manages the main control panel UI, including playback controls, settings inputs, and action buttons.
 * It handles user interactions for configuring animation properties and controlling the timeline.
 */
export class ControlPanel extends EventEmitter {
  private playbackController: PlaybackController;

  private playButton: HTMLButtonElement;
  private saveButton: HTMLButtonElement;
  private discardButton: HTMLButtonElement;
  private pingButton: HTMLButtonElement;
  private clearPreviewButton: HTMLButtonElement;
  private exportButton: HTMLButtonElement;
  private resetButton: HTMLButtonElement;
  private selectPreviewFrameButton: HTMLButtonElement;
  private keyframeSelect: HTMLSelectElement;
  private initialDelayInput: HTMLInputElement;
  private durationInput: HTMLInputElement;
  private easingSelect: HTMLSelectElement;
  private interruptTypeSelect: HTMLSelectElement;
  private continueCheckbox: HTMLInputElement;
  private throttleUpdatesCheckbox: HTMLInputElement;
  private variantNameDisplay: HTMLElement;

  private initialSettings: AnimationSettings = {
    initialDelay: 0,
    duration: 0.3,
    easing: "Linear",
    interruptType: "None",
  };

  constructor(playbackController: PlaybackController) {
    super();
    this.playbackController = playbackController;

    this.playButton = document.getElementById("play-button") as HTMLButtonElement;
    this.saveButton = document.getElementById("save-button") as HTMLButtonElement;
    this.discardButton = document.getElementById("discard-button") as HTMLButtonElement;
    this.pingButton = document.getElementById("ping-button") as HTMLButtonElement;
    this.clearPreviewButton = document.getElementById("clear-preview-button") as HTMLButtonElement;
    this.exportButton = document.getElementById("export-button") as HTMLButtonElement;
    this.resetButton = document.getElementById("reset-button") as HTMLButtonElement;
    this.selectPreviewFrameButton = document.getElementById("select-preview-frame-button") as HTMLButtonElement;
    this.keyframeSelect = document.getElementById("keyframe-select") as HTMLSelectElement;
    this.initialDelayInput = document.getElementById("initial-delay") as HTMLInputElement;
    this.durationInput = document.getElementById("duration") as HTMLInputElement;
    this.easingSelect = document.getElementById("easing") as HTMLSelectElement;
    this.interruptTypeSelect = document.getElementById("interrupt-type") as HTMLSelectElement;
    this.continueCheckbox = document.getElementById("continue-checkbox") as HTMLInputElement;
    this.throttleUpdatesCheckbox = document.getElementById("throttle-updates-checkbox") as HTMLInputElement;
    this.variantNameDisplay = document.getElementById("variant-name-display") as HTMLElement;

    this.setupEventListeners();
    this.setupPlaybackListeners();

    // Initialize controller state
    this.playbackController.setContinue(this.continueCheckbox.checked);
    this.playbackController.setThrottleUpdates(this.throttleUpdatesCheckbox.checked);
  }

  private setupEventListeners() {
    this.playButton.onclick = () => {
      if (this.playbackController.isPlaying) {
        this.playbackController.pause();
      } else {
        this.playbackController.play();
      }
    };

    this.continueCheckbox.onchange = () => {
      this.playbackController.setContinue(this.continueCheckbox.checked);
    };

    this.throttleUpdatesCheckbox.onchange = () => {
      this.playbackController.setThrottleUpdates(this.throttleUpdatesCheckbox.checked);
    };

    this.saveButton.onclick = () => this.emit("save", this.getCurrentSettings());
    this.discardButton.onclick = () => this.emit("discard");
    this.exportButton.onclick = () => this.emit("export");
    this.resetButton.onclick = () => this.emit("reset");
    this.pingButton.onclick = () => this.emit("ping");
    this.clearPreviewButton.onclick = () => this.emit("clear-preview");
    
    this.selectPreviewFrameButton.onclick = () => {
       const isSelecting = this.selectPreviewFrameButton.classList.contains("selecting");
       this.emit("select-preview", isSelecting);
    };

    this.keyframeSelect.addEventListener("change", () => {
      this.emit("frame-changed", parseInt(this.keyframeSelect.value, 10));
    });

    const checkChange = () => this.checkPropertiesChanged();
    this.initialDelayInput.addEventListener("input", checkChange);
    this.durationInput.addEventListener("input", checkChange);
    this.easingSelect.addEventListener("change", checkChange);
    this.interruptTypeSelect.addEventListener("change", checkChange);
  }

  private setupPlaybackListeners() {
    this.playbackController.on("play", () => {
      this.playButton.textContent = "Pause";
    });

    this.playbackController.on("pause", () => {
      this.playButton.textContent = "Play";
    });

    this.playbackController.on("stop", (...args: unknown[]) => {
      const stopIndex = args[0] as number | undefined;
      this.playButton.textContent = "Play";
      if (stopIndex !== undefined) {
        this.keyframeSelect.value = String(stopIndex);
        // We might want to update UI but usually stop comes with frame update which handles it
        this.emit("playback-stopped", stopIndex);
      }
    });
  }

  /**
   * Updates the panel with the settings from the selected variant.
   * Disables save/discard buttons initially.
   * @param variant The variant to display settings for.
   */
  public setVariant(variant: Variant | undefined) {
    this.variantNameDisplay.textContent = variant ? variant.name : "N/A";

    if (!variant || !variant.animation || !variant.animation.spec) {
      this.initialSettings = { initialDelay: 0, duration: 0.3, easing: "Linear", interruptType: "None" };
      this.updateInputs(this.initialSettings);
      this.saveButton.disabled = true;
      this.discardButton.disabled = true;
      return;
    }

    const spec = variant.animation.spec;
    const delay = (spec.initial_delay?.secs || 0) + (spec.initial_delay?.nanos || 0) / 1e9;
    
    let duration = 0;
    let easing = "Linear";
    
    if (spec.animation && spec.animation.Smooth) {
        const d = spec.animation.Smooth.duration;
        duration = (d.secs || 0) + (d.nanos || 0) / 1e9;
        easing = spec.animation.Smooth.easing;
    }
    
    const interruptType = spec.interrupt_type || "None";

    this.initialSettings = {
      initialDelay: delay,
      duration: duration,
      easing: easing,
      interruptType: interruptType
    };

    this.updateInputs(this.initialSettings);
    this.saveButton.disabled = true;
    this.discardButton.disabled = true;
  }

  /**
   * Updates the input fields with the provided settings.
   * @param settings The animation settings to display.
   */
  public updateInputs(settings: AnimationSettings) {
    this.initialDelayInput.value = settings.initialDelay.toFixed(2);
    this.durationInput.value = settings.duration.toFixed(2);
    this.easingSelect.value = settings.easing;
    this.interruptTypeSelect.value = settings.interruptType;
  }

  /**
   * Retrieves the current animation settings from the input fields.
   * @returns The current animation settings.
   */
  public getCurrentSettings(): AnimationSettings {
    return {
      initialDelay: parseFloat(this.initialDelayInput.value),
      duration: parseFloat(this.durationInput.value),
      easing: this.easingSelect.value,
      interruptType: this.interruptTypeSelect.value
    };
  }

  /**
   * Checks if the current settings differ from the saved settings and updates button states.
   */
  public checkPropertiesChanged() {
    const current = this.getCurrentSettings();
    const initial = this.initialSettings;

    const changed = 
        Math.abs(current.initialDelay - initial.initialDelay) > 0.0001 ||
        Math.abs(current.duration - initial.duration) > 0.0001 ||
        current.easing !== initial.easing ||
        current.interruptType !== initial.interruptType;

    this.saveButton.disabled = !changed;
    this.discardButton.disabled = !changed;
  }

  /**
   * Updates the keyframe selection dropdown.
   * @param variants The list of available variants.
   * @param currentIndex The index of the currently selected variant.
   */
  public updateKeyframeSelector(variants: Variant[], currentIndex: number) {
    const { keyframeTimes } = DataMapper.calculateKeyframeData(variants);
    this.keyframeSelect.innerHTML = "";
    keyframeTimes.forEach((keyframe: KeyframeTime) => {
      if (keyframe.isLoop) return;
      const option = document.createElement("option");
      option.value = String(keyframe.index);
      option.textContent = keyframe.name;
      this.keyframeSelect.appendChild(option);
    });
    this.keyframeSelect.value = String(currentIndex);
  }

  /**
   * Updates the state and UI of the preview frame selection button.
   * @param isSelecting Whether selection mode is active.
   * @param name Optional name of the selected preview frame to display.
   */
  public setSelectingPreview(isSelecting: boolean, name?: string) {
      if (isSelecting) {
          this.selectPreviewFrameButton.classList.add("selecting");
          this.selectPreviewFrameButton.textContent = "Selecting Preview Frame...";
      } else {
          this.selectPreviewFrameButton.classList.remove("selecting");
          if (name) {
            this.selectPreviewFrameButton.textContent = `Preview: ${name}`;
          } else {
             this.selectPreviewFrameButton.textContent = "Select Preview Frame";
          }
      }
  }
  
  /**
   * Sets the state of the 'Continue' (loop) checkbox.
   * @param checked Whether playback should continue looping.
   */
  public setContinue(checked: boolean) {
      this.continueCheckbox.checked = checked;
      this.playbackController.setContinue(checked);
  }

  /**
   * Sets the currently selected frame in the dropdown.
   * @param index The index of the frame to select.
   */
  public setSelectedFrame(index: number) {
      this.keyframeSelect.value = String(index);
  }
}
