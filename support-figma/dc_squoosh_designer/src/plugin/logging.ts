export function _logNode(
  node: SceneNode,
  indent: string,
  logString: string,
): string {
  logString += `${indent} L ${node.name} (${node.type})\n`;
  if ("children" in node) {
    for (const child of node.children) {
      logString = _logNode(child, indent + "  ", logString);
    }
  }
  return logString;
}
