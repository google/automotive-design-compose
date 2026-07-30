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

import { Variant, SerializedNode } from "./timeline/types";
import { PlaybackController } from "./timeline/PlaybackController";
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { getAnimationSegment } from "./timeline/utils";
import { DataMapper } from "./services/DataMapper";
import { PropertiesPanel } from "./ui/PropertiesPanel";
import { ControlPanel, AnimationSettings } from "./ui/ControlPanel";
import { TimelineManager } from "./ui/TimelineManager";
import { ResizeManager } from "./ui/ResizeManager";
import { SimpleTestRunner } from "./utils/SimpleTestRunner";
import { compareAndPrintChanges } from "./utils/debug_compare";
import { registerUITests } from "./ui/ui_tests";

class AnimationUI {
  private timelineManager: TimelineManager;
  private playbackController: PlaybackController;
  private propertiesPanel: PropertiesPanel;
  private controlPanel: ControlPanel;
  private resizeManager: ResizeManager;

  private timelineContainer: HTMLElement;
  private testRunner: SimpleTestRunner;

  private currentSerializedVariants: SerializedNode[] = [];
  private currentVariants: Variant[] = [];
  private currentFrameIndex: number = 0;
  private isAnimationReady: boolean = false;
  private playAfterReady: boolean = false;
  private isSeekingFromDropdown: boolean = false;

  constructor() {
    this.timelineContainer = document.getElementById("timeline-container")!;
    this.playbackController = new PlaybackController({
      animationData: null,
      serializedVariants: [],
      variants: [],
    });

    // Initialize Components
    this.controlPanel = new ControlPanel(this.playbackController);

    this.timelineManager = new TimelineManager(
      this.timelineContainer,
      this.playbackController,
      null,
      () => this.currentVariants,
      () => this.currentSerializedVariants,
    );

    this.propertiesPanel = new PropertiesPanel(
      this.timelineManager.editor,
      this.playbackController,
    );
    this.timelineManager.setPropertiesPanel(this.propertiesPanel);

    this.resizeManager = new ResizeManager(
      document.getElementById("resizeHandle")!,
      document.getElementById("timeline-resizer")!,
      this.timelineContainer,
    );

    this.testRunner = new SimpleTestRunner("test-logs");
    registerUITests(
      this.testRunner,
      this.controlPanel,
      this.timelineManager,
      this.propertiesPanel,
    );

    this.setupEventListeners();
  }
  private setupEventListeners() {
    // Wiring ControlPanel events
    this.controlPanel.on("save", (settings: AnimationSettings) =>
      this.handleSave(settings),
    );
    this.controlPanel.on("discard", () => this.handleDiscard());
    this.controlPanel.on("delete-transition", (index: number) =>
      this.handleDeleteTransition(index),
    );
    this.controlPanel.on("export", () => this.handleExport());
    this.controlPanel.on("import", (file: File) => this.handleImport(file));
    this.controlPanel.on("reset", () =>
      parent.postMessage({ pluginMessage: { type: "reset-data" } }, "*"),
    );
    this.controlPanel.on("ping", () => {
      parent.postMessage({ pluginMessage: { type: "ping" } }, "*");
    });
    this.controlPanel.on("clear-preview", () =>
      parent.postMessage({ pluginMessage: { type: "clear-preview" } }, "*"),
    );
    this.controlPanel.on("select-preview", (isSelecting: boolean) => {
      if (isSelecting) {
        parent.postMessage(
          { pluginMessage: { type: "select-preview-frame" } },
          "*",
        );
      } else {
        this.controlPanel.setSelectingPreview(true);
        parent.postMessage(
          { pluginMessage: { type: "select-preview-frame" } },
          "*",
        );
      }
    });

    this.controlPanel.on("frame-changed", (index: number) => {
      if (index !== this.currentFrameIndex) {
        this.currentFrameIndex = index;
        this.controlPanel.setVariant(
          this.currentVariants[this.currentFrameIndex],
          this.currentVariants,
        );
        if (this.currentVariants[this.currentFrameIndex]) {
          this.timelineManager.updateRootNodeName(
            this.currentVariants[this.currentFrameIndex].name,
          );
        }

        const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(
          this.currentVariants,
        );
        const selectedKeyframe = keyframeTimes.find((kf) => kf.index === index);
        if (selectedKeyframe && totalTime > 0) {
          this.isSeekingFromDropdown = true;
          this.playbackController.seek(selectedKeyframe.time);
          this.isSeekingFromDropdown = false;
        }
        this.updateFigmaPreview();
      }
    });

    // Test Runner Events
    const runTestsButton = document.getElementById("run-tests-button");
    const closeTestsButton = document.getElementById("close-tests-button");
    const testContainer = document.getElementById("test-runner-container");

    if (runTestsButton && testContainer && closeTestsButton) {
      runTestsButton.onclick = () => {
        testContainer.style.display = "block";
        this.testRunner.run();
      };
      closeTestsButton.onclick = () => {
        testContainer.style.display = "none";
      };
    }

    // Playback Controller Events
    this.playbackController.on("timeupdate", (time: number) => {
      const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(
        this.currentVariants,
      );
      if (totalTime > 0) {
        this.timelineManager.editor.setPlayheadPosition(time / totalTime);

        let newFrameIndex = -1;
        const exactKeyframe = keyframeTimes.find(
          (kt) => Math.abs(kt.time - time) < 0.001,
        );
        if (exactKeyframe) {
          newFrameIndex = exactKeyframe.index;
        } else {
          const segment = getAnimationSegment(
            time / totalTime,
            keyframeTimes,
            totalTime,
            this.currentVariants,
          );
          if (segment) {
            newFrameIndex = keyframeTimes[segment.endIndex].index;
          } else if (time >= totalTime && keyframeTimes.length > 0) {
            newFrameIndex = keyframeTimes[0].index;
          }
        }

        const timeDisplay = document.getElementById("playhead-time-display");
        if (timeDisplay) {
          timeDisplay.textContent = `Time: ${time.toFixed(2)}s / ${totalTime.toFixed(2)}s`;
        }

        if (
          !this.isSeekingFromDropdown &&
          newFrameIndex !== -1 &&
          newFrameIndex !== this.currentFrameIndex
        ) {
          this.currentFrameIndex = newFrameIndex;
          this.controlPanel.setVariant(
            this.currentVariants[this.currentFrameIndex],
            this.currentVariants,
          );
          this.controlPanel.setSelectedFrame(this.currentFrameIndex);
          if (this.currentVariants[this.currentFrameIndex]) {
            this.timelineManager.updateRootNodeName(
              this.currentVariants[this.currentFrameIndex].name,
            );
          }
        }
      }
    });

    this.playbackController.on("keyframe-changed", (index: number) => {
      if (!this.isSeekingFromDropdown && index !== this.currentFrameIndex) {
        this.currentFrameIndex = index;
        this.controlPanel.setVariant(
          this.currentVariants[this.currentFrameIndex],
          this.currentVariants,
        );
        this.controlPanel.setSelectedFrame(this.currentFrameIndex);
      }
    });

    // Global Events
    window.addEventListener("mouseup", () => {
      document.body.classList.remove("no-select");
    });

    window.addEventListener("load", () =>
      parent.postMessage({ pluginMessage: { type: "ready" } }, "*"),
    );

    window.onmessage = (event: MessageEvent) => this.handleMessage(event);
  }

  private handleSave(settings: AnimationSettings) {
    const existingAnimation =
      this.currentVariants[this.currentFrameIndex].animation || {};

    const newSpec = {
      initial_delay: {
        secs: Math.floor(settings.initialDelay),
        nanos: Math.round((settings.initialDelay % 1) * 1e9),
      },
      animation: {
        Smooth: {
          duration: {
            secs: Math.floor(settings.duration),
            nanos: Math.round((settings.duration % 1) * 1e9),
          },
          repeat_type: "NoRepeat",
          easing: settings.easing,
        },
      },
      interrupt_type: settings.interruptType,
    };

    const newAnimationData = { ...existingAnimation };

    if (!newAnimationData.transitions) {
      newAnimationData.transitions = [];
    }

    const toVariantName = this.currentVariants[this.currentFrameIndex].name;

    if (
      settings.fromVariant === "*" &&
      settings.animationName === "Default" &&
      (toVariantName === "*" || !toVariantName)
    ) {
      newAnimationData.default_spec = newSpec;
    } else {
      let targetTransitionIndex = newAnimationData.transitions.findIndex(
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (t: any) =>
          t.from === settings.fromVariant &&
          t.name === settings.animationName &&
          (t.to === toVariantName || (!t.to && !toVariantName)),
      );

      if (targetTransitionIndex >= 0) {
        newAnimationData.transitions[targetTransitionIndex].spec = newSpec;
      } else {
        newAnimationData.transitions.push({
          from: settings.fromVariant,
          to: toVariantName,
          name: settings.animationName,
          spec: newSpec,
          timelines: {},
        });
        targetTransitionIndex = newAnimationData.transitions.length - 1;
      }
    }

    // Preserve legacy spec temporarily to prevent older UI code from crashing
    newAnimationData.spec = newSpec;

    this.currentVariants[this.currentFrameIndex].animation = newAnimationData;

    parent.postMessage(
      {
        pluginMessage: {
          type: "save-data",
          frameName: this.currentVariants[this.currentFrameIndex].name,
          data: JSON.stringify(newAnimationData),
        },
      },
      "*",
    );

    const newAnimationDataObject = DataMapper.transformDataToAnimationData(
      this.currentVariants,
      this.currentSerializedVariants,
    );

    this.playbackController.updateData({
      animationData: newAnimationDataObject,
      serializedVariants: this.currentSerializedVariants,
      variants: this.currentVariants,
    });

    this.timelineManager.editor.setData(newAnimationDataObject);

    const savedIndex = Math.max(0, targetTransitionIndex || 0);
    this.controlPanel.setVariant(
      this.currentVariants[this.currentFrameIndex],
      this.currentVariants,
      savedIndex,
    );
  }

  private handleDiscard() {
    this.controlPanel.setVariant(
      this.currentVariants[this.currentFrameIndex],
      this.currentVariants,
      this.controlPanel.getSelectedTransitionIndex(),
    );
  }

  private handleDeleteTransition(index: number) {
    const existingAnimation =
      this.currentVariants[this.currentFrameIndex].animation || {};

    if (
      existingAnimation.transitions &&
      index >= 0 &&
      index < existingAnimation.transitions.length &&
      existingAnimation.transitions.length > 1
    ) {
      existingAnimation.transitions.splice(index, 1);
    }

    this.currentVariants[this.currentFrameIndex].animation = existingAnimation;

    parent.postMessage(
      {
        pluginMessage: {
          type: "save-data",
          frameName: this.currentVariants[this.currentFrameIndex].name,
          data: JSON.stringify(existingAnimation),
        },
      },
      "*",
    );

    const newAnimationDataObject = DataMapper.transformDataToAnimationData(
      this.currentVariants,
      this.currentSerializedVariants,
    );

    this.playbackController.updateData({
      animationData: newAnimationDataObject,
      serializedVariants: this.currentSerializedVariants,
      variants: this.currentVariants,
    });

    this.timelineManager.editor.setData(newAnimationDataObject);

    this.controlPanel.setVariant(
      this.currentVariants[this.currentFrameIndex],
      this.currentVariants,
      0,
    );
  }

  private handleExport() {
    const dataToExport = {
      variants: this.currentVariants,
      serializedVariants: this.currentSerializedVariants,
      animationData: DataMapper.transformDataToAnimationData(
        this.currentVariants,
        this.currentSerializedVariants,
      ),
    };
    const dataStr =
      "data:text/json;charset=utf-8," +
      encodeURIComponent(JSON.stringify(dataToExport, null, 2));
    const downloadAnchorNode = document.createElement("a");
    downloadAnchorNode.setAttribute("href", dataStr);
    downloadAnchorNode.setAttribute("download", "animation_data.json");
    document.body.appendChild(downloadAnchorNode);
    downloadAnchorNode.click();
    downloadAnchorNode.remove();
  }

  private handleImport(file: File) {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const content = e.target?.result as string;
        if (!content) return;
        const imported = JSON.parse(content);
        if (!imported.variants || !Array.isArray(imported.variants)) {
          alert("Invalid animation_data.json file: missing variants array.");
          return;
        }

        imported.variants.forEach((importedVariant: Variant) => {
          const targetVariant = this.currentVariants.find(
            (v) => v.name === importedVariant.name,
          );
          if (targetVariant && importedVariant.animation) {
            targetVariant.animation = importedVariant.animation;

            parent.postMessage(
              {
                pluginMessage: {
                  type: "save-data",
                  frameName: targetVariant.name,
                  data: JSON.stringify(importedVariant.animation),
                },
              },
              "*",
            );
          }
        });

        const newAnimationData = DataMapper.transformDataToAnimationData(
          this.currentVariants,
          this.currentSerializedVariants,
        );

        this.playbackController.updateData({
          animationData: newAnimationData,
          serializedVariants: this.currentSerializedVariants,
          variants: this.currentVariants,
        });

        this.timelineManager.editor.setData(newAnimationData);
        this.controlPanel.updateKeyframeSelector(
          this.currentVariants,
          this.currentFrameIndex,
        );
        this.controlPanel.setVariant(
          this.currentVariants[this.currentFrameIndex],
          this.currentVariants,
        );
        if (this.currentVariants[this.currentFrameIndex]) {
          this.timelineManager.updateRootNodeName(
            this.currentVariants[this.currentFrameIndex].name,
          );
        }

        const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(
          this.currentVariants,
        );
        const selectedKeyframe = keyframeTimes.find(
          (kf) => kf.index === this.currentFrameIndex,
        );
        if (selectedKeyframe && totalTime > 0) {
          this.isSeekingFromDropdown = true;
          this.playbackController.seek(selectedKeyframe.time);
          this.isSeekingFromDropdown = false;
        }

        alert("Successfully imported and saved animation data to Figma!");
      } catch (err) {
        console.error("Error importing file:", err);
        alert("Failed to parse imported JSON file.");
      }
    };
    reader.readAsText(file);
  }

  private updateFigmaPreview() {
    const animatedNodes: SerializedNode[] = [];
    const variantNode = this.currentSerializedVariants[this.currentFrameIndex];
    DataMapper.collectNodesForPreview(variantNode, null, true, animatedNodes);
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

  private handleMessage(event: MessageEvent) {
    const pluginMessage = event.data.pluginMessage;
    if (!pluginMessage) return;

    switch (pluginMessage.type) {
      case "animation-ready": {
        this.isAnimationReady = true;
        this.updateFigmaPreview(); // Ensure properties and metadata are synced
        if (this.playAfterReady) {
          this.playAfterReady = false;
          this.playbackController.play();
        }
        break;
      }
      case "pong": {
        break;
      }
      case "selection-mode-started": {
        this.controlPanel.setSelectingPreview(true);
        break;
      }
      case "selection-mode-ended": {
        this.controlPanel.setSelectingPreview(false);
        break;
      }
      case "preview-frame-selected": {
        this.controlPanel.setSelectingPreview(false, pluginMessage.name);
        break;
      }
      case "preview-frame-cleared": {
        this.controlPanel.setSelectingPreview(false);
        break;
      }
      case "clear-timeline": {
        this.currentSerializedVariants = [];
        this.currentVariants = [];
        this.currentFrameIndex = 0;
        // Do not destroy/recreate the editor, just clear its data to preserve references
        this.timelineManager.editor.setData({ nodes: [], duration: 1 });
        break;
      }
      case "clear": {
        document.getElementById("editor")!.style.display = "none";
        this.currentSerializedVariants = [];
        this.currentVariants = [];
        this.currentFrameIndex = 0;
        break;
      }
      case "update": {
        this.isAnimationReady = false;
        document.getElementById("editor")!.style.display = "block";
        const { variants, serializedVariants, selectedVariantName } =
          pluginMessage;
        this.currentSerializedVariants = serializedVariants;
        this.currentVariants = variants;
        this.propertiesPanel.setCurrentVariants(variants);

        const animationData = DataMapper.transformDataToAnimationData(
          variants,
          serializedVariants,
        );
        this.timelineManager.editor.setData(animationData);
        this.playbackController.updateData({
          animationData: animationData,
          serializedVariants: this.currentSerializedVariants,
          variants: this.currentVariants,
        });

        let startIndex = 0;
        if (selectedVariantName) {
          const index = variants.findIndex(
            (v: Variant) => v.name === selectedVariantName,
          );
          if (index !== -1) startIndex = index;
        }
        this.currentFrameIndex = startIndex;

        this.controlPanel.updateKeyframeSelector(variants, startIndex);
        this.controlPanel.setVariant(variants[startIndex], variants);
        if (variants[startIndex]) {
          this.timelineManager.updateRootNodeName(variants[startIndex].name);
        }

        const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(
          this.currentVariants,
        );
        const selectedKeyframe = keyframeTimes.find(
          (kf) => kf.index === startIndex,
        );
        if (selectedKeyframe && totalTime > 0) {
          this.isSeekingFromDropdown = true;
          this.playbackController.seek(selectedKeyframe.time);
          this.isSeekingFromDropdown = false;
        }
        const timeDisplay = document.getElementById("playhead-time-display");
        if (timeDisplay) {
          const initialTime = selectedKeyframe ? selectedKeyframe.time : 0;
          timeDisplay.textContent = `Time: ${initialTime.toFixed(2)}s / ${totalTime.toFixed(2)}s`;
        }

        parent.postMessage(
          { pluginMessage: { type: "prepare-animation" } },
          "*",
        );
        break;
      }
      case "preview-node-changed": {
        const { originalNodeId, nodeProps } = pluginMessage;
        compareAndPrintChanges(
          originalNodeId,
          nodeProps,
          this.currentSerializedVariants,
          this.currentFrameIndex,
          this.timelineManager,
          this.playbackController,
        );
        break;
      }
      case "preview-node-selected": {
        break;
      }
      case "preview-update-complete": {
        this.playbackController.acknowledgePreviewUpdate();
        break;
      }
    }
  }
}

new AnimationUI();
