/**
 * @file This is the main logic file for the Figma plugin.
 * It handles communication with the UI, listens for selection changes in Figma,
 * and processes component data to extract animation timelines and property changes.
 */

// Keep track of the window size. Initialize with the default size.
let currentWindowSize: { width: number, height: number } = { width: 400, height: 600 };

// State for tracking window resizing. Stores initial mouse position and window size.
let initialResizeState: {
  startX: number;
  startY: number;
  startWidth: number;
  startHeight: number;
} | null = null;

// Show the plugin UI using the stored size.
figma.showUI(__html__, currentWindowSize);

/**
 * Handles messages sent from the plugin's UI (ui.html).
 * This is the primary way the UI communicates back to the main plugin logic.
 * @param {object} msg - The message object sent from the UI.
 */
figma.ui.onmessage = async (msg: { type: string, [key: string]: any }) => {
  console.log('Received message from UI:', msg);
  try {
    const selection = figma.currentPage.selection;

    switch (msg.type) {
      // This message is sent from the UI when it's finished loading.
      case 'ui-ready':
        handleSelectionChange();
        break;

      // This message is sent when a user clicks a keyframe in the timeline.
      case 'get-variant-animation-data':
        const node = await figma.getNodeByIdAsync(msg.id);
        if (node) {
          // We update the selection to the clicked variant. This ensures that when the user
          // saves animation data, it's applied to the correct variant.
          figma.currentPage.selection = [node as SceneNode];
          loadAnimationData(node as SceneNode);
        }
        break;

      // Saves the animation data to the currently selected node.
      case 'save-animation':
        if (selection.length === 1) {
          selection[0].setSharedPluginData('designcompose', 'animations', JSON.stringify(msg.spec));
          figma.notify('Animation saved!');
          if (msg.close) figma.closePlugin();
        } else {
          figma.notify("Please select a single node to update.");
        }
        break;

      // Clears animation data from the currently selected node.
      case 'clear-animation':
        if (selection.length === 1) {
          selection[0].setSharedPluginData('designcompose', 'animations', '');
          figma.notify('Animation cleared!');
          if (msg.close) figma.closePlugin();
        } else {
          figma.notify("Please select a single node to update.");
        }
        break;
      
      // --- Resizing Logic ---
      // Stores the initial mouse position and the current known window size.
      case 'resize-start':
        initialResizeState = {
          startX: msg.screenX,
          startY: msg.screenY,
          startWidth: currentWindowSize.width,
          startHeight: currentWindowSize.height
        };
        break;
      
      // Calculates the new size based on the drag delta and resizes the window.
      case 'resize-drag':
        if (initialResizeState) {
          const deltaX = msg.screenX - initialResizeState.startX;
          const deltaY = msg.screenY - initialResizeState.startY;
          
          const newWidth = initialResizeState.startWidth + deltaX;
          const newHeight = initialResizeState.startHeight + deltaY;

          // Update the stored size and resize the UI.
          currentWindowSize = { width: newWidth, height: newHeight };
          figma.ui.resize(newWidth, newHeight);
        }
        break;
      
      // Clears the resize state when the drag ends.
      case 'resize-end':
        initialResizeState = null;
        break;

      // This message is sent from the UI when a property is edited in the table.
      case 'update-property':
        const { nodeKey, propName, variantId, newValue } = msg;
        
        // Find the variant node by its ID.
        const variant = await figma.getNodeByIdAsync(variantId);
        if (variant) {
          // Function to find a specific child node by its key (name or 'ROOT_NODE').
          const findNodeToUpdate = (node: BaseNode, key: string): BaseNode | null => {
            if (key === 'ROOT_NODE' && node.id === variantId) return node;
            if (node.name === key) return node;
            if ("children" in node) {
              for (const child of node.children) {
                const found = findNodeToUpdate(child, key);
                if (found) return found;
              }
            }
            return null;
          };

          const nodeToUpdate = findNodeToUpdate(variant, nodeKey) as SceneNode;

          if (nodeToUpdate) {
            let timelineNeedsUpdate = false;
            // --- Handle Timing Properties ---
            if (propName === 'initialDelay' || propName === 'duration') {
              let data = nodeToUpdate.getSharedPluginData('designcompose', 'animations');
              let spec = data ? JSON.parse(data) : {};

              // Ensure a default spec structure exists
              if (!spec.override) spec.override = 'Custom';
              if (!spec.spec) spec.spec = {};
              if (!spec.spec.animation) spec.spec.animation = { Smooth: { duration: { nanos: 0 }}};
              if (!spec.spec.animation.Smooth) spec.spec.animation.Smooth = { duration: { nanos: 0 }};

              if (propName === 'initialDelay') {
                if (!spec.spec.initial_delay) spec.spec.initial_delay = { nanos: 0 };
                spec.spec.initial_delay.nanos = newValue * 1000000;
              } else if (propName === 'duration') {
                // Note: This currently only supports Smooth animations
                if (spec.spec.animation.Smooth) {
                  if (!spec.spec.animation.Smooth.duration) spec.spec.animation.Smooth.duration = { nanos: 0 };
                  spec.spec.animation.Smooth.duration.nanos = newValue * 1000000;
                }
              }
              nodeToUpdate.setSharedPluginData('designcompose', 'animations', JSON.stringify(spec));
              timelineNeedsUpdate = true;
            } 
            // --- Handle SceneNode Properties ---
            else if (propName === 'width') {
              if ('resize' in nodeToUpdate) {
              if ((nodeToUpdate.parent as FrameNode).layoutMode !== "NONE") {
                if ((nodeToUpdate.parent as FrameNode).layoutMode === "HORIZONTAL") (nodeToUpdate as FrameNode).primaryAxisSizingMode = 'FIXED';
                else (nodeToUpdate as FrameNode).counterAxisSizingMode = 'FIXED';
              }
              nodeToUpdate.resize(newValue, nodeToUpdate.height);
            } 
            } else if (propName === 'height') {
              if ('resize' in nodeToUpdate) {
              if ((nodeToUpdate.parent as FrameNode).layoutMode !== "NONE") {
                if ((nodeToUpdate.parent as FrameNode).layoutMode === "VERTICAL") (nodeToUpdate as FrameNode).primaryAxisSizingMode = 'FIXED';
                else (nodeToUpdate as FrameNode).counterAxisSizingMode = 'FIXED';
              }
              nodeToUpdate.resize(nodeToUpdate.width, newValue);
            }
          }
             else {
              (nodeToUpdate as any)[propName] = newValue;
            }

            // If a timing property was changed, send a full timeline update.
            if (timelineNeedsUpdate) {
              await sendUpdatedTimelineData();
            }
          }
        }
        break;
    }
  } catch (e) {
    console.error('Error in onmessage handler:', e);
    figma.notify('An error occurred in the plugin. See console for details.');
  }
};

/**
 * A map of animatable properties. This is used to identify which properties
 * can be animated. In the future, this could be expanded to include default
 * values or other metadata.
 */
const animatableProperties: Set<string> = new Set([
  'x', 'y', 'width', 'height', 'opacity', 'rotation', 'fills',
  // Stroke properties
  'strokes', 'strokeWeight', 'strokeAlign', 'strokeCap', 'strokeJoin',
  // Corner radius properties
  'cornerRadius', 'topLeftRadius', 'topRightRadius', 'bottomLeftRadius', 'bottomRightRadius',
  // Auto Layout properties
  'paddingLeft', 'paddingRight', 'paddingTop', 'paddingBottom', 'itemSpacing',
  'layoutMode', 'primaryAxisSizingMode', 'counterAxisSizingMode',
  'primaryAxisAlignItems', 'counterAxisAlignItems'
]);

/**
 * Recursively traverses the scene graph starting from a given node, applying a
 * visitor function to each node.
 * @param {SceneNode} node - The node to start traversal from.
 * @param {Function} visitor - The function to apply to each node.
 */
function traverse(node: SceneNode, visitor: (node: SceneNode) => void) {
  visitor(node);
  if ("children" in node) {
    for (const child of node.children) {
      traverse(child as SceneNode, visitor);
    }
  }
}

/**
 * Computes all property changes between the variants of a component set.
 * This function is optimized to avoid redundant lookups and to create a data
 * structure that is easy for the UI to render.
 *
 * @param {ComponentSetNode} componentSet - The component set to analyze.
 * @returns {object} An object where keys are node IDs and values are objects
 *                   containing the node's name and a map of its changed
 *                   properties.
 */
function computePropertyChanges(componentSet: ComponentSetNode) {
  const variants = componentSet.children as ComponentNode[];
  const propertyValuesByNodeKey = new Map<string, { nodeName: string, properties: Map<string, Map<string, any>> }>();

  for (const variant of variants) {
    traverse(variant, (node) => {
      const isRoot = node.id === variant.id;
      const nodeKey = isRoot ? 'ROOT_NODE' : node.name;
      const nodeDisplayName = isRoot ? 'Root' : node.name;

      if (!propertyValuesByNodeKey.has(nodeKey)) {
        propertyValuesByNodeKey.set(nodeKey, {
          nodeName: nodeDisplayName,
          properties: new Map(),
        });
      }

      const nodeData = propertyValuesByNodeKey.get(nodeKey)!;

      // --- Special Handling for Root Node Timing Properties ---
      if (isRoot) {
        const data = node.getSharedPluginData('designcompose', 'animations');
        let duration = 0;
        let initialDelay = 0;

        if (data) {
          try {
            const parsedData = JSON.parse(data);
            if (parsedData && parsedData.spec) {
              if (parsedData.spec.initial_delay) {
                  initialDelay = parsedData.spec.initial_delay.nanos / 1000000;
              }
              if (parsedData.spec.animation) {
                  if (parsedData.spec.animation.Smooth) {
                    duration = parsedData.spec.animation.Smooth.duration.nanos / 1000000;
                  } else if (parsedData.spec.animation.KeyFrame) {
                    duration = parsedData.spec.animation.KeyFrame.steps.reduce((total: number, step: any) => total + (step.duration.nanos / 1000000), 0);
                  }
              }
            }
          } catch (e) { console.error('Error parsing animation data for timing:', e); }
        }
        
        if (!nodeData.properties.has('initialDelay')) nodeData.properties.set('initialDelay', new Map());
        nodeData.properties.get('initialDelay')!.set(variant.id, initialDelay);
        
        if (!nodeData.properties.has('duration')) nodeData.properties.set('duration', new Map());
        nodeData.properties.get('duration')!.set(variant.id, duration);
      }

      for (const propName of animatableProperties) {
        if (isRoot && (propName === 'x' || propName === 'y')) {
          continue;
        }

        if ((node as any)[propName] !== undefined) {
          if (!nodeData.properties.has(propName)) {
            nodeData.properties.set(propName, new Map());
          }

          let value = (node as any)[propName];

          // Helper to convert a SolidPaint object to an rgba string
          const solidPaintToString = (paint: SolidPaint) => {
            const { r, g, b } = paint.color;
            const alpha = paint.opacity === undefined ? 1 : paint.opacity;
            const round = (c: number) => Math.round(c * 255);
            return `rgba(${round(r)}, ${round(g)}, ${round(b)}, ${alpha.toFixed(2)})`;
          };

          if (propName === 'fills' || propName === 'strokes') {
            if (Array.isArray(value) && value.length > 0) {
              const firstSolid = value.find(paint => paint.type === 'SOLID');
              if (firstSolid) {
                value = solidPaintToString(firstSolid);
              } else {
                value = 'Non-Solid';
              }
            } else {
              value = 'None';
            }
          }

          nodeData.properties.get(propName)!.set(variant.id, value);
        }
      }
    });
  }

  // Custom replacer for JSON.stringify to handle Map objects
  const mapReplacer = (key: string, value: any) => {
    if(value instanceof Map) {
      return Array.from(value.entries()).reduce((main, [key, val]) => ({...main, [key]: val}), {});
    }
    return value;
  };
  console.log("All collected properties:", JSON.stringify(Array.from(propertyValuesByNodeKey.entries()).reduce((main, [key, val]) => ({...main, [key]: val}), {}), mapReplacer, 2));

  // Step 2: Filter out properties that have no changes across variants.
  const changedNodes = new Map<string, { nodeName: string, properties: Map<string, Map<string, any>> }>();
  for (const [nodeKey, nodeData] of propertyValuesByNodeKey.entries()) {
    const changedProperties = new Map<string, Map<string, any>>();
    for (const [propName, values] of nodeData.properties.entries()) {
      const uniqueValues = new Set(values.values());
      if (uniqueValues.size > 1) {
        changedProperties.set(propName, values);
      }
    }

    if (changedProperties.size > 0) {
      changedNodes.set(nodeKey, {
        nodeName: nodeData.nodeName,
        properties: changedProperties,
      });
    }
  }
  
  console.log("Filtered changed nodes:", JSON.stringify(Array.from(changedNodes.entries()).reduce((main, [key, val]) => ({...main, [key]: val}), {}), mapReplacer, 2));

  return changedNodes;
}

async function handleSelectionChange() {
  console.log('Selection changed. Current selection:', figma.currentPage.selection);
  const selection = figma.currentPage.selection;

  if (selection.length === 0) {
    figma.ui.postMessage({ type: 'selection-name', name: 'No selection' });
    console.log('Selection empty. Sending no-selection message.');
    figma.ui.postMessage({ type: 'no-selection' });
    return;
  }

  if (selection.length > 1) {
    figma.ui.postMessage({ type: 'selection-name', name: 'Multiple selection' });
    console.log('Multiple nodes selected. Sending no-selection message.');
    figma.ui.postMessage({ type: 'no-selection' });
    return;
  }

  // From here, selection.length is 1
  const selectedNode = selection[0];
  console.log(`Selected node type: ${selectedNode.type}`);

  if (selectedNode.type === 'COMPONENT_SET') {
    console.log('Component Set selected. Sending timeline data.');
    await sendUpdatedTimelineData();
  } else {
    console.log('Single node selected. Loading animation data.');
    console.log(`Posting 'selection-name' message with name: ${selectedNode.name}`);
    figma.ui.postMessage({ type: 'selection-name', name: selectedNode.name });
    loadAnimationData(selectedNode);
  }
}

/**
 * This is the main entry point for the plugin's functionality. It listens for
 * changes in the user's selection within the Figma document.
 */
figma.on('selectionchange', handleSelectionChange);

/**
 * Recursively traverses a Figma node and its children to build a serializable
 * tree of node properties. This tree contains all the necessary information
 * for the UI to render and animate the nodes on a Fabric.js canvas.
 *
 * @param {SceneNode} node - The Figma node to start traversal from.
 * @returns {object} A plain object representing the node and its children.
 */
function getNodeTreeWithProperties(node: SceneNode): object {
  // --- Base properties for any node ---
  const props: {[key: string]: any} = {
    id: node.id,
    name: node.name,
    type: node.type,
    visible: node.visible,
  };

  // --- Properties specific to SceneNodes (geometry, paint, etc.) ---
  // We check for 'width' as a proxy for a SceneNode.
  if ('width' in node) {
    props.x = node.x;
    props.y = node.y;
    props.width = node.width;
    props.height = node.height;
  }
  if ('rotation' in node) {
    props.rotation = node.rotation;
  }
  if ('opacity' in node) {
    props.opacity = node.opacity;
  }
  if ('transform' in node) {
    // The transformation matrix is crucial for calculating the final
    // position, rotation, and scale in the UI.
    props.transform = node.transform;
  }

    // --- Paint Properties ---
    // We simplify fills and strokes to a single color for now.
    // The UI will handle converting this to a format Fabric.js understands.
    if ('fills' in node && Array.isArray(node.fills) && node.fills.length > 0) {
      props.fills = node.fills.filter((f: any) => f.visible);
    }
    if ('strokes' in node && Array.isArray(node.strokes) && node.strokes.length > 0) {
      props.strokes = node.strokes.filter((s: any) => s.visible);
    }
    if ('strokeWeight' in node) {
    props.strokeWeight = node.strokeWeight;
  }
  
  // --- Recursion for Children ---
  // If the node has children, recursively call this function for each child.
  if ("children" in node && node.children) {
    props.children = node.children.map(child => getNodeTreeWithProperties(child as SceneNode));
  }

  return props;
}


/**
 * Gathers all timeline-related data (variants, property changes, timing) from
 * the current selection and sends it to the UI to be rendered.
 * This version sends a full node property tree for each variant instead of an SVG.
 */
async function sendUpdatedTimelineData() {
  const selection = figma.currentPage.selection;
  if (selection.length !== 1 || selection[0].type !== 'COMPONENT_SET') {
    figma.ui.postMessage({ type: 'no-selection' });
    return;
  }
  
  const componentSet = selection[0] as ComponentSetNode;

  // Asynchronously process all variants to get their animation data and node trees.
  const variantPromises = (componentSet.children as ComponentNode[]).map(async (child) => {
    const data = child.getSharedPluginData('designcompose', 'animations');
    let duration: number | null = null;
    let initialDelay: number | null = null;

    // --- Parse Timing Data ---
    // This logic remains the same as it's stored on the variant node.
    if (data) {
      try {
        const parsedData = JSON.parse(data);
        if (parsedData && parsedData.override === 'Custom' && parsedData.spec) {
          if (parsedData.spec.initial_delay) {
              initialDelay = parsedData.spec.initial_delay.nanos / 1000000;
          }
          if (parsedData.spec.animation) {
              if (parsedData.spec.animation.Smooth) {
                duration = parsedData.spec.animation.Smooth.duration.nanos / 1000000;
              } else if (parsedData.spec.animation.KeyFrame) {
                duration = parsedData.spec.animation.KeyFrame.steps.reduce((total: number, step: any) => total + (step.duration.nanos / 1000000), 0);
              }
          }
        }
      } catch (e) {
        console.error('Error parsing animation data for duration/delay:', e);
      }
    }

    // --- Get Node Properties ---
    // Instead of exporting an SVG, we now build the node property tree.
    const nodeTree = getNodeTreeWithProperties(child);

    return {
      id: child.id,
      name: child.name,
      duration: duration,
      initialDelay: initialDelay,
      nodeTree: nodeTree, // Send the full property tree
    };
  });

  // Wait for all the variant processing to complete.
  const variants = await Promise.all(variantPromises);
  const propertyChangesMap = computePropertyChanges(componentSet);

  // Deep convert the Maps to plain objects for serialization.
  const propertyChangesObject: {[key: string]: any} = {};
  for (const [nodeKey, nodeData] of propertyChangesMap.entries()) {
      const propertiesObject: {[key: string]: any} = {};
      for (const [propName, valuesMap] of nodeData.properties.entries()) {
          propertiesObject[propName] = Array.from(valuesMap.entries()).reduce((main, [key, val]) => ({...main, [key]: val}), {});
      }
      propertyChangesObject[nodeKey] = {
          nodeName: nodeData.nodeName,
          properties: propertiesObject,
      };
  }

  // Send the processed data to the UI.
  figma.ui.postMessage({
    type: 'load-timeline-data',
    name: componentSet.name,
    data: {
      variants: variants,
      propertyChanges: propertyChangesObject,
    }
  });
}

/**
 * Loads animation data from a given node and sends it to the UI.
 * @param {SceneNode} node - The node to load animation data from.
 */
function loadAnimationData(node: SceneNode) {
  const data = node.getSharedPluginData('designcompose', 'animations');
  let spec = null;
  if (data) {
    try {
      spec = JSON.parse(data);
    } catch (e) {
      console.error('Error parsing animation data:', e);
      figma.notify('Error parsing animation data. Check the console for details.');
    }
  }
  figma.ui.postMessage({
    type: 'animation-data',
    spec: spec,
    id: node.id,
    name: node.name,
  });
}