import { SerializedNode, Node, Timeline } from "../timeline/types";
import { PlaybackController } from "../timeline/PlaybackController";
import { DataMapper } from "../services/DataMapper";
import { TimelineManager } from "../ui/TimelineManager";

export function findNodeById(root: SerializedNode, id: string): SerializedNode | null {
    if (root.id === id) return root;
    if (root.children) {
        for (const child of root.children) {
            const found = findNodeById(child, id);
            if (found) return found;
        }
    }
    return null;
}

export function findNodeByName(root: SerializedNode, name: string): SerializedNode | null {
    if (root.name === name) return root;
    if (root.children) {
        for (const child of root.children) {
            const found = findNodeByName(child, name);
            if (found) return found;
        }
    }
    return null;
}

export function findAnimationNodeByFigmaId(nodes: Node[], figmaId: string): Node | null {
    for (const node of nodes) {
        if (node.figmaId === figmaId) return node;
        const found = findAnimationNodeByFigmaId(node.children, figmaId);
        if (found) return found;
    }
    return null;
}

export function compareAndPrintChanges(
    originalNodeId: string | null,
    newProps: SerializedNode,
    currentSerializedVariants: SerializedNode[],
    currentFrameIndex: number,
    timelineManager: TimelineManager,
    playbackController: PlaybackController
) {
    // 1. Get base state from the CURRENT active variant context.
    const variantRoot = currentSerializedVariants[currentFrameIndex];
    if (!variantRoot) return;

    let originalNodeStruct: SerializedNode | null = null;

    if (originalNodeId) {
        originalNodeStruct = findNodeById(variantRoot, originalNodeId);
    }

    // Fallback: Find by name if ID lookup failed or ID was missing
    if (!originalNodeStruct) {
        originalNodeStruct = findNodeByName(variantRoot, newProps.name);
    }

    if (!originalNodeStruct) {
        return;
    }

    // Deep clone to create the "Expected Node"
    const expectedNode = JSON.parse(JSON.stringify(originalNodeStruct));

    // 2. Apply animations to Expected Node
    const animData = timelineManager.editor.getData();
    // ... rest of function remains same but we need to resolve originalNodeId for animation lookup if it was null
    // If originalNodeId was null, we need the ID of the found struct to look up animation data.
    const resolvedId = originalNodeStruct.id;

    if (animData) {
        const animNode = findAnimationNodeByFigmaId(animData.nodes, resolvedId);
        if (animNode) {
            const currentTime = playbackController.currentTime;
            const totalTime = animData.duration || 1; // Avoid div by zero if duration 0

            animNode.timelines.forEach((timeline: Timeline) => {
                const val = playbackController.getInterpolatedValue(timeline.id, currentTime);
                if (val !== undefined) {
                    // ...
                }
            });
        }
    }

    console.group(`Changes for node "${originalNodeStruct.name}"`);

    // Check all timelines first (animated properties)
    if (animData) {
        const animNode = findAnimationNodeByFigmaId(animData.nodes, resolvedId);
        if (animNode) {
            const currentTime = playbackController.currentTime;
            animNode.timelines.forEach((timeline: Timeline) => {
                const expectedVal = playbackController.getInterpolatedValue(timeline.id, currentTime);
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                const actualVal = DataMapper.getNodePropertyValue(newProps as any, timeline.property);

                // Simple equality check (imperfect for colors/objects but good start)
                if (JSON.stringify(expectedVal) !== JSON.stringify(actualVal)) {
                     // If it's color, we might have Hex vs RGB mismatch.
                     // TODO: normalize colors?
                }
            });
        }
    }

    // Check static properties (those without timelines)
    const staticKeys: (keyof SerializedNode)[] = [
        "x", "y", "width", "height", "opacity", "visible",
        "cornerRadius", "strokeWeight",
        "fills", "strokes",
        "topLeftRadius", "topRightRadius", "bottomLeftRadius", "bottomRightRadius",
        "arcData"
    ];

    staticKeys.forEach(key => {
        // Skip if this property is animated (has a timeline)
        if (animData) {
            const animNode = findAnimationNodeByFigmaId(animData.nodes, resolvedId);
            if (animNode && animNode.timelines.some((t: Timeline) => t.property === key)) return;
        }

        const v1 = originalNodeStruct![key];
        const v2 = newProps[key];

        // For floating point numbers, use epsilon
        if (typeof v1 === 'number' && typeof v2 === 'number') {
            if (Math.abs(v1 - v2) > 0.001) {
            }
        } else if (JSON.stringify(v1) !== JSON.stringify(v2)) {
        }
    });

    console.groupEnd();
}
