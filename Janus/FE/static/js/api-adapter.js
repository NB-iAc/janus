class ApiModels {
    static buildingToCreateDTO(building) {
        return {
            name: building.building_name || "",
            description: building.description || ""
        };
    }
    static buildingFromDTO(dto) {
        return {
            building_id: dto.id.toString(),
            building_name: dto.name,
            description: dto.description,
            total_floors: 0,
            createdAt: dto.createdAt,
            updatedAt: dto.updatedAt
        };
    }
    static floorToCreateDTO(floor) {
        return {
            buildingId: parseInt(floor.building_id),
            floorNumber: floor.floor_number,
            displayName: floor.display_name || `Floor ${floor.floor_number}`,
            accessible: floor.accessible !== false
        };
    }
    static floorFromDTO(dto) {
        return {
            id: dto.id,
            building_id: dto.buildingId.toString(),
            floor_number: dto.floorNumber,
            display_name: dto.displayName,
            accessible: dto.accessible
        };
    }
    static mapObjectToCreateDTO(mapObject) {
        let objectType, category;
        if (mapObject.metadata && mapObject.metadata.category) {
            switch (mapObject.metadata.category) {
                case 'room':
                    objectType = 'ROOM';
                    category = 'STANDARD';
                    break;
                case 'utility':
                    objectType = 'UTILITY';
                    category = mapObject.metadata.utilityType?.toUpperCase() || 'STANDARD';
                    break;
                case 'elevation':
                    objectType = 'ELEVATION';
                    category = mapObject.metadata.elevationType?.toUpperCase() || 'ELEVATOR';
                    break;
                default:
                    objectType = 'OTHER';
                    category = 'STANDARD';
            }
        } else {
            objectType = 'OTHER';
            category = 'STANDARD';
        }
        const points = mapObject.type === 'square' ? 
            mapObject.points.map(p => ({ x: p.x, y: p.y })) : 
            mapObject.linePointList.map(p => ({ x: p.x, y: p.y }));
        const contactDetails = `${mapObject.metadata?.owner || ""}|${mapObject.metadata?.email || ""}|${mapObject.metadata?.phone || ""}`;
        return {
            floorId: parseInt(mapObject.floorId),
            objectType,
            name: mapObject.metadata?.roomName || "",
            roomId: mapObject.metadata?.roomId || "",
            category,
            contactDetails: contactDetails,
            roomType: objectType === 'ROOM' ? 'STANDARD' : null,
            description: mapObject.metadata?.description || "",
            entranceNodeId: mapObject.entranceNode?.id || null,
            points,
            accessible: true
        };
    }
    static mapObjectFromDTO(dto) {
        const isRoom = dto.objectType === 'ROOM';
        const isUtility = dto.objectType === 'UTILITY';
        const isElevation = dto.objectType === 'ELEVATION';
        const contactParts = (dto.contactDetails || "").split('|').map(p => p.trim());
        const isLine = dto.points && dto.points.length > 2 && !this._formsClosed4PointPolygon(dto.points);
        return {
            id: dto.id,
            floorId: dto.floorId,
            type: isLine ? "line" : "square",
            points: isLine ? [] : dto.points,
            linePointList: isLine ? dto.points : [],
            metadata: {
                roomName: dto.name,
                roomId: dto.roomId,
                category: isRoom ? 'room' : (isUtility ? 'utility' : (isElevation ? 'elevation' : 'other')),
                description: dto.description || "",
                owner: contactParts[0] || "",
                email: contactParts[1] || "",
                phone: contactParts[2] || "",
                utilityType: isUtility ? dto.category.toLowerCase() : null,
                elevationType: isElevation ? dto.category.toLowerCase() : null,
                connectedFloors: []
            },
            entranceNode: dto.entranceNodeId ? { id: dto.entranceNodeId } : null,
            connectionPoints: [],
            createdAt: dto.createdAt,
            updatedAt: dto.updatedAt
        };
    }
    static _formsClosed4PointPolygon(points) {
        if (points.length !== 4) return false;
        const edges = [
            [0, 1], [1, 2], [2, 3], [3, 0]
        ];
        return edges.every(([i, j]) => {
            const dx = points[i].x - points[j].x;
            const dy = points[i].y - points[j].y;
            return dx !== 0 || dy !== 0;
        });
    }
    static nodeToCreateDTO(node) {
        return {
            x: node.x,
            y: node.y,
            floorId: parseInt(node.floorId),
            isElevationNode: node.isElevation === true,
            nodeType: node.isElevation ? "ELEVATION" : "HALLWAY"
        };
    }
    static nodeFromDTO(dto) {
        return {
            id: dto.id,
            x: dto.x,
            y: dto.y,
            floorId: dto.floorId,
            isElevation: dto.isElevationNode,
            nodeType: dto.nodeType,
            neighborIds: dto.neighborIds || []
        };
    }
    static connectionToDTO(connection) {
        return {
            sourceNodeId: connection.start.id,
            targetNodeId: connection.end.id,
            distance: this._calculateDistance(connection.start, connection.end),
            bidirectional: true
        };
    }
    static connectionFromDTO(dto) {
        return {
            sourceNodeId: dto.sourceNodeId,
            targetNodeId: dto.targetNodeId,
            distance: dto.distance,
            bidirectional: dto.bidirectional
        };
    }
    static _calculateDistance(point1, point2) {
        if (!point1 || !point2) return 0;
        const dx = point2.x - point1.x;
        const dy = point2.y - point1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    static toBuildingDataDTO(data) {
        const building = data.building_data || {};
        const floors = data.floors || [];
        const mapObjects = data.floor_drawings || [];
        const nodes = data.connection_points || [];
        const connections = data.connections || [];
        const mapObjectsByFloor = {};
        mapObjects.forEach(mapObject => {
            const floorId = mapObject.floorId;
            if (!mapObjectsByFloor[floorId]) {
                mapObjectsByFloor[floorId] = [];
            }
            mapObjectsByFloor[floorId].push(this.mapObjectToCreateDTO(mapObject));
        });
        return {
            building: {
                id: parseInt(building.building_id) || null,
                name: building.building_name || "",
                description: building.description || ""
            },
            floors: floors.map(floor => this.floorToCreateDTO(floor)),
            mapObjectsByFloor,
            nodes: nodes.map(node => this.nodeToCreateDTO(node)),
            connections: connections.map(conn => this.connectionToDTO(conn))
        };
    }
    static fromBuildingDataDTO(dto) {
        const building = this.buildingFromDTO(dto.building);
        const floors = dto.floors.map(floor => this.floorFromDTO(floor));
        building.total_floors = floors.length;
        let mapObjects = [];
        Object.keys(dto.mapObjectsByFloor).forEach(floorId => {
            mapObjects = mapObjects.concat(
                dto.mapObjectsByFloor[floorId].map(obj => this.mapObjectFromDTO(obj))
            );
        });
        const nodes = dto.nodes.map(node => this.nodeFromDTO(node));
        const connections = dto.connections.map(conn => this.connectionFromDTO(conn));
        return {
            building_data: building,
            floors,
            floor_drawings: mapObjects,
            connection_points: nodes,
            connections
        };
    }
    static convertFloorPlanToApiFormat(floorPlan, floorId, buildingId) {
        const floorDrawings = floorPlan.floor_drawings || [];
        const mapObjects = floorDrawings.map(drawing => {
            drawing.floorId = floorId;
            return this.mapObjectToCreateDTO(drawing);
        });
        const nodes = (floorPlan.connection_points || []).map(point => {
            point.floorId = floorId;
            return this.nodeToCreateDTO(point);
        });
        const connections = (floorPlan.connections || []).map(conn => {
            return this.connectionToDTO(conn);
        });
        return {
            mapObjects,
            nodes,
            connections
        };
    }
    static convertApiFormatToFloorPlan(apiData) {
        return {
            building_data: apiData.buildingData ? this.buildingFromDTO(apiData.buildingData) : {},
            floor_drawings: apiData.mapObjects ? apiData.mapObjects.map(obj => this.mapObjectFromDTO(obj)) : [],
            connection_points: apiData.nodes ? apiData.nodes.map(node => this.nodeFromDTO(node)) : [],
            connections: apiData.connections ? apiData.connections.map(conn => this.connectionFromDTO(conn)) : []
        };
    }
}