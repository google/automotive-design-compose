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

import { Keyframe } from "../../timeline/types";
import { interpolateColor } from "../../utils/common";

const easingFunctions: { [key: string]: (t: number) => number } = {
  Linear: (t: number) => t,
  EaseIn: (t: number) => t * t,
  EaseOut: (t: number) => t * (2 - t),
  EaseInOut: (t: number) => (t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t),
  EaseInCubic: (t: number) => t * t * t,
  EaseOutCubic: (t: number) => --t * t * t + 1,
  EaseInOutCubic: (t: number) =>
    t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1,
  Instant: (_t: number) => 0,
};

export function applyEasing(progress: number, easing: string): number {
  const easeFn = easingFunctions[easing];
  return easeFn ? easeFn(progress) : easingFunctions.Linear(progress);
}

export class PropertyElementBuilder {
  static buildGradientEditor(container: HTMLElement, keyframe: Keyframe) {
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
        colorInput.disabled = keyframe.locked === true;

        const positionInput = document.createElement("input");
        positionInput.type = "number";
        positionInput.step = "0.01";
        positionInput.min = "0";
        positionInput.max = "1";
        positionInput.value = stop.position.toFixed(2);
        positionInput.disabled = keyframe.locked === true;

        const removeBtn = document.createElement("button");
        removeBtn.textContent = "-";
        removeBtn.disabled = keyframe.locked === true;

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

  static buildArcEditor(container: HTMLElement, keyframe: Keyframe) {
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
    startingAngleInput.disabled = keyframe.locked === true;

    const endingAngleLabel = document.createElement("label");
    endingAngleLabel.textContent = "Ending Angle";
    const endingAngleInput = document.createElement("input");
    endingAngleInput.type = "number";
    endingAngleInput.step = "0.1";
    endingAngleInput.value = arc.endingAngle.toFixed(2);
    endingAngleInput.disabled = keyframe.locked === true;

    const innerRadiusLabel = document.createElement("label");
    innerRadiusLabel.textContent = "Inner Radius";
    const innerRadiusInput = document.createElement("input");
    innerRadiusInput.type = "number";
    innerRadiusInput.step = "0.1";
    innerRadiusInput.value = arc.innerRadius.toFixed(2);
    innerRadiusInput.disabled = keyframe.locked === true;

    editor.appendChild(startingAngleLabel);
    editor.appendChild(startingAngleInput);
    editor.appendChild(endingAngleLabel);
    editor.appendChild(endingAngleInput);
    editor.appendChild(innerRadiusLabel);
    editor.appendChild(innerRadiusInput);

    container.appendChild(editor);
  }

  static buildDetailedGradientPreview(
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
          const easedProgress = applyEasing(linearProgress, easing);
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
}
