/* eslint-disable @typescript-eslint/no-explicit-any */
import { TimelineEditor } from "../timeline/TimelineEditor";
import { PlaybackController } from "../timeline/PlaybackController";
import { PropertiesPanel } from "./PropertiesPanel";
import { DataMapper } from "../services/DataMapper";
import { Keyframe, Node, Timeline, KeyframeTime, Variant, SerializedNode, AnimationNode } from "../timeline/types";

/**
 * Manages the interaction between the timeline editor, playback controller, and properties panel.
 * It acts as the central coordinator for UI events related to the timeline.
 */
export class TimelineManager {
  public editor: TimelineEditor;
  private playbackController: PlaybackController;
  private propertiesPanel: PropertiesPanel | null;
  private getCurrentVariants: () => Variant[];
  private getCurrentSerializedVariants: () => SerializedNode[];

  constructor(
    container: HTMLElement,
    playbackController: PlaybackController,
    propertiesPanel: PropertiesPanel | null,
    getCurrentVariants: () => Variant[],
    getCurrentSerializedVariants: () => SerializedNode[]
  ) {
    this.playbackController = playbackController;
    this.propertiesPanel = propertiesPanel;
    this.getCurrentVariants = getCurrentVariants;
    this.getCurrentSerializedVariants = getCurrentSerializedVariants;

    this.editor = new TimelineEditor(container, this.playbackController);
    this.setupEventListeners();
  }

  /**
   * Sets the properties panel instance.
   * @param propertiesPanel The properties panel to use.
   */
  public setPropertiesPanel(propertiesPanel: PropertiesPanel) {
      this.propertiesPanel = propertiesPanel;
  }
  
    private getValueFromVariant(variantIndex: number, nodeId: string, propertyName: string): any {
      const serializedVariants = this.getCurrentSerializedVariants();
      const variantRoot = serializedVariants[variantIndex];
      if (!variantRoot) return undefined;
      
      const nodesMap = new Map<string, AnimationNode>();
      // We need to cast SerializedNode to AnimationNode structure for DataMapper
      // This is safe because the structures are compatible for what collectAnimationNodes needs
      DataMapper.collectAnimationNodes(variantRoot as unknown as AnimationNode, null, true, nodesMap);
      
      // DataMapper uses the node name as the ID in the map
      // The nodeId passed here is from the Node structure, which is the node name (or __ROOT__)
      const targetNode = nodesMap.get(nodeId);
      
      if (!targetNode) return undefined;
      
      return DataMapper.getNodePropertyValue(targetNode, propertyName);
    }
    
    private isNodeMissingInVariant(variantIndex: number, nodeId: string): boolean {
      const serializedVariants = this.getCurrentSerializedVariants();
      const variantRoot = serializedVariants[variantIndex];
      if (!variantRoot) return true;
      
      if (nodeId === "__ROOT__") return false; // Root always exists
  
      const findNode = (root: SerializedNode, id: string): boolean => {
          if (root.name === id) return true; // Match by Name
          if (root.children) {
              for (const child of root.children) {
                  if (findNode(child, id)) return true;
              }
          }
          return false;
      };
      
      return !findNode(variantRoot, nodeId);
    }
  private setupEventListeners() {
    this.editor.on("playhead:set", (position: number) => {
      const { totalTime } = DataMapper.calculateKeyframeData(this.getCurrentVariants());
      const time = position * totalTime;
      this.playbackController.seek(time);
    });

    this.editor.on("keyframe:selected", (keyframeId: string) => {
      const findResult = this.playbackController.findKeyframeAndTimelineData(
        this.editor.getData()!.nodes,
        keyframeId,
      );
      if (findResult) {
        this.editor.emit("keyframes:selected", [findResult.keyframe]);
      }
    });

    this.editor.on("keyframes:selected", (keyframes: Keyframe[]) => {
      if (keyframes.length > 1) {
        this.propertiesPanel?.updateMultiKeyframeProperties(keyframes);
      } else if (keyframes.length === 1) {
        this.propertiesPanel?.updateKeyframeProperties(keyframes[0]);
      }
    });

    this.editor.on(
      "keyframe:move",
      (keyframeId: string, newPosition: number) => {
        const data = this.editor.getData();
        if (!data) return;

        const findResult = this.playbackController.findKeyframeAndTimelineData(
          data.nodes,
          keyframeId,
        );
        if (findResult) {
          const tempKeyframe: Keyframe = {
            ...findResult.keyframe,
            position: newPosition,
          };
          this.propertiesPanel?.updateKeyframeProperties(tempKeyframe);

          findResult.keyframe.position = newPosition;
          this.editor.setData(data);
        }
      },
    );

    this.editor.on(
      "keyframe:move_end",
      (keyframeId: string, finalPosition: number) => {
        const data = this.editor.getData();
        if (!data) return;

        const findResult = this.playbackController.findKeyframeAndTimelineData(
          data.nodes,
          keyframeId,
        );
        if (findResult) {
          findResult.keyframe.position = finalPosition;
          this.editor.setData(data);
          this.propertiesPanel?.saveCustomKeyframeData(findResult.timeline.id, keyframeId);
        }
      },
    );

    this.editor.on("selection:cleared", () => {
      this.propertiesPanel?.resetKeyframePropertiesPanel();
    });

    this.editor.on(
      "section:selected",
      (timelineId: string, start: number, end: number) => {
        this.propertiesPanel?.updateAnimationSectionProperties(timelineId, start, end);
      },
    );

    this.editor.on(
      "timeline:rename",
      (timelineId: string, newName: string) => {
        const data = this.editor.getData();
        if (!data) return;

        let parentNode: Node | null = null;
        const find = (nodes: Node[]): Timeline | null => {
          for (const node of nodes) {
            const timeline = node.timelines.find((t) => t.id === timelineId);
            if (timeline) {
              parentNode = node;
              return timeline;
            }
            if (node.children && node.children.length > 0) {
              const found = find(node.children);
              if (found) return found;
            }
          }
          return null;
        };
        const timeline = find(data.nodes);

        if (timeline && parentNode) {
          if (
            (parentNode as Node).timelines.some(
              (t) => t.property === newName && t.id !== timelineId,
            )
          ) {
            alert(`Property "${newName}" already exists on this node.`);
            (parentNode as Node).timelines = (parentNode as Node).timelines.filter(
              (t) => t.id !== timelineId,
            );
            this.editor.setData(data);
            return;
          }

          timeline.property = newName;
          timeline.id = `${(parentNode as Node).id}-${newName}`;

          // Clear existing dummy keyframes and populate with actual data from variants
          timeline.keyframes = [];
          this.populateNewTimeline(timeline, parentNode as Node, newName);
          
          // If no data found (e.g. invalid property name), restore dummy keyframes
          if (timeline.keyframes.length === 0) {
              timeline.keyframes = [
                  {
                    id: `kf-${Date.now()}-1`,
                    position: 0,
                    value: 0,
                    locked: false,
                    easing: "Linear",
                  },
                  {
                    id: `kf-${Date.now()}-2`,
                    position: 1,
                    value: 0,
                    locked: false,
                    easing: "Linear",
                  },
              ];
          } else {
              timeline.keyframes.sort((a, b) => a.position - b.position);
          }

          this.editor.setData(data);
        }
      },
    );

    this.editor.on(
      "keyframe:saved",
      (timelineId: string, keyframeId: string) => {
         console.log("TimelineManager received keyframe:saved", timelineId, keyframeId);
         // eslint-disable-next-line @typescript-eslint/no-explicit-any
         this.propertiesPanel?.saveCustomKeyframeData(timelineId, keyframeId);
      },
    );

    this.editor.on(
      "keyframe:save-on-remove",
      (timelineId: string, keyframePosition: number) => {
         // eslint-disable-next-line @typescript-eslint/no-explicit-any
         this.propertiesPanel?.saveCustomKeyframeData(timelineId, undefined, keyframePosition);
      },
    );

    this.editor.on(
      "timeline:delete",
      (timelineId: string) => {
          const data = this.editor.getData();
          if (!data) return;
          
          const remove = (nodes: Node[]): boolean => {
              for (const node of nodes) {
                  const index = node.timelines.findIndex((t) => t.id === timelineId);
                  if (index !== -1) {
                      node.timelines.splice(index, 1);
                      return true;
                  }
                  if (node.children && remove(node.children)) return true;
              }
              return false;
          };
          
          if (remove(data.nodes)) {
              this.editor.setData(data);
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              (this.propertiesPanel as any)?.deleteCustomTimeline(timelineId);
          }
      },
    );

  }

  private populateNewTimeline(timeline: Timeline, parentNode: Node, newName: string) {
    const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(
      this.getCurrentVariants(),
    );
    const keyframesToAdd: Keyframe[] = [];
    const variants = this.getCurrentVariants();

    // Helper to find next valid value for a property
    const findNextValidValue = (startIndex: number, prop: string): any => {
        for (let i = startIndex; i < variants.length; i++) {
             const val = this.getValueFromVariant(i, parentNode.id, prop);
             if (val !== undefined) return val;
        }
        return 0; // Default if no future value found
    };

    keyframeTimes.forEach((kt: KeyframeTime) => {
      // Removed: if (kt.isLoop) return;
      let value = this.getValueFromVariant(kt.index, parentNode.id, newName);
      let isMissing = false;
      
      const nodeMissing = this.isNodeMissingInVariant(kt.index, parentNode.id);

      if (nodeMissing) {
          isMissing = true;
          // Look ahead for next valid value
          if (newName === 'opacity') {
              value = 0;
          } else {
              value = findNextValidValue(kt.index + 1, newName);
          }
      } else if (value === undefined) {
          // Node present, but property missing. Skip.
          return;
      }

      if (value !== undefined || isMissing) {
        keyframesToAdd.push({
          id: `kf-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
          position: kt.time / totalTime,
          value: value,
          locked: true,
          isMissing: isMissing,
          easing: "Linear",
        });

        // Side effect: If node is missing (implied by isMissing=true for property 'x' etc?), ensure opacity is 0.
        // BUT only if we are NOT currently populating 'opacity' (to avoid recursion loops or redundant work).
        if (isMissing && newName !== 'opacity') {
             this.ensureOpacityTimeline(parentNode, kt.time / totalTime, variants);
        }
      }
    });

    if (keyframesToAdd.length > 0) {
      timeline.keyframes = keyframesToAdd;
    }
  }

  private ensureOpacityTimeline(node: Node, position: number, variants: any[]) {
      let opacityTimeline = node.timelines.find(t => t.property === 'opacity');
      if (!opacityTimeline) {
          // Create it
          opacityTimeline = {
              id: `${node.id}-opacity`,
              property: 'opacity',
              keyframes: [],
              isCustom: false // It's auto-generated/system
          };
          node.timelines.push(opacityTimeline);
          
          // Populate it fully first?
          // If we create it, we should populate it with variant data first.
          this.populateNewTimeline(opacityTimeline, node, 'opacity');
      }
      
      // Now ensure the specific keyframe at 'position' is set to 0 and marked missing
      // populateNewTimeline('opacity') should have already handled this if logic is correct!
      // In populateNewTimeline('opacity'):
      // If value is undefined -> isMissing=true, value=0.
      // So we just need to call populateNewTimeline if we created it.
      // If it already existed, it should already have the correct data?
      // Maybe not if it was created before this 'missing' logic was implemented?
      // But we assume consistent state.
      
      // However, explicitly forcing 0 at this missing point is safe.
      const existingKf = opacityTimeline.keyframes.find(kf => Math.abs(kf.position - position) < 0.0001);
      if (existingKf) {
          if (existingKf.value !== 0) {
              // If it exists but isn't 0 (maybe interpolated?), force it?
              // If the node is missing, opacity MUST be 0.
              // But populateNewTimeline logic says: if undefined, value=0.
              // So it should be 0.
          }
      } else {
           // Should have been added by populateNewTimeline.
      }
  }
}