import { Node } from "./types";
import { EventEmitter } from "./EventEmitter";

/**
 * Renders and manages the tree view of animation nodes (layers).
 * Allows expanding/collapsing nodes, adding new property timelines, and selecting nodes.
 */
export class NodeTreeView {
  private container: HTMLElement;
  private nodes: Node[];
  private eventEmitter: EventEmitter;
  private selectedNodeId: string | null = null;
  private editingTimelineId: string | null = null;
  private clickHandler: (event: MouseEvent) => void;
  private blurHandler: (event: FocusEvent) => void;
  private keydownHandler: (event: KeyboardEvent) => void;

  constructor(
    container: HTMLElement,
    nodes: Node[],
    eventEmitter: EventEmitter,
  ) {
    this.container = container;
    this.nodes = nodes;
    this.eventEmitter = eventEmitter;
    this.clickHandler = this.handleClick.bind(this);
    this.blurHandler = this.handleBlur.bind(this);
    this.keydownHandler = this.handleKeydown.bind(this);
    this.render();
    this.addEventListeners();
  }

  private addEventListeners(): void {
    this.container.addEventListener("click", this.clickHandler);
    this.container.addEventListener("blur", this.blurHandler, true);
    this.container.addEventListener("keydown", this.keydownHandler, true);
  }

  private handleBlur(event: FocusEvent): void {
    const target = event.target as HTMLInputElement;
    if (target.tagName === "INPUT") {
      this.saveTimelineName(target);
    }
  }

  private handleKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLInputElement;
    if (event.key === "Enter" && target.tagName === "INPUT") {
      this.saveTimelineName(target);
    }
  }

  private saveTimelineName(input: HTMLInputElement): void {
    const timelineId = input.closest("li")?.dataset.timelineId;
    if (timelineId) {
      this.eventEmitter.emit("timeline:rename", timelineId, input.value);
      this.editingTimelineId = null;
      this.render();
    }
  }

  private handleClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const nodeElement = target.closest("li");
    if (!nodeElement) {
      return;
    }
    const nodeId = nodeElement.dataset.nodeId;

    if (target.classList.contains("delete-timeline")) {
        const timelineLi = target.closest("li.property") as HTMLElement;
        if (timelineLi && timelineLi.dataset.timelineId) {
            const timelineId = timelineLi.dataset.timelineId;
            this.eventEmitter.emit("timeline:delete", timelineId);
        }
        return;
    }

    if (target.classList.contains("add-timeline")) {
      if (nodeId) {
        const node = this.findNodeById(nodeId);
        if (node) {
          this.eventEmitter.emit("timeline:add", nodeId, "New Property");
        }
      }
      return;
    }
    if (target.classList.contains("node-name")) {
      const nodeToToggle = target.closest(".has-children");
      if (nodeToToggle) {
        nodeToToggle.classList.toggle("collapsed");
        if (nodeId) {
          this.eventEmitter.emit(
            "node:toggle",
            nodeId,
            nodeToToggle.classList.contains("collapsed"),
          );
        }
      }
    }

    if (nodeId && this.selectedNodeId !== nodeId) {
      if (this.selectedNodeId) {
        const prevSelected = this.container.querySelector(
          `[data-node-id="${this.selectedNodeId}"]`,
        );
        if (prevSelected) {
          prevSelected.classList.remove("selected");
        }
      }
      nodeElement.classList.add("selected");
      this.selectedNodeId = nodeId;
      this.eventEmitter.emit("node:select", this.findNodeById(nodeId));
    }
  }

  /**
   * Focuses an input field to rename a specific timeline property.
   * @param timelineId The ID of the timeline to edit.
   */
  public setEditingTimeline(timelineId: string): void {
    this.editingTimelineId = timelineId;
    this.render();
    const input = this.container.querySelector(
      `[data-timeline-id="${timelineId}"] input`,
    ) as HTMLInputElement;
    if (input) {
      input.focus();
      input.select();
    }
  }

  /**
   * Updates the node data and re-renders the tree.
   * @param nodes The new list of nodes.
   */
  public setData(nodes: Node[]): void {
    this.nodes = nodes;
    this.render();
  }

  /**
   * Cleans up event listeners and DOM elements.
   */
  public destroy(): void {
    this.container.removeEventListener("click", this.clickHandler);
    this.container.innerHTML = "";
  }

  private findNodeById(nodeId: string): Node | null {
    const find = (nodes: Node[]): Node | null => {
      for (const node of nodes) {
        if (node.id === nodeId) {
          return node;
        }
        const found = find(node.children);
        if (found) {
          return found;
        }
      }
      return null;
    };
    return find(this.nodes);
  }

  private render(): void {
    this.container.innerHTML = this.renderNodes(this.nodes);
  }

  private renderNodes(nodes: Node[]): string {
    return `
      <datalist id="property-options">
        <option value="x">
        <option value="y">
        <option value="rotation">
        <option value="opacity">
        <option value="width">
        <option value="height">
        <option value="visible">
        <option value="cornerRadius">
        <option value="topLeftRadius">
        <option value="topRightRadius">
        <option value="bottomLeftRadius">
        <option value="bottomRightRadius">
        <option value="strokeWeight">
        <option value="arcData">
        <option value="fills.0.solid">
        <option value="fills.0.solid.opacity">
        <option value="strokes.0.solid">
        <option value="strokes.0.solid.opacity">
      </datalist>
      <ul>
        ${nodes.map((node) => this.renderNode(node)).join("")}
      </ul>
    `;
  }

  private renderNode(node: Node): string {
    const hasChildren = node.children.length > 0;
    const hasTimelines = node.timelines.length > 0;
    return `
      <li class="${hasChildren || hasTimelines ? "has-children" : ""}" data-node-id="${node.id}">
        <span class="node-name">
          ${hasChildren || hasTimelines ? '<span class="toggle-arrow"></span>' : ""}
          ${node.name}
          <button class="add-timeline">+</button>
        </span>
        ${
          hasTimelines
            ? `
          <ul>
            ${node.timelines
              .map(
                (timeline) => `
              <li class="property" data-timeline-id="${timeline.id}">
                ${
                  this.editingTimelineId === timeline.id
                    ? `<input type="text" value="${timeline.property}" list="property-options" />`
                    : timeline.property
                }
                ${timeline.isCustom ? '<button class="delete-timeline">-</button>' : ''}
              </li>
            `,
              )
              .join("")}
          </ul>
        `
            : ""
        }
        ${hasChildren ? this.renderNodes(node.children) : ""}
      </li>
    `;
  }
}
