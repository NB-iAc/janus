const urlParams = new URLSearchParams(window.location.search);
const buildingId = urlParams.get("building_id");
const floors = parseInt(urlParams.get("floors"), 10);
const floorSelect = document.getElementById("floorSelect");
const canvas = document.getElementById("drawingCanvas");
const ctx = canvas.getContext("2d");
const metadataForm = document.getElementById("metadataForm");
let isDrawing = false;
let floorDrawings = {};  
let undoStack = {};  
let redoStack = {};
let currentFloor = 1;  
let startPoint = null;
let drawingMode = "line";
let currentShape = null;
let currentShapeIndex = null;
let linePointList = [];
const gridSize = 40;
const gridColor = "#d3d3d3";
let connectionMode = false;
let tempConnection = null;
let connections = [];
let selectedConnectionPoint = null;
let connectionPoints = [];
let placingEntranceNode = false;
let currentShapeForEntrance = null;
let previewShape = null;
let buildingData = {};  
let isPanning = false;
let panStart = { x: 0, y: 0 };
let canvasOffset = { x: 0, y: 0 };
let panModeActive = false; 
let hasUnsavedChanges = false;
function hideMetadataForm() {
    metadataForm.style.display = "none";
}
function validateMetadataForm() {
    return true;
}
function clearCanvas() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);  
}
function drawGrid() {
    ctx.strokeStyle = gridColor;
    ctx.lineWidth = 0.5;
    const adjustedGridSize = gridSize;
    const visibleStartX = -Math.max(0, canvasOffset.x) - adjustedGridSize;
    const visibleEndX = canvas.width / window.zoomLevel - Math.min(0, canvasOffset.x) + adjustedGridSize;
    const visibleStartY = -Math.max(0, canvasOffset.y) - adjustedGridSize;
    const visibleEndY = canvas.height / window.zoomLevel - Math.min(0, canvasOffset.y) + adjustedGridSize;
    ctx.lineWidth = 0.5 / Math.max(0.5, window.zoomLevel);
    let startX = Math.floor(visibleStartX / adjustedGridSize) * adjustedGridSize;
    while (startX <= visibleEndX) {
        ctx.beginPath();
        ctx.moveTo(startX, visibleStartY);
        ctx.lineTo(startX, visibleEndY);
        ctx.stroke();
        startX += adjustedGridSize;
    }
    let startY = Math.floor(visibleStartY / adjustedGridSize) * adjustedGridSize;
    while (startY <= visibleEndY) {
        ctx.beginPath();
        ctx.moveTo(visibleStartX, startY);
        ctx.lineTo(visibleEndX, startY);
        ctx.stroke();
        startY += adjustedGridSize;
    }
}
function redrawCanvas() {
    clearCanvas();
    ctx.save();
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    ctx.translate(centerX, centerY);
    ctx.scale(window.zoomLevel, window.zoomLevel);
    ctx.translate(-centerX, -centerY);
    ctx.translate(canvasOffset.x, canvasOffset.y);
    drawGrid();
    ctx.strokeStyle = "#000000";
    ctx.lineWidth = 2;
    (floorDrawings[currentFloor] || []).forEach((shape, index) => {
        if (shape.metadata?.category === 'STAIR' ||
            shape.metadata?.category === 'ELEVATOR' ||
            shape.metadata?.category === 'ESCALATOR') {
            ctx.strokeStyle = "#000000";
            ctx.lineWidth = 2;
            ctx.setLineDash([]);
        } else {
            ctx.strokeStyle = "#85b1ff";
            ctx.lineWidth = 2;
            ctx.setLineDash([]);
        }
        ctx.beginPath();
        if (shape.type === "line") {
            shape.linePointList.forEach((point, i) => {
                if (i === 0) ctx.moveTo(point.x, point.y);
                else ctx.lineTo(point.x, point.y);
            });
            ctx.closePath();
        } else if (shape.type === "square") {
            ctx.moveTo(shape.points[0].x, shape.points[0].y);
            ctx.lineTo(shape.points[1].x, shape.points[1].y);
            ctx.lineTo(shape.points[2].x, shape.points[2].y);
            ctx.lineTo(shape.points[3].x, shape.points[3].y);
            ctx.closePath();
        }
        ctx.stroke();
        if (shape.entranceNode) {
            ctx.fillStyle = shape.metadata?.category === 'elevation' ? "#FF0000" : "#00FF00";
            ctx.beginPath();
            ctx.arc(shape.entranceNode.x, shape.entranceNode.y, 5, 0, Math.PI * 2);
            ctx.fill();
        }
        if (shape.connectionPoints) {
            shape.connectionPoints.forEach(point => {
                ctx.fillStyle = "#FF00FF";
                ctx.beginPath();
                ctx.arc(point.x, point.y, 5, 0, Math.PI * 2);
                ctx.fill();
            });
        }
        function isPointInPolygon(x, y, polygon) {
            let inside = false;
            for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
                const xi = polygon[i].x, yi = polygon[i].y;
                const xj = polygon[j].x, yj = polygon[j].y;
                const intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
                if (intersect) inside = !inside;
            }
            return inside;
        }
        if (shape.metadata && shape.metadata.roomName) {
            if (shape.metadata.category.toLowerCase == "building") {
            } else {
                ctx.fillStyle = "#000";
                ctx.font = "12px Arial";
                ctx.textAlign = "center";
                ctx.textBaseline = "middle";
                let centerX, centerY;
                if (shape.type === "line") {
                    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
                    const points = shape.linePointList;
                    for (let i = 0; i < points.length; i++) {
                        minX = Math.min(minX, points[i].x);
                        minY = Math.min(minY, points[i].y);
                        maxX = Math.max(maxX, points[i].x);
                        maxY = Math.max(maxY, points[i].y);
                    }
                    const boxCenterX = (minX + maxX) / 2;
                    const boxCenterY = (minY + maxY) / 2;
                    if (isPointInPolygon(boxCenterX, boxCenterY, points)) {
                        centerX = boxCenterX;
                        centerY = boxCenterY;
                    } else {
                        if (shape.entranceNode) {
                            centerX = shape.entranceNode.x;
                            centerY = shape.entranceNode.y;
                        } else if (points.length > 0) {
                            const midIndex = Math.floor(points.length / 2);
                            centerX = points[midIndex].x;
                            centerY = points[midIndex].y;
                        }
                        const offsetX = boxCenterX > centerX ? 10 : -10;
                        const offsetY = boxCenterY > centerY ? 10 : -10;
                        if (centerX + offsetX > minX && centerX + offsetX < maxX) {
                            centerX += offsetX * 0.5;
                        }
                        if (centerY + offsetY > minY && centerY + offsetY < maxY) {
                            centerY += offsetY * 0.5;
                        }
                    }
                } else if (shape.type === "square") {
                    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
                    for (let i = 0; i < shape.points.length; i++) {
                        minX = Math.min(minX, shape.points[i].x);
                        minY = Math.min(minY, shape.points[i].y);
                        maxX = Math.max(maxX, shape.points[i].x);
                        maxY = Math.max(maxY, shape.points[i].y);
                    }
                    centerX = (minX + maxX) / 2;
                    centerY = (minY + maxY) / 2;
                }
                const textSize = Math.max(8, Math.min(12, 12 / window.zoomLevel));
                ctx.font = `${textSize}px Arial`;
                const textWidth = ctx.measureText(shape.metadata.roomName).width;
                const textHeight = textSize + 2;
                const padding = 3;
                ctx.fillStyle = "rgba(255, 255, 255, 0.7)";
                ctx.fillRect(
                    centerX - textWidth/2 - padding, 
                    centerY - textHeight/2 - padding,
                    textWidth + padding*2, 
                    textHeight + padding*2
                );
                ctx.fillStyle = "#000";
                if (shape.importedFrom) {
                    ctx.fillText(`${shape.metadata.roomName} (Floor ${shape.importedFrom})`, centerX, centerY);
                } else {
                    ctx.fillText(shape.metadata.roomName, centerX, centerY);
                }
            }
        }
    });
    const floorConnectionPoints = connectionPoints.filter(point => point.floor === currentFloor);
    floorConnectionPoints.forEach(point => {
        const nodeSize = Math.max(3, Math.min(5, 5 / window.zoomLevel));
        ctx.fillStyle = point.isElevation ? "#FF0000" : "#FF00FF";
        ctx.beginPath();
        ctx.arc(point.x, point.y, nodeSize, 0, Math.PI * 2);
        ctx.fill();
    });
    const floorConnections = connections.filter(conn => 
        conn.start?.floor === currentFloor && 
        conn.end?.floor === currentFloor
    );
    ctx.setLineDash([5, 5]);
    floorConnections.forEach(conn => {
        ctx.beginPath();
        ctx.moveTo(conn.start.x, conn.start.y);
        ctx.lineTo(conn.end.x, conn.end.y);
        ctx.stroke();
    });
    ctx.setLineDash([]);
    if (tempConnection) {
        ctx.setLineDash([5, 5]);
        ctx.beginPath();
        ctx.moveTo(tempConnection.start.x, tempConnection.start.y);
        ctx.lineTo(tempConnection.end.x, tempConnection.end.y);
        ctx.stroke();
        ctx.setLineDash([]);
    }
    if (previewShape) {
        ctx.strokeStyle = "#808080";
        ctx.lineWidth = 2;
        ctx.beginPath();
        if (previewShape.type === "line") {
            ctx.moveTo(previewShape.start.x, previewShape.start.y);
            ctx.lineTo(previewShape.end.x, previewShape.end.y);
        } else if (previewShape.type === "square") {
            ctx.rect(previewShape.start.x, previewShape.start.y, previewShape.width, previewShape.height);
        }
        ctx.stroke();
    }
    if (linePointList.length > 0) {
        ctx.strokeStyle = "#000000";
        ctx.lineWidth = 2;          
        ctx.beginPath();
        linePointList.forEach((point, i) => {
            if (i === 0) ctx.moveTo(point.x, point.y);
            else ctx.lineTo(point.x, point.y);
        });
        ctx.stroke();
    }
    ctx.restore();
}
function snapToGrid(value) {
    return Math.round(value / gridSize) * gridSize;
}
function showLoading(isLoading) {
    const loadingIndicator = document.getElementById("loadingIndicator") || createLoadingIndicator();
    loadingIndicator.style.display = isLoading ? "flex" : "none";
}
function createLoadingIndicator() {
    const indicator = document.createElement("div");
    indicator.id = "loadingIndicator";
    indicator.innerHTML = '<div class="spinner"></div><span>Loading...</span>';
    indicator.style.cssText = "position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;flex-direction:column;justify-content:center;align-items:center;color:white;z-index:1000;";
    document.body.appendChild(indicator);
    return indicator;
}
function showErrorMessage(message) {
    alert(message);
}
function showSuccessMessage(message) {
    alert(message);
}
function updateFloorNames() {
    floorSelect.innerHTML = '';
    for (let i = 1; i <= buildingData.total_floors || floors; i++) {
        const option = document.createElement("option");
        option.value = i;
        option.textContent = floorNames[i] || `Floor ${i}`;
        floorSelect.appendChild(option);
    }
    floorSelect.value = currentFloor;
}
document.addEventListener('DOMContentLoaded', function() {
    initializeElevationConnectionModal();
    const exportButton = document.getElementById('exportPDFButton');
    if (exportButton) {
        exportButton.addEventListener('click', exportFloorPlansToPDF);
    }
    if (!window.jspdf) {
        const script = document.createElement('script');
        script.src = 'https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js';
        document.head.appendChild(script);
    }
});
function applyIdMapping(idMapping) {
    if (!idMapping) return;
    connectionPoints.forEach(point => {
        const realId = idMapping[point.id];
        if (realId) point.id = realId;
    });
    Object.keys(floorDrawings).forEach(floorNum => {
        floorDrawings[floorNum].forEach(drawing => {
            const realId = idMapping[drawing.id];
            if (realId) {
                drawing.id = realId;
            }
            if (drawing.entranceNode) {
                if (drawing.entranceNode.id) {
                    const realNodeId = idMapping[drawing.entranceNode.id];
                    if (realNodeId) {
                        drawing.entranceNode.id = realNodeId;
                    }
                } else {
                }
            }
        });
    });
    connections.forEach(conn => {
        const realId = idMapping[conn.id];
        if (realId) conn.id = realId;
        if (conn.start && conn.start.id) {
            const realStartId = idMapping[conn.start.id];
            if (realStartId) conn.start.id = realStartId;
        }
        if (conn.end && conn.end.id) {
            const realEndId = idMapping[conn.end.id];
            if (realEndId) conn.end.id = realEndId;
        }
    });
}
function processElevationConnections() {
    const elevations = {};
    Object.keys(floorDrawings).forEach(floorNum => {
        const floor = parseInt(floorNum);
        floorDrawings[floor].forEach(shape => {
            if ((shape.metadata?.category === 'STAIR' || 
                 shape.metadata?.category === 'ELEVATOR' || 
                 shape.metadata?.category === 'ESCALATOR') && 
                shape.entranceNode && 
                shape.metadata?.roomName) {
                if (!elevations[shape.metadata.category]) {
                    elevations[shape.metadata.category] = {};
                }
                if (!elevations[shape.metadata.category][shape.metadata.roomName]) {
                    elevations[shape.metadata.category][shape.metadata.roomName] = [];
                }
                elevations[shape.metadata.category][shape.metadata.roomName].push({
                    floor: floor,
                    shape: shape
                });
            }
        });
    });
    Object.keys(elevations).forEach(category => {
        Object.keys(elevations[category]).forEach(name => {
            const sameNameElevations = elevations[category][name];
            if (sameNameElevations.length >= 2) {
                for (let i = 0; i < sameNameElevations.length; i++) {
                    for (let j = i + 1; j < sameNameElevations.length; j++) {
                        const elev1 = sameNameElevations[i];
                        const elev2 = sameNameElevations[j];
                        const connectionExists = connections.some(conn => 
                            (conn.start?.id === elev1.shape.entranceNode.id && conn.end?.id === elev2.shape.entranceNode.id) ||
                            (conn.start?.id === elev2.shape.entranceNode.id && conn.end?.id === elev1.shape.entranceNode.id)
                        );
                        if (!connectionExists) {
                            const conn1 = window.FloorPlanModels.createConnection(
                                elev1.shape.entranceNode,
                                elev2.shape.entranceNode,
                                category,
                                {
                                    elevationName: name,
                                    startFloor: elev1.floor,
                                    endFloor: elev2.floor,
                                    elevationType: category
                                }
                            );
                            const conn2 = window.FloorPlanModels.createConnection(
                                elev2.shape.entranceNode,
                                elev1.shape.entranceNode,
                                category,
                                {
                                    elevationName: name,
                                    startFloor: elev2.floor,
                                    endFloor: elev1.floor,
                                    elevationType: category
                                }
                            );
                            connections.push(conn1);
                            connections.push(conn2);
                            if (!elev1.shape.metadata.connectedFloors) {
                                elev1.shape.metadata.connectedFloors = [];
                            }
                            if (!elev2.shape.metadata.connectedFloors) {
                                elev2.shape.metadata.connectedFloors = [];
                            }
                            if (!elev1.shape.metadata.connectedFloors.includes(elev2.floor)) {
                                elev1.shape.metadata.connectedFloors.push(elev2.floor);
                            }
                            if (!elev2.shape.metadata.connectedFloors.includes(elev1.floor)) {
                                elev2.shape.metadata.connectedFloors.push(elev1.floor);
                            }
                        }
                    }
                }
            }
        });
    });
}
function displayFloor(floorNumber) {
    if (!floorDrawings[floorNumber] && floorNumber > 0 && floorNumber <= floors) {
        floorDrawings[floorNumber] = [];
    }
    currentFloor = floorNumber;
    floorSelect.value = floorNumber;
    redrawCanvas();
}
function saveDrawing() {
    showLoading(true);
    Object.keys(floorDrawings).forEach(floorNum => {
        floorDrawings[floorNum].forEach(drawing => {
            if (drawing.entranceNode && !drawing.entranceNode.id) {
                const isElevation = drawing.metadata?.category === 'STAIR' || 
                                   drawing.metadata?.category === 'ELEVATOR' || 
                                   drawing.metadata?.category === 'ESCALATOR';
                const fixedNode = window.FloorPlanModels.createNode(
                    drawing.entranceNode.x,
                    drawing.entranceNode.y,
                    isElevation,
                    parseInt(floorNum)
                );
                drawing.entranceNode = fixedNode;
                connectionPoints.push(fixedNode);
            }
        });
    });
    const drawingData = {
        building_data: buildingData,
        floorDrawings: floorDrawings,
        connections: connections,
        connection_points: connectionPoints
    };
    const apiData = window.FloorPlanTransformer.drawingToApiFormat(drawingData);
    window.apiClient.saveBuildingData(buildingId, apiData)
        .then(response => {
            showLoading(false);
            if (response.success) {
                showSuccessMessage(response.message || "Building data saved successfully!");
                hasUnsavedChanges = false;
                if (response.id_mapping) {
                    applyIdMapping(response.id_mapping);
                }
            } else {
                showErrorMessage(response.error || "Error saving building data");
            }
        })
        .catch(err => {
            showLoading(false);
            showErrorMessage("Error saving building data: " + err);
        });
}
function initializeCanvasEvents() {
    canvas.addEventListener("mousedown", handleMouseDown);
    canvas.addEventListener("mousemove", handleMouseMove);
    canvas.addEventListener("mouseup", handleMouseUp);
    canvas.addEventListener("contextmenu", handleContextMenu);
    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('keyup', handleKeyUp);
}
function handleMouseMove(e) {
    if (isPanning) {
        const dx = e.clientX - panStart.x;
        const dy = e.clientY - panStart.y;
        canvasOffset.x = Math.round(canvasOffset.x + dx / window.zoomLevel);
        canvasOffset.y = Math.round(canvasOffset.y + dy / window.zoomLevel);
        panStart = { x: e.clientX, y: e.clientY };
        redrawCanvas();
        return;
    }
    const coords = screenToCanvasCoords(e.clientX, e.clientY);
    const x = coords.x;
    const y = coords.y;
    if (drawingMode === "connection" && selectedConnectionPoint) {
        tempConnection.end = { x, y };
        redrawCanvas();
        return;
    }
    if (!isDrawing) return;
    const endPoint = {
        x: snapToGrid(x),
        y: snapToGrid(y)
    };
    if (drawingMode === "line") {
        previewShape.end = endPoint;
    } else if (drawingMode === "square") {
        previewShape.width = endPoint.x - startPoint.x;
        previewShape.height = endPoint.y - startPoint.y;
    }
    redrawCanvas();
}
function handleMouseUp(e) {
    if (isPanning) {
        isPanning = false;
        canvas.style.cursor = panModeActive ? "grab" : "default";
        return;
    }
    if (!isDrawing) return;
    isDrawing = false;
    const coords = screenToCanvasCoords(e.clientX, e.clientY);
    const endPoint = {
        x: snapToGrid(coords.x),
        y: snapToGrid(coords.y)
    };
    if (!floorDrawings[currentFloor]) floorDrawings[currentFloor] = [];
    if (!undoStack[currentFloor]) undoStack[currentFloor] = [];
    if (!redoStack[currentFloor]) redoStack[currentFloor] = [];
    saveStateForUndo();
    if (drawingMode === "line") {
        if (linePointList.length === 0) {
            linePointList.push(startPoint);
        }
        linePointList.push(endPoint);
        const firstPoint = linePointList[0];
        const currentPoint = linePointList[linePointList.length - 1];
        if (linePointList.length >= 3 && 
            Math.abs(firstPoint.x - currentPoint.x) < 5 && 
            Math.abs(firstPoint.y - currentPoint.y) < 5) {
            floorDrawings[currentFloor].push({
                type: "line",
                linePointList: [...linePointList],
                metadata: null
            });
            hasUnsavedChanges = true;
            currentShapeIndex = floorDrawings[currentFloor].length - 1;
            linePointList = [];
            showMetadataForm();
        }
    } else if (drawingMode === "square") {
        const x1 = Math.min(startPoint.x, endPoint.x);
        const y1 = Math.min(startPoint.y, endPoint.y);
        const x2 = Math.max(startPoint.x, endPoint.x);
        const y2 = Math.max(startPoint.y, endPoint.y);
        const width = x2 - x1;
        const height = y2 - y1;
        if (width > 0 && height > 0) {
            floorDrawings[currentFloor].push({
                type: "square",
                points: [
                    { x: x1, y: y1 },
                    { x: x2, y: y1 },
                    { x: x2, y: y2 },
                    { x: x1, y: y2 }
                ],
                metadata: null
            });
            hasUnsavedChanges = true;
            currentShapeIndex = floorDrawings[currentFloor].length - 1;
            showMetadataForm();
        }
    }
    previewShape = null;
    redrawCanvas();
    currentShape = null;
}
function handleContextMenu(e) {
    e.preventDefault();
    if (!connectionMode) return;
    const coords = screenToCanvasCoords(e.clientX, e.clientY);
    const x = coords.x;
    const y = coords.y;
    let deleted = false;
    for (let i = connections.length - 1; i >= 0; i--) {
        const conn = connections[i];
        if (conn.start.floor !== currentFloor || conn.end.floor !== currentFloor) {
            continue;
        }
        const dx = conn.end.x - conn.start.x;
        const dy = conn.end.y - conn.start.y;
        const length = Math.sqrt(dx * dx + dy * dy);
        const t = ((x - conn.start.x) * dx + (y - conn.start.y) * dy) / (length * length);
        if (t < 0 || t > 1) continue;
        const proj = {
            x: conn.start.x + t * dx,
            y: conn.start.y + t * dy
        };
        const dist = Math.sqrt((x - proj.x) ** 2 + (y - proj.y) ** 2);
        if (dist < 10 / window.zoomLevel) {
            saveStateForUndo();
            connections.splice(i, 1);
            deleted = true;
            break;
        }
    }
    if (deleted) {
        redrawCanvas();
    }
}
initializeZoomControls();
function handleMouseDown(e) {
    if (e.button !== 0) return;
    const coords = screenToCanvasCoords(e.clientX, e.clientY);
    const x = coords.x;
    const y = coords.y;
    if (panModeActive || e.shiftKey) {
        isPanning = true;
        panStart = { x: e.clientX, y: e.clientY };
        canvas.style.cursor = "grabbing";
        return;
    }
    if (drawingMode === "pointer") {
        if (trySelectExistingShape(x, y)) {
            return;
        }
    }
    if (placingEntranceNode) {
        if (!currentShapeForEntrance.metadata) {
            alert("Please fill in the room details before placing the entrance node.");
            return;
        }
        currentShapeForEntrance.entranceNode = window.FloorPlanModels.createNode(
            x,
            y,
            currentShapeForEntrance.metadata?.category === 'STAIR' || 
            currentShapeForEntrance.metadata?.category === 'ELEVATOR' ||
            currentShapeForEntrance.metadata?.category === 'ESCALATOR',
            currentFloor
        );
        const newNode = window.FloorPlanModels.createNode(
            x,
            y,
            currentShapeForEntrance.metadata?.category === 'STAIR' || 
            currentShapeForEntrance.metadata?.category === 'ELEVATOR' ||
            currentShapeForEntrance.metadata?.category === 'ESCALATOR',
            currentFloor
        );
        currentShapeForEntrance.entranceNode = newNode;
        connectionPoints.push(newNode);
        placingEntranceNode = false;
        currentShapeForEntrance = null;
        redrawCanvas();
        return;
    }
    if (drawingMode === "connection") {
        handleConnectionModeClick(x, y);
        return;
    }
    if (trySelectExistingShape(x, y)) {
        return;
    }
    const snappedX = snapToGrid(x);
    const snappedY = snapToGrid(y);
    isDrawing = true;
    if (drawingMode === "line") {
        if (linePointList.length === 0) {
            linePointList.push({ x: snappedX, y: snappedY });
        }
        startPoint = { x: snappedX, y: snappedY };
        previewShape = { type: "line", start: startPoint, end: startPoint };
    } else if (drawingMode === "square") {
        startPoint = { x: snappedX, y: snappedY };
        previewShape = { type: "square", start: startPoint, width: 0, height: 0 };
    }
    currentShape = { type: drawingMode, start: startPoint };
}
function handleConnectionModeClick(x, y) {
    let clickedPoint = null;
    (floorDrawings[currentFloor] || []).forEach(shape => {
        if (shape.entranceNode) {
            const dx = shape.entranceNode.x - x;
            const dy = shape.entranceNode.y - y;
            if (Math.sqrt(dx*dx + dy*dy) < 10) {
                clickedPoint = shape.entranceNode;
            }
        }
    });
    connectionPoints.forEach(point => {
        if (point.floor === currentFloor) {
            const dx = point.x - x;
            const dy = point.y - y;
            if (Math.sqrt(dx*dx + dy*dy) < 10) {
                clickedPoint = point;
            }
        }
    });
    if (!undoStack[currentFloor]) undoStack[currentFloor] = [];
    undoStack[currentFloor].push({
        drawings: JSON.parse(JSON.stringify(floorDrawings[currentFloor] || [])),
        linePoints: [...linePointList],
        connections: JSON.parse(JSON.stringify(connections)),
        connectionPoints: JSON.parse(JSON.stringify(connectionPoints))
    });
    if (!redoStack[currentFloor]) redoStack[currentFloor] = [];
    redoStack[currentFloor] = [];
    if (clickedPoint) {
        if (!selectedConnectionPoint) {
            selectedConnectionPoint = clickedPoint;
            tempConnection = { start: clickedPoint, end: clickedPoint };
        } else {
            connections.push(
                window.FloorPlanModels.createConnection(
                    selectedConnectionPoint,
                    clickedPoint
                )
            );
            selectedConnectionPoint = null;
            tempConnection = null;
        }
    } else {
        connectionPoints.push(
            window.FloorPlanModels.createNode(
                x,
                y,
                false,
                currentFloor
            )
        );
    }
    redrawCanvas();
}
function handleKeyDown(e) {
    if (e.shiftKey && !panModeActive) {
        canvas.classList.add('pan-cursor');
    }
}
function handleKeyUp(e) {
    if (!e.shiftKey && !panModeActive) {
        canvas.classList.remove('pan-cursor');
    }
}
initializeCanvasEvents();
function initializeUndoRedoHandlers() {
    document.getElementById("undoButton").addEventListener("click", handleUndo);
    document.getElementById("redoButton").addEventListener("click", handleRedo);
}
function handleUndo() {
    if (!undoStack[currentFloor] || undoStack[currentFloor].length === 0) {
        return;
    }
    if (!redoStack[currentFloor]) redoStack[currentFloor] = [];
    redoStack[currentFloor].push({
        drawings: JSON.parse(JSON.stringify(floorDrawings[currentFloor] || [])),
        linePoints: [...linePointList],
        connections: JSON.parse(JSON.stringify(connections)),
        connectionPoints: JSON.parse(JSON.stringify(connectionPoints))
    });
    const previousState = undoStack[currentFloor].pop();
    floorDrawings[currentFloor] = previousState.drawings;
    linePointList = previousState.linePoints;
    const currentConnections = connections.filter(conn => 
        conn.start.floor !== currentFloor && conn.end.floor !== currentFloor);
    const previousConnections = previousState.connections.filter(conn => 
        conn.start.floor === currentFloor || conn.end.floor === currentFloor);
    connections = [...currentConnections, ...previousConnections];
    const currentPoints = connectionPoints.filter(point => point.floor !== currentFloor);
    const previousPoints = previousState.connectionPoints.filter(point => 
        point.floor === currentFloor);
    connectionPoints = [...currentPoints, ...previousPoints];
    redrawCanvas();
    hasUnsavedChanges = true;
}
function handleRedo() {
    if (!redoStack[currentFloor] || redoStack[currentFloor].length === 0) {
        return;
    }
    if (!undoStack[currentFloor]) undoStack[currentFloor] = [];
    undoStack[currentFloor].push({
        drawings: JSON.parse(JSON.stringify(floorDrawings[currentFloor] || [])),
        linePoints: [...linePointList],
        connections: JSON.parse(JSON.stringify(connections)),
        connectionPoints: JSON.parse(JSON.stringify(connectionPoints))
    });
    const nextState = redoStack[currentFloor].pop();
    floorDrawings[currentFloor] = nextState.drawings;
    linePointList = nextState.linePoints;
    const currentConnections = connections.filter(conn => 
        conn.start.floor !== currentFloor && conn.end.floor !== currentFloor);
    const nextConnections = nextState.connections.filter(conn => 
        conn.start.floor === currentFloor || conn.end.floor === currentFloor);
    connections = [...currentConnections, ...nextConnections];
    const currentPoints = connectionPoints.filter(point => point.floor !== currentFloor);
    const nextPoints = nextState.connectionPoints.filter(point => 
        point.floor === currentFloor);
    connectionPoints = [...currentPoints, ...nextPoints];
    redrawCanvas();
    hasUnsavedChanges = true;
}
function saveStateForUndo() {
    if (!undoStack[currentFloor]) undoStack[currentFloor] = [];
    undoStack[currentFloor].push({
        drawings: JSON.parse(JSON.stringify(floorDrawings[currentFloor] || [])),
        linePoints: [...linePointList],
        connections: JSON.parse(JSON.stringify(connections)),
        connectionPoints: JSON.parse(JSON.stringify(connectionPoints))
    });
    if (!redoStack[currentFloor]) redoStack[currentFloor] = [];
    redoStack[currentFloor] = [];
}
initializeUndoRedoHandlers();
function initializeFloorManagement() {
    floorSelect.addEventListener("change", handleFloorChange);
    document.getElementById("renameFloorButton").addEventListener("click", handleRenameFloor);
    document.getElementById("addFloorButton").addEventListener("click", handleAddFloor);
    document.getElementById("clearFloorButton").addEventListener("click", handleClearFloor);
    populateFloorSelector();
}
function populateFloorSelector() {
    floorSelect.innerHTML = '';
    const totalFloors = buildingData.total_floors || floors;
    for (let i = 1; i <= totalFloors; i++) {
        const option = document.createElement("option");
        option.value = i;
        const floorName = buildingData.floorNames?.[i] || `Floor ${i}`;
        option.textContent = floorName;
        floorSelect.appendChild(option);
    }
    floorSelect.value = currentFloor;
}
function handleFloorChange() {
    const newFloor = parseInt(floorSelect.value, 10);
    displayFloor(newFloor);
}
function displayFloor(floorNumber) {
    if (!floorDrawings[floorNumber] && floorNumber > 0) {
        floorDrawings[floorNumber] = [];
    }
    currentFloor = floorNumber;
    floorSelect.value = floorNumber;
    linePointList = [];
    previewShape = null;
    tempConnection = null;
    selectedConnectionPoint = null;
    redrawCanvas();
}
function handleRenameFloor() {
    const floorOption = document.querySelector(`#floorSelect option[value="${currentFloor}"]`);
    const currentName = floorOption ? floorOption.textContent : `Floor ${currentFloor}`;
    const newName = prompt("Enter new floor name:", currentName);
    if (newName && newName.trim() !== "") {
        if (floorOption) {
            floorOption.textContent = newName;
        }
        if (!buildingData.floorNames) buildingData.floorNames = {};
        buildingData.floorNames[currentFloor] = newName;
        if (window.apiClient && window.apiClient.updateFloorName) {
            window.apiClient.updateFloorName(currentFloor, currentName, newName, buildingId, buildingData.name)
        }
        hasUnsavedChanges = true;
    }
}
function handleAddFloor() {
    const totalFloors = (buildingData.total_floors || floors) + 1;
    const newFloorNumber = totalFloors;
    const newFloorId = FloorPlanModels.generateTempFloorId();
    buildingData.total_floors = totalFloors;
    if (!buildingData.floorIdMapping) {
        buildingData.floorIdMapping = {};
    }
    buildingData.floorIdMapping[newFloorNumber] = newFloorId;
    floorDrawings[newFloorNumber] = [];
    undoStack[newFloorNumber] = [];
    redoStack[newFloorNumber] = [];
    populateFloorSelector();
    displayFloor(newFloorNumber);
    alert(`Floor ${newFloorNumber} has been added. You are now editing this floor.`);
    hasUnsavedChanges = true;
}
function handleClearFloor() {
    if (confirm("Are you sure you want to clear all elements from this floor? This cannot be undone.")) {
        if (!undoStack[currentFloor]) undoStack[currentFloor] = [];
        undoStack[currentFloor].push({
            drawings: JSON.parse(JSON.stringify(floorDrawings[currentFloor] || [])),
            linePoints: [...linePointList],
            connections: JSON.parse(JSON.stringify(connections)),
            connectionPoints: JSON.parse(JSON.stringify(connectionPoints))
        });
        if (!redoStack[currentFloor]) redoStack[currentFloor] = [];
        redoStack[currentFloor] = [];
        floorDrawings[currentFloor] = [];
        connections = connections.filter(conn => {
            return !(conn.start?.floor === currentFloor || conn.end?.floor === currentFloor);
        });
        connectionPoints = connectionPoints.filter(point => point.floor !== currentFloor);
        linePointList = [];
        redrawCanvas();
        hasUnsavedChanges = true;
    }
}
initializeFloorManagement();
function initializeMetadataForm() {
    document.getElementById("saveMetadata").addEventListener("click", handleSaveMetadata);
    document.getElementById("cancelMetadata").addEventListener("click", handleCancelMetadata);
    document.getElementById("category").addEventListener("change", handleCategoryChange);
    metadataForm.style.display = "none";
}
function showMetadataForm() {
    const categoryMapping = {
        'STAIR': { dropdown: 'elevation', subtype: 'stairs' },
        'ELEVATOR': { dropdown: 'elevation', subtype: 'elevator' },
        'ESCALATOR': { dropdown: 'elevation', subtype: 'escalator' },
        'UTILITY': { dropdown: 'utility', subtype: 'utilityType' },
        'BUILDING': { dropdown: 'building' },
        'WALL': { dropdown: 'wall' },
        'ROOM': { dropdown: 'room' }
    };
    document.getElementById("roomName").value = "";
    document.getElementById("category").value = "room";
    document.getElementById("roomCategory").value = "";
    document.getElementById("description").value = "";
    document.getElementById("owner").value = "";
    document.getElementById("email").value = "";
    document.getElementById("phone").value = "";
    if (document.getElementById("utilityType")) {
        document.getElementById("utilityType").value = "";
    }
    if (document.getElementById("elevationType")) {
        document.getElementById("elevationType").value = "elevator";
    }
    if (currentShapeIndex !== null && floorDrawings[currentFloor] && floorDrawings[currentFloor][currentShapeIndex]) {
        const shape = floorDrawings[currentFloor][currentShapeIndex];
        if (shape.metadata) {
            document.getElementById("roomName").value = shape.metadata.roomName || "";
            const category = shape.metadata.category ? shape.metadata.category.toUpperCase() : 'ROOM';
            const mapping = categoryMapping[category] || categoryMapping['ROOM'];
            document.getElementById("category").value = mapping.dropdown;
            const event = new Event('change');
            document.getElementById("category").dispatchEvent(event);
            if (mapping.dropdown === 'elevation') {
                document.getElementById("elevationType").value = mapping.subtype;
            }
            if (mapping.dropdown === 'utility' && document.getElementById("utilityType")) {
                document.getElementById("utilityType").value = shape.metadata.utilityType || "";
            }
            document.getElementById("roomCategory").value = shape.metadata.roomCategory || "";
            document.getElementById("description").value = shape.metadata.description || "";
            document.getElementById("owner").value = shape.metadata.owner || "";
            document.getElementById("email").value = shape.metadata.email || "";
            document.getElementById("phone").value = shape.metadata.phone || "";
            if (['STAIR', 'ELEVATOR', 'ESCALATOR'].includes(category) && 
                document.getElementById("floorCheckboxes")) {
                populateFloorCheckboxes(shape.metadata.connectedFloors || []);
            }
        }
    }
    metadataForm.style.display = "block";
}
function hideMetadataForm() {
    metadataForm.style.display = "none";
    document.getElementById("roomName").value = "";
    document.getElementById("category").value = "room";
    document.getElementById("roomCategory").value = "";
    document.getElementById("description").value = "";
    document.getElementById("owner").value = "";
    document.getElementById("email").value = "";
    document.getElementById("phone").value = "";
    if (document.getElementById("utilityType")) {
        document.getElementById("utilityType").value = "";
    }
    if (document.getElementById("elevationType")) {
        document.getElementById("elevationType").value = "elevator";
    }
}
function handleSaveMetadata() {
    if (!validateMetadataForm()) {
        return;
    }
    hasUnsavedChanges = true;
    const metadata = {
        roomName: document.getElementById("roomName").value,
        category: document.getElementById("category").value,
        roomCategory: document.getElementById("roomCategory").value,
        description: document.getElementById("description").value,
        owner: document.getElementById("owner").value,
        email: document.getElementById("email").value,
        phone: document.getElementById("phone").value,
        floor: currentFloor
    };
    if (metadata.category === 'elevation') {
        const elevationType = document.getElementById("elevationType").value.toUpperCase();
        if (elevationType === 'STAIRS') {
            metadata.category = 'STAIR';
        } else {
            metadata.category = elevationType;
        }
        if (document.getElementById("floorCheckboxes")) {
            metadata.connectedFloors = Array.from(
                document.querySelectorAll('#floorCheckboxes input:checked')
            ).map(cb => parseInt(cb.value));
        } else {
            metadata.connectedFloors = [];
        }
    } else if (metadata.category === 'utility') {
        metadata.utilityType = document.getElementById("utilityType").value;
    }
    if (currentShapeIndex !== null && floorDrawings[currentFloor] && floorDrawings[currentFloor][currentShapeIndex]) {
        const currentShape = floorDrawings[currentFloor][currentShapeIndex];
        const isExistingShape = currentShape.metadata !== null;
        if (!currentShape.id) {
            currentShape.id = window.FloorPlanModels.generateTempObjectId();
          }
        currentShape.metadata = metadata;
        if (currentShape.entranceNode) {
            currentShape.entranceNode.floor = currentFloor;
        }
        if (isExistingShape && currentShape.entranceNode) {
            if (currentShape.metadata.category === 'STAIR' || 
                currentShape.metadata.category === 'ELEVATOR' || 
                currentShape.metadata.category === 'ESCALATOR') {
                currentShape.entranceNode.isElevation = true;
            } else {
                currentShape.entranceNode.isElevation = false;
            }
            hideMetadataForm();
            redrawCanvas();
        } else {
            placingEntranceNode = true;
            currentShapeForEntrance = currentShape;
            hideMetadataForm();
            alert("Please click on the canvas to place the entrance node for this shape.");
        }
    }
    redrawCanvas();
}
function handleCancelMetadata() {
    if (currentShapeIndex !== null && 
        floorDrawings[currentFloor] && 
        floorDrawings[currentFloor][currentShapeIndex] && 
        !floorDrawings[currentFloor][currentShapeIndex].metadata) {
        floorDrawings[currentFloor].splice(currentShapeIndex, 1);
        currentShapeIndex = null;
        redrawCanvas();
    }
    hideMetadataForm();
}
function handleCategoryChange() {
    const category = document.getElementById("category").value;
    const utilityFields = document.getElementById("utilityFields");
    const elevationFields = document.getElementById("elevationFields");
    if (category === 'utility') {
        utilityFields.style.display = 'block';
        elevationFields.style.display = 'none';
    } else if (category === 'elevation') {
        utilityFields.style.display = 'none';
        elevationFields.style.display = 'block';
        populateFloorCheckboxes();
    } else {
        utilityFields.style.display = 'none';
        elevationFields.style.display = 'none';
    }
    const nonBuildingFields = document.querySelectorAll(
        "#roomCategory, #description, #owner, #email, #phone, " +
        "label[for='roomCategory'], label[for='description'], " +
        "label[for='owner'], label[for='email'], label[for='phone']"
    );
    if (category.toLowerCase === "building") {
        nonBuildingFields.forEach(field => {
            field.style.display = "none";
            if (field.hasAttribute("required")) {
                field.removeAttribute("required");
            }
        });
    } else {
        nonBuildingFields.forEach(field => {
            field.style.display = "block";
            if (field.tagName !== "LABEL") {
                field.setAttribute("required", "true");
            }
        });
    }
}
function validateMetadataForm() {
    const category = document.getElementById("category").value;
    if (category.toLowerCase === "building") {
        return true;
    }
    const roomName = document.getElementById("roomName").value.trim();
    const roomCategory = document.getElementById("roomCategory").value.trim();
    const description = document.getElementById("description").value.trim();
    const owner = document.getElementById("owner").value.trim();
    const email = document.getElementById("email").value.trim();
    const phone = document.getElementById("phone").value.trim();
    if (!roomName || !roomCategory || !description || !owner || !email || !phone) {
        alert("Please fill in all required fields.");
        return false;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        alert("Please enter a valid email address.");
        return false;
    }
    if (phone.length < 7) {
        alert("Please enter a valid phone number.");
        return false;
    }
    if (category === 'utility') {
        const utilityType = document.getElementById("utilityType").value.trim();
        if (!utilityType) {
            alert("Please enter a utility type.");
            return false;
        }
    } else if (category === 'elevation') {
        const elevationType = document.getElementById("elevationType").value;
        if (!elevationType) {
            alert("Please select an elevation type.");
            return false;
        }
        if (document.getElementById("floorCheckboxes")) {
            const checkedFloors = document.querySelectorAll('#floorCheckboxes input:checked');
            if (checkedFloors.length === 0) {
                alert("Please select at least one connected floor.");
                return false;
            }
        }
    }
    return true;
}
initializeMetadataForm();
function getRoomTypeColor(metadata) {
    if (!metadata) return "#000000";
    const roomType = (metadata.category || metadata.roomType || metadata.roomCategory || "").toUpperCase();
    const colorMap = {
        "ROOM": "#4285F4",    
        "ELEVATOR": "#EA4335",
        "STAIR": "#FBBC05",   
        "ESCALATOR": "#34A853",
        "UTILITY": "#9C27B0", 
        "BUILDING": "#795548",
        "WALL": "#9E9E9E",    
        "HALLWAY": "#03A9F4", 
        "BATHROOM": "#FF9800",
        "OFFICE": "#8BC34A",  
        "CONFERENCE": "#FF5722",
        "STANDARD": "#2196F3",
    };
    return colorMap[roomType] || "#000000";
}
function calculateContentBounds(floorNumber) {
    const bounds = {
        minX: Infinity,
        minY: Infinity,
        maxX: -Infinity,
        maxY: -Infinity,
        isEmpty: true
    };
    if (!floorDrawings[floorNumber] || floorDrawings[floorNumber].length === 0) {
        return bounds;
    }
    floorDrawings[floorNumber].forEach(object => {
        const allPoints = [];
        if (object.linePointList && object.linePointList.length > 0) {
            allPoints.push(...object.linePointList);
        }
        if (object.points && object.points.length > 0) {
            allPoints.push(...object.points);
        }
        allPoints.forEach(point => {
            bounds.minX = Math.min(bounds.minX, point.x);
            bounds.minY = Math.min(bounds.minY, point.y);
            bounds.maxX = Math.max(bounds.maxX, point.x);
            bounds.maxY = Math.max(bounds.maxY, point.y);
            bounds.isEmpty = false;
        });
    });
    const sidePadding = 100;
    const topPadding = 200;
    const bottomPadding = 100;
    if (!bounds.isEmpty) {
        bounds.minX = bounds.minX - sidePadding;
        bounds.minY = bounds.minY - topPadding;
        bounds.maxX = bounds.maxX + sidePadding;
        bounds.maxY = bounds.maxY + bottomPadding;
    }
    return bounds;
}
function addRoomTypeLegend(ctx, width, roomTypesWithCounts) {
    const lineHeight = 20;
    const longestTypeLength = Math.max(...roomTypesWithCounts.map(item => item.type.length));
    const legendWidth = Math.max(210, longestTypeLength * 10 + 100);
    const legendX = width - legendWidth - 20;
    const legendY = 20;
    ctx.fillStyle = "rgba(255, 255, 255, 0.9)";
    ctx.fillRect(legendX - 10, legendY - 15, legendWidth, (roomTypesWithCounts.length + 1) * lineHeight + 10);
    ctx.strokeStyle = "#000000";
    ctx.lineWidth = 1;
    ctx.strokeRect(legendX - 10, legendY - 15, legendWidth, (roomTypesWithCounts.length + 1) * lineHeight + 10);
    ctx.fillStyle = "#000000";
    ctx.font = "bold 12px Arial";
    ctx.textAlign = "left";
    ctx.textBaseline = "middle";
    ctx.fillText("Room Types", legendX, legendY);
    roomTypesWithCounts.forEach((item, index) => {
        const itemY = legendY + ((index + 1) * lineHeight);
        ctx.fillStyle = getRoomTypeColor({ category: item.type });
        ctx.fillRect(legendX, itemY - 6, 15, 12);
        ctx.strokeStyle = "#000000";
        ctx.lineWidth = 1;
        ctx.strokeRect(legendX, itemY - 6, 15, 12);
        ctx.fillStyle = "#000000";
        ctx.font = "12px Arial";
        ctx.fillText(`${item.type} (${item.count})`, legendX + 25, itemY);
    });
}
function generateFloorPlanPNG(floorNumber) {
    const bounds = calculateContentBounds(floorNumber);
    if (bounds.isEmpty) {
        return null;
    }
    const width = bounds.maxX - bounds.minX;
    const height = bounds.maxY - bounds.minY;
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    const floorName = buildingData.floorNames?.[floorNumber] || `Floor ${floorNumber}`;
    ctx.fillStyle = '#000000';
    ctx.font = 'bold 24px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    ctx.fillText(floorName, width / 2, 20);
    ctx.translate(-bounds.minX, -bounds.minY);
    const roomTypeCounts = {};
    if (floorDrawings[floorNumber]) {
        floorDrawings[floorNumber].forEach(shape => {
            if (shape.metadata) {
                const roomType = (shape.metadata.category || 
                                 shape.metadata.roomType || 
                                 shape.metadata.roomCategory || 
                                 "UNDEFINED").toUpperCase();
                if (!roomTypeCounts[roomType]) {
                    roomTypeCounts[roomType] = 0;
                }
                roomTypeCounts[roomType]++;
            }
            const strokeColor = getRoomTypeColor(shape.metadata);
            if (shape.type === "line" && shape.linePointList && shape.linePointList.length > 0) {
                ctx.beginPath();
                ctx.strokeStyle = strokeColor;
                ctx.lineWidth = 2;
                shape.linePointList.forEach((point, i) => {
                    if (i === 0) ctx.moveTo(point.x, point.y);
                    else ctx.lineTo(point.x, point.y);
                });
                if (shape.linePointList.length > 2) {
                    ctx.closePath();
                }
                ctx.fillStyle = strokeColor + "40";
                ctx.fill();
                ctx.stroke();
            } 
            else if (shape.type === "square" && shape.points && shape.points.length === 4) {
                ctx.beginPath();
                ctx.strokeStyle = strokeColor;
                ctx.lineWidth = 2;
                ctx.moveTo(shape.points[0].x, shape.points[0].y);
                ctx.lineTo(shape.points[1].x, shape.points[1].y);
                ctx.lineTo(shape.points[2].x, shape.points[2].y);
                ctx.lineTo(shape.points[3].x, shape.points[3].y);
                ctx.closePath();
                ctx.fillStyle = strokeColor + "40";
                ctx.fill();
                ctx.stroke();
            }
            if (shape.metadata && shape.metadata.roomName) {
                let centerX = 0;
                let centerY = 0;
                let points = [];
                if (shape.linePointList && shape.linePointList.length > 0) {
                    points = shape.linePointList;
                } else if (shape.points && shape.points.length > 0) {
                    points = shape.points;
                }
                if (points.length > 0) {
                    points.forEach(point => {
                        centerX += point.x;
                        centerY += point.y;
                    });
                    centerX /= points.length;
                    centerY /= points.length;
                    ctx.fillStyle = "#000";
                    ctx.font = "bold 12px Arial";
                    ctx.textAlign = "center";
                    ctx.textBaseline = "middle";
                    ctx.fillText(shape.metadata.roomName, centerX, centerY);
                    const roomType = shape.metadata.category || shape.metadata.roomType || shape.metadata.roomCategory;
                    if (roomType) {
                        ctx.font = "italic 10px Arial";
                        ctx.fillText(roomType, centerX, centerY + 15);
                    }
                }
            }
        });
    }
    ctx.resetTransform();
    const roomTypesWithCounts = Object.entries(roomTypeCounts).map(([type, count]) => ({
        type,
        count
    }));
    roomTypesWithCounts.sort((a, b) => b.count - a.count);
    ctx.fillStyle = '#000000';
    ctx.font = '14px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    const totalObjects = floorDrawings[floorNumber].length;
    ctx.fillText(`Total Objects: ${totalObjects}`, width / 2, 50);
    let topTypesText = "";
    if (roomTypesWithCounts.length > 0) {
        const topTypes = roomTypesWithCounts.slice(0, Math.min(3, roomTypesWithCounts.length));
        topTypesText = topTypes.map(item => `${item.type}: ${item.count}`).join(', ');
        ctx.fillText(topTypesText, width / 2, 70);
    }
    if (roomTypesWithCounts.length > 0) {
        addRoomTypeLegend(ctx, width, roomTypesWithCounts);
    }
    return {
        imageData: canvas.toDataURL('image/png'),
        width: width,
        height: height,
        roomTypeCounts: roomTypeCounts,
        floorName: floorName,
        totalObjects: totalObjects
    };
}
function exportFloorPlansToPDF() {
    const { jsPDF } = window.jspdf;
    const buildingName = buildingData.name || "Building Floor Plans";
    const currentState = {
        floor: currentFloor,
        offset: { ...canvasOffset },
        zoom: window.zoomLevel
    };
    canvasOffset = { x: 0, y: 0 };
    window.zoomLevel = 1.0;
    const floorImages = [];
    const totalFloors = buildingData.total_floors || floors;
    for (let floor = 1; floor <= totalFloors; floor++) {
        displayFloor(floor);
        const floorImage = generateFloorPlanPNG(floor);
        if (floorImage) {
            floorImages.push({
                floor: floor,
                ...floorImage
            });
        }
    }
    if (floorImages.length === 0) {
        alert("No floor plans to export");
        return;
    }
    const doc = new jsPDF({
        orientation: 'landscape',
        unit: 'pt',
        format: 'a4'
    });
    doc.setFontSize(24);
    doc.text(buildingName, doc.internal.pageSize.getWidth() / 2, 60, { align: 'center' });
    doc.setFontSize(16);
    doc.text("Floor Plans", doc.internal.pageSize.getWidth() / 2, 90, { align: 'center' });
    doc.setFontSize(12);
    doc.text(`Generated: ${new Date().toLocaleDateString()}`, doc.internal.pageSize.getWidth() / 2, 120, { align: 'center' });
    floorImages.forEach((floorImage, index) => {
        doc.addPage([floorImage.width, floorImage.height].map(dim => Math.min(dim, 1500)));
        doc.addImage(floorImage.imageData, 'PNG', 0, 0, floorImage.width, floorImage.height);
    });
    currentFloor = currentState.floor;
    canvasOffset = currentState.offset;
    window.zoomLevel = currentState.zoom;
    displayFloor(currentFloor);
    doc.save(`${buildingName.replace(/\s+/g, '_')}_FloorPlans.pdf`);
    alert("Floor plans exported to PDF successfully");
}
function initializeDrawingModeControls() {
    document.getElementById("lineModeButton").addEventListener("click", activateLineMode);
    document.getElementById("squareModeButton").addEventListener("click", activateSquareMode);
    document.getElementById("connectionModeButton").addEventListener("click", activateConnectionMode);
    document.getElementById("panModeButton").addEventListener("click", togglePanMode);
    document.getElementById("pointerModeButton").addEventListener("click", activatePointerMode);
    document.getElementById("backButton").addEventListener("click", handleBackNavigation);
    document.getElementById("saveButton").addEventListener("click", saveDrawing);
}
function activatePointerMode() {
    clearActiveButtonStates();
    drawingMode = "pointer"; 
    connectionMode = false; 
    panModeActive = false; 
    canvas.classList.remove("pan-cursor"); 
    document.getElementById("pointerModeButton").classList.add("active");
}
function activateLineMode() {
    clearActiveButtonStates();
    drawingMode = "line";
    connectionMode = false;
    panModeActive = false;
    canvas.classList.remove("pan-cursor");
    canvas.style.cursor = "default";
    document.getElementById("lineModeButton").classList.add("active");
}
function activateSquareMode() {
    clearActiveButtonStates();
    drawingMode = "square";
    connectionMode = false;
    panModeActive = false;
    canvas.classList.remove("pan-cursor");
    canvas.style.cursor = "default";
    document.getElementById("squareModeButton").classList.add("active");
}
function activateConnectionMode() {
    clearActiveButtonStates();
    drawingMode = "connection";
    connectionMode = true;
    panModeActive = false;
    canvas.classList.remove("pan-cursor");
    document.getElementById("connectionModeButton").classList.add("active");
}
function togglePanMode() {
    clearActiveButtonStates();
    panModeActive = !panModeActive;
    if (panModeActive) {
        drawingMode = "pan";
        connectionMode = false;
        canvas.classList.add("pan-cursor");
        document.getElementById("panModeButton").classList.add("active");
    } else {
        drawingMode = "line";
        canvas.classList.remove("pan-cursor");
        canvas.style.cursor = "default";
        document.getElementById("lineModeButton").classList.add("active");
    }
}
function activatePointerMode() {
    clearActiveButtonStates();
    drawingMode = "pointer";
    connectionMode = false;
    panModeActive = false;
    canvas.classList.remove("pan-cursor");
    document.getElementById("pointerModeButton").classList.add("active");
}
function clearActiveButtonStates() {
    const modeButtons = [
        "lineModeButton",
        "squareModeButton",
        "connectionModeButton",
        "panModeButton",
        "pointerModeButton"
    ];
    modeButtons.forEach(buttonId => {
        document.getElementById(buttonId).classList.remove("active");
    });
}
function handleBackNavigation() {
    if (hasUnsavedChanges) {
        const confirmLeave = confirm("You have unsaved changes. Are you sure you want to leave without saving?");
        if (!confirmLeave) {
            return;
        }
    }
    window.location.href = "/admin_homepage";
}
initializeDrawingModeControls();
function initializeZoomControls() {
    window.zoomLevel = 1.0;
    window.minZoomLevel = 0.25;
    window.maxZoomLevel = 10.0;
    window.zoomStep = 0.25;
    addZoomButtons();
    canvas.addEventListener('wheel', handleMouseWheel);
    updateZoomDisplay();
}
function screenToCanvasCoords(screenX, screenY) {
    const rect = canvas.getBoundingClientRect();
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    const canvasX = (screenX - rect.left - centerX) / window.zoomLevel + centerX - canvasOffset.x;
    const canvasY = (screenY - rect.top - centerY) / window.zoomLevel + centerY - canvasOffset.y;
    return { x: canvasX, y: canvasY };
}
function handleMouseWheel(e) {
    if (e.ctrlKey) {
        e.preventDefault();
        if (e.deltaY < 0) {
            zoomIn();
        } else {
            zoomOut();
        }
    }
}
function applyZoom() {
    updateZoomDisplay();
    redrawCanvas();
}
function updateZoomDisplay() {
    const zoomPercent = Math.round(window.zoomLevel * 100);
    const zoomDisplay = document.getElementById('zoomLevelDisplay');
    if (zoomDisplay) {
        zoomDisplay.textContent = `${zoomPercent}%`;
    }
    const zoomInButton = document.getElementById('zoomInButton');
    const zoomOutButton = document.getElementById('zoomOutButton');
    if (zoomInButton) {
        zoomInButton.disabled = window.zoomLevel >= window.maxZoomLevel;
    }
    if (zoomOutButton) {
        zoomOutButton.disabled = window.zoomLevel <= window.minZoomLevel;
    }
}
function addZoomButtons() {
    const zoomControlsDiv = document.createElement('div');
    zoomControlsDiv.className = 'zoom-controls';
    zoomControlsDiv.style.cssText = `
        position: absolute; 
        bottom: 20px; 
        right: 20px; 
        display: flex; 
        align-items: center; 
        background: rgba(255,255,255,0.8); 
        padding: 8px 12px; 
        border-radius: 4px; 
        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
        font-family: Arial, sans-serif;
    `;
    const zoomOutBtn = document.createElement('button');
    zoomOutBtn.id = 'zoomOutButton';
    zoomOutBtn.textContent = '−';
    zoomOutBtn.style.cssText = `
        background: #f0f0f0; 
        border: 1px solid #ccc; 
        border-radius: 3px; 
        cursor: pointer; 
        margin-right: 8px; 
        width: 30px; 
        height: 30px; 
        font-size: 20px; 
        line-height: 1; 
        display: flex; 
        align-items: center; 
        justify-content: center;
    `;
    zoomOutBtn.addEventListener('click', zoomOut);
    zoomOutBtn.title = 'Zoom Out';
    const zoomText = document.createElement('span');
    zoomText.id = 'zoomLevelDisplay';
    zoomText.textContent = '100%';
    zoomText.style.cssText = `
        margin: 0 10px; 
        min-width: 60px; 
        text-align: center; 
        font-size: 14px;
    `;
    const zoomResetBtn = document.createElement('button');
    zoomResetBtn.id = 'zoomResetButton';
    zoomResetBtn.textContent = 'Reset';
    zoomResetBtn.style.cssText = `
        background: #f0f0f0; 
        border: 1px solid #ccc; 
        border-radius: 3px; 
        padding: 4px 8px; 
        cursor: pointer; 
        margin-left: 8px; 
        font-size: 12px;
    `;
    zoomResetBtn.addEventListener('click', resetZoom);
    zoomResetBtn.title = 'Reset Zoom (100%)';
    const zoomInBtn = document.createElement('button');
    zoomInBtn.id = 'zoomInButton';
    zoomInBtn.textContent = '+';
    zoomInBtn.style.cssText = `
        background: #f0f0f0; 
        border: 1px solid #ccc; 
        border-radius: 3px; 
        cursor: pointer; 
        margin-left: 8px; 
        width: 30px; 
        height: 30px; 
        font-size: 20px; 
        line-height: 1; 
        display: flex; 
        align-items: center; 
        justify-content: center;
    `;
    zoomInBtn.addEventListener('click', zoomIn);
    zoomInBtn.title = 'Zoom In';
    zoomControlsDiv.appendChild(zoomOutBtn);
    zoomControlsDiv.appendChild(zoomText);
    zoomControlsDiv.appendChild(zoomInBtn);
    zoomControlsDiv.appendChild(zoomResetBtn);
    canvas.parentElement.style.position = 'relative';
    canvas.parentElement.appendChild(zoomControlsDiv);
}
function zoomIn() {
    if (window.zoomLevel < window.maxZoomLevel) {
        window.zoomLevel = Math.min(window.zoomLevel + window.zoomStep, window.maxZoomLevel);
        applyZoom();
    }
}
function zoomOut() {
    if (window.zoomLevel > window.minZoomLevel) {
        window.zoomLevel = Math.max(window.zoomLevel - window.zoomStep, window.minZoomLevel);
        applyZoom();
    }
}
function resetZoom() {
    window.zoomLevel = 1.0;
    applyZoom();
}
function trySelectExistingShape(x, y) {
    if (drawingMode === "pan" || drawingMode === "line" || drawingMode === "square" || drawingMode === "connection") {
        return false;
    }
    function isPointInPolygon(x, y, polygon) {
        let inside = false;
        for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            const xi = polygon[i].x, yi = polygon[i].y;
            const xj = polygon[j].x, yj = polygon[j].y;
            const intersect = ((yi > y) != (yj > y))
                && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
    for (let i = 0; i < (floorDrawings[currentFloor] || []).length; i++) {
        const shape = floorDrawings[currentFloor][i];
        if (shape.metadata?.category.toLowerCase === "building") {
            continue; 
        }
        let isSelected = false;
        if (shape.type === "line" && shape.linePointList && shape.linePointList.length > 2) {
            isSelected = isPointInPolygon(x, y, shape.linePointList);
        } else if (shape.type === "square" && shape.points) {
            const points = shape.points;
            let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
            points.forEach(point => {
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
            });
            isSelected = (x >= minX && x <= maxX && y >= minY && y <= maxY);
        }
        if (isSelected) {
            currentShapeIndex = i;
            showMetadataForm();
            return true;
        }
    }
    return false;
}
function initializeCanvas() {
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);
}
function resizeCanvas() {
    const container = canvas.parentElement;
    const containerWidth = container.clientWidth - 200;
    const containerHeight = window.innerHeight - 300;
    canvas.width = containerWidth > 0 ? containerWidth : 800;
    canvas.height = containerHeight > 0 ? containerHeight : 600;
    redrawCanvas();
}
function connectElevationNodes(shape) {
    if (shape.metadata?.category !== 'STAIR' &&
        shape.metadata?.category !== 'ELEVATOR' &&
        shape.metadata?.category !== 'ESCALATOR') {
        return;
    }
    const connectedFloors = shape.metadata.connectedFloors || [];
    const currentFloorNode = shape.entranceNode;
    connectedFloors.forEach(floor => {
        if (floor === currentFloor) return;
        const otherFloorNode = findElevationNodeOnFloor(shape.metadata.roomName, floor);
        if (otherFloorNode) {
            const forwardConnection = window.FloorPlanModels.createConnection(
                currentFloorNode,
                otherFloorNode,
                shape.metadata.category,
                {
                    elevationName: shape.metadata.roomName,
                    startFloor: currentFloor,
                    endFloor: floor
                }
            );
            connections.push(forwardConnection);
        }
    });
}
function findElevationNodeOnFloor(name, floor) {
    if (!floorDrawings[floor]) return null;
    for (const shape of floorDrawings[floor]) {
        if ((shape.metadata?.category === 'STAIR' || 
             shape.metadata?.category === 'ELEVATOR' || 
             shape.metadata?.category === 'ESCALATOR') && 
            shape.metadata?.roomName === name && 
            shape.entranceNode) {
            return shape.entranceNode;
        }
    }
    return null;
}
function initializeElevationConnectionModal() {
    const connectElevationsButton = document.getElementById('connectElevationsButton');
    const elevationModal = document.getElementById('elevationConnectionModal');
    const closeModalButtons = document.querySelectorAll('#closeElevationConnectionModal');
    const connectButton = document.getElementById('connectElevationsConfirmButton');
    const refreshButton = document.getElementById('refreshElevationListsButton');
    const sourceFloorDropdown = document.getElementById('sourceFloorDropdown');
    const sourceRoomDropdown = document.getElementById('sourceRoomDropdown');
    const targetFloorDropdown = document.getElementById('targetFloorDropdown');
    const targetRoomDropdown = document.getElementById('targetRoomDropdown');
    if (connectButton) {
        connectButton.disabled = true;
    }
    if (connectElevationsButton) {
        connectElevationsButton.addEventListener('click', function() {
            const allElevations = findElevationNodes();
            elevationModal.style.display = 'block';
            populateFloorDropdowns();
            updateRoomDropdowns();
            validateElevationSelections();
        });
    }
    closeModalButtons.forEach(button => {
        button.addEventListener('click', function() {
            elevationModal.style.display = 'none';
        });
    });
    window.addEventListener('click', function(event) {
        if (event.target === elevationModal) {
            elevationModal.style.display = 'none';
        }
    });
    connectButton.addEventListener('click', function() {
        connectElevationNodes();
    });
    if (refreshButton) {
        refreshButton.addEventListener('click', function() {
            updateRoomDropdowns();
            validateElevationSelections();
        });
    }
    sourceFloorDropdown.addEventListener('change', function() {
        updateRoomDropdown(sourceFloorDropdown.value, sourceRoomDropdown);
        validateElevationSelections();
    });
    targetFloorDropdown.addEventListener('change', function() {
        updateRoomDropdown(targetFloorDropdown.value, targetRoomDropdown);
        validateElevationSelections();
    });
    sourceRoomDropdown.addEventListener('change', function() {
        validateElevationSelections();
    });
    targetRoomDropdown.addEventListener('change', function() {
        validateElevationSelections();
    });
}
function populateFloorDropdowns() {
    const sourceFloorDropdown = document.getElementById('sourceFloorDropdown');
    const targetFloorDropdown = document.getElementById('targetFloorDropdown');
    sourceFloorDropdown.innerHTML = '';
    targetFloorDropdown.innerHTML = '';
    const totalFloors = buildingData.total_floors || floors;
    for (let i = 1; i <= totalFloors; i++) {
        const sourceOption = document.createElement('option');
        sourceOption.value = i;
        sourceOption.textContent = buildingData.floorNames?.[i] || `Floor ${i}`;
        sourceFloorDropdown.appendChild(sourceOption);
        const targetOption = document.createElement('option');
        targetOption.value = i;
        targetOption.textContent = buildingData.floorNames?.[i] || `Floor ${i}`;
        targetFloorDropdown.appendChild(targetOption);
    }
    sourceFloorDropdown.value = currentFloor;
    targetFloorDropdown.value = currentFloor === 1 ? 2 : 1;
}
function updateRoomDropdowns() {
    const sourceFloorDropdown = document.getElementById('sourceFloorDropdown');
    const sourceRoomDropdown = document.getElementById('sourceRoomDropdown');
    const targetFloorDropdown = document.getElementById('targetFloorDropdown');
    const targetRoomDropdown = document.getElementById('targetRoomDropdown');
    updateRoomDropdown(sourceFloorDropdown.value, sourceRoomDropdown);
    updateRoomDropdown(targetFloorDropdown.value, targetRoomDropdown);
}
function getNormalizedCategory(shape) {
    let category = shape.metadata?.category?.toUpperCase?.() || '';
    if (category === 'ELEVATION' && shape.metadata?.elevationType) {
        category = shape.metadata.elevationType.toUpperCase();
        if (category === 'STAIRS') category = 'STAIR';
    }
    if (!category && shape.objectType) {
        category = shape.objectType.toUpperCase();
    }
    return category;
}
function validateElevationSelections() {
    const sourceFloorDropdown = document.getElementById('sourceFloorDropdown');
    const sourceRoomDropdown = document.getElementById('sourceRoomDropdown');
    const targetFloorDropdown = document.getElementById('targetFloorDropdown');
    const targetRoomDropdown = document.getElementById('targetRoomDropdown');
    const connectButton = document.getElementById('connectElevationsConfirmButton');
    const sourceFloor = parseInt(sourceFloorDropdown.value);
    const targetFloor = parseInt(targetFloorDropdown.value);
    const sourceShapeId = sourceRoomDropdown.value;
    const targetShapeId = targetRoomDropdown.value;
    const statusMessage = document.getElementById('connectionStatusMessage');
    const hasSourceSelection = sourceShapeId && sourceShapeId !== '';
    const hasTargetSelection = targetShapeId && targetShapeId !== '';
    const isSameElevation = sourceShapeId === targetShapeId && sourceFloor === targetFloor;
    let differentTypes = false;
    if (hasSourceSelection && hasTargetSelection) {
        const sourceOption = sourceRoomDropdown.options[sourceRoomDropdown.selectedIndex];
        const targetOption = targetRoomDropdown.options[targetRoomDropdown.selectedIndex];
        let sourceCategory = sourceOption.dataset.category;
        let targetCategory = targetOption.dataset.category;
        if (sourceCategory && targetCategory) {
            sourceCategory = sourceCategory.toUpperCase();
            targetCategory = targetCategory.toUpperCase();
            if (sourceCategory === 'STAIRS') sourceCategory = 'STAIR';
            if (targetCategory === 'STAIRS') targetCategory = 'STAIR';
            differentTypes = sourceCategory !== targetCategory;
        } else {
            const sourceFloor = parseInt(sourceFloorDropdown.value);
            const targetFloor = parseInt(targetFloorDropdown.value);
            const sourceShapeId = sourceRoomDropdown.value;
            const targetShapeId = targetRoomDropdown.value;
            const sourceShape = floorDrawings[sourceFloor]?.find(s => 
                s.id == sourceShapeId || s._id == sourceShapeId || s.objectId == sourceShapeId);
            const targetShape = floorDrawings[targetFloor]?.find(s => 
                s.id == targetShapeId || s._id == targetShapeId || s.objectId == targetShapeId);
            if (sourceShape && targetShape) {
                const sourceCat = getNormalizedCategory(sourceShape);
                const targetCat = getNormalizedCategory(targetShape);
                if (sourceCat && targetCat) {
                    differentTypes = sourceCat !== targetCat;
                }
            }
        }
    }
    if (!hasSourceSelection || !hasTargetSelection) {
        connectButton.disabled = true;
        if (statusMessage) {
            statusMessage.textContent = 'Select both source and target elevations to connect';
            statusMessage.className = 'status-message info';
        }
    } else if (isSameElevation) {
        connectButton.disabled = true;
        if (statusMessage) {
            statusMessage.textContent = 'Cannot connect an elevation to itself';
            statusMessage.className = 'status-message error';
        }
    } else if (differentTypes) {
        connectButton.disabled = false;
        if (statusMessage) {
            statusMessage.textContent = 'Warning: Connecting different elevation types';
            statusMessage.className = 'status-message warning';
        }
    } else {
        connectButton.disabled = false;
        if (statusMessage) {
            statusMessage.textContent = 'Ready to connect';
            statusMessage.className = 'status-message success';
        }
    }
}
function updateRoomDropdown(floorNumber, roomDropdown) {
    roomDropdown.innerHTML = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = '-- Select an elevation --';
    roomDropdown.appendChild(defaultOption);
    const floorDrawing = floorDrawings[floorNumber] || [];
    const elevationShapes = floorDrawing.filter(shape => {
        const category = shape.metadata?.category?.toUpperCase?.() || '';
        const isElevationType = 
            category === 'STAIR' || 
            category === 'ELEVATOR' || 
            category === 'ESCALATOR' ||
            category === 'ELEVATION' ||
            (shape.objectType && 
                (shape.objectType.toUpperCase() === 'STAIR' ||
                 shape.objectType.toUpperCase() === 'ELEVATOR' ||
                 shape.objectType.toUpperCase() === 'ESCALATOR'));
        const hasEntranceNode = !!shape.entranceNode;
        const hasName = !!shape.metadata?.roomName || !!shape.name;
        return isElevationType && hasEntranceNode && hasName;
    });
    const elevationTypes = {
        'ELEVATOR': [],
        'STAIR': [],
        'ESCALATOR': []
    };
    elevationShapes.forEach(shape => {
        let category = shape.metadata?.category?.toUpperCase?.() || '';
        if (category === 'ELEVATION' && shape.metadata?.elevationType) {
            const elevType = shape.metadata.elevationType.toUpperCase();
            if (elevType === 'ELEVATOR' || elevType === 'STAIR' || elevType === 'STAIRS' || elevType === 'ESCALATOR') {
                category = elevType === 'STAIRS' ? 'STAIR' : elevType;
            }
        }
        if (!elevationTypes[category] && shape.objectType) {
            category = shape.objectType.toUpperCase();
        }
        if (category === 'STAIRS') {
            category = 'STAIR';
        }
        if (elevationTypes[category]) {
            elevationTypes[category].push(shape);
        } else {
            if (shape.metadata?.elevationType?.toUpperCase?.() === 'ELEVATOR') {
                elevationTypes['ELEVATOR'].push(shape);
            } else if (shape.metadata?.elevationType?.toUpperCase?.() === 'ESCALATOR') {
                elevationTypes['ESCALATOR'].push(shape);
            } else if (shape.metadata?.elevationType?.toUpperCase?.() === 'STAIRS' || 
                       shape.metadata?.elevationType?.toUpperCase?.() === 'STAIR') {
                elevationTypes['STAIR'].push(shape);
            } else {
                elevationTypes['ELEVATOR'].push(shape);
            }
        }
    });
    Object.keys(elevationTypes).forEach(type => {
        if (elevationTypes[type].length > 0) {
            const optgroup = document.createElement('optgroup');
            optgroup.label = type.charAt(0) + type.slice(1).toLowerCase() + 's';
            elevationTypes[type].forEach(shape => {
                const option = document.createElement('option');
                option.value = shape.id;
                const displayName = shape.metadata?.roomName || shape.name || `Unnamed ${type.toLowerCase()}`;
                const floorText = shape.importedFrom ? ` (Floor ${shape.importedFrom})` : '';
                option.textContent = `${displayName}${floorText}`;
                option.dataset.category = shape.metadata?.category || type;
                optgroup.appendChild(option);
            });
            roomDropdown.appendChild(optgroup);
        }
    });
    if (elevationShapes.length === 0) {
        const noElevationsOption = document.createElement('option');
        noElevationsOption.disabled = true;
        noElevationsOption.textContent = 'No elevations found on this floor';
        roomDropdown.appendChild(noElevationsOption);
    }
}
function connectElevationNodes() {
    const sourceFloorDropdown = document.getElementById('sourceFloorDropdown');
    const sourceRoomDropdown = document.getElementById('sourceRoomDropdown');
    const targetFloorDropdown = document.getElementById('targetFloorDropdown');
    const targetRoomDropdown = document.getElementById('targetRoomDropdown');
    const connectButton = document.getElementById('connectElevationsConfirmButton');
    if (connectButton.disabled) {
        return;
    }
    const sourceFloor = parseInt(sourceFloorDropdown.value);
    const targetFloor = parseInt(targetFloorDropdown.value);
    const sourceShapeId = sourceRoomDropdown.value;
    const targetShapeId = targetRoomDropdown.value;
    if (!sourceShapeId || !targetShapeId) {
        alert('Please select both source and target elevations.');
        return;
    }
    if (sourceShapeId === targetShapeId && sourceFloor === targetFloor) {
        alert('Cannot connect an elevation to itself.');
        return;
    }
    const sourceShape = findShapeInFloor(sourceFloor, sourceShapeId);
    const targetShape = findShapeInFloor(targetFloor, targetShapeId);
    if (!sourceShape || !targetShape) {
        alert('Error: Could not find the selected elevations.');
        return;
    }
    if (!sourceShape.entranceNode || !targetShape.entranceNode) {
        alert('Error: Both elevations must have entrance nodes to connect them.');
        return;
    }
    function getNormalizedCategory(shape) {
        let category = shape.metadata?.category?.toUpperCase?.() || '';
        if (category === 'ELEVATION' && shape.metadata?.elevationType) {
            category = shape.metadata.elevationType.toUpperCase();
            if (category === 'STAIRS') category = 'STAIR';
        }
        return category;
    }
    const sourceCategory = getNormalizedCategory(sourceShape);
    const targetCategory = getNormalizedCategory(targetShape);
    if (sourceCategory && targetCategory && sourceCategory !== targetCategory) {
        const confirm = window.confirm(`Warning: You're connecting a ${sourceCategory.toLowerCase()} to a ${targetCategory.toLowerCase()}. Do you still want to connect them?`);
        if (!confirm) return;
    }
    const connectionExists = connections.some(conn => 
        (conn.start?.id === sourceShape.entranceNode.id && conn.end?.id === targetShape.entranceNode.id) ||
        (conn.start?.id === targetShape.entranceNode.id && conn.end?.id === sourceShape.entranceNode.id)
    );
    if (connectionExists) {
        const confirm = window.confirm('A connection already exists between these elevations. Do you want to create another one?');
        if (!confirm) return;
    }
    function findShapeInFloor(floorNumber, shapeId) {
        if (!floorDrawings[floorNumber]) return null;
        let shape = floorDrawings[floorNumber].find(s => s.id == shapeId);
        if (!shape) {
            shape = floorDrawings[floorNumber].find(s => 
                s._id == shapeId || 
                s.objectId == shapeId || 
                s.roomId == shapeId
            );
        }
        return shape;
    }
    if (sourceShape.entranceNode && targetShape.entranceNode) {
        const sourceNode = {
            ...sourceShape.entranceNode,
            floor: sourceFloor,
            id: sourceShape.entranceNode.id || FloorPlanModels.generateTempNodeId(),
            isElevation: true
        };
        const targetNode = {
            ...targetShape.entranceNode,
            floor: targetFloor,
            id: targetShape.entranceNode.id || FloorPlanModels.generateTempNodeId(),
            isElevation: true
        };
        sourceShape.entranceNode = sourceNode;
        targetShape.entranceNode = targetNode;
        const sourceCategory = getNormalizedCategory(sourceShape);
        const targetCategory = getNormalizedCategory(targetShape);
        const sourceName = sourceShape.metadata?.roomName || sourceShape.name || 'Unnamed Elevation';
        const targetName = targetShape.metadata?.roomName || targetShape.name || 'Unnamed Elevation';
        const connection1 = window.FloorPlanModels.createConnection(
            sourceNode,
            targetNode,
            sourceCategory,
            {
                elevationName: sourceName,
                startFloor: sourceFloor,
                endFloor: targetFloor,
                elevationType: sourceCategory
            }
        );
        const connection2 = window.FloorPlanModels.createConnection(
            targetNode,
            sourceNode,
            targetCategory,
            {
                elevationName: targetName,
                startFloor: targetFloor,
                endFloor: sourceFloor,
                elevationType: targetCategory
            }
        );
        connections.push(connection1);
        connections.push(connection2);
        if (!sourceShape.metadata) sourceShape.metadata = {};
        if (!targetShape.metadata) targetShape.metadata = {};
        if (!sourceShape.metadata.connectedFloors) {
            sourceShape.metadata.connectedFloors = [];
        }
        if (!targetShape.metadata.connectedFloors) {
            targetShape.metadata.connectedFloors = [];
        }
        if (!sourceShape.metadata.connectedFloors.includes(targetFloor)) {
            sourceShape.metadata.connectedFloors.push(targetFloor);
        }
        if (!targetShape.metadata.connectedFloors.includes(sourceFloor)) {
            targetShape.metadata.connectedFloors.push(sourceFloor);
        }
        redrawCanvas();
        hasUnsavedChanges = true;
        document.getElementById('elevationConnectionModal').style.display = 'none';
        alert('Elevations connected successfully!');
    } else {
        alert('Error: One or both of the selected elevations do not have entrance nodes.');
    }
}
function findElevationNodes(targetFloor = null) {
    let allMapObjects = [];
    Object.keys(floorDrawings).forEach(floorNum => {
        const floor = parseInt(floorNum);
        if (floorDrawings[floor] && Array.isArray(floorDrawings[floor])) {
            floorDrawings[floor].forEach(obj => {
                obj.floor = floor;
                allMapObjects.push(obj);
            });
        }
    });
    const elevationTypes = ['ELEVATOR', 'STAIR', 'ESCALATOR', 'STAIRS'];
    const elevationObjects = allMapObjects.filter(obj => {
        const objectType = obj.objectType?.toUpperCase?.();
        const category = obj.metadata?.category?.toUpperCase?.();
        const elevationType = obj.metadata?.elevationType?.toUpperCase?.();
        return elevationTypes.includes(objectType) || 
               elevationTypes.includes(category) || 
               elevationTypes.includes(elevationType) ||
               category === 'ELEVATION';
    });
    let floorFilteredObjects = elevationObjects;
    if (targetFloor !== null) {
        floorFilteredObjects = elevationObjects.filter(obj => obj.floor === targetFloor);
    }
    return floorFilteredObjects;
}
function loadDrawing(buildingId) {
    showLoading(true);
    window.apiClient.loadBuildingData(buildingId)
        .then(data => {
            const transformedData = window.FloorPlanTransformer.apiToDrawingFormat(data);
            floorDrawings = transformedData.floorDrawings || {};
            connections = transformedData.connections || [];
            connectionPoints = transformedData.connection_points || [];
            buildingData = transformedData.building_data || {};
            for (let i = 1; i <= buildingData.total_floors; i++) {
                if (!floorDrawings[i]) {
                    floorDrawings[i] = [];
                }
                if (!undoStack[i]) undoStack[i] = [];
                if (!redoStack[i]) redoStack[i] = [];
            }
            populateFloorSelector();
            displayFloor(currentFloor);
            hasUnsavedChanges = false;
            showLoading(false);
        })
        .catch(err => {
            showErrorMessage("Failed to load building data. Please try again.");
            showLoading(false);
        });
}
window.onload = function() {
    initializeCanvas();
    drawingMode = "line";
    document.getElementById("lineModeButton").classList.add("active");
    drawGrid();
    if (buildingId) {
        loadDrawing(buildingId);
    } else {
        showErrorMessage("No building ID provided. Please go back and select a building.");
    }
};
