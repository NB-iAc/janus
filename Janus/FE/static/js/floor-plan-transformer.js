class FloorPlanTransformer {
    static mapObjectType(category, metadata) {
        if (!category) return "ROOM";
        const upperCategory = category.toUpperCase();
        switch (upperCategory) {
            case 'BUILDING':
                return "BUILDING";
            case 'WALL':
                return "WALL";
            case 'UTILITY':
                return "ROOM";
            case 'ELEVATION':
                switch (metadata?.elevationType?.toUpperCase()) {
                    case 'ELEVATOR':
                        return "ELEVATOR";
                    case 'STAIRS':
                        return "STAIR";
                    case 'ESCALATOR':
                        return "ESCALATOR";
                    default:
                        return "ROOM";
                }
            case 'STAIR':
                return "STAIR";
            case 'ELEVATOR':
                return "ELEVATOR";
            case 'ESCALATOR':
                return "ESCALATOR";
            default:
                return "ROOM";
        }
    }
    static apiToDrawingFormat(data) {
        const floorIdToNumber = {};
        const floorIdToName = {};
        const nodeIdToNode = {};
        const floorNumberToId = {};
        const result = {
            building_data: {
                id: data.building.id,
                total_floors: data.floors.length,
                name: data.building.name,
                description: data.building.description,
                floorNames: {},
                floorIdMapping: {}
            },
            floorDrawings: {},
            connections: [],
            connection_points: []
        };
        data.floors.forEach(floor => {
            floorIdToNumber[floor.id] = floor.floorNumber;
            floorIdToName[floor.id] = floor.displayName || `Floor ${floor.floorNumber}`;
            floorNumberToId[floor.floorNumber] = floor.id;
            result.floorDrawings[floor.floorNumber] = [];
            result.building_data.floorNames[floor.floorNumber] = floor.displayName || `Floor ${floor.floorNumber}`;
            result.building_data.floorIdMapping[floor.floorNumber] = floor.id;
        });
        data.nodes.forEach(node => {
            const floorNumber = floorIdToNumber[node.floorId];
            const isElevation = node.isElevationNode === true || 
            node.isElevation === true || 
            node.nodeType === "ELEVATION";
            const transformedNode = {
                id: node.id,
                x: node.x,
                y: node.y,
                floor: floorNumber,
                isElevation: isElevation
            };
            nodeIdToNode[node.id] = transformedNode;
        });
        Object.keys(data.mapObjectsByFloor).forEach(floorId => {
            const floorNumber = floorIdToNumber[floorId] || parseInt(floorId, 10);
            const mapObjects = data.mapObjectsByFloor[floorId];
            if (!result.floorDrawings[floorNumber]) {
                result.floorDrawings[floorNumber] = [];
            }
            mapObjects.forEach(obj => {
                const isLine = obj.points && obj.points.length > 4;
                const shapeType = isLine ? "line" : "square";
                const entranceNode = nodeIdToNode[obj.entranceNodeId];
                const transformedObj = {
                    id: obj.id,
                    type: shapeType,
                    points: obj.points,
                    linePointList: obj.points,
                    entranceNode: entranceNode,
                    metadata: {
                        roomName: obj.name,
                        category: obj.objectType,
                        roomCategory: obj.roomType || obj.category,
                        description: obj.description,
                        owner: obj.contactDetails,
                        email: "",
                        phone: "",
                        floor: floorNumber
                    }
                };
                if (obj.contactDetails && obj.contactDetails.includes("|")) {
                    const parts = obj.contactDetails.split("|");
                    if (parts.length > 1) {
                        transformedObj.metadata.owner = parts[0].trim();
                        transformedObj.metadata.email = parts[1].trim();
                        transformedObj.metadata.phone = parts[2].trim();
                    }
                }
                result.floorDrawings[floorNumber].push(transformedObj);
            });
        });
        const connections = [];
        data.connections.forEach(conn => {
            const sourceNode = nodeIdToNode[conn.sourceNodeId];
            const targetNode = nodeIdToNode[conn.targetNodeId];
            if (sourceNode && targetNode) {
                connections.push({
                    id: conn.id,
                    start: sourceNode,
                    end: targetNode,
                    metadata: {
                        distance: conn.distance,
                        bidirectional: conn.bidirectional
                    }
                });
            }
        });
        result.connections = connections;
        result.connection_points = Object.values(nodeIdToNode);
        return result;
    }
    static drawingToApiFormat(drawingData) {
        const result = {
            building: {
                id: drawingData.building_data.id || 1,
                name: drawingData.building_data.name || "Building",
                description: drawingData.building_data.description || ""
            },
            floors: [],
            mapObjectsByFloor: {},
            nodes: [],
            connections: []
        };
        for (let i = 1; i <= drawingData.building_data.total_floors; i++) {
        const floorId = drawingData.building_data.floorIdMapping && drawingData.building_data.floorIdMapping[i] 
            ? drawingData.building_data.floorIdMapping[i] 
            : i;
            result.floors.push({
                id: floorId,
                buildingId: result.building.id,
                floorNumber: i,
                displayName: drawingData.building_data.floorNames?.[i] || `Floor ${i}`,
                accessible: true
            });
            result.mapObjectsByFloor[floorId] = [];
        }
        Object.keys(drawingData.floorDrawings).forEach(floorNumber => {
            const floorNum = parseInt(floorNumber);
            const floorObject = result.floors.find(f => f.floorNumber === floorNum);
            if (!floorObject) {
                return;
            }    
            const floorId = floorObject.id;
            const drawings = drawingData.floorDrawings[floorNumber];
            if (!result.mapObjectsByFloor[floorId]) {
                result.mapObjectsByFloor[floorId] = [];
            }
            drawings.forEach(drawing => {
                const mapObject = {
                    id: drawing.id,
                    floorId: floorId,
                    objectType: this.mapObjectType(drawing.metadata?.category, drawing.metadata),
                    name: drawing.metadata?.roomName || "Unnamed",
                    roomId: `RM-${drawing.id}`,
                    category: drawing.metadata?.roomCategory || "DEFAULT",
                    contactDetails: `${drawing.metadata?.owner || ""} | ${drawing.metadata?.email} | ${drawing.metadata?.phone || ""}`.trim(),
                    roomType: drawing.metadata?.roomCategory || "",
                    description: drawing.metadata?.description || "",
                    entranceNodeId: drawing.entranceNode?.id,
                    points: drawing.type === "line" ? drawing.linePointList : drawing.points,
                    accessible: true
                };
                result.mapObjectsByFloor[floorId].push(mapObject);
            });
        });
        const processedNodeIds = new Set();
        drawingData.connection_points.forEach(point => {
            if (!processedNodeIds.has(point.id)) {
                processedNodeIds.add(point.id);
                const floorNum = parseInt(point.floor);
                const floorObject = result.floors.find(f => f.floorNumber === floorNum);
                if (!floorObject) {
                    return;
                }    
                const floorId = floorObject.id;
                if (!result.mapObjectsByFloor[floorId]) {
                    result.mapObjectsByFloor[floorId] = [];
                }
                const apiNode = {
                    id: point.id,
                    x: point.x,
                    y: point.y,
                    floorId: floorId,
                    isElevationNode: point.isElevation,
                    nodeType: point.isElevation ? "ELEVATION" : "",
                    neighborIds: [],
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString()
                };
                drawingData.connections.forEach(conn => {
                    if (conn.start.id === point.id) {
                        apiNode.neighborIds.push(conn.end.id);
                    } else if (conn.end.id === point.id) {
                        apiNode.neighborIds.push(conn.start.id);
                    }
                });
                result.nodes.push(apiNode);
            }
        });
        const processedConnections = new Set();
        drawingData.connections.forEach(conn => {
            const connKey = `${conn.start.id}-${conn.end.id}`;
            const reverseKey = `${conn.end.id}-${conn.start.id}`;
            if (!processedConnections.has(connKey) && !processedConnections.has(reverseKey)) {
                processedConnections.add(connKey);
                let distance = conn.metadata?.distance;
                if (!distance) {
                    distance = Math.sqrt(
                        Math.pow(conn.end.x - conn.start.x, 2) + 
                        Math.pow(conn.end.y - conn.start.y, 2)
                    );
                    distance = parseFloat(distance.toFixed(1));
                }
                result.connections.push({
                    id: conn.id,
                    sourceNodeId: conn.start.id,
                    targetNodeId: conn.end.id,
                    distance: distance,
                    bidirectional: conn.metadata?.bidirectional !== false
                });
            }
        });
        return result;
    }
}
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FloorPlanTransformer;
} else {
    window.FloorPlanTransformer = FloorPlanTransformer;
}