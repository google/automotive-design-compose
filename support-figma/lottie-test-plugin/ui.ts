
declare const fabric: any;

const canvas = new fabric.Canvas('canvas');
const canvasContainer = document.getElementById('canvas-container') as HTMLElement;
let previousNodes: { [path: string]: any } = {};

const ANIMATION_DURATION = 300; // ms
const ANIMATION_DELAY = 0; // ms

window.onmessage = async (event: MessageEvent) => {
    const msg = event.data.pluginMessage;
    if (msg.type === 'selection') {
        setTimeout(() => {
            const newNodes: { [path: string]: any } = {};
            canvas.clear();
            if (msg.nodes.length > 0) {
                const bounds = {
                    minX: Infinity,
                    minY: Infinity,
                    maxX: -Infinity,
                    maxY: -Infinity,
                };
                // The root node is the first in the array
                drawNode(msg.nodes[0], bounds, newNodes, true);
                zoomToFit(bounds);
            }
            previousNodes = newNodes;
        }, ANIMATION_DELAY);
    }
};

function drawNode(node: any, bounds: any, newNodes: { [path: string]: any }, isRoot: boolean) {
    if (!node) return;

    const { x, y, width, height, fills, relativeTransform } = node;
    const path = isRoot ? "root" : node.name;

    updateBounds(bounds, x, y, width, height);

    let fabricObject: any;

    if (fills && fills.length > 0) {
        const fill = fills[0];
        if (fill.type === 'SOLID') {
            fabricObject = new fabric.Rect({
                width,
                height,
                fill: `rgba(${fill.color.r * 255}, ${fill.color.g * 255}, ${fill.color.b * 255}, ${fill.opacity || 1})`,
                originX: 'left',
                originY: 'top',
            });
        }
    }

    if (fabricObject) {
        const newMatrix = relativeTransform;
        const newDecomposed = fabric.util.qrDecompose(newMatrix);

        if (previousNodes[path]) {
            const oldDecomposed = fabric.util.qrDecompose(previousNodes[path].relativeTransform);
            
            console.log(`Animating node: ${path}`);
            console.log('Previous properties:', oldDecomposed);
            console.log('New properties:', newDecomposed);

            fabricObject.set({
                left: oldDecomposed.translateX,
                top: oldDecomposed.translateY,
                angle: oldDecomposed.angle,
                scaleX: oldDecomposed.scaleX,
                scaleY: oldDecomposed.scaleY,
            });
            fabricObject.animate({
                left: newDecomposed.translateX,
                top: newDecomposed.translateY,
                angle: newDecomposed.angle,
                scaleX: newDecomposed.scaleX,
                scaleY: newDecomposed.scaleY,
            }, {
                onChange: () => canvas.requestRenderAll(),
                duration: ANIMATION_DURATION,
                easing: fabric.util.ease.easeInOutQuad
            });
        } else {
            console.log(`Creating node: ${path}`);
            console.log('Initial properties:', newDecomposed);

            fabricObject.set({
                left: newDecomposed.translateX,
                top: newDecomposed.translateY,
                angle: newDecomposed.angle,
                scaleX: newDecomposed.scaleX,
                scaleY: newDecomposed.scaleY,
            });
        }
        canvas.add(fabricObject);
    }

    newNodes[path] = node;

    if (node.children) {
        node.children.forEach((child: any) => {
            drawNode(child, bounds, newNodes, false);
        });
    }
}

function updateBounds(bounds: any, x: number, y: number, width: number, height: number) {
    if (x < bounds.minX) bounds.minX = x;
    if (y < bounds.minY) bounds.minY = y;
    if (x + width > bounds.maxX) bounds.maxX = x + width;
    if (y + height > bounds.maxY) bounds.maxY = y + height;
}

function zoomToFit(bounds: any) {
    const objectWidth = bounds.maxX - bounds.minX;
    const objectHeight = bounds.maxY - bounds.minY;

    const scaleX = canvasContainer.clientWidth / objectWidth;
    const scaleY = canvasContainer.clientHeight / objectHeight;
    const scale = Math.min(scaleX, scaleY) * 0.9;

    canvas.setZoom(scale);
    canvas.viewportTransform[4] = (canvasContainer.clientWidth - objectWidth * scale) / 2 - bounds.minX * scale;
    canvas.viewportTransform[5] = (canvasContainer.clientHeight - objectHeight * scale) / 2 - bounds.minY * scale;
    canvas.requestRenderAll();
}
