import { AnimatedNode, AnimatedNodeProps, Paint } from "../timeline/types";
import { EPSILON, hexToRgb } from "../utils/common";

type FigmaPaint = Omit<SolidPaint, "type"> | Omit<GradientPaint, "type"> | Omit<ImagePaint, "type"> | Omit<VideoPaint, "type">;

export async function cloneChildren(
  original: readonly SceneNode[],
  parent: FrameNode | ComponentNode | InstanceNode,
) {
  for (const child of original) {
    const newChild = child.clone();
    tagOriginalNodeId(newChild, child);
    if ("appendChild" in parent) {
      parent.appendChild(newChild);
    }
  }
}

export function tagOriginalNodeId(previewNode: SceneNode, originalNode: SceneNode) {
  if ("setPluginData" in previewNode) {
    previewNode.setPluginData("originalNodeId", originalNode.id);
    console.log(
      `Setting originalNodeId for previewNode: ${previewNode.name} (ID: ${previewNode.id}) to originalNode.id: ${originalNode.id}`,
    );
  }
  if ("children" in previewNode && "children" in originalNode) {
    const previewChildren = previewNode.children;
    const originalChildren = originalNode.children;
    // Assuming clone() preserves order and count, which it should.
    if (previewChildren.length === originalChildren.length) {
      for (let i = 0; i < previewChildren.length; i++) {
        tagOriginalNodeId(previewChildren[i], originalChildren[i]);
      }
    }
  }
}

/**
 * Updates the given instance with the animated node properties.
 * Returns a promise that resolves when complete.
 */
export async function updateFigmaPreview(
  nodeId: string,
  animatedNodes: AnimatedNode[],
): Promise<void> {
  const instance = await figma.getNodeByIdAsync(nodeId);
  if (
    !instance ||
    (instance.type !== "FRAME" &&
      instance.type !== "COMPONENT" &&
      instance.type !== "INSTANCE")
  )
    return;

  const childrenMap = new Map<string, SceneNode>();
  const findChildren = (node: SceneNode) => {
    if ("children" in node) {
      for (const child of node.children) {
        childrenMap.set(child.name, child);
        findChildren(child);
      }
    }
  };
  findChildren(instance);

  animatedNodes.forEach((animatedNode) => {
    const node =
      animatedNode.nodeName === "__ROOT__"
        ? instance
        : childrenMap.get(animatedNode.nodeName);

    if (node) {
      if (animatedNode.originalNodeId && "setPluginData" in node) {
        node.setPluginData("originalNodeId", animatedNode.originalNodeId);
      }
      const props = animatedNode.props;
      try {
        if (
          animatedNode.nodeName !== "__ROOT__" &&
          "relativeTransform" in node &&
          (props.x !== undefined || props.y !== undefined || props.rotation !== undefined)
        ) {
          const transform = node.relativeTransform;
          
          let angleInRadians = Math.atan2(transform[1][0], transform[0][0]);
          if (props.rotation !== undefined) {
             angleInRadians = -props.rotation * Math.PI / 180;
          }
          const cos = Math.cos(angleInRadians);
          const sin = Math.sin(angleInRadians);

          const newTx = props.x !== undefined ? props.x : transform[0][2];
          const newTy = props.y !== undefined ? props.y : transform[1][2];

          const newTransform: [
            [number, number, number],
            [number, number, number],
          ] = [
              [cos, -sin, newTx],
              [sin,  cos, newTy],
            ];
          node.relativeTransform = newTransform;
        }
        if (
          props.width !== undefined &&
          props.height !== undefined &&
          "resize" in node
        ) {
          node.resize(props.width, props.height);
        }
        if (props.opacity !== undefined && "opacity" in node) {
          node.opacity = props.opacity;
        }
        if ("visible" in node) {
          if (props.visible !== undefined) {
            node.visible = props.visible;
          } else if (props.opacity !== undefined) {
            node.visible = props.opacity > 0;
          }
        }
        if (props.cornerRadius !== undefined) {
          if (Array.isArray(props.cornerRadius)) {
            if ("topLeftRadius" in node)
              node.topLeftRadius = props.cornerRadius[0];
            if ("topRightRadius" in node)
              node.topRightRadius = props.cornerRadius[1];
            if ("bottomRightRadius" in node)
              node.bottomRightRadius = props.cornerRadius[2];
            if ("bottomLeftRadius" in node)
              node.bottomLeftRadius = props.cornerRadius[3];
          } else {
            if ("topLeftRadius" in node)
              node.topLeftRadius = props.cornerRadius;
            if ("topRightRadius" in node)
              node.topRightRadius = props.cornerRadius;
            if ("bottomLeftRadius" in node)
              node.bottomLeftRadius = props.cornerRadius;
            if ("bottomRightRadius" in node)
              node.bottomRightRadius = props.cornerRadius;
          }
        }
        if (props.topLeftRadius !== undefined && "topLeftRadius" in node) {
          node.topLeftRadius = props.topLeftRadius;
        }
        if (props.topRightRadius !== undefined && "topRightRadius" in node) {
          node.topRightRadius = props.topRightRadius;
        }
        if (
          props.bottomLeftRadius !== undefined &&
          "bottomLeftRadius" in node
        ) {
          node.bottomLeftRadius = props.bottomLeftRadius;
        }
        if (
          props.bottomRightRadius !== undefined &&
          "bottomRightRadius" in node
        ) {
          node.bottomRightRadius = props.bottomRightRadius;
        }

        if ("fills" in node && node.fills && (node.fills as readonly Paint[]).length > 0) {
          const newFills: Paint[] = JSON.parse(JSON.stringify(node.fills));
          let fillsModified = false;
          for (const key in props) {
            if (key.startsWith("fills.")) {
              const parts = key.split(".");
              const index = parseInt(parts[1], 10);
              if (index < newFills.length) {
                const fillProp = parts.slice(2).join(".");
                if (fillProp === "solid") {
                  const color = hexToRgb(props[key as keyof AnimatedNodeProps] as string);
                  if (color) {
                    newFills[index].color = {
                      r: color.r / 255,
                      g: color.g / 255,
                      b: color.b / 255,
                    };
                    fillsModified = true;
                  }
                } else if (fillProp === "solid.opacity") {
                  newFills[index].opacity = props[key as keyof AnimatedNodeProps] as number;
                  fillsModified = true;
                } else if (fillProp === "gradient.stops") {
                  try {
                    const stops =
                      typeof props[key as keyof AnimatedNodeProps] === "string"
                        ? JSON.parse(props[key as keyof AnimatedNodeProps] as string)
                        : props[key as keyof AnimatedNodeProps];
                    if (Array.isArray(stops)) {
                      newFills[index].gradientStops = stops.map(
                        (stop: { color: string; position: number }) => {
                          const color = hexToRgb(stop.color);
                          return {
                            position:
                              typeof stop.position === "number"
                                ? stop.position
                                : 0,
                            color: color
                              ? {
                                r: color.r / 255,
                                g: color.g / 255,
                                b: color.b / 255,
                                a: 1,
                              }
                              : { r: 0, g: 0, b: 0, a: 1 }, // Fallback to black
                          };
                        },
                      );
                      fillsModified = true;
                    }
                  } catch (e) {
                    console.error("Error parsing gradient stops from preview", e);
                  }
                } else if (fillProp === "gradient.positions") {
                  try {
                    newFills[index].gradientHandlePositions = JSON.parse(
                      props[key as keyof AnimatedNodeProps] as string,
                    );
                    fillsModified = true;
                  } catch (e) {
                    console.error(
                      "Error parsing gradient positions from preview",
                      e,
                    );
                  }
                }
              }
            }
          }
          if (fillsModified) {
            node.fills = newFills as unknown as readonly FigmaPaint[];
          }
        }

        if ("strokes" in node && node.strokes && (node.strokes as readonly Paint[]).length > 0) {
          const newStrokes: Paint[] = JSON.parse(JSON.stringify(node.strokes));
          let strokesModified = false;
          for (const key in props) {
            if (key.startsWith("strokes.")) {
              const parts = key.split(".");
              const index = parseInt(parts[1], 10);
              if (index < newStrokes.length) {
                const strokeProp = parts.slice(2).join(".");
                if (strokeProp === "solid") {
                  const color = hexToRgb(props[key as keyof AnimatedNodeProps] as string);
                  if (color) {
                    newStrokes[index].color = {
                      r: color.r / 255,
                      g: color.g / 255,
                      b: color.b / 255,
                    };
                    strokesModified = true;
                  }
                } else if (strokeProp === "opacity") {
                  newStrokes[index].opacity = props[key as keyof AnimatedNodeProps] as number;
                  strokesModified = true;
                }
              }
            }
          }
          if (strokesModified) {
            node.strokes = newStrokes as unknown as readonly FigmaPaint[];
          }
        }

        if (props.strokeWeight !== undefined && "strokeWeight" in node) {
          node.strokeWeight = props.strokeWeight;
        }
        if (props.arcData && node.type === "ELLIPSE" && "arcData" in node) {
          node.arcData = Object.assign({}, node.arcData, props.arcData);
        }
      } catch (e) {
        console.error("Error applying property:", e);
      }
    }
  });
}
