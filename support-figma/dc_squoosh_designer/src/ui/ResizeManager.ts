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

/**
 * Manages resizing of the timeline panel and the entire plugin window.
 * Handles drag events on resize handles and communicates size changes to the main thread.
 */
export class ResizeManager {
  private resizeHandle: HTMLElement;
  private timelineResizer: HTMLElement;
  private timelineContainer: HTMLElement;

  constructor(
    resizeHandle: HTMLElement,
    timelineResizer: HTMLElement,
    timelineContainer: HTMLElement,
  ) {
    this.resizeHandle = resizeHandle;
    this.timelineResizer = timelineResizer;
    this.timelineContainer = timelineContainer;
    this.setupEventListeners();
  }

  private setupEventListeners() {
    this.resizeHandle.addEventListener("pointerdown", (e: PointerEvent) => {
      let isResizing = true;
      const startX = e.clientX;
      const startY = e.clientY;
      const startWidth = window.innerWidth;
      const startHeight = window.innerHeight;

      const handlePointerMove = (e: PointerEvent) => {
        if (!isResizing) return;
        const newWidth = startWidth + (e.clientX - startX);
        const newHeight = startHeight + (e.clientY - startY);
        this.postResizeMessage(newWidth, newHeight);
      };

      const throttledPointerMove = this.throttle(handlePointerMove, 50);

      const handlePointerUp = () => {
        isResizing = false;
        this.postResizePersistMessage(window.innerWidth, window.innerHeight);
        window.removeEventListener("pointermove", throttledPointerMove);
        window.removeEventListener("pointerup", handlePointerUp);
      };

      window.addEventListener("pointermove", throttledPointerMove);
      window.addEventListener("pointerup", handlePointerUp);
    });

    this.timelineResizer.addEventListener("pointerdown", (e: PointerEvent) => {
      let isResizing = true;
      const startY = e.clientY;
      const startHeight = this.timelineContainer.offsetHeight;

      const handlePointerMove = (e: PointerEvent) => {
        if (!isResizing) return;
        const newHeight = startHeight + (e.clientY - startY);
        this.timelineContainer.style.height = `${newHeight}px`;
      };

      const handlePointerUp = () => {
        isResizing = false;
        window.removeEventListener("pointermove", handlePointerMove);
        window.removeEventListener("pointerup", handlePointerUp);
      };

      window.addEventListener("pointermove", handlePointerMove);
      window.addEventListener("pointerup", handlePointerUp);
    });
  }

  /**
   * Throttles a function execution to limit how often it runs.
   * @param callback The function to throttle.
   * @param delay The delay in milliseconds.
   * @returns A throttled version of the callback.
   */
  private throttle(callback: (...args: unknown[]) => void, delay: number) {
    let timeout: number | null = null;
    return (...args: unknown[]) => {
      if (timeout) return;
      timeout = window.setTimeout(() => {
        callback(...args);
        timeout = null;
      }, delay);
    };
  }

  /**
   * Sends a message to the main thread to resize the plugin window.
   * @param width The new width.
   * @param height The new height.
   */
  private postResizeMessage(width: number, height: number) {
    parent.postMessage(
      {
        pluginMessage: {
          type: "resize",
          width: Math.round(width),
          height: Math.round(height),
        },
      },
      "*",
    );
  }

  /**
   * Sends a message to the main thread to persist the window size.
   * @param width The width to save.
   * @param height The height to save.
   */
  private postResizePersistMessage(width: number, height: number) {
    parent.postMessage(
      {
        pluginMessage: {
          type: "persist-size",
          width: Math.round(width),
          height: Math.round(height),
        },
      },
      "*",
    );
  }
}
