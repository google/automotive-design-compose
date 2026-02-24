import { EPSILON } from "../utils/common";

export async function compareNodes(
  node1: SceneNode,
  node2: SceneNode,
  path: string = "",
) {
  const currentPath = path ? `${path} -> ${node1.name}` : node1.name;

  const compatibleTypes =
    (node1.type === "FRAME" && node2.type === "COMPONENT") ||
    (node1.type === "COMPONENT" && node2.type === "FRAME");
  if (node1.type !== node2.type && !compatibleTypes) {
    console.log(
      `[${currentPath}] Type mismatch: ${node1.type} vs ${node2.type}`,
    );
    return;
  }

  const propsToCompare = [
    "x",
    "y",
    "rotation",
    "width",
    "height",
    "opacity",
    "visible",
  ];
  for (const prop of propsToCompare) {
    if (prop in node1 && prop in node2) {
      const val1: string | number | boolean | undefined =
        node1[prop as keyof typeof node1];
      const val2: string | number | boolean | undefined =
        node2[prop as keyof typeof node2];
      if (typeof val1 === "number" && typeof val2 === "number") {
        if (Math.abs(val1 - val2) > EPSILON) {
          console.log(
            `[${currentPath}] Property "${prop}" mismatch: ${val1.toFixed(
              2,
            )} (preview) vs ${val2.toFixed(2)} (variant)`,
          );
        }
      } else if (val1 !== val2) {
        console.log(
          `[${currentPath}] Property "${prop}" mismatch: ${val1} (preview) vs ${val2} (variant)`,
        );
      }
    }
  }

  if ("relativeTransform" in node1 && "relativeTransform" in node2) {
    const t1 = node1.relativeTransform;
    const t2 = node2.relativeTransform;
    const matrixProps = ["a", "b", "c", "d", "tx", "ty"];
    const flat1 = [
      t1[0][0],
      t1[1][0],
      t1[0][1],
      t1[1][1],
      t1[0][2],
      t1[1][2],
    ];
    const flat2 = [
      t2[0][0],
      t2[1][0],
      t2[0][1],
      t2[1][1],
      t2[0][2],
      t2[1][2],
    ];

    for (let i = 0; i < flat1.length; i++) {
      if (Math.abs(flat1[i] - flat2[i]) > EPSILON) {
        console.log(
          `[${currentPath}] relativeTransform mismatch on ${
            matrixProps[i]
          }: ${flat1[i].toFixed(2)} (preview) vs ${flat2[i].toFixed(
            2,
          )} (variant)`,
        );
      }
    }
  }

  if ("children" in node1 && "children" in node2) {
    const children1 = node1.children;
    const children2 = node2.children;

    if (children1.length !== children2.length) {
      console.log(
        `[${currentPath}] Children count mismatch: ${children1.length} vs ${children2.length}`,
      );
      return;
    }

    const children2Map = new Map(
      children2.map((child) => [child.name, child]),
    );

    for (const child1 of children1) {
      const child2 = children2Map.get(child1.name);
      if (child2) {
        await compareNodes(child1, child2, currentPath);
      } else {
        console.log(
          `[${currentPath}] Child "${child1.name}" not found in variant.`,
        );
      }
    }
  }
}
