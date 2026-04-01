export async function resolveComponentContext(node: SceneNode) {
  let componentSet: ComponentSetNode | null = null;
  let singleComponent: ComponentNode | null = null;

  if (node.type === "INSTANCE") {
    const mainComponent = await node.getMainComponentAsync();
    if (mainComponent?.parent?.type === "COMPONENT_SET") {
      componentSet = mainComponent.parent;
    } else if (mainComponent) {
      singleComponent = mainComponent;
    }
  } else if (node.type === "COMPONENT_SET") {
    componentSet = node;
  } else if (node.type === "COMPONENT") {
    if (node.parent?.type === "COMPONENT_SET") {
      componentSet = node.parent;
    } else {
      singleComponent = node;
    }
  }
  return { componentSet, singleComponent };
}

export function isDescendantOf(node: SceneNode, ancestorId: string): boolean {
  let current: BaseNode | null = node;
  while (current) {
    if (current.id === ancestorId) {
      return true;
    }
    current = current.parent;
  }
  return false;
}
