/**
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This is a counter widget with buttons to increment and decrement the number.

const { widget } = figma
const { useSyncedState, useSyncedMap, usePropertyMenu, useEffect, waitForTask, AutoLayout, Frame, Image, Text, SVG } = widget

// Parse a component name for multiple variants by checking for commas, and return the name
// rearranged with the property=variant parts alphabetized. This will normalize a name such as
// "#item=Grid, #playing=On" so that if a developer specifies preview content for the node name as
// "#playing=On, #item=Grid", we will still find the node.
function sortedComponentName(name: string): string {
  let parts = name.split(",");
  for(var i = 0; i < parts.length; ++i) {
    parts[i] = parts[i].trim();
  }
  parts.sort();
  let newName = parts.join(", ");
  return newName;
}

// Recursively parse the Figma document looking for nodes that match nodes in contentNodes.
// If any of the nodes added are COMPONENT_SET nodes, find and add their variant children instead.
// If any of the nodes are COMPONENT or FRAME nodes, just fill in their node IDs
function findContentNodes(node: BaseNode, contentNodes: Map<string, string>) {
  if (!node) return;
  let nodeName = sortedComponentName(node.name);
  if (contentNodes.has(nodeName)) {
    if (node.type == 'COMPONENT_SET') {
      // Delete the COMPONENT_SET in contentNodes and replace with all the variant children
      contentNodes.delete(nodeName);
      if ((node as any).children) {
        let parent: ChildrenMixin = node as ChildrenMixin;
        for (const child of parent.children) {
          if (child.type == "COMPONENT") {
            contentNodes.set(sortedComponentName(child.name), child.id);
          }
        }
      }
      return;
    }
    else if (node.type == "FRAME" || node.type == "COMPONENT") {
      // Update contentNodes with the node ID if there isn't one already set
      let nodeId = contentNodes.get(nodeName);
      if (nodeId.length == 0)
        contentNodes.set(nodeName, node.id);
    }
  }

  if ((node as any).children) {
    let parent: ChildrenMixin = node as ChildrenMixin;
    for (const child of parent.children) {
      findContentNodes(child, contentNodes);
    }
  }
}

enum DesignCustomizationKind {
  Text = "text",
  TextStyle = "text_style",
  Image = "image",
  ImageWithContext = "image_with_context",
  Visibility = "visibility",
  VariantProperty = "variant_property",
  ComponentReplacement = "component_replacement",
  ContentReplacement = "content_replacement",
  ListContent = "list_content",
  Modifier = "modifier",
  // Not covering "placeholder"; we should remove it.
}

interface DesignCustomization {
  kind: DesignCustomizationKind,
  name: string,
  node: string,
}

interface DesignCustomizationVariantProperty extends DesignCustomization {
  kind: DesignCustomizationKind.VariantProperty,
  values: string[]
}

interface PreviewPage {
  name: string,
  content: string[]
}

interface DesignCustomizationListContent extends DesignCustomization {
  kind: DesignCustomizationKind.ListContent,
  content: string[],
  previewContent: PreviewPage[]
}

interface DesignComponentSpec {
  name: string,
  node: string,
  isRoot?: boolean, // is this the root component? We must know this since we pass customizations down to children.
  customizations: DesignCustomization[]
}

interface DesignDocSpec {
  name: string,
  version: string,
  components: DesignComponentSpec[]
}

class NodeSpan {
  nodeName: string = "";
  span: number = 1;
}

enum LayoutType {
  FixedColumns = "fixedColumns",
  FixedRows = "fixedRows",
  AutoColumns = "autoColumns",
  AutoRows = "autoRows",
  Horizontal = "horizontal",
  Vertical = "vertical",
}

function layoutType(layout: string): LayoutType {
  if (layout == LayoutType.FixedColumns)
    return LayoutType.FixedColumns;
  else if (layout == LayoutType.FixedRows)
    return LayoutType.FixedRows;
  else if (layout == LayoutType.AutoColumns)
    return LayoutType.AutoColumns;
  else if (layout == LayoutType.AutoRows)
    return LayoutType.AutoRows;
  else if (layout == LayoutType.Vertical)
    return LayoutType.Vertical;
  else
    return LayoutType.Horizontal;
}

function isColumnLayout(layoutType: LayoutType) {
  return layoutType == LayoutType.FixedColumns || layoutType == LayoutType.AutoColumns;
}

function isRowLayout(layoutType: LayoutType) {
  return layoutType == LayoutType.FixedRows || layoutType == LayoutType.AutoRows;
}

function isGridLayout(layoutType: LayoutType) {
  return layoutType != LayoutType.Horizontal && layoutType != LayoutType.Vertical;
}

class LayoutOptions {
  layout: LayoutType = LayoutType.Horizontal;
  columnsRows: number = 1;
  adaptiveMinSize: number = 1;
  marginLeft: number = 0;
  marginRight: number = 0;
  marginTop: number = 0;
  marginBottom: number = 0;
  autoSpacing: boolean = false;
  autoSpacingItemSize: number = 1;
  verticalSpacing: number = 0;
  horizontalSpacing: number = 0;
  itemSpacing: number = 0;
  spaceBetween: boolean = false;
  verticalAlignment: 'center' | 'start' | 'end' | 'baseline' = "start";
  horizontalAlignment: 'center' | 'start' | 'end' | 'baseline' = "start";

  constructor(extendedLayout) {
    if (extendedLayout != null) {
      this.layout = layoutType(extendedLayout.layout);
      let commonData = extendedLayout.commonData;
      if (commonData != null) {
        this.marginLeft = commonData.marginLeft;
        this.marginRight = commonData.marginRight;
        this.marginTop = commonData.marginTop;
        this.marginBottom = commonData.marginBottom;
      }

      let gridLayout = extendedLayout.gridLayoutData;
      if (gridLayout != null) {
        this.columnsRows = gridLayout.columnsRows;
        this.adaptiveMinSize = gridLayout.adaptiveMinSize;
        this.autoSpacing = gridLayout.autoSpacing;
        this.autoSpacingItemSize = gridLayout.autoSpacingItemSize;
        this.verticalSpacing = gridLayout.verticalSpacing;
        this.horizontalSpacing = gridLayout.horizontalSpacing;
      }

      let autoLayout = extendedLayout.autoLayoutData;
      if (autoLayout != null && Object.keys(autoLayout).length > 0) {
        this.itemSpacing = autoLayout.itemSpacing;
        this.verticalAlignment = autoLayout.verticalAlignment;
        this.horizontalAlignment = autoLayout.horizontalAlignment;
        this.spaceBetween = autoLayout.spaceBetween;
      }
    }
  }
}

class OverflowOptions {
  limitContent: boolean = false;
  maxItems: number = 0;
  nodeName: string = null;
  nodeId: string = null;
  width: number = 0;
  height: number = 0;
  image: string = null;
}

class NodeContent {
  content: Map<string, string> = new Map();
  previewContent: PreviewPage[] = [];
}

function loadDesignSpec(): DesignDocSpec {
  // Get the json plugin data from our root node
  let designSpec = figma.root.getSharedPluginData("designcompose", "clippy-json");
  if (!designSpec)
    return null;

  try {
    return JSON.parse(designSpec);
  } catch(e) {
    console.log("Error parsing designSpec JSON: " + e);
  }
}

function findNodeId(node, contentNodes): string {
  let nodeName = sortedComponentName(node);
  if (contentNodes.has(nodeName))
    return contentNodes.get(nodeName);
  return null
}

// Returns a map of replacement content nodes with the content types they hold formatted as:
// <container frame node name> -> { content node name -> content node id }
function loadNodeContentData(designSpecOverride?: DesignDocSpec): Map<string, NodeContent> {
  let designSpec = designSpecOverride ? designSpecOverride : loadDesignSpec();
  if (designSpec == null)
    return null;

  // Parse the JSON to find content replacement nodes that have content data.
  let contentData: Map<string, NodeContent> = new Map();
  for (const component of designSpec.components) {
    for (const c of component.customizations) {
      let contentNode = c as DesignCustomizationListContent;
      if (contentNode != null) {
        let contentNodes: Map<string, string> = new Map();
        let previewContent: PreviewPage[] = [];

        if (contentNode.content != null) {
          for (const node of contentNode.content) {
            contentNodes.set(node, "");
          }
          // Look through the Figma document and fill in the node IDs
          findContentNodes(figma.root, contentNodes);
        }

        // Find the preview content nodes that we will use to populate the widget with
        // preview content
        if (contentNode.previewContent != null) {
          for (const previewPage of contentNode.previewContent) {
            let previewNodes: string[] = [];
            for (const node of previewPage.content) {
              let nodeId = findNodeId(node, contentNodes);
              if (nodeId != null)
                previewNodes.push(nodeId);
            }
            previewContent.push({
              name: previewPage.name,
              content: previewNodes
            });
          }
        }

        if (contentNodes.size > 0 || previewContent.length > 0) {
          let nodeContent = new NodeContent();
          nodeContent.content = contentNodes;
          nodeContent.previewContent = previewContent;
          contentData.set(c.node, nodeContent);
        }
      }
    }
  }

  return contentData;
}

function toNumber(str, def) {
  let num = parseInt(str, 10);
  if (isNaN(num)) {
    return def;
  }
  return num;
}

function ListPreview() {
  const widgetId = widget.useWidgetId();
  // Size of the parent, which is the size of the widget
  const [parentSize, setParentSize] = useSyncedState("parentSize", [0, 0]);
  // Node name used for auto content
  const [contentNodeName, setContentNodeName] = useSyncedState("contentNodeName", "ListPreview");
  // Name of current preview page used to preview content
  const [previewPage, setPreviewPage] = useSyncedState("previewPage", "");
  // List of items to show in the current preview page
  const [itemList, setItemList] = useSyncedState("itemList", [])
  // Keep track of the item size of the first preview item that has a span of 1. This is used
  // for auto (edge-to-edge) spacing calculations
  const [itemSize, setItemSize] = useSyncedState("itemSize", [0, 0]);
  // Current layout options (spacing, margins, etc)
  const [layoutOptions, setLayoutOptions] = useSyncedState("layoutOptions", new LayoutOptions(null));
  // Hash of span sizes for all possible items
  const spanContent = useSyncedMap<[number, boolean]>("spanContent");
  // True when we are in overflow node selection mode
  const [selectOverflowState, setSelectOverflowState] = useSyncedState("selectOverflowState", false);
  // True when we are in parent frame selection mode
  const [selectParentMode, setSelectParentMode] = useSyncedState("selectParentMode", false);
  // Parmeters used to keep track of overflow node
  const [overflowOptions, setOverflowOptions] = useSyncedState("overflowOptions", new OverflowOptions());

  function showLayoutUI() {
    figma.showUI(__html__, { width: 400, height: 600 });
    verifyParentFrame();
  }

  function initLayoutUI(contentNodeNameOverride?: string, previewPageOverride?: string, showingUI?: boolean) {
    let widgetContentNodeName = contentNodeNameOverride ? contentNodeNameOverride : contentNodeName
    let previewPageName = previewPageOverride ? previewPageOverride : previewPage;
    
    // Load accompanying JSON file to get content data about the nodes 
    let designSpec = loadDesignSpec();
    let nodeContentData = loadNodeContentData(designSpec);

    let contentNodeNameOptions = []
    if (nodeContentData) {
      for (const key of nodeContentData.keys()) {
        contentNodeNameOptions.push(key);
      }
    }

    refreshWidgetSize();
    
    // Get the extended layout data for this widget
    const node = figma.getNodeById(widgetId) as WidgetNode;
    let extendedLayoutData = node.getSharedPluginData("designcompose", "vsw-extended-auto-layout");
    let extendedLayout = (extendedLayoutData && extendedLayoutData.length) ? JSON.parse(extendedLayoutData) : {};
    let content = [];
    if (nodeContentData && nodeContentData.has(widgetContentNodeName)) {
      for (const [nodeName, nodeId] of nodeContentData.get(widgetContentNodeName).content.entries()) {
        content.push({ nodeName: nodeName, nodeId: nodeId });
      }
    }

    // Calculate initial itemSize with first item in preview content
    let previewContent = nodeContentData ? nodeContentData.get(widgetContentNodeName) : null;
    if (previewContent && previewContent.content && (itemSize[0] == 0 || itemSize[1] == 0)) {
      for (const [nodeName, nodeId] of nodeContentData.get(widgetContentNodeName).content.entries()) {
        let contentNode = figma.getNodeById(nodeId) as DefaultFrameMixin;
        if (contentNode && contentNode.width > 0 && contentNode.height > 0) {
          setItemSize([contentNode.width, contentNode.height]);
          break;
        }
      }
    }

    // Get frame data for widget
    let widgetContentStr = node.getSharedPluginData("designcompose", "widget-content");
    let widgetContent = (widgetContentStr && widgetContentStr.length) ? JSON.parse(widgetContentStr) : {
      widgetContentNodeName,
      previewPageName
    };

    // Populate the list of preview content
    let previewPageOptions = []
    let previewPageFound = false;
    if (nodeContentData != null) {
      let nodeContent = nodeContentData.get(widgetContent.contentNodeName);
      if (nodeContent != null) {
        for (const page of nodeContent.previewContent) {
          previewPageOptions.push(page.name)
          if (page.name == previewPageName)
            previewPageFound = true;
        }
      }
    }

    // If our saved preview page is not found, select the first one in the list
    if (!previewPageFound) {
      let newPreviewPage = previewPageOptions.length > 0 ? previewPageOptions[0] : "";
      setPreviewPage(newPreviewPage);

      // Since the preview page changed, save it into the plugin data
      widgetContent.previewPage = newPreviewPage;
      widgetContentStr = JSON.stringify(widgetContent);
      node.setSharedPluginData("designcompose", "widget-content", widgetContentStr);
    }
      
    // Get the json metadata from our root node
    let file = figma.root.getSharedPluginData("designcompose", "clippy-json-file");
    let name = designSpec ? designSpec.name : null;
    let version = designSpec ? designSpec.version : null;

    let layoutOptions = new LayoutOptions(extendedLayout);
    populatePreviewContent(widgetContent, layoutOptions);

    if (!showingUI)
      figma.ui.postMessage({
        msg: 'init',
        extendedLayout,
        content: content,
        contentNodeNameOptions: contentNodeNameOptions,
        previewPageOptions: previewPageOptions,
        hasKeywordData: nodeContentData != null,
        widgetContent,
        file,
        name,
        version,
      });
  }

  // Return the number of fixed columns or calculate the number of adaptive columns
  function getNumColumns(layoutOptions: LayoutOptions) {
    if (layoutOptions.layout == LayoutType.FixedColumns) {
      return layoutOptions.columnsRows;
    } else {
      let frameWidth = parentSize[0] - layoutOptions.marginLeft - layoutOptions.marginRight;
      return Math.floor(frameWidth / (layoutOptions.adaptiveMinSize + layoutOptions.horizontalSpacing));
    }
  }

  // Calculate the column width based on frame width, number of columns and spacing
  function getColumnWidth(layoutOptions: LayoutOptions) {
    let frameWidth = parentSize[0] - layoutOptions.marginLeft - layoutOptions.marginRight;
    let columns = getNumColumns(layoutOptions);
    let horizontalSpacing = getHorizontalSpacing(layoutOptions, frameWidth);
    if (isColumnLayout(layoutOptions.layout)) {
      let extraSpacing = (horizontalSpacing * (columns - 1)) / columns;
      return frameWidth / columns - extraSpacing;
    } else {
      console.log("Error: getColumnWidth called on non-column format " + layoutOptions.layout);
      return 1;
    }
  }

  // Return the number of fixed rows or calculate the number of adaptive rows
  function getNumRows(layoutOptions: LayoutOptions) {
    if (layoutOptions.layout == LayoutType.FixedRows) {
      return layoutOptions.columnsRows;
    } else {
      let frameHeight = parentSize[1] - layoutOptions.marginTop - layoutOptions.marginBottom;
      return Math.floor(frameHeight / (layoutOptions.adaptiveMinSize + layoutOptions.verticalSpacing));
    }
  }
 
  // Calculate the row height based on frame height, number of rows and spacing
  function getRowHeight(layoutOptions: LayoutOptions) {
    let frameHeight = parentSize[1] - layoutOptions.marginTop - layoutOptions.marginBottom;
    let rows = getNumRows(layoutOptions);
    let verticalSpacing = getVerticalSpacing(layoutOptions, frameHeight);
    if (!isColumnLayout(layoutOptions.layout)) {
      let extraSpacing = (verticalSpacing * (rows - 1)) / rows;
      return frameHeight / rows - extraSpacing;
    } else {
      console.log("Error: getRowHeight called on non-row format " + layoutOptions.layout);
      return 1;
    }
  }

  // Get the span of a node ID
  function getSpan(nodeId: string, layoutOptions: LayoutOptions) {
    let span = 1;
    let maxSpan = false;
    if (spanContent.has(nodeId)) {
      let spanData = spanContent.get(nodeId);
      span = spanData[0];
      if (!span || span < 0)
        span = 1;
      maxSpan = spanData[1];
    }
    if (maxSpan) {
      if (isColumnLayout(layoutOptions.layout))
        return getNumColumns(layoutOptions);
      else
        return getNumRows(layoutOptions);
    }
    return span;
  }

  // Calculate the horizontal spacing between items if autoSpacing is set
  function getHorizontalSpacing(layoutOptions: LayoutOptions, frameWidth: number): number {
    let numColumns = getNumColumns(layoutOptions);
    if (layoutOptions.autoSpacing && numColumns > 1)
      return (frameWidth - (itemSize[0] * numColumns)) / (numColumns - 1);
    else
      return layoutOptions.horizontalSpacing;
  }

  // Calculate the vertical spacing between items if autoSpacing is set
  function getVerticalSpacing(layoutOptions: LayoutOptions, frameHeight: number): number {
    let numRows = getNumRows(layoutOptions);
    if (layoutOptions.autoSpacing && numRows > 1)
      return (frameHeight - (itemSize[1] * numRows)) / (numRows - 1);
    else
      return layoutOptions.verticalSpacing;
  }

  function populatePreviewContent(widgetContent: any, layoutOptions: LayoutOptions) {
    let nodeContentData = loadNodeContentData();
    if (nodeContentData && nodeContentData.has(widgetContent.contentNodeName)) {
      let nodeContent = nodeContentData.get(widgetContent.contentNodeName);

      // Clear out old preview content
      let newItemList = [];
      setItemList(newItemList);

      // Find the page we want to use and populate its contents
      let didSetItemSize = false;
      for (const previewPage of nodeContent.previewContent.values()) {
        if (previewPage.name == widgetContent.previewPage) {
          for (const nodeId of previewPage.content) {

            // if span is 1 and this is the first one, set item size
            let node = figma.getNodeById(nodeId) as DefaultFrameMixin;
            if (node && !didSetItemSize && spanContent.has(nodeId)) {
              let spanData = spanContent.get(nodeId);
              let span = spanData[0];
              let maxSpan = spanData[1];
              if (!maxSpan && span && span == 1) {
                setItemSize([node.width, node.height]);
                didSetItemSize = true;
              }
            }
            addPreviewNode(nodeId, newItemList, layoutOptions);
          }
          break;
        }
      }
    }
  }

  function addPreviewNode(nodeId, list, layoutOptions: LayoutOptions) {
    let node = figma.getNodeById(nodeId) as DefaultFrameMixin;
    if (node == null) {
      console.log("Error getting node for nodeId " + nodeId);
      return;
    }

    let deleteNode = false;
    if (isGridLayout(layoutOptions.layout)) {
      // To add a preview node first we create a copy of the node. Then we stretch it to fit the column or row.
      // Then we export it into an image, and finally we delete the node.
      let [frameWidth, frameHeight] = parentSize

      frameWidth = frameWidth - layoutOptions.marginLeft - layoutOptions.marginRight;
      frameHeight = frameHeight - layoutOptions.marginTop - layoutOptions.marginBottom;
      let [nodeWidth, nodeHeight] = [node.width, node.height];
      let span = getSpan(nodeId, layoutOptions);
      let horizontalSpacing = getHorizontalSpacing(layoutOptions, frameWidth);
      let verticalSpacing = getVerticalSpacing(layoutOptions, frameHeight);
      
      if (isColumnLayout(layoutOptions.layout)) {
        let columnWidth = getColumnWidth(layoutOptions);
        nodeWidth = columnWidth * span + (span - 1) * horizontalSpacing;
      } else {
        let rowHeight = getRowHeight(layoutOptions);
        nodeHeight = rowHeight * span + (span - 1) * verticalSpacing;
      }

      let frameNode = node as FrameNode; 
      if (frameNode != null) {
        node = frameNode.clone();
        node.resize(nodeWidth, nodeHeight);
        deleteNode = true;
      }
    }

    (async () => {
      const bytes = await node.exportAsync({
        format: 'PNG'
      })
      const base64String = figma.base64Encode(bytes);

      // Add the node image to the item list
      list.push({
        nodeId: nodeId,
        width: node.width,
        height: node.height,
        image: base64String
      })
      setItemList(list);

      if (deleteNode)
        node.remove();
    })()
  }

  function updateLayoutOptions(extendedlayout) {
    let lo = new LayoutOptions(extendedlayout);
    setLayoutOptions(lo);
    
    if (isGridLayout(lo.layout)) {
      for (const node of extendedlayout.gridLayoutData.spanContent) {
        spanContent.set(node.nodeId, [node.span as number, node.maxSpan]);
      }
    }
  }

  function updateLimitContentData(extendedLayout) {
    let limitContentData = extendedLayout.limitContentData;

    let nodeId = limitContentData.overflowNodeId;
    let node = figma.getNodeById(nodeId) as DefaultFrameMixin;

    // Create an image for the overflow node
    if (node != null) {
      (async () => {
        const bytes = await node.exportAsync({
          format: 'PNG'
        })
        const base64String = figma.base64Encode(bytes);

        let options = new OverflowOptions();
        options.limitContent = extendedLayout.limitContent;
        options.maxItems = limitContentData.maxNumItems;
        options.nodeName = node.name;
        options.nodeId = nodeId;
        options.width = node.width;
        options.height = node.height;
        options.image = base64String;
        setOverflowOptions(options);
      })()
    } else {
      let options = new OverflowOptions();
      options.limitContent = extendedLayout.limitContent;
      options.maxItems = limitContentData.maxNumItems;
      options.nodeName = "";
      options.nodeId = "";
      setOverflowOptions(options);
    }
  }

  function sendOverflowNodeData(node) {
    //  export the node into a PNG resized to 20px and give it to the UI
    (async () => {
      let longSide  = node.width > node.height ? node.width : node.height;
      let scale = 20 / longSide;
      const bytes = await node.exportAsync({
        format: 'PNG',
        constraint: { type: 'SCALE', value: scale }
      })
      const base64String = figma.base64Encode(bytes);

      figma.ui.postMessage({
        msg: 'overflow-node-selected',
        name: node.name,
        id: node.id,
        data: base64String
      });
    })()
  }

  function onSelectionChanged() {
    if (selectOverflowState) {
      setSelectOverflowState(false);

      let selection = figma.currentPage.selection;
      if (selection && selection.length == 1 && selection[0]) {
        let node = selection[0];
        sendOverflowNodeData(node);
      }
    } else if (selectParentMode) {
      setSelectParentMode(false);

      let selection = figma.currentPage.selection;
      if (selection && selection.length == 1 && selection[0] && selection[0].type == 'FRAME') {
        let parentFrame = selection[0];
        let widgetNode = figma.getNodeById(widgetId) as SceneNode;
        parentFrame.appendChild(widgetNode);

        initLayoutUI();
      }
    } else {
      figma.closePlugin();
    }
  }

  function onDocumentChanged(event: DocumentChangeEvent) {
    // We only care if something changed with the parent because we want to always resize
    // to our parent frame size. If we don't filter other changes out, this will get
    // called repeatedly since initialization of this widget will create and delete nodes
    // in order to generate preview content images.
    const node = figma.getNodeById(widgetId) as WidgetNode;
    let parentNode = node.parent as FrameNode;
    let parentChanged = false;
    for (const c of event.documentChanges) {
      if (c.id == parentNode.id) {
        parentChanged = true;
        break;
      }
    }

    if (parentChanged) {
      verifyParentFrame();
    }
  }

  // The widget always fills to fit its parent frame. This function saves the parent frame size
  function refreshWidgetSize() {
    const node = figma.getNodeById(widgetId) as WidgetNode;
    let parentNode = node.parent as FrameNode;
    if (parentNode != null && parentNode.type == 'FRAME') {
      if (parentNode.width != parentSize[0] || parentNode.height != parentSize[1])
        setParentSize([parentNode.width, parentNode.height]);
      node.x = 0;
      node.y = 0;
    } else {
      if (parentSize[0] != 0 || parentSize[1] != 0)
        setParentSize([0, 0]);
    }
  }

  // Check to see if the parent frame changed. Show an error if a change has been detected.
  function verifyParentFrame() {
    let widgetNode = figma.getNodeById(widgetId) as SceneNode;
    let parent = widgetNode.parent;
    if (parent == null || parent.type != 'FRAME')
      figma.ui.postMessage({
        msg: 'need-parent'
      });
    else
      initLayoutUI();
  }

  // Name the widget and its parent based on the content frame used
  function updateFrameNames(name: string) {
    let widgetNode = figma.getNodeById(widgetId);
    widgetNode.name = name + "-widget";
    if (widgetNode.parent)
      widgetNode.parent.name = name + "-parent";
  }

  let menuItems: WidgetPropertyMenuItem[] = [
    {
      itemType: 'action',
      tooltip: 'Edit auto content',
      propertyName: 'configure',
      icon:       
        `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <g clip-path="url(#clip0_1020_815)">
          <path d="M3.09375 20.9062H4.75L17.9688 7.71875L17.125 6.875L16.2813 6.03125L3.09375 19.25V20.9062ZM0.75 23.25V18.25L17.9688 1.09375C18.4063 0.635415 18.9479 0.406248 19.5938 0.406248C20.2604 0.406248 20.8229 0.635415 21.2813 1.09375L22.9063 2.78125C23.3646 3.21875 23.5938 3.76042 23.5938 4.40625C23.5938 5.05208 23.3646 5.59375 22.9063 6.03125L5.75 23.25H0.75ZM21.2188 4.40625L19.5938 2.75L21.2188 4.40625ZM17.9688 7.71875L17.125 6.875L16.2813 6.03125L17.9688 7.71875Z" fill="white"/>
          </g>
          <defs>
          <clipPath id="clip0_1020_815">
          <rect width="24" height="24" fill="white"/>
          </clipPath>
          </defs>
        </svg>`
    },
  ];
  // If there is a preview page, also show the refresh button
  if (previewPage.length > 0) {
    menuItems.push(
      {
        itemType: 'separator',
      }
    );
    menuItems.push(
      {
        itemType: 'action',
        tooltip: 'Refresh widget size to frame',
        propertyName: 'refresh',
        icon:
          `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M1 13.5V10.9688H3.34375V13.5H1ZM1 8.4375V5.90625H3.34375V8.4375H1ZM1 3.34375V1H3.34375V3.34375H1ZM5.90625 3.34375V1H8.4375V3.34375H5.90625ZM11 23.5V21.1563H13.5312V23.5H11ZM11 3.34375V1H13.5312V3.34375H11ZM16.0625 23.5V21.1563H18.5938V23.5H16.0625ZM21.1563 23.5V21.1563H23.5V23.5H21.1563ZM21.1563 18.5938V16.0625H23.5V18.5938H21.1563ZM21.1563 13.5V10.9688H23.5V13.5H21.1563ZM21.1563 8.4375V3.34375H16.0625V1H23.5V8.4375H21.1563ZM1 23.5V16.0625H3.34375V21.1563H8.4375V23.5H1Z" fill="white"/>
            <path d="M12.25 18.5C10.7292 18.5 9.39583 18.0313 8.25 17.0938C7.125 16.1354 6.41667 14.9375 6.125 13.5H8.0625C8.33333 14.4167 8.84375 15.1667 9.59375 15.75C10.3646 16.3333 11.25 16.625 12.25 16.625C13.4583 16.625 14.4896 16.1979 15.3438 15.3438C16.1979 14.4896 16.625 13.4583 16.625 12.25C16.625 11.0417 16.1979 10.0104 15.3438 9.15625C14.4896 8.30208 13.4583 7.875 12.25 7.875C11.6458 7.875 11.0833 7.98958 10.5625 8.21875C10.0417 8.42708 9.58333 8.72917 9.1875 9.125H11V11H6V6H7.875V7.78125C8.4375 7.23958 9.09375 6.8125 9.84375 6.5C10.5938 6.16667 11.3958 6 12.25 6C13.9792 6 15.4479 6.61458 16.6562 7.84375C17.8854 9.05208 18.5 10.5208 18.5 12.25C18.5 13.9792 17.8854 15.4583 16.6562 16.6875C15.4479 17.8958 13.9792 18.5 12.25 18.5Z" fill="white"/>
          </svg>`
      }
    );
  }

  usePropertyMenu(menuItems,
    ({propertyName}) => {
      if (propertyName === "configure") {
        return new Promise((resolve) => {
          showLayoutUI();
        })
      } else if (propertyName == "refresh") {
        initLayoutUI(null, null, true);
      }
    },
  )

  useEffect(() => {
    figma.on('selectionchange', onSelectionChanged);
    figma.on('documentchange', onDocumentChanged);

    figma.ui.onmessage = (msg) => {
      if (msg.msg == 'set-widget-content') {
        let widgetContent = JSON.stringify(msg.widgetContent);
        let widgetNode = figma.getNodeById(widgetId);
        if (widgetNode) {
          widgetNode.setSharedPluginData("designcompose", "widget-content", widgetContent);
        }

        setContentNodeName(msg.widgetContent.contentNodeName);
        updateFrameNames(msg.widgetContent.contentNodeName);
        setPreviewPage(msg.widgetContent.previewPage);

        initLayoutUI(msg.widgetContent.contentNodeName, msg.widgetContent.previewPage);
      }
      else if (msg.msg === 'save-extended-layout') {
        // Save the current layout options into the plugin data
        let extendedLayout = msg.extendedLayout;
        
        // This replaces autoSpacingItemSize with our 1 span item size
        let isColumns = isColumnLayout(extendedLayout.layout);
        extendedLayout.gridLayoutData.autoSpacingItemSize = isColumns ? itemSize[0] : itemSize[1];

        let extendedLayoutStr = JSON.stringify(msg.extendedLayout);
        let widgetNode = figma.getNodeById(widgetId);
        if (widgetNode) {
          widgetNode.setSharedPluginData("designcompose", "vsw-extended-auto-layout", extendedLayoutStr);        
          updateLayoutOptions(extendedLayout);
          updateLimitContentData(extendedLayout);
        }

        initLayoutUI();
      }
      else if (msg.msg == 'clippy-file-selected') {
        // Once a json file has been updated, reinitialize the popup UI and reload the options
        // used in the property menu
        figma.root.setSharedPluginData("designcompose", "clippy-json-file", msg.fileName);
        figma.root.setSharedPluginData("designcompose", "clippy-json", msg.contents);
        figma.notify("Keyword data from " + msg.fileName + " uploaded: " + msg.contents.length + " bytes");
        
        initLayoutUI();
      }
      else if (msg.msg == 'clippy-file-cleared') {
        figma.root.setSharedPluginData("designcompose", "clippy-json-file", "");
        figma.root.setSharedPluginData("designcompose", "clippy-json", "");
        figma.notify("Keyword data cleared");
        
        initLayoutUI();
      }
      else if (msg.msg == 'overflow-node-clicked') {
        // Go into node selection mode
        setSelectOverflowState(true);
      }
      else if (msg.msg == 'overflow-node-closed') {
        // Cancel node selection mode
        setSelectOverflowState(false);
      }
      else if (msg.msg == 'show-node') {
        // Listen for node-highlight messages; maybe we can have a "refresh button" to run
        // clippy again in the future, too?
        var highlightNode = figma.getNodeById(msg.node);
        if (highlightNode) {
          figma.viewport.scrollAndZoomIntoView([highlightNode]);
          figma.currentPage.selection = [highlightNode as any]; // XXX support multiple pages!
        } else {
          console.log(`Error: Can't find node ${msg.node}`);
        }
      }
      else if (msg.msg == 'set-parent-frame') {
        setSelectParentMode(true);
      }
      else if (msg.msg == 'set-parent-frame-closed') {
        setSelectParentMode(false);
      }
    };

    return () => {
      figma.off('selectionchange', onSelectionChanged);
      figma.off('documentchange', onDocumentChanged);
    }
  })

  class LayoutState {
    frameWidth: number = 0;
    frameHeight: number = 0;
    lastX: number = 0;
    lastY: number = 0;
    lastColumnRow: number = 0;
    maxItemHeight: number = 0;
    maxItemWidth: number = 0;
  }

  function getGridColumnPosition(layoutState: LayoutState, node: any) {
    let span = getSpan(node.nodeId, layoutOptions);

    if (isColumnLayout(layoutOptions.layout)) {
      let numColumns = getNumColumns(layoutOptions);
      if (layoutState.lastColumnRow + span > numColumns) {
        // We can't fit this item in the current row so go to the next
        let x = 0;
        let y = layoutState.lastY + layoutState.maxItemHeight + layoutOptions.verticalSpacing;
        layoutState.lastX = node.width;
        layoutState.lastY = y;
        layoutState.maxItemHeight = node.height;
        layoutState.lastColumnRow = span;
        return [x, y];
      }
  
      // Find the position of the next column to place the item
      let horizontalSpacing = getHorizontalSpacing(layoutOptions, layoutState.frameWidth);
      let extraSpacing = (horizontalSpacing * (numColumns - 1)) / numColumns;
      let columnWidth = layoutState.frameWidth / numColumns - extraSpacing + horizontalSpacing;
      let x = layoutState.lastColumnRow * columnWidth;
      let y = layoutState.lastY;
      layoutState.lastX = x + node.width;
      layoutState.maxItemHeight = Math.max(layoutState.maxItemHeight, node.height);
      layoutState.lastColumnRow = layoutState.lastColumnRow + span;
      return [x, y];
    } else {
      let numRows = getNumRows(layoutOptions);
      if (layoutState.lastColumnRow + span > numRows) {
        // We can't fit this item in the current column so go to the next
        let x = layoutState.lastX + layoutState.maxItemWidth + layoutOptions.horizontalSpacing;
        let y = 0;
        layoutState.lastX = x;
        layoutState.lastY = node.height;
        layoutState.maxItemWidth = node.width;
        layoutState.lastColumnRow = span;
        return [x, y];
      }
  
      // Find the position of the next row to place the item
      let verticalSpacing = getVerticalSpacing(layoutOptions, layoutState.frameHeight);
      let extraSpacing = (verticalSpacing * (numRows - 1)) / numRows;
      let rowHeight = layoutState.frameHeight / numRows - extraSpacing + verticalSpacing;
      let x = layoutState.lastX;
      let y = layoutState.lastColumnRow * rowHeight;
      layoutState.lastY = y + node.height;
      layoutState.maxItemWidth = Math.max(layoutState.maxItemWidth, node.width);
      layoutState.lastColumnRow = layoutState.lastColumnRow + span;
      return [x, y];
    }
  }

  function getItem(node: any, index: number, layoutState: LayoutState) {
    let position = getGridColumnPosition(layoutState, node);
    let x = position[0];
    let y = position[1];
    layoutState.lastX = x;
    layoutState.lastY = y;
    let imgSrc = "data:image/png;base64," + node.image;
    return (
        <Image
          key={index}
          x={x + layoutOptions.marginLeft}
          y={y + layoutOptions.marginTop}
          width={node.width}
          height={node.height}
          src={imgSrc}
        />
    )
  }
  function getAutolayoutItem(node: any, index: number) {
    let imgSrc = "data:image/png;base64," + node.image;
    return (
        <Image
          key={index}
          width={node.width}
          height={node.height}
          src={imgSrc}
        />
    )
  }

  // If no preview page has been selected, show the initial UI with the + button
  if (previewPage.length == 0)
    return (
      <Frame
        name={"Auto Content Preview"}
        width={400}
        height={400}
        stroke={"#66B5FF"}
        strokeWidth={6}
        cornerRadius={30}
      >
        <Text 
          fontSize={32}
          fontWeight={"bold"}
          x={100}
          y={20}
        >
          Auto Content
        </Text>
        <Image
          src="data:image/png;base64,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"
          width={350}
          height={285}
          x={25}
          y={90}
        />
        <Image
          src="data:image/png;base64,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"
          width={161}
          height={161}
          x={120}
          y={140}
          onClick={() =>
            new Promise((resolve) => {
              showLayoutUI();
            })
          }
        />
      </Frame>
    )

  let [width, height] = [400, 400];
  if (parentSize[0] > 0 && parentSize[1] > 0)
    [width, height] = parentSize;

  let layoutState = new LayoutState();
  layoutState.frameWidth = width - layoutOptions.marginLeft - layoutOptions.marginRight;
  layoutState.frameHeight = height - layoutOptions.marginTop - layoutOptions.marginBottom;

  if (!isGridLayout(layoutOptions.layout)) {
    let direction: 'horizontal' | 'vertical' = layoutOptions.layout == LayoutType.Horizontal ? "horizontal" : "vertical";
    let horizontalAlignItems: 'center' | 'start' | 'end' | 'baseline' = layoutOptions.horizontalAlignment;
    let verticalAlignItems: 'center' | 'start' | 'end' | 'baseline' = layoutOptions.verticalAlignment;
    let spacing: number | 'auto' = layoutOptions.spaceBetween ? 'auto' : layoutOptions.itemSpacing;
    let padding = {
      top: layoutOptions.marginTop, 
      left: layoutOptions.marginLeft,
      bottom: layoutOptions.marginBottom,
      right: layoutOptions.marginRight,
    };
    return (
      <AutoLayout
        name={contentNodeName}
        width={width}
        height={height}
        direction={direction}
        horizontalAlignItems={horizontalAlignItems}
        verticalAlignItems={verticalAlignItems}
        spacing={spacing}
        padding={padding}
        x={0}
        y={0}
      >
        {
          itemList.map((node, index) => {
            let maxItems = overflowOptions.maxItems;
            if (!overflowOptions.limitContent) {
              return getAutolayoutItem(node, index);
            } else if (index < maxItems) {
              if (maxItems < itemList.length && index == maxItems - 1 && overflowOptions.nodeName) {
                return getAutolayoutItem(overflowOptions, index);
              }
              else {
                return getAutolayoutItem(node, index);
              }
            }
          })
        }
      </AutoLayout>
    );
  } else {
    return (
      <Frame
        name={contentNodeName}
        width={width}
        height={height}
        x={0}
        y={0}
      >
        {
          itemList.map((node, index) => {
            let maxItems = overflowOptions.maxItems;
            if (!overflowOptions.limitContent) {
              return getItem(node, index, layoutState);
            } else if (index < maxItems) {
              if (maxItems < itemList.length && index == maxItems - 1 && overflowOptions.nodeName) {
                return getItem(overflowOptions, index, layoutState);
              }
              else {
                return getItem(node, index, layoutState);
              }
            }
          })
        }
      </Frame>
    );
  }
}

widget.register(ListPreview)
