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
        () => this.currentSerializedVariants
    );

    this.propertiesPanel = new PropertiesPanel(this.timelineManager.editor, this.playbackController);
    this.timelineManager.setPropertiesPanel(this.propertiesPanel);

    this.resizeManager = new ResizeManager(
        document.getElementById("resizeHandle")!,
        document.getElementById("timeline-resizer")!,
        this.timelineContainer
    );

    this.testRunner = new SimpleTestRunner('test-logs');
    registerUITests(this.testRunner, this.controlPanel, this.timelineManager, this.propertiesPanel);

    this.setupEventListeners();
  }
  private setupEventListeners() {
    // Wiring ControlPanel events
    this.controlPanel.on("save", (settings: AnimationSettings) => this.handleSave(settings));
    this.controlPanel.on("discard", () => this.handleDiscard());
    this.controlPanel.on("export", () => this.handleExport());
    this.controlPanel.on("reset", () => parent.postMessage({ pluginMessage: { type: "reset-data" } }, "*"));
    this.controlPanel.on("ping", () => {
        parent.postMessage({ pluginMessage: { type: "ping" } }, "*");
    });
    this.controlPanel.on("clear-preview", () => parent.postMessage({ pluginMessage: { type: "clear-preview" } }, "*"));
    this.controlPanel.on("select-preview", (isSelecting: boolean) => {
        if (isSelecting) {
             parent.postMessage({ pluginMessage: { type: "select-preview-frame" } }, "*");
        } else {
             this.controlPanel.setSelectingPreview(true);
             parent.postMessage({ pluginMessage: { type: "select-preview-frame" } }, "*");
        }
    });

    this.controlPanel.on("frame-changed", (index: number) => {
        if (index !== this.currentFrameIndex) {
            this.currentFrameIndex = index;
            this.controlPanel.setVariant(this.currentVariants[this.currentFrameIndex]);

            const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(this.currentVariants);
            const selectedKeyframe = keyframeTimes.find((kf) => kf.index === index);
            if (selectedKeyframe && totalTime > 0) {
                this.playbackController.seek(selectedKeyframe.time);
            }
            this.updateFigmaPreview();
        }
    });

    // Test Runner Events
    const runTestsButton = document.getElementById('run-tests-button');
    const closeTestsButton = document.getElementById('close-tests-button');
    const testContainer = document.getElementById('test-runner-container');

    if (runTestsButton && testContainer && closeTestsButton) {
        runTestsButton.onclick = () => {
            testContainer.style.display = 'block';
            this.testRunner.run();
        };
        closeTestsButton.onclick = () => {
            testContainer.style.display = 'none';
        };
    }

    // Playback Controller Events
    this.playbackController.on("timeupdate", (time: number) => {
      const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(this.currentVariants);
      if (totalTime > 0) {
        this.timelineManager.editor.setPlayheadPosition(time / totalTime);

        let newFrameIndex = -1;
        const exactKeyframe = keyframeTimes.find((kt) => Math.abs(kt.time - time) < 0.0001);
        if (exactKeyframe) {
            newFrameIndex = exactKeyframe.index;
        } else {
            const segment = getAnimationSegment(time / totalTime, keyframeTimes, totalTime, this.currentVariants);
            if (segment) {
                newFrameIndex = keyframeTimes[segment.endIndex].index;
            } else if (time >= totalTime && keyframeTimes.length > 0) {
                newFrameIndex = keyframeTimes[0].index;
            }
        }

        if (newFrameIndex !== -1 && newFrameIndex !== this.currentFrameIndex) {
          this.currentFrameIndex = newFrameIndex;
          this.controlPanel.setVariant(this.currentVariants[this.currentFrameIndex]);
          this.controlPanel.setSelectedFrame(this.currentFrameIndex);
        }
      }
    });

    this.playbackController.on("keyframe-changed", (index: number) => {
      this.currentFrameIndex = index;
      this.controlPanel.setVariant(this.currentVariants[this.currentFrameIndex]);
      this.controlPanel.setSelectedFrame(this.currentFrameIndex);
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
      const existingAnimation = this.currentVariants[this.currentFrameIndex].animation || {};
      const newAnimationData = {
        ...existingAnimation,
        spec: {
          initial_delay: { secs: Math.floor(settings.initialDelay), nanos: (settings.initialDelay % 1) * 1e9 },
          animation: {
            Smooth: {
              duration: {
                secs: Math.floor(settings.duration),
                nanos: (settings.duration % 1) * 1e9,
              },
              repeat_type: "NoRepeat",
              easing: settings.easing,
            },
          },
          interrupt_type: settings.interruptType,
        },
      };

      this.currentVariants[this.currentFrameIndex].animation = newAnimationData;

      parent.postMessage({
          pluginMessage: {
            type: "save-data",
            frameName: this.currentVariants[this.currentFrameIndex].name,
            data: JSON.stringify(newAnimationData),
          },
      }, "*");

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

      this.controlPanel.setVariant(this.currentVariants[this.currentFrameIndex]); // Updates initial properties
  }

  private handleDiscard() {
      this.controlPanel.setVariant(this.currentVariants[this.currentFrameIndex]);
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
      const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(dataToExport, null, 2));
      const downloadAnchorNode = document.createElement("a");
      downloadAnchorNode.setAttribute("href", dataStr);
      downloadAnchorNode.setAttribute("download", "animation_data.json");
      document.body.appendChild(downloadAnchorNode);
      downloadAnchorNode.click();
      downloadAnchorNode.remove();
  }

  private updateFigmaPreview() {
        const animatedNodes: SerializedNode[] = [];
        const variantNode = this.currentSerializedVariants[this.currentFrameIndex];
        DataMapper.collectNodesForPreview(variantNode, null, true, animatedNodes);
        parent.postMessage({
            pluginMessage: {
              type: "update-figma-preview",
              animatedNodes: animatedNodes,
            },
        }, "*");
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
          const { variants, serializedVariants, selectedVariantName } = pluginMessage;
          this.currentSerializedVariants = serializedVariants;
          this.currentVariants = variants;
          this.propertiesPanel.setCurrentVariants(variants);

          const animationData = DataMapper.transformDataToAnimationData(variants, serializedVariants);
          this.timelineManager.editor.setData(animationData);
          this.playbackController.updateData({
            animationData: animationData,
            serializedVariants: this.currentSerializedVariants,
            variants: this.currentVariants,
          });

          let startIndex = 0;
          if (selectedVariantName) {
            const index = variants.findIndex((v: Variant) => v.name === selectedVariantName);
            if (index !== -1) startIndex = index;
          }
          this.currentFrameIndex = startIndex;

          this.controlPanel.updateKeyframeSelector(variants, startIndex);
          this.controlPanel.setVariant(variants[startIndex]);

          const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(this.currentVariants);
          const selectedKeyframe = keyframeTimes.find((kf) => kf.index === startIndex);
          if (selectedKeyframe && totalTime > 0) {
            this.playbackController.seek(selectedKeyframe.time);
          }

          parent.postMessage({ pluginMessage: { type: "prepare-animation" } }, "*");
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
            this.playbackController
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
