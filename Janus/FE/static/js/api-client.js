class JanusApiClient {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl || window.location.origin;
        this.token = localStorage.getItem('janus_token');
        this.enableLogging = true;
        this.logLevel = 'debug';
    }
    loadBuildingData(buildingId) {
        return this.get(`/api/proxy/building/${buildingId}`);
    }
    saveBuildingData(buildingId, data) {
        return this.post(`/api/proxy/building/${buildingId}`, data);
    }
    async get(endpoint, params = {}) {
        try {
            const url = new URL(endpoint, window.location.origin);
            Object.keys(params).forEach(key => {
                url.searchParams.append(key, params[key]);
            });
            const response = await fetch(url.toString(), {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`API error: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            throw error;
        }
    }
    async post(endpoint, data) {
        try {
            const url = new URL(endpoint, window.location.origin);
            const response = await fetch(url.toString(), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(data)
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`API error: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            throw error;
        }
    }
    log(level, message, data = null) {
        if (!this.enableLogging) return;
        const levels = {
            debug: 0,
            info: 1,
            warn: 2,
            error: 3
        };
        if (levels[level] >= levels[this.logLevel]) {
            const timestamp = new Date().toISOString();
            let logMessage = `[${timestamp}] [JanusApiClient] [${level.toUpperCase()}] ${message}`;
            if (data) {
                let stringifiedData;
                try {
                    const getCircularReplacer = () => {
                        const seen = new WeakSet();
                        return (key, value) => {
                            if (typeof value === "object" && value !== null) {
                                if (seen.has(value)) {
                                    return '[Circular Reference]';
                                }
                                seen.add(value);
                            }
                            return value;
                        };
                    };
                    if (typeof data === 'object' && data !== null) {
                        stringifiedData = JSON.stringify(data, getCircularReplacer(), 2);
                        if (stringifiedData.length > 2000) {
                            stringifiedData = stringifiedData.substring(0, 2000) + '... [truncated]';
                        }
                    } else {
                        stringifiedData = String(data);
                    }
                } catch (e) {
                    stringifiedData = '[Could not stringify data: ' + e.message + ']';
                }
                logMessage += `\nData: ${stringifiedData}`;
            }
            switch (level) {
                case 'debug':
                    break;
                case 'info':
                    break;
                case 'warn':
                    break;
                case 'error':
                    break;
            }
        }
    }
    debug(message, data = null) {
        this.log('debug', message, data);
    }
    info(message, data = null) {
        this.log('info', message, data);
    }
    warn(message, data = null) {
        this.log('warn', message, data);
    }
    error(message, data = null) {
        this.log('error', message, data);
    }
    updateFloorName(floorId, currentName,newName, buildingId, buildingName) {
        this.debug(`Updating floor ${floorId} name to "${newName}"`);
        return this.fetchApi(`/update_floor_name`, {
            method: 'POST',
            body: JSON.stringify({ 
                floor_id: floorId, 
                current_name: currentName,
                new_name: newName, 
                building_id: buildingId,
                building_name: buildingName
            })
        });
    }
    async requestBuildingAccess(buildingId, accessType = 'standard') {
        const userId = this.getUserId();
        this.debug(`Requesting access for building ${buildingId}`, { userId, accessType });
        return this.fetchApi('/api/protected/request-access', {
            method: 'POST',
            body: JSON.stringify({
                userId: userId,
                buildingId: buildingId,
                accessType: accessType
            })
        });
    }
    setToken(token) {
        this.token = token;
        localStorage.setItem('janus_token', token);
        this.debug('Token updated');
    }
    clearToken() {
        this.token = null;
        localStorage.removeItem('janus_token');
        this.debug('Token cleared');
    }
    async fetchApi(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        this.debug(`Making API request to ${url}`, { method: options.method || 'GET' });
        options.headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };
        if (this.token) {
            options.headers['Authorization'] = `Bearer ${this.token}`;
            this.debug('Adding authorization token to request');
        }
        if (options.body) {
            try {
                const bodyObj = JSON.parse(options.body);
                const safeBody = { ...bodyObj };
                if (safeBody.password) safeBody.password = '********';
                this.debug('Request body:', safeBody);
            } catch (e) {
                this.debug('Request body (non-JSON):', '[unable to parse]');
            }
        }
        try {
            this.debug(`Sending ${options.method || 'GET'} request to ${endpoint}`);
            const startTime = performance.now();
            const response = await fetch(url, options);
            const endTime = performance.now();
            const responseTime = Math.round(endTime - startTime);
            this.debug(`Response received in ${responseTime}ms with status ${response.status}`);
            if (response.status === 401) {
                this.warn('Authentication failed (401) - clearing token and redirecting to login');
                this.clearToken();
                window.location.href = '/';
                return null;
            }
            if (response.status !== 204) {
                const responseData = await response.json();
                this.debug(`Response data:`, responseData);
                return responseData;
            }
            this.debug('No content in response (204)');
            return { success: true };
        } catch (error) {
            this.error(`API error for ${endpoint}: ${error.message || error}`, error);
            throw error;
        }
    }
    async getAllBuildings() {
        this.debug('Fetching all buildings');
        return this.fetchApi('/get_buildings')
            .then(data => {
                this.debug(`Retrieved ${data.buildings ? data.buildings.length : 0} buildings`);
                return data;
            });
    }
    async deleteBuilding(buildingId) {
        this.debug(`Deleting building ${buildingId}`);
        return this.fetchApi('/delete_building', {
            method: 'POST',
            body: JSON.stringify({ building_id: buildingId })
        });
    }
    async requestRoomAccess(buildingId, roomId, accessType = 'standard') {
        const userId = this.getUserId();
        this.debug(`Requesting access for room ${roomId} in building ${buildingId}`, { userId, accessType });
        return this.fetchApi('/api/protected/request-access', {
            method: 'POST',
            body: JSON.stringify({
                userId: userId,
                buildingId: buildingId,
                roomId: roomId,
                accessType: accessType
            })
        });
    }
    async getPendingRequests(buildingId) {
        this.debug(`Fetching pending requests for building ${buildingId}`);
        return this.fetchApi(`/api/protected/pending-requests?buildingId=${buildingId}`);
    }
    async loadDrawing(buildingId, floorNumber) {
        this.debug(`Loading drawing for building ${buildingId}, floor ${floorNumber}`);
        const startTime = performance.now();
        return this.fetchApi(`/load_drawing?building_id=${buildingId}&floor_number=${floorNumber}`)
            .then(data => {
                const endTime = performance.now();
                const responseTime = Math.round(endTime - startTime);
                this.debug(`Drawing loaded in ${responseTime}ms`);
                this.debug(`Retrieved data summary:`, {
                    buildingName: data.building_data?.building_name,
                    floorDrawings: data.floor_drawings?.length || 0,
                    connections: data.connections?.length || 0,
                    connectionPoints: data.connection_points?.length || 0
                });
                return data;
            });
    }
    async saveDrawing(buildingId, floorNumber, drawingData) {
        this.debug(`Saving drawing for building ${buildingId}, floor ${floorNumber}`);
        this.debug(`Drawing data summary:`, {
            floorDrawings: drawingData.floor_drawings?.length || 0,
            connections: drawingData.connections?.length || 0,
            connectionPoints: drawingData.connection_points?.length || 0
        });
        const startTime = performance.now();
        return this.fetchApi(`/save_drawing?building_id=${buildingId}&floor_number=${floorNumber}`, {
            method: 'POST',
            body: JSON.stringify(drawingData)
        }).then(response => {
            const endTime = performance.now();
            const responseTime = Math.round(endTime - startTime);
            this.debug(`Drawing saved in ${responseTime}ms`, response);
            return response;
        });
    }
async getAccessibleRooms() {
    return this.fetchApi(`/api/protected/rooms/accessible`)
        .then(response => {
            if (response && response.buildings) {
                this.debug(`Retrieved ${response.buildings?.length || 0} buildings with rooms`);
                return response;
            } else {
                throw new Error('Failed to fetch accessible rooms or invalid response format');
            }
        })
        .catch(error => {
            this.error(`Error fetching accessible rooms: ${error.message}`, error);
            throw error;
        });
}
async getAllAvailableBuildings() {
    this.debug('Fetching inaccessible buildings with detailed structure');
    try {
        const response = await fetch('/api/protected/buildings/inaccessible');
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        const buildings = await response.json();
        this.debug(`Retrieved ${buildings.length} inaccessible buildings with detailed structure`);
        return buildings;
    } catch (error) {
        this.error('Error fetching inaccessible buildings:', error);
        throw error;
    }
}
async getBuildingRooms(buildingId) {
    this.debug(`Fetching rooms for building ${buildingId}`);
    try {
        const response = await fetch(`/get_rooms?building_id=${buildingId}`);
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        const data = await response.json();
        const rooms = data.rooms || [];
        this.debug(`Retrieved ${rooms.length} rooms for building ${buildingId}`);
        return rooms;
    } catch (error) {
        this.error(`Error fetching rooms for building ${buildingId}:`, error);
        throw error;
    }
}
async getUserRequests() {
    this.debug('Fetching current user access requests');
    try {
        const response = await fetch('/api/protected/user/my-requests');
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        const data = await response.json();
        const requests = data.items || [];
        this.debug(`Retrieved ${requests.length} access requests for current user`);
        return requests;
    } catch (error) {
        this.error('Error fetching user access requests:', error);
        throw error;
    }
}
async getAdminBuildingRequests() {
    this.debug('Fetching pending access requests for admin buildings');
    try {
        const response = await fetch('/api/protected/admin/building-requests');
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        const data = await response.json();
        const requests = data.items || [];
        this.debug(`Retrieved ${requests.length} pending admin building requests`);
        return requests;
    } catch (error) {
        this.error('Error fetching admin building requests:', error);
        throw error;
    }
}
async processAccessRequest(requestId, approved) {
    const processorId = this.getUserId();
    this.debug(`Processing access request ${requestId}`, { approved, processorId });
    try {
        const response = await fetch('/api/protected/process-request', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                requestId,
                approved,
                processorId
            })
        });
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        const result = await response.json();
        this.debug(`Access request processing result:`, result);
        return result;
    } catch (error) {
        this.error(`Error processing access request ${requestId}:`, error);
        throw error;
    }
}
async requestBuildingAccess(buildingId, accessType = 'standard') {
    const userId = this.getUserId();
    this.debug(`Requesting ${accessType} access for building ${buildingId}`, { userId });
    try {
        const response = await fetch('/api/protected/request-access', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId,
                buildingId,
                accessType
            })
        });
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        const result = await response.json();
        this.debug(`Building access request result:`, result);
        return result;
    } catch (error) {
        this.error(`Error requesting building access for ${buildingId}:`, error);
        throw error;
    }
}
async requestRoomAccess(buildingId, roomId, accessType = 'standard') {
    const userId = this.getUserId();
    this.debug(`Requesting ${accessType} access for room ${roomId} in building ${buildingId}`, { userId });
    try {
        const response = await fetch('/api/protected/request-access', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId,
                buildingId,
                roomId,
                accessType
            })
        });
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        const result = await response.json();
        this.debug(`Room access request result:`, result);
        return result;
    } catch (error) {
        this.error(`Error requesting room access for ${roomId}:`, error);
        throw error;
    }
}
async getPendingAccessRequests(buildingId) {
    this.debug(`Fetching pending access requests for building ${buildingId}`);
    try {
        const response = await fetch(`/api/protected/pending-requests?buildingId=${buildingId}`);
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        const requests = await response.json();
        this.debug(`Retrieved ${requests.length} pending requests for building ${buildingId}`);
        return requests;
    } catch (error) {
        this.error(`Error fetching pending requests for building ${buildingId}:`, error);
        throw error;
    }
}
async updateRoomDetails(roomId, roomData) {
    this.debug(`Updating details for room ${roomId}`);
    this.debug('Room data:', roomData);
    if (roomData.contactDetails && roomData.contactDetails.includes(' | ')) {
        roomData.contactDetails = roomData.contactDetails.replace(/ \| /g, '|');
    }
    return this.put(`/api/map-objects/${roomId}/update-room`, roomData);
}
    async saveBuildingShape(buildingData) {
        this.debug(`Saving building shape`, {
            buildingName: buildingData.building_name,
            floors: buildingData.floors,
            shapeElements: buildingData.shape?.length || 0
        });
        return this.fetchApi('/save_building_shape', {
            method: 'POST',
            body: JSON.stringify(buildingData)
        });
    }
    async getBuildingLogs(buildingId) {
        this.debug(`Fetching logs for building ${buildingId}`);
        return this.fetchApi(`/building-logs/building/${buildingId}`);
    }
    getUserId() {
        const userId = sessionStorage.getItem('user_id');
        this.debug(`Getting user ID from session storage: ${userId}`);
        return userId;
    }
}
window.apiClient = new JanusApiClient();
window.apiClient.info('JANUS API Client initialized');