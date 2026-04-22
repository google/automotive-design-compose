import { EventEmitter } from "./EventEmitter";

/**
 * Renders the timeline header (ruler) and handles playhead scrubbing interactions.
 * Displays time markers and allows the user to click or drag to set the playhead position.
 */
export class TimelineHeaderView {
  private container: HTMLElement;
  private eventEmitter: EventEmitter;
  private duration: number = 1.0; // Default duration
  private mouseDownHandler: (event: MouseEvent) => void;
  private mouseMoveHandler: (event: MouseEvent) => void;
  private mouseUpHandler: (event: MouseEvent) => void;
  private zoom: number = 1;
  private scrollLeft: number = 0;
  private isPanning: boolean = false;
  private lastMouseX: number = 0;
  private startMouseX: number = 0;
  private didDrag: boolean = false;
  private scrollbarWidth: number = 0;

  constructor(
    container: HTMLElement,
    eventEmitter: EventEmitter,
    duration?: number,
  ) {
    this.container = container;
    this.eventEmitter = eventEmitter;
    if (duration) {
      this.duration = duration;
    }
    this.mouseDownHandler = this.handleMouseDown.bind(this);
    this.mouseMoveHandler = this.handleMouseMove.bind(this);
    this.mouseUpHandler = this.handleMouseUp.bind(this);
    this.render();
    this.container.addEventListener("mousedown", this.mouseDownHandler);
  }

  private handleMouseDown(event: MouseEvent): void {
    this.isPanning = true;
    this.lastMouseX = event.clientX;
    this.startMouseX = event.clientX;
    this.didDrag = false;
    document.addEventListener("mousemove", this.mouseMoveHandler);
    document.addEventListener("mouseup", this.mouseUpHandler);
  }

  private handleMouseMove(event: MouseEvent): void {
    if (!this.isPanning) return;

    if (!this.didDrag && Math.abs(event.clientX - this.startMouseX) > 5) {
      this.didDrag = true;
    }

    if (this.didDrag) {
      const deltaX = event.clientX - this.lastMouseX;
      this.lastMouseX = event.clientX;
      this.eventEmitter.emit("scroll:pan", -deltaX / this.zoom);
    }
  }

  private handleMouseUp(event: MouseEvent): void {
    this.isPanning = false;
    document.removeEventListener("mousemove", this.mouseMoveHandler);
    document.removeEventListener("mouseup", this.mouseUpHandler);

    if (!this.didDrag) {
      const rect = this.container.getBoundingClientRect();
      const effectiveWidth = rect.width - this.scrollbarWidth;
      const position =
        (event.clientX - rect.left + this.scrollLeft) /
        (effectiveWidth * this.zoom);
      const clampedPosition = Math.max(0, Math.min(1, position));
      this.eventEmitter.emit("playhead:set", clampedPosition);
    }
  }

  private render(): void {
    this.container.innerHTML = `
      <div class="timeline-header">
        <div class="ruler">
          <div class="ruler-track"></div>
        </div>
      </div>
    `;
    this.drawRuler();
  }

  private drawRuler(): void {
    const rulerTrack = this.container.querySelector(
      ".ruler-track",
    ) as HTMLElement;
    if (!rulerTrack) return;

    rulerTrack.innerHTML = "";
    rulerTrack.style.width = `${100 * this.zoom}%`;

    const effectiveWidth = this.container.offsetWidth - this.scrollbarWidth;
    const rulerWidth = effectiveWidth * this.zoom;
    const desiredMarkerInterval = 100; // approx pixels

    const numMarkers = Math.max(
      1,
      Math.round(rulerWidth / desiredMarkerInterval),
    );

    for (let i = 0; i <= numMarkers; i++) {
      const marker = document.createElement("div");
      marker.className = "marker";

      const positionPercent = (i / numMarkers) * 100;
      marker.style.left = `${positionPercent}%`;

      const label = document.createElement("span");
      label.className = "label";
      const timeValue = i / numMarkers;
      label.textContent = timeValue.toFixed(2);
      marker.appendChild(label);

      const label2 = document.createElement("span");
      label2.className = "label2";
      const timeValue2 = (i / numMarkers) * this.duration;
      label2.textContent = timeValue2.toFixed(2) + "s";
      marker.appendChild(label2);

      rulerTrack.appendChild(marker);
    }
  }

  /**
   * Updates the total duration and redraws the ruler.
   * @param duration The total duration in seconds.
   */
  public setData(duration: number): void {
    this.duration = duration;
    this.drawRuler();
  }

  /**
   * Adjusts the ruler to account for the scrollbar width.
   * This ensures alignment with the grid view below.
   * @param width The width of the scrollbar in pixels.
   */
  public setScrollbarWidth(width: number): void {
    this.scrollbarWidth = width;
    const ruler = this.container.querySelector(".ruler") as HTMLElement;
    if (ruler) {
      ruler.style.paddingRight = `${width}px`;
    }
    this.drawRuler();
  }

  /**
   * Updates the horizontal scroll position of the ruler.
   * @param scrollLeft The scroll position in pixels.
   */
  public setScrollLeft(scrollLeft: number): void {
    this.scrollLeft = scrollLeft;
    const rulerTrack = this.container.querySelector(
      ".ruler-track",
    ) as HTMLElement;
    if (rulerTrack) {
      rulerTrack.style.transform = `translateX(-${scrollLeft}px)`;
    }
  }

  /**
   * Updates the zoom level and redraws the ruler.
   * @param zoom The new zoom level.
   */
  public setZoom(zoom: number): void {
    this.zoom = zoom;
    this.drawRuler();
  }

  /**
   * Updates the duration and redraws the ruler.
   * @param duration The new duration.
   */
  public setDuration(duration: number): void {
    this.duration = duration;
    this.drawRuler();
  }

  /**
   * Cleans up event listeners and DOM elements.
   */
  public destroy(): void {
    this.container.removeEventListener("mousedown", this.mouseDownHandler);
    document.removeEventListener("mousemove", this.mouseMoveHandler);
    document.removeEventListener("mouseup", this.mouseUpHandler);
    this.container.innerHTML = "";
  }
}
