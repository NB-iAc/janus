const BuildingModel = {
    building_id: null,
    building_name: null,
    description: null,
    total_floors: null
};
const NodeModel = {
    x: null,
    y: null,
    isElevation: false,
    floor: null
};
const MapObjectModel = {
    type: null, 
    linePointList: [], 
    points: [], 
    metadata: {
        roomName: null,
        category: null, 
        description: null,
        owner: null,
        email: null,
        phone: null,
        utilityType: null, 
        elevationType: null, 
        connectedFloors: [] 
    },
    entranceNode: null,
    connectionPoints: [],
    importedFrom: null 
};
const ConnectionModel = {
    start: null,
    end: null,
    connectionType: 'default',
    metadata: null
};
const FloorDrawingModel = {
    building_data: BuildingModel,
    floor_drawings: [], 
    connections: [],    
    connection_points: [] 
};
function createBuilding(id, name, description, totalFloors) {
    if (typeof id !== 'string' || !id.trim()) {
        throw new Error('Invalid building ID');
    }
    if (typeof totalFloors !== 'number' || totalFloors <= 0) {
        throw new Error('Invalid total floors');
    }
    return {
        ...BuildingModel,
        building_id: id,
        building_name: name || null,
        description: description || null,
        total_floors: totalFloors
    };
}
function createNode(x, y, isElevation = false, floor = null) {
    if (typeof x !== 'number' || typeof y !== 'number') {
        throw new Error('Invalid coordinates for node');
    }
    if (floor !== null && (typeof floor !== 'number' || floor <= 0)) {
        throw new Error('Invalid floor number');
    }
    return {
        ...NodeModel,
        x,
        y,
        isElevation,
        floor
    };
}
function createMapObject(type, points, metadata = {}) {
    if (!['line', 'square'].includes(type)) {
        throw new Error('Invalid map object type');
    }
    if (!Array.isArray(points) || points.length === 0) {
        throw new Error('Invalid points for map object');
    }
    return {
        ...MapObjectModel,
        type,
        ...(type === 'line' ? { linePointList: points } : { points }),
        metadata: {
            ...MapObjectModel.metadata,
            ...metadata
        }
    };
}
function createConnection(start, end, connectionType = 'default', metadata = null) {
    if (connectionType === 'elevation') {
        if (!start.isElevation || !end.isElevation) {
            throw new Error('Elevation connections require elevation nodes');
        }
        if (start.floor === end.floor) {
            throw new Error('Elevation connections must span different floors');
        }
        return {
            ...ConnectionModel,
            start,
            end,
            connectionType,
            metadata: {
                elevationName: metadata?.elevationName || `${start.floor}-${end.floor}-elevation`,
                connectedFloors: [start.floor, end.floor].sort(),
                elevationType: metadata?.elevationType || 'elevator'
            }
        };
    }
    return {
        ...ConnectionModel,
        start,
        end,
        connectionType: 'default'
    };
}
window.FloorPlanModels = {
    BuildingModel,
    NodeModel,
    MapObjectModel,
    ConnectionModel,
    FloorDrawingModel,
    createBuilding,
    createNode,
    createMapObject,
    createConnection
};