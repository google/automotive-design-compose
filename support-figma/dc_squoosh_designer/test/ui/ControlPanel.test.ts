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

import { ControlPanel } from "../../src/ui/ControlPanel";
import { PlaybackController } from "../../src/timeline/PlaybackController";

// Mock PlaybackController
jest.mock("../../src/timeline/PlaybackController");

describe("ControlPanel", () => {
  let controlPanel: ControlPanel;
  let playbackController: PlaybackController;
  let mockElements: { [key: string]: HTMLElement };

  beforeEach(() => {
    // Setup DOM mocks
    mockElements = {
      "play-button": document.createElement("button"),
      "save-button": document.createElement("button"),
      "discard-button": document.createElement("button"),
      "ping-button": document.createElement("button"),
      "clear-preview-button": document.createElement("button"),
      "export-button": document.createElement("button"),
      "import-button": document.createElement("button"),
      "import-file-input": document.createElement("input"),
      "reset-button": document.createElement("button"),
      "select-preview-frame-button": document.createElement("button"),
      "keyframe-select": document.createElement("select"),
      "from-variant": document.createElement("select"),
      "animation-name": document.createElement("input"),
      "initial-delay": document.createElement("input"),
      duration: document.createElement("input"),
      easing: document.createElement("select"),
      "interrupt-type": document.createElement("select"),
      "continue-checkbox": document.createElement("input"),
      "variant-name-display": document.createElement("div"),
      "throttle-updates-checkbox": document.createElement("input"),
      "transition-select": document.createElement("select"),
      "delete-transition-button": document.createElement("button"),
    };

    // Setup specific checkbox
    (mockElements["throttle-updates-checkbox"] as HTMLInputElement).type =
      "checkbox";

    // Populate select options for jsdom to handle value property
    const optStar = document.createElement("option");
    optStar.value = "*";
    optStar.textContent = "*";
    mockElements["from-variant"].appendChild(optStar);

    ["Linear", "EaseIn", "EaseOut"].forEach((val) => {
      const opt = document.createElement("option");
      opt.value = val;
      mockElements["easing"].appendChild(opt);
    });
    ["None", "Immediate", "Finish"].forEach((val) => {
      const opt = document.createElement("option");
      opt.value = val;
      mockElements["interrupt-type"].appendChild(opt);
    });

    jest.spyOn(document, "getElementById").mockImplementation((id: string) => {
      return mockElements[id] || null;
    });

    // Manually mock methods needed in constructor
    const MockPlaybackController = PlaybackController as jest.MockedClass<
      typeof PlaybackController
    >;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    playbackController = new MockPlaybackController({} as any);

    // Ensure 'on' method is mockable
    playbackController.on = jest.fn();

    controlPanel = new ControlPanel(playbackController);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("should initialize properly", () => {
    expect(controlPanel).toBeDefined();
    expect(playbackController.on).toHaveBeenCalledWith(
      "play",
      expect.any(Function),
    );
    expect(playbackController.on).toHaveBeenCalledWith(
      "pause",
      expect.any(Function),
    );
  });

  it("should toggle play/pause on button click", () => {
    const playBtn = mockElements["play-button"] as HTMLButtonElement;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (playbackController as any).isPlaying = false;
    playBtn.click();
    expect(playbackController.play).toHaveBeenCalled();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (playbackController as any).isPlaying = true;
    playBtn.click();
    expect(playbackController.pause).toHaveBeenCalled();
  });

  it("should emit save event with settings", () => {
    const saveBtn = mockElements["save-button"] as HTMLButtonElement;
    const saveSpy = jest.fn();
    controlPanel.on("save", saveSpy);

    // Set inputs
    (mockElements["initial-delay"] as HTMLInputElement).value = "1.5";
    (mockElements["duration"] as HTMLInputElement).value = "2.0";
    (mockElements["easing"] as HTMLSelectElement).value = "EaseIn";
    (mockElements["interrupt-type"] as HTMLSelectElement).value = "Immediate";

    saveBtn.click();

    expect(saveSpy).toHaveBeenCalledWith({
      initialDelay: 1.5,
      duration: 2.0,
      easing: "EaseIn",
      interruptType: "Immediate",
      fromVariant: "*",
      animationName: "",
    });
  });

  it("should update inputs when setVariant is called", () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const variant: any = {
      name: "Test Variant",
      animation: {
        spec: {
          initial_delay: { secs: 1, nanos: 500000000 }, // 1.5s
          animation: {
            Smooth: {
              duration: { secs: 0, nanos: 500000000 }, // 0.5s
              easing: "EaseOut",
            },
          },
          interrupt_type: "Finish",
        },
      },
    };

    controlPanel.setVariant(variant);

    expect((mockElements["initial-delay"] as HTMLInputElement).value).toBe(
      "1.50",
    );
    expect((mockElements["duration"] as HTMLInputElement).value).toBe("0.50");
    expect((mockElements["easing"] as HTMLSelectElement).value).toBe("EaseOut");
    expect((mockElements["interrupt-type"] as HTMLSelectElement).value).toBe(
      "Finish",
    );
    expect(mockElements["variant-name-display"].textContent).toBe(
      "Test Variant",
    );
  });

  it("should disable save button initially when variant set", () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const variant: any = {
      name: "Test Variant",
      animation: { spec: {} },
    };
    controlPanel.setVariant(variant);
    expect((mockElements["save-button"] as HTMLButtonElement).disabled).toBe(
      true,
    );
  });

  it("should enable save button when properties change", () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const variant: any = {
      name: "Test Variant",
      animation: {
        spec: {
          initial_delay: { secs: 0, nanos: 0 },
          animation: {
            Smooth: {
              duration: { secs: 0, nanos: 300000000 },
              easing: "Linear",
            },
          },
        },
      },
    };
    controlPanel.setVariant(variant);

    // Change duration
    (mockElements["duration"] as HTMLInputElement).value = "0.5";
    // Trigger input event
    (mockElements["duration"] as HTMLInputElement).dispatchEvent(
      new Event("input"),
    );

    expect((mockElements["save-button"] as HTMLButtonElement).disabled).toBe(
      false,
    );
  });

  it("should parse and display matrix transition specifications when setVariant is called", () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const variant: any = {
      name: "VariantB",
      animation: {
        transitions: [
          {
            from: "VariantA",
            to: "VariantB",
            name: "CustomPop",
            spec: {
              initial_delay: { secs: 0, nanos: 200000000 },
              animation: {
                Smooth: {
                  duration: { secs: 0, nanos: 800000000 },
                  easing: "EaseIn",
                },
              },
              interrupt_type: "Immediate",
            },
          },
        ],
      },
    };

    // Populate option for fromVariant dropdown so value assignment works
    const opt = document.createElement("option");
    opt.value = "VariantA";
    opt.textContent = "VariantA";
    mockElements["from-variant"].appendChild(opt);

    controlPanel.setVariant(variant, [
      { name: "VariantA", animation: null },
      { name: "VariantB", animation: null },
    ]);

    expect((mockElements["from-variant"] as HTMLSelectElement).value).toBe(
      "VariantA",
    );
    expect((mockElements["animation-name"] as HTMLInputElement).value).toBe(
      "CustomPop",
    );
    expect((mockElements["initial-delay"] as HTMLInputElement).value).toBe(
      "0.20",
    );
    expect((mockElements["duration"] as HTMLInputElement).value).toBe("0.80");
    expect((mockElements["easing"] as HTMLSelectElement).value).toBe("EaseIn");
    expect((mockElements["interrupt-type"] as HTMLSelectElement).value).toBe(
      "Immediate",
    );
  });

  it("should correctly handle boundary input values when getCurrentSettings is called", () => {
    (mockElements["from-variant"] as HTMLSelectElement).value = "*";
    (mockElements["animation-name"] as HTMLInputElement).value = "Default";
    (mockElements["initial-delay"] as HTMLInputElement).value = "0.00";
    (mockElements["duration"] as HTMLInputElement).value = "0.05";
    (mockElements["easing"] as HTMLSelectElement).value = "Linear";
    (mockElements["interrupt-type"] as HTMLSelectElement).value = "None";

    const settings = controlPanel.getCurrentSettings();

    expect(settings).toEqual({
      fromVariant: "*",
      animationName: "Default",
      initialDelay: 0,
      duration: 0.05,
      easing: "Linear",
      interruptType: "None",
    });
  });

  it("should parse wildcard destination transitions (* -> *) in matrix when setVariant is called", () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const variant: any = {
      name: "VariantB",
      animation: {
        transitions: [
          {
            from: "*",
            to: "*",
            name: "WildcardAnim",
            spec: {
              initial_delay: { secs: 0, nanos: 0 },
              animation: {
                Smooth: {
                  duration: { secs: 1, nanos: 0 },
                  easing: "Linear",
                },
              },
              interrupt_type: "None",
            },
          },
        ],
      },
    };

    controlPanel.setVariant(variant, [
      { name: "VariantA", animation: null },
      { name: "VariantB", animation: null },
    ]);

    expect((mockElements["from-variant"] as HTMLSelectElement).value).toBe("*");
    expect((mockElements["animation-name"] as HTMLInputElement).value).toBe(
      "WildcardAnim",
    );
    expect((mockElements["duration"] as HTMLInputElement).value).toBe("1.00");
  });

  it("should populate transition-select and support selecting a transition by index", () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const variant: any = {
      name: "VariantB",
      animation: {
        transitions: [
          {
            from: "*",
            to: "VariantB",
            name: "Default",
            spec: {
              initial_delay: { secs: 0, nanos: 0 },
              animation: {
                Smooth: {
                  duration: { secs: 0, nanos: 500000000 },
                  easing: "Linear",
                },
              },
              interrupt_type: "None",
            },
          },
          {
            from: "VariantA",
            to: "VariantB",
            name: "AlertPop",
            spec: {
              initial_delay: { secs: 1, nanos: 0 },
              animation: {
                Smooth: { duration: { secs: 2, nanos: 0 }, easing: "EaseOut" },
              },
              interrupt_type: "None",
            },
          },
        ],
      },
    };

    controlPanel.setVariant(
      variant,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      [{ name: "VariantA" }, { name: "VariantB" }] as any,
      1,
    );
    expect((mockElements["transition-select"] as HTMLSelectElement).value).toBe(
      "1",
    );
    expect((mockElements["from-variant"] as HTMLSelectElement).value).toBe(
      "VariantA",
    );
    expect((mockElements["animation-name"] as HTMLInputElement).value).toBe(
      "AlertPop",
    );
    expect((mockElements["duration"] as HTMLInputElement).value).toBe("2.00");
  });

  it("should toggle play/pause when Space key is pressed on window", () => {
    const event = new KeyboardEvent("keydown", { code: "Space", key: " " });
    window.dispatchEvent(event);
    expect(playbackController.play).toHaveBeenCalled();
  });

  it("should trigger file input click when import button is clicked", () => {
    const clickSpy = jest.spyOn(mockElements["import-file-input"], "click");
    mockElements["import-button"].click();
    expect(clickSpy).toHaveBeenCalled();
  });
});
