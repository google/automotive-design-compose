
figma.showUI(__html__, { width: 400, height: 600 });

figma.on("selectionchange", () => {
    const selection = figma.currentPage.selection;
    let nodes: any[] = [];
    if (selection.length > 0) {
        // Treat the first selected node as the root of the keyframe
        const rootNode = selection[0];
        nodes.push(serializeNode(rootNode, "root", true));
    }
    figma.ui.postMessage({ type: 'selection', nodes: nodes });
})

function serializeNode(node: any, path: string, isRoot: boolean): any {
    if (!node) return null;

    let children: any[] = [];
    if ("children" in node) {
        node.children.forEach((child: any) => {
            // Children are identified by their name
            children.push(serializeNode(child, child.name, false));
        });
    }

    return {
        id: node.id,
        path: path,
        name: node.name,
        type: node.type,
        x: node.x,
        y: node.y,
        width: node.width,
        height: node.height,
        fills: node.fills,
        relativeTransform: node.relativeTransform,
        children: children,
    };
}
