"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g = Object.create((typeof Iterator === "function" ? Iterator : Object).prototype);
    return g.next = verb(0), g["throw"] = verb(1), g["return"] = verb(2), typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (g && (g = 0, op[0] && (_ = 0)), _) try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [op[0] & 2, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};
var canvas = new fabric.Canvas('canvas');
var canvasContainer = document.getElementById('canvas-container');
var previousNodes = {};
var ANIMATION_DURATION = 300; // ms
var ANIMATION_DELAY = 0; // ms
window.onmessage = function (event) { return __awaiter(void 0, void 0, void 0, function () {
    var msg;
    return __generator(this, function (_a) {
        msg = event.data.pluginMessage;
        if (msg.type === 'selection') {
            setTimeout(function () {
                var newNodes = {};
                canvas.clear();
                if (msg.nodes.length > 0) {
                    var bounds = {
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
        return [2 /*return*/];
    });
}); };
function drawNode(node, bounds, newNodes, isRoot) {
    if (!node)
        return;
    var x = node.x, y = node.y, width = node.width, height = node.height, fills = node.fills, relativeTransform = node.relativeTransform;
    var path = isRoot ? "root" : node.name;
    updateBounds(bounds, x, y, width, height);
    var fabricObject;
    if (fills && fills.length > 0) {
        var fill = fills[0];
        if (fill.type === 'SOLID') {
            fabricObject = new fabric.Rect({
                width: width,
                height: height,
                fill: "rgba(".concat(fill.color.r * 255, ", ").concat(fill.color.g * 255, ", ").concat(fill.color.b * 255, ", ").concat(fill.opacity || 1, ")"),
                originX: 'left',
                originY: 'top',
            });
        }
    }
    if (fabricObject) {
        var newMatrix = relativeTransform;
        var newDecomposed = fabric.util.qrDecompose(newMatrix);
        if (previousNodes[path]) {
            var oldDecomposed = fabric.util.qrDecompose(previousNodes[path].relativeTransform);
            console.log("Animating node: ".concat(path));
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
                onChange: function () { return canvas.requestRenderAll(); },
                duration: ANIMATION_DURATION,
                easing: fabric.util.ease.easeInOutQuad
            });
        }
        else {
            console.log("Creating node: ".concat(path));
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
        node.children.forEach(function (child) {
            drawNode(child, bounds, newNodes, false);
        });
    }
}
function updateBounds(bounds, x, y, width, height) {
    if (x < bounds.minX)
        bounds.minX = x;
    if (y < bounds.minY)
        bounds.minY = y;
    if (x + width > bounds.maxX)
        bounds.maxX = x + width;
    if (y + height > bounds.maxY)
        bounds.maxY = y + height;
}
function zoomToFit(bounds) {
    var objectWidth = bounds.maxX - bounds.minX;
    var objectHeight = bounds.maxY - bounds.minY;
    var scaleX = canvasContainer.clientWidth / objectWidth;
    var scaleY = canvasContainer.clientHeight / objectHeight;
    var scale = Math.min(scaleX, scaleY) * 0.9;
    canvas.setZoom(scale);
    canvas.viewportTransform[4] = (canvasContainer.clientWidth - objectWidth * scale) / 2 - bounds.minX * scale;
    canvas.viewportTransform[5] = (canvasContainer.clientHeight - objectHeight * scale) / 2 - bounds.minY * scale;
    canvas.requestRenderAll();
}
