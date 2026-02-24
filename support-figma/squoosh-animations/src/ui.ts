let mainContainer: HTMLDivElement,
    singleNodeView: HTMLDivElement,
    multiNodeView: HTMLDivElement,
    nodeList: HTMLDivElement,
    overrideType: HTMLSelectElement,
    customFields: HTMLDivElement,
    animationType: HTMLSelectElement,
    smoothFields: HTMLDivElement,
    keyframeFields: HTMLDivElement,
    easing: HTMLSelectElement,
    bezierFields: HTMLDivElement,
    repeatType: HTMLSelectElement,
    repeatCountFields: HTMLDivElement,
    keyframesContainer: HTMLDivElement,
    addKeyframeButton: HTMLButtonElement,
    warningDiv: HTMLDivElement,
    playButton: HTMLButtonElement,
    timelineView: HTMLDivElement,
    propertyInspector: HTMLDivElement,
    previewLabel: HTMLHeadingElement;

let fabricCanvas: fabric.Canvas;
let currentKeyframeIndex: number;

const timelineState: {
  keyframes: any[];
  totalDuration: number;
  padding: number;
  trackHeight: number;
  markerRadius: number;
} = {
  keyframes: [],
  totalDuration: 0,
  padding: 0,
  trackHeight: 40,
  markerRadius: 5,
};

function renderTimeline() {
  const canvas = document.getElementById('timeline-canvas') as HTMLCanvasElement;
  if (!canvas) return;
  const ctx = canvas.getContext('2d')!;

  canvas.width = canvas.clientWidth;
  canvas.height = canvas.clientHeight;

  const { padding, trackHeight, markerRadius } = timelineState;
  const trackY = (canvas.height - trackHeight) / 2;
  const availableWidth = canvas.width - (padding * 2);
  const pixelsPerMs = timelineState.totalDuration > 0 ? availableWidth / timelineState.totalDuration : 0;

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Dynamic Tick Interval Calculation
  let tickInterval = 100;
  if (timelineState.totalDuration > 0) {
      const minPixelsPerTick = 60;
      const maxTicks = Math.max(2, availableWidth / minPixelsPerTick);
      const minInterval = timelineState.totalDuration / maxTicks;
      const magnitude = Math.pow(10, Math.floor(Math.log10(minInterval)));
      const residual = minInterval / magnitude;
      let niceResidual = residual > 5 ? 10 : (residual > 2 ? 5 : (residual > 1 ? 2 : 1));
      tickInterval = niceResidual * magnitude;
  }

  // Draw time ruler
  for (let t = 0; t <= timelineState.totalDuration; t += tickInterval) {
      const x = padding + t * pixelsPerMs;
      ctx.fillStyle = '#ccc';
      ctx.fillRect(x, trackY - 10, 1, 5);
      ctx.fillStyle = '#666';
      ctx.font = '10px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(`${Math.round(t)}`, x, trackY - 15);
  }

  // Draw track background
  ctx.fillStyle = '#f0f0f0';
  ctx.fillRect(padding, trackY, availableWidth, trackHeight);

  // Draw delay and duration bars
  timelineState.keyframes.forEach((keyframe) => {
      const segmentStartX = padding + keyframe.startTime * pixelsPerMs;
      if (keyframe.initialDelay > 0) {
          ctx.fillStyle = '#e0e0e0';
          ctx.fillRect(segmentStartX, trackY, keyframe.initialDelay * pixelsPerMs, trackHeight);
      }
      if (keyframe.duration > 0) {
          const durationStartX = padding + (keyframe.startTime + keyframe.initialDelay) * pixelsPerMs;
          ctx.fillStyle = `rgba(70, 130, 180, 0.7)`;
          ctx.fillRect(durationStartX, trackY, keyframe.duration * pixelsPerMs, trackHeight);
      }
  });

  // Draw keyframe markers and labels
  timelineState.keyframes.forEach(keyframe => {
      const x = padding + keyframe.startTime * pixelsPerMs;
      const y = trackY + trackHeight / 2;
      keyframe.x = x;
      keyframe.y = y;

      ctx.fillStyle = 'steelblue';
      ctx.beginPath();
      ctx.arc(x, y, markerRadius, 0, 2 * Math.PI);
      ctx.fill();

      ctx.fillStyle = '#333';
      ctx.font = '11px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(keyframe.name, x, y + markerRadius + 15);
  });

  // Align table column widths
  timelineState.keyframes.forEach((keyframe, index) => {
      const columnHeader = document.getElementById(`variant-col-${keyframe.id}`);
      if (columnHeader) {
          let segmentDuration = (index < timelineState.keyframes.length - 1)
              ? timelineState.keyframes[index + 1].startTime - keyframe.startTime
              : keyframe.initialDelay + keyframe.duration;
          
          columnHeader.style.width = (timelineState.totalDuration === 0)
              ? `${100 / timelineState.keyframes.length}%`
              : `${segmentDuration * pixelsPerMs}px`;
          columnHeader.style.minWidth = '80px';
      }
  });
}

function renderPropertyInspector(variants: any[], changedNodes: any) {
  if (!changedNodes || Object.keys(changedNodes).length === 0) {
    propertyInspector.innerHTML = '<div style="padding: 10px;">No property changes detected.</div>';
    return;
  }

  const editableProperties = new Set(['width', 'height', 'rotation', 'x', 'y', 'initialDelay', 'duration']);
  let tableHTML = `<table class="property-table"><thead><tr><th></th><td colspan="${variants.length}" id="timeline-cell"><div id="timeline-canvas-container"><canvas id="timeline-canvas"></canvas></div></td></tr><tr><th>Property</th>`;
  variants.forEach(v => { tableHTML += `<th id="variant-col-${v.id}">${v.name}</th>`; });
  tableHTML += '</tr></thead><tbody>';

  (Object.keys(changedNodes).map(key => [key, changedNodes[key]]) as [string, any][]).forEach(([nodeKey, nodeData]: [string, any], i) => {
    const groupId = `node-group-${i}`;
    tableHTML += `<tr class="node-header-row" data-group-id="${groupId}"><td colspan="${variants.length + 1}"><span class="arrow">&#9662;</span> ${nodeData.nodeName}</td></tr>`;
    (Object.keys(nodeData.properties).map(key => [key, nodeData.properties[key]]) as [string, any][]).forEach(([propName, values]: [string, any]) => {
      tableHTML += `<tr class="property-row ${groupId}"><td class="property-name-cell">${propName}</td>`;
      variants.forEach(v => {
        const value = values[v.id];
        const isEditable = editableProperties.has(propName);
        tableHTML += `<td ${isEditable ? `id="cell--${nodeKey}--${propName}--${v.id}"` : ''} class="${isEditable ? 'editable-cell' : ''}" data-value="${value}">${value !== undefined ? value : 'N/A'}</td>`;
      });
      tableHTML += '</tr>';
    });
  });

  propertyInspector.innerHTML = tableHTML + '</tbody></table>';
  renderTimeline();

  // --- Event Delegation for Table ---
  propertyInspector.addEventListener('click', (event) => {
    const header = (event.target as HTMLElement).closest('.node-header-row');
    if (header) {
      const groupId = (header as HTMLElement).dataset.groupId;
      const isCollapsed = header.classList.toggle('collapsed');
      header.querySelector('.arrow')!.innerHTML = isCollapsed ? '&#9656; ' : '&#9662; ';
      document.querySelectorAll(`.property-row.${groupId}`).forEach(row => {
        (row as HTMLElement).style.display = isCollapsed ? 'none' : 'table-row';
      });
      return;
    }

    const cell = (event.target as HTMLElement).closest('.editable-cell');
    if (cell && !cell.querySelector('input')) {
      const currentValue = (cell as HTMLElement).dataset.value!;
      cell.innerHTML = `<input type="number" value="${currentValue}" style="width: 90%;" />`;
      const input = cell.querySelector('input')!;
      input.focus();
      input.select();

      const handleUpdate = () => {
        const newValue = parseFloat(input.value);
        cell.innerHTML = newValue.toString();
        (cell as HTMLElement).dataset.value = newValue.toString();
        if (newValue !== parseFloat(currentValue)) {
          const [, nodeKey, propName, variantId] = cell.id.split('--');
          parent.postMessage({ pluginMessage: { type: 'update-property', nodeKey, propName, variantId, newValue }}, '*');
        }
      };
      input.addEventListener('blur', handleUpdate);
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') handleUpdate();
        else if (e.key === 'Escape') cell.innerHTML = currentValue;
      });
    }
  });

  const timelineCanvas = document.getElementById('timeline-canvas') as HTMLCanvasElement;
  if (timelineCanvas) {
    timelineCanvas.addEventListener('click', (event) => {
      const rect = timelineCanvas.getBoundingClientRect();
      const mouseX = event.clientX - rect.left;
      const mouseY = event.clientY - rect.top;
      timelineState.keyframes.forEach(keyframe => {
          const distance = Math.sqrt(Math.pow(mouseX - keyframe.x, 2) + Math.pow(mouseY - keyframe.y, 2));
          if (distance < timelineState.markerRadius + 2) {
              renderNodeTree(keyframe.nodeTree, fabricCanvas);
              parent.postMessage({ pluginMessage: { type: 'get-variant-animation-data', id: keyframe.id } }, '*');
          }
      });
    });
  }
}

function toggleOverrideFields() { customFields.style.display = overrideType.value === 'Custom' ? 'block' : 'none'; }
function toggleAnimationTypeFields() {
  smoothFields.style.display = animationType.value === 'Smooth' ? 'block' : 'none';
  keyframeFields.style.display = animationType.value === 'KeyFrame' ? 'block' : 'none';
}
function toggleBezierFields() { bezierFields.style.display = easing.value === 'Bezier' ? 'block' : 'none'; }
function toggleRepeatCountFields() { repeatCountFields.style.display = repeatType.value === 'Repeat' ? 'block' : 'none'; }
function addKeyframe(value = 0.0, duration = 100) {
    const keyframeDiv = document.createElement('div');
    keyframeDiv.className = 'keyframe';
    keyframeDiv.innerHTML = `<label>Value:</label><input type="number" class="keyframe-value" value="${value}" step="0.1"><label>Duration (ms):</label><input type="number" class="keyframe-duration" value="${duration}"><span class="delete-keyframe">❌</span>`;
    keyframesContainer.appendChild(keyframeDiv);
    keyframeDiv.querySelector('.delete-keyframe')!.addEventListener('click', () => keyframeDiv.remove());
}

function getSpec() {
  const override = overrideType.value;
  if (override === 'Default') return { override: override, default: true };
  if (override === 'None') return { override: override, disable: true };
  const spec: any = { initial_delay: { secs: 0, nanos: parseInt((document.getElementById('initial-delay') as HTMLInputElement).value) * 1000000 }, animation: {} };
  if (animationType.value === 'Smooth') {
      let easingValue = (easing.value === 'Bezier') ? { Bezier: { p0: 0.0, p1: parseFloat((document.getElementById('p1') as HTMLInputElement).value), p2: parseFloat((document.getElementById('p2') as HTMLInputElement).value), p3: 1.0 } } : easing.value;
      spec.animation.Smooth = {
          duration: { secs: 0, nanos: parseInt((document.getElementById('duration') as HTMLInputElement).value) * 1000000 },
          repeat_type: repeatType.value === 'Repeat' ? { Repeat: parseInt((document.getElementById('repeat-count') as HTMLInputElement).value) } : repeatType.value,
          easing: easingValue,
          interrupt_type: (document.getElementById('interrupt-type') as HTMLSelectElement).value,
      };
  } else {
      const keyframes = Array.from(document.getElementsByClassName('keyframe')).map(el => ({
          value: parseFloat((el.getElementsByClassName('keyframe-value')[0] as HTMLInputElement).value),
          duration: { secs: 0, nanos: parseInt((el.getElementsByClassName('keyframe-duration')[0] as HTMLInputElement).value) * 1000000 }
      }));
      spec.animation.KeyFrame = { steps: keyframes, repeat_type: repeatType.value === 'Repeat' ? { Repeat: parseInt((document.getElementById('repeat-count') as HTMLInputElement).value) } : repeatType.value, interrupt_type: (document.getElementById('interrupt-type') as HTMLSelectElement).value };
  }
  if (spec.animation.Smooth && spec.animation.Smooth.interrupt_type === 'None') spec.animation.Smooth.interrupt_type = null;
  if (spec.animation.KeyFrame && spec.animation.KeyFrame.interrupt_type === 'None') spec.animation.KeyFrame.interrupt_type = null;
  return { override: override, spec: spec };
}



function populateAnimationForm(spec: any) {
  if (spec === null || spec.override === 'Default') {
      overrideType.value = 'Default';
  } else if (spec && spec.override === 'Custom') {
      overrideType.value = 'Custom';
      (document.getElementById('initial-delay') as HTMLInputElement).value = (spec.spec.initial_delay.nanos / 1000000).toString();
      
      if (spec.spec.animation.Smooth) {
          animationType.value = 'Smooth';
          const smooth = spec.spec.animation.Smooth;
          (document.getElementById('interrupt-type') as HTMLSelectElement).value = smooth.interrupt_type || 'None';
          (document.getElementById('duration') as HTMLInputElement).value = (smooth.duration.nanos / 1000000).toString();
          if (typeof smooth.repeat_type === 'object' && smooth.repeat_type.Repeat) {
              repeatType.value = 'Repeat';
              (document.getElementById('repeat-count') as HTMLInputElement).value = smooth.repeat_type.Repeat;
          } else {
              repeatType.value = smooth.repeat_type;
          }
          if (typeof smooth.easing === 'object' && smooth.easing.Bezier) {
              easing.value = 'Bezier';
              (document.getElementById('p1') as HTMLInputElement).value = smooth.easing.Bezier.p1;
              (document.getElementById('p2') as HTMLInputElement).value = smooth.easing.Bezier.p2;
          } else {
              easing.value = smooth.easing;
          }
      } else if (spec.spec.animation.KeyFrame) {
          animationType.value = 'KeyFrame';
          // Keyframe UI population logic would go here...
      }
  } else if (spec && spec.override === 'None') {
      overrideType.value = `None`;
  } else {
      overrideType.value = 'Default';
  }
  // Update the visibility of form fields based on the loaded data.
  toggleOverrideFields();
  toggleAnimationTypeFields();
  toggleBezierFields();
  toggleRepeatCountFields();
}

function multiplyMatrices(m1: number[][], m2: number[][]): number[][] {
  const [a1, c1, e1] = m1[0];
  const [b1, d1, f1] = m1[1];
  const [a2, c2, e2] = m2[0];
  const [b2, d2, f2] = m2[1];

  return [
    [a1 * a2 + c1 * b2, a1 * c2 + c1 * d2, a1 * e2 + c1 * f2 + e1],
    [b1 * a2 + d1 * b2, b1 * c2 + d1 * d2, b1 * e2 + d1 * f2 + f1],
  ];
}

function decomposeMatrix(matrix: number[][]) {
  const [a, c, e] = matrix[0];
  const [b, d, f] = matrix[1];

  const delta = a * d - b * c;

  let translateX = e;
  let translateY = f;
  let angle = 0;
  let scaleX = 1;
  let scaleY = 1;

  if (a !== 0 || b !== 0) {
    const r = Math.sqrt(a * a + b * b);
    angle = b > 0 ? Math.acos(a / r) : -Math.acos(a / r);
    scaleX = r;
    scaleY = delta / r;
  } else if (c !== 0 || d !== 0) {
    const s = Math.sqrt(c * c + d * d);
    angle = Math.PI / 2 - (d > 0 ? Math.acos(-c / s) : -Math.acos(c / s));
    scaleX = delta / s;
    scaleY = s;
  } else {
    // Matrix is zero, no transformation.
  }

  return {
    translateX: translateX,
    translateY: translateY,
    angle: angle * (180 / Math.PI), // Convert radians to degrees for Fabric.js
    scaleX: scaleX,
    scaleY: scaleY,
  };
}

function solidPaintToString(paint: SolidPaint): string {
  if (!paint || paint.type !== 'SOLID') return 'rgba(0,0,0,0)';
  const { r, g, b } = paint.color;
  const alpha = paint.opacity === undefined ? 1 : paint.opacity;
  const round = (c: number) => Math.round(c * 255);
  return `rgba(${round(r)}, ${round(g)}, ${round(b)}, ${alpha.toFixed(2)})`;
};

function renderNodeTree(figmaRootNode: any, canvas: fabric.Canvas) {
  canvas.clear();

  function renderNodeRecursive(figmaNode: any, parentTransform: number[][]) {
    if (!figmaNode.visible) return;

    const globalTransform = multiplyMatrices(parentTransform, figmaNode.transform);
    const transformProps = decomposeMatrix(globalTransform);

    let fabricObject: fabric.Object | undefined;

    if (figmaNode.type === 'RECTANGLE' || figmaNode.type === 'FRAME' || figmaNode.type === 'COMPONENT' || figmaNode.type === 'INSTANCE' || figmaNode.type === 'COMPONENT_SET') {
      const fillColor = (figmaNode.fills && figmaNode.fills.length > 0) ? solidPaintToString(figmaNode.fills[0]) : 'rgba(196, 196, 196, 0)';
      const strokeColor = (figmaNode.strokes && figmaNode.strokes.length > 0) ? solidPaintToString(figmaNode.strokes[0]) : 'rgba(0,0,0,0)';

      fabricObject = new fabric.Rect({
        width: figmaNode.width,
        height: figmaNode.height,
        fill: fillColor,
        stroke: strokeColor,
        strokeWidth: figmaNode.strokeWeight || 0,
        opacity: figmaNode.opacity,
        originX: 'left',
        originY: 'top',
      });
    }

    if (fabricObject) {
      fabricObject.set({
        left: transformProps.translateX,
        top: transformProps.translateY,
        angle: transformProps.angle,
        scaleX: transformProps.scaleX,
        scaleY: transformProps.scaleY,
      });
      (fabricObject as any).figmaNodeId = figmaNode.id;
      canvas.add(fabricObject);
    }

    if (figmaNode.children) {
      [...figmaNode.children].reverse().forEach(child => {
        renderNodeRecursive(child, globalTransform);
      });
    }
  }

  const identityMatrix: number[][] = [[1, 0, 0], [0, 1, 0]];
  renderNodeRecursive(figmaRootNode, identityMatrix);
  
  canvas.renderAll();
}

function animateSegment(fromIndex: number, toIndex: number) {
  const fromKeyframe = timelineState.keyframes[fromIndex];
  const toKeyframe = timelineState.keyframes[toIndex];
  const segmentDuration = toKeyframe.startTime - fromKeyframe.startTime;

  renderNodeTree(fromKeyframe.nodeTree, fabricCanvas);

  const toNodeMap = new Map<string, any>();
  function buildTargetMap(figmaNode: any, parentTransform: number[][]) {
    const globalTransform = multiplyMatrices(parentTransform, figmaNode.transform);
    const transformProps = decomposeMatrix(globalTransform);
    
    toNodeMap.set(figmaNode.id, {
      ...figmaNode,
      ...transformProps,
    });

    if (figmaNode.children) {
      figmaNode.children.forEach((child: any) => buildTargetMap(child, globalTransform));
    }
  }
  buildTargetMap(toKeyframe.nodeTree, [[1, 0, 0], [0, 1, 0]]);

  const objectsToAnimate = fabricCanvas.getObjects().length;
  let animationsCompleted = 0;

  fabricCanvas.getObjects().forEach((fabricObj: fabric.Object) => {
    const targetNode = toNodeMap.get((fabricObj as any).figmaNodeId);

    if (targetNode) {
      const animationProps = {
        left: targetNode.translateX,
        top: targetNode.translateY,
        angle: targetNode.angle,
        scaleX: targetNode.scaleX,
        scaleY: targetNode.scaleY,
        width: targetNode.width,
        height: targetNode.height,
        opacity: targetNode.opacity,
        fill: (targetNode.fills && targetNode.fills.length > 0) ? solidPaintToString(targetNode.fills[0]) : 'rgba(0,0,0,0)',
        stroke: (targetNode.strokes && targetNode.strokes.length > 0) ? solidPaintToString(targetNode.strokes[0]) : 'rgba(0,0,0,0)',
        strokeWidth: targetNode.strokeWeight || 0,
      };

      fabricObj.animate(animationProps, {
        duration: segmentDuration,
        onChange: fabricCanvas.renderAll.bind(fabricCanvas),
        onComplete: () => {
          animationsCompleted++;
          if (animationsCompleted === objectsToAnimate) {
            currentKeyframeIndex = toIndex;
            const nextIndex = (currentKeyframeIndex + 1) % timelineState.keyframes.length;
            
            if (nextIndex === 0) {
              renderNodeTree(timelineState.keyframes[timelineState.keyframes.length - 1].nodeTree, fabricCanvas);
              playButton.disabled = false;
              return;
            }
            animateSegment(currentKeyframeIndex, nextIndex);
          }
        },
        easing: fabric.util.ease.easeInOutQuad,
      });
    }
  });
}


