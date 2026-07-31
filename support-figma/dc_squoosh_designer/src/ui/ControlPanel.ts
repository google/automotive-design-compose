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
  fromVariant: string;
  animationName: string;
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
  private importButton: HTMLButtonElement;
  private importFileInput: HTMLInputElement;
  private resetButton: HTMLButtonElement;
  private selectPreviewFrameButton: HTMLButtonElement;
  private keyframeSelect: HTMLSelectElement;
  private fromVariantSelect: HTMLSelectElement;
  private animationNameInput: HTMLInputElement;
  private initialDelayInput: HTMLInputElement;
  private durationInput: HTMLInputElement;
  private easingSelect: HTMLSelectElement;
  private interruptTypeSelect: HTMLSelectElement;
  private continueCheckbox: HTMLInputElement;
  private throttleUpdatesCheckbox: HTMLInputElement;
  private transitionSelect?: HTMLSelectElement;
  private deleteTransitionButton?: HTMLButtonElement;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private currentTransitions: any[] = [];
  private selectedTransitionIndex: number = 0;
  private variantNameDisplay: HTMLElement;

  private initialSettings: AnimationSettings = {
    fromVariant: "*",
    animationName: "Default",
    initialDelay: 0,
    duration: 0.3,
    easing: "Linear",
    interruptType: "None",
  };

  constructor(playbackController: PlaybackController) {
    super();
    this.playbackController = playbackController;

    this.playButton = document.getElementById(
      "play-button",
    ) as HTMLButtonElement;
    this.saveButton = document.getElementById(
      "save-button",
    ) as HTMLButtonElement;
    this.discardButton = document.getElementById(
      "discard-button",
    ) as HTMLButtonElement;
    this.pingButton = document.getElementById(
      "ping-button",
    ) as HTMLButtonElement;
    this.clearPreviewButton = document.getElementById(
      "clear-preview-button",
    ) as HTMLButtonElement;
    this.exportButton = document.getElementById(
      "export-button",
    ) as HTMLButtonElement;
    this.importButton = document.getElementById(
      "import-button",
    ) as HTMLButtonElement;
    this.importFileInput = document.getElementById(
      "import-file-input",
    ) as HTMLInputElement;
    this.resetButton = document.getElementById(
      "reset-button",
    ) as HTMLButtonElement;
    this.selectPreviewFrameButton = document.getElementById(
      "select-preview-frame-button",
    ) as HTMLButtonElement;
    this.keyframeSelect = document.getElementById(
      "keyframe-select",
    ) as HTMLSelectElement;
    this.fromVariantSelect = document.getElementById(
      "from-variant",
    ) as HTMLSelectElement;
    this.animationNameInput = document.getElementById(
      "animation-name",
    ) as HTMLInputElement;
    this.initialDelayInput = document.getElementById(
      "initial-delay",
    ) as HTMLInputElement;
    this.durationInput = document.getElementById(
      "duration",
    ) as HTMLInputElement;
    this.easingSelect = document.getElementById("easing") as HTMLSelectElement;
    this.interruptTypeSelect = document.getElementById(
      "interrupt-type",
    ) as HTMLSelectElement;
    this.continueCheckbox = document.getElementById(
      "continue-checkbox",
    ) as HTMLInputElement;
    this.throttleUpdatesCheckbox = document.getElementById(
      "throttle-updates-checkbox",
    ) as HTMLInputElement;
    this.variantNameDisplay = document.getElementById(
      "variant-name-display",
    ) as HTMLElement;
    this.transitionSelect =
      (document.getElementById("transition-select") as HTMLSelectElement) ||
      undefined;
    this.deleteTransitionButton =
      (document.getElementById(
        "delete-transition-button",
      ) as HTMLButtonElement) || undefined;

    this.setupEventListeners();
    this.setupPlaybackListeners();

    // Initialize controller state
    this.playbackController.setContinue(this.continueCheckbox.checked);
    this.playbackController.setThrottleUpdates(
      this.throttleUpdatesCheckbox.checked,
    );
  }

  private setupEventListeners() {
    this.playButton.onclick = () => {
      if (this.playbackController.isPlaying) {
        this.playbackController.pause();
      } else {
        this.playbackController.play();
      }
    };

    window.addEventListener("keydown", (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      if (
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.tagName === "SELECT" ||
          target.isContentEditable)
      ) {
        return;
      }
      if (e.code === "Space" || e.key === " ") {
        e.preventDefault();
        if (this.playbackController.isPlaying) {
          this.playbackController.pause();
        } else {
          this.playbackController.play();
        }
      }
    });

    this.continueCheckbox.onchange = () => {
      this.playbackController.setContinue(this.continueCheckbox.checked);
    };

    this.throttleUpdatesCheckbox.onchange = () => {
      this.playbackController.setThrottleUpdates(
        this.throttleUpdatesCheckbox.checked,
      );
    };

    this.saveButton.onclick = () =>
      this.emit("save", this.getCurrentSettings());
    this.discardButton.onclick = () => this.emit("discard");
    if (this.deleteTransitionButton) {
      this.deleteTransitionButton.onclick = () =>
        this.emit("delete-transition", this.selectedTransitionIndex);
    }
    this.exportButton.onclick = () => this.emit("export");
    this.importButton.onclick = () => {
      this.importFileInput.click();
    };
    this.importFileInput.onchange = () => {
      const files = this.importFileInput.files;
      if (files && files[0]) {
        this.emit("import", files[0]);
        this.importFileInput.value = "";
      }
    };
    this.resetButton.onclick = () => this.emit("reset");
    this.pingButton.onclick = () => this.emit("ping");
    this.clearPreviewButton.onclick = () => this.emit("clear-preview");

    if (this.transitionSelect) {
      this.transitionSelect.addEventListener("change", () =>
        this.handleTransitionSelectChange(),
      );
    }

    this.selectPreviewFrameButton.onclick = () => {
      const isSelecting =
        this.selectPreviewFrameButton.classList.contains("selecting");
      this.emit("select-preview", isSelecting);
    };

    this.keyframeSelect.addEventListener("change", () => {
      this.emit("frame-changed", parseInt(this.keyframeSelect.value, 10));
    });

    const checkChange = () => this.checkPropertiesChanged();
    this.fromVariantSelect.addEventListener("change", checkChange);
    this.animationNameInput.addEventListener("input", checkChange);
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
  public setVariant(
    variant: Variant | undefined,
    allVariants: Variant[] = [],
    selectedTransitionIndex: number | string = 0,
  ) {
    this.variantNameDisplay.textContent = variant ? variant.name : "N/A";

    this.fromVariantSelect.innerHTML =
      '<option value="*">* (Any Origin)</option>';
    allVariants.forEach((v) => {
      if (v.name !== (variant ? variant.name : "")) {
        const option = document.createElement("option");
        option.value = v.name;
        option.textContent = v.name;
        this.fromVariantSelect.appendChild(option);
      }
    });

    if (
      !variant ||
      !variant.animation ||
      (!variant.animation.spec &&
        !variant.animation.default_spec &&
        (!variant.animation.transitions ||
          variant.animation.transitions.length === 0))
    ) {
      this.currentTransitions = [];
      if (this.transitionSelect) {
        this.transitionSelect.innerHTML = "";
      }
      if (this.deleteTransitionButton) {
        this.deleteTransitionButton.disabled = true;
      }
      this.initialSettings = {
        fromVariant: "*",
        animationName: "Default",
        initialDelay: 0,
        duration: 0.3,
        easing: "Linear",
        interruptType: "None",
      };
      this.updateInputs(this.initialSettings);
      this.saveButton.disabled = true;
      this.discardButton.disabled = true;
      return;
    }

    if (
      variant.animation.transitions &&
      variant.animation.transitions.length > 0
    ) {
      this.currentTransitions = variant.animation.transitions;
    } else {
      this.currentTransitions = [
        {
          from: "*",
          to: variant.name,
          name: "Default",
          spec: variant.animation.spec || variant.animation.default_spec,
          timelines: {},
        },
      ];
    }

    const transitionSelect = this.transitionSelect;
    if (transitionSelect) {
      transitionSelect.innerHTML = "";
      this.currentTransitions.forEach((t, idx) => {
        const option = document.createElement("option");
        option.value = String(idx);
        const fromStr = t.from || "*";
        const nameStr = t.name || "Default";
        option.textContent = `${fromStr} → ${variant.name} : ${nameStr}`;
        transitionSelect.appendChild(option);
      });
      const newOption = document.createElement("option");
      newOption.value = "NEW";
      newOption.textContent = "+ Create New Transition...";
      transitionSelect.appendChild(newOption);

      let targetIdx = 0;
      if (typeof selectedTransitionIndex === "number") {
        if (
          selectedTransitionIndex >= 0 &&
          selectedTransitionIndex < this.currentTransitions.length
        ) {
          targetIdx = selectedTransitionIndex;
        }
        this.selectedTransitionIndex = targetIdx;
        transitionSelect.value = String(targetIdx);
        this.loadTransitionAtIndex(targetIdx);
      } else if (selectedTransitionIndex === "NEW") {
        transitionSelect.value = "NEW";
        this.handleTransitionSelectChange();
      }
    } else {
      this.loadTransitionAtIndex(
        typeof selectedTransitionIndex === "number" &&
          selectedTransitionIndex >= 0 &&
          selectedTransitionIndex < this.currentTransitions.length
          ? selectedTransitionIndex
          : 0,
      );
    }
  }

  private loadTransitionAtIndex(idx: number) {
    const matchingTrans = this.currentTransitions[idx];
    let fromVariant = "*";
    let animationName = "Default";
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let specToUse: any = undefined;

    if (matchingTrans) {
      fromVariant = matchingTrans.from || "*";
      animationName = matchingTrans.name || "Default";
      specToUse = matchingTrans.spec;
    }

    let delay = 0;
    let duration = 0;
    let easing = "Linear";
    let interruptType = "None";

    if (specToUse) {
      delay =
        (specToUse.initial_delay?.secs || 0) +
        (specToUse.initial_delay?.nanos || 0) / 1e9;
      if (specToUse.animation && specToUse.animation.Smooth) {
        const d = specToUse.animation.Smooth.duration;
        duration = (d.secs || 0) + (d.nanos || 0) / 1e9;
        easing = specToUse.animation.Smooth.easing;
      }
      interruptType = specToUse.interrupt_type || "None";
    }

    this.initialSettings = {
      fromVariant: fromVariant,
      animationName: animationName,
      initialDelay: delay,
      duration: duration,
      easing: easing,
      interruptType: interruptType,
    };

    this.updateInputs(this.initialSettings);
    this.saveButton.disabled = true;
    this.discardButton.disabled = true;
    if (this.deleteTransitionButton) {
      this.deleteTransitionButton.disabled =
        this.currentTransitions.length <= 1;
    }
  }

  private handleTransitionSelectChange() {
    if (!this.transitionSelect) return;
    if (this.transitionSelect.value === "NEW") {
      this.selectedTransitionIndex = -1;
      this.initialSettings = {
        fromVariant: "*",
        animationName: "NewTransition",
        initialDelay: 0,
        duration: 0.3,
        easing: "Linear",
        interruptType: "None",
      };
      this.updateInputs(this.initialSettings);
      this.saveButton.disabled = false;
      this.discardButton.disabled = false;
      if (this.deleteTransitionButton) {
        this.deleteTransitionButton.disabled = true;
      }
    } else {
      const idx = parseInt(this.transitionSelect.value, 10);
      this.selectedTransitionIndex = idx;
      this.loadTransitionAtIndex(idx);
      this.emit("transition-selected", idx);
    }
  }

  public getSelectedTransitionIndex(): number {
    return this.selectedTransitionIndex;
  }

  /**
   * Updates the input fields with the provided settings.
   * @param settings The animation settings to display.
   */
  public updateInputs(settings: AnimationSettings) {
    this.fromVariantSelect.value = settings.fromVariant;
    this.animationNameInput.value = settings.animationName;
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
      fromVariant: this.fromVariantSelect.value,
      animationName: this.animationNameInput.value,
      initialDelay: parseFloat(this.initialDelayInput.value),
      duration: parseFloat(this.durationInput.value),
      easing: this.easingSelect.value,
      interruptType: this.interruptTypeSelect.value,
    };
  }

  /**
   * Checks if the current settings differ from the saved settings and updates button states.
   */
  public checkPropertiesChanged() {
    const current = this.getCurrentSettings();
    const initial = this.initialSettings;

    const changed =
      current.fromVariant !== initial.fromVariant ||
      current.animationName !== initial.animationName ||
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
