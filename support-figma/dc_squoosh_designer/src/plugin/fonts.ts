export async function loadFontsForNode(node: SceneNode) {
  const fontsToLoad = new Set<FontName>();

  await findFonts(node, fontsToLoad);

  await Promise.all(Array.from(fontsToLoad).map(figma.loadFontAsync));
}

export async function findFonts(node: SceneNode, fontsToLoad: Set<FontName>) {
  if (node.type === "TEXT") {
    if (node.fontName !== figma.mixed) {
      fontsToLoad.add(node.fontName);
    } else {
      const len = node.characters.length;
      for (let i = 0; i < len; i++) {
        const font = node.getRangeFontName(i, i + 1) as FontName;
        fontsToLoad.add(font);
      }
    }
  }

  if ("children" in node) {
    for (const child of node.children) {
      await findFonts(child, fontsToLoad);
    }
  }
}
