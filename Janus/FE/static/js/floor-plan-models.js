class FloorPlanModels {
    static nodeCounter = -1;
    static objectCounter = -1;
    static floorCounter = -10;
    static generateTempNodeId() {
        return --this.nodeCounter;
    }
    static generateTempObjectId() {
        return --this.objectCounter;
    }
    static generateTempFloorId() {
        return --this.floorCounter;
    }
    static createNode(x, y, isElevation = false, floor = 1) {
        return {
            id: this.generateTempNodeId(),
            x: x,
            y: y,
            floor: floor,
            isElevation: isElevation
        };
    }
    static createConnection(startNode, endNode, category = null, metadata = {}) {
        return {
            start: startNode,
            end: endNode,
            category: category,
            metadata: metadata
        };
    }
    static createSquare(x, y, width, height) {
        return {
            id: this.generateTempObjectId(),
            type: "square",
            points: [
                { x: x, y: y },
                { x: x + width, y: y },
                { x: x + width, y: y + height },
                { x: x, y: y + height }
            ],
            metadata: null,
            entranceNode: null
        };
    }
    static createLine(points) {
        return {
            id: this.generateTempObjectId(),
            type: "line",
            linePointList: points,
            metadata: null,
            entranceNode: null
        };
    }
}
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FloorPlanModels;
} else {
    window.FloorPlanModels = FloorPlanModels;
}