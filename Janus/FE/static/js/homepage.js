let allBuildingsData = [];        
let buildingsData = [];           
let roomsData = [];               
let expandedBuildingIds = new Map();
let allAccessBuildingsData = [];
let selectedBuildingForRooms = null;
let pendingRequests = [];
function showMetadataForm() {
    document.getElementById("roomName").value = "";
    document.getElementById("category").value = "Room";
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
            let category = shape.metadata.category;
            if (category === "STAIR") {
                document.getElementById("category").value = "elevation";
                document.getElementById("elevationType").value = "stairs";
            } else if (category === "ELEVATOR" || category === "ESCALATOR") {
                document.getElementById("category").value = "elevation";
                document.getElementById("elevationType").value = category.toLowerCase();
            } else {
                document.getElementById("category").value = category || "room";
            }
            const event = new Event('change');
            document.getElementById("category").dispatchEvent(event);
            document.getElementById("roomCategory").value = shape.metadata.roomCategory || "";
            document.getElementById("description").value = shape.metadata.description || "";
            document.getElementById("owner").value = shape.metadata.owner || "";
            document.getElementById("email").value = shape.metadata.email || "";
            document.getElementById("phone").value = shape.metadata.phone || "";
            if (shape.metadata.category === 'utility' && document.getElementById("utilityType")) {
                document.getElementById("utilityType").value = shape.metadata.utilityType || "";
            }
            if ((shape.metadata.category === 'STAIR' || 
                 shape.metadata.category === 'ELEVATOR' || 
                 shape.metadata.category === 'ESCALATOR') && 
                document.getElementById("floorCheckboxes")) {
                populateFloorCheckboxes(shape.metadata.connectedFloors || []);
            }
        }
    }
    metadataForm.style.display = "block";
}
function initAccessManagement() {
    document.querySelectorAll('.access-tab-button').forEach(button => {
        button.addEventListener('click', function() {
            document.querySelectorAll('.access-tab-button').forEach(btn => {
                btn.classList.remove('active');
            });
            document.querySelectorAll('.access-tab-content').forEach(content => {
                content.classList.remove('active');
            });
            this.classList.add('active');
            const tabId = this.dataset.tab;
            document.getElementById(tabId).classList.add('active');
            if (tabId === 'request-access') {
                loadAllBuildingsForAccess();
            } else if (tabId === 'grant-access') {
                loadPendingRequests();
            } else if (tabId === 'your-requests') {
                loadAccessRequests();
            } else if (tabId === 'building-tenants') {
                initTenantManagement(); 
            }
        });
    });
    const searchInput = document.getElementById('accessBuildingSearch');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            filterAccessBuildings(this.value);
        });
    }
    const roomSearchInput = document.getElementById('roomSearchInput');
    if (roomSearchInput) {
        roomSearchInput.addEventListener('input', function() {
            filterRoomsInModal(this.value);
        });
    }
    const closeBuildingRoomsBtn = document.getElementById('closeBuildingRoomsModal');
    if (closeBuildingRoomsBtn) {
        closeBuildingRoomsBtn.addEventListener('click', closeBuildingRoomsModal);
    }
    loadAllBuildingsForAccess();
}
function loadAllBuildingsForAccess() {
    const buildingsList = document.getElementById('accessBuildingsList');
    if (allAccessBuildingsData.length > 0) {
        renderAccessBuildingsList();
        return;
    }
    buildingsList.innerHTML = '<div class="loading-indicator">Loading buildings...</div>';
    window.apiClient.getAllAvailableBuildings()
        .then(data => {
            allAccessBuildingsData = data;
            renderAccessBuildingsList();
        })
        .catch(error => {
            buildingsList.innerHTML = `<div class="error-message">Failed to load buildings: ${error.message}</div>`;
        });
}
function renderAccessBuildingsList() {
    const buildingsList = document.getElementById('accessBuildingsList');
    buildingsList.innerHTML = '';
    if (allAccessBuildingsData.length === 0) {
        buildingsList.innerHTML = '<div class="no-buildings">No buildings found.</div>';
        return;
    }
    allAccessBuildingsData.forEach(building => {
        const card = document.createElement('div');
        card.className = 'building-access-card';
        card.dataset.id = building.buildingId;
        card.dataset.name = building.buildingName;
        card.dataset.description = building.buildingDescription || '';
        const name = document.createElement('div');
        name.className = 'building-access-name';
        name.textContent = building.buildingName;
        const description = document.createElement('div');
        description.className = 'building-access-description';
        description.textContent = building.buildingDescription || 'No description available';
        const actions = document.createElement('div');
        actions.className = 'building-access-actions';
        const requestBtn = document.createElement('button');
        requestBtn.className = 'request-full-access-btn';
        requestBtn.textContent = 'Request Full Access';
        requestBtn.dataset.buildingId = building.buildingId;
        const viewRoomsBtn = document.createElement('button');
        viewRoomsBtn.className = 'view-rooms-btn';
        viewRoomsBtn.textContent = 'View Rooms';
        viewRoomsBtn.dataset.buildingId = building.buildingId;
        viewRoomsBtn.dataset.buildingName = building.buildingName;
        actions.appendChild(requestBtn);
        actions.appendChild(viewRoomsBtn);
        card.appendChild(name);
        card.appendChild(description);
        card.appendChild(actions);
        buildingsList.appendChild(card);
    });
    addAccessBuildingListeners();
}
function addAccessBuildingListeners() {
    document.querySelectorAll('.request-full-access-btn').forEach(button => {
        button.addEventListener('click', function() {
            const buildingId = this.dataset.buildingId;
            requestBuildingAccess(buildingId);
        });
    });
    document.querySelectorAll('.view-rooms-btn').forEach(button => {
        button.addEventListener('click', function() {
            const buildingId = this.dataset.buildingId;
            const buildingName = this.dataset.buildingName;
            openBuildingRoomsModal(buildingId, buildingName);
        });
    });
}
function requestBuildingAccess(buildingId) {
    if (confirm('Are you sure you want to request access to this building?')) {
        window.apiClient.requestBuildingAccess(buildingId, 'full')
            .then(data => {
                if (data.success) {
                    alert('Access request submitted successfully!');
                } else {
                    alert(data.message || 'Failed to submit access request.');
                }
            })
            .catch(error => {
                alert(`Error: ${error.message}`);
            });
    }
}
function openBuildingRoomsModal(buildingId, buildingName) {
    const building = allAccessBuildingsData.find(b => b.buildingId == buildingId);
    if (!building) {
        return;
    }
    document.getElementById('buildingRoomsTitle').textContent = `Rooms in ${buildingName}`;
    const roomsList = document.getElementById('buildingRoomsList');
    roomsList.innerHTML = '';
    const floorDropdown = document.createElement('select');
    floorDropdown.id = 'buildingFloorsSelect';
    floorDropdown.className = 'building-floors-dropdown';
    building.floors.forEach(floor => {
        const option = document.createElement('option');
        option.value = floor.floorId;
        option.textContent = floor.floorName || `Floor ${floor.floorNumber}`;
        floorDropdown.appendChild(option);
    });
    const roomsContainer = document.createElement('div');
    roomsContainer.id = 'floorRoomsList';
    roomsContainer.className = 'floor-rooms-list';
    const roomSearchInput = document.createElement('input');
    roomSearchInput.type = 'text';
    roomSearchInput.placeholder = 'Search rooms';
    roomSearchInput.className = 'room-search-input';
    roomsList.appendChild(floorDropdown);
    roomsList.appendChild(roomSearchInput);
    roomsList.appendChild(roomsContainer);
    roomSearchInput.addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase().trim();
        const visibleRooms = roomsContainer.querySelectorAll('.room-item');
        visibleRooms.forEach(roomItem => {
            const roomName = roomItem.querySelector('.room-name').textContent.toLowerCase();
            const roomDescription = roomItem.querySelector('.room-description').textContent.toLowerCase();
            const matches = roomName.includes(searchTerm) || roomDescription.includes(searchTerm);
            roomItem.style.display = matches ? '' : 'none';
        });
    });
    floorDropdown.addEventListener('change', function() {
        const selectedFloorId = this.value;
        const selectedFloor = building.floors.find(f => f.floorId == selectedFloorId);
        roomsContainer.innerHTML = '';
        if (selectedFloor && selectedFloor.rooms && selectedFloor.rooms.length > 0) {
            selectedFloor.rooms.forEach(room => {
                const roomItem = document.createElement('div');
                roomItem.className = 'room-item';
                roomItem.dataset.roomId = room.id;
                roomItem.dataset.roomName = room.name;
                const roomName = document.createElement('div');
                roomName.className = 'room-name';
                roomName.textContent = room.name || room.roomId;
                const roomDescription = document.createElement('div');
                roomDescription.className = 'room-description';
                roomDescription.textContent = room.description || 'No description';
                const requestBtn = document.createElement('button');
                requestBtn.className = 'request-room-access-btn';
                requestBtn.textContent = 'Request Access';
                requestBtn.dataset.roomId = room.id;
                requestBtn.dataset.buildingId = buildingId;
                roomItem.appendChild(roomName);
                roomItem.appendChild(roomDescription);
                roomItem.appendChild(requestBtn);
                roomsContainer.appendChild(roomItem);
            });
            roomsContainer.querySelectorAll('.request-room-access-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const roomId = this.dataset.roomId;
                    const buildingId = this.dataset.buildingId;
                    requestRoomAccess(buildingId, roomId);
                });
            });
        } else {
            roomsContainer.innerHTML = '<div class="no-rooms">No rooms found on this floor.</div>';
        }
    });
    floorDropdown.dispatchEvent(new Event('change'));
    document.getElementById('buildingRoomsModal').style.display = 'flex';
    document.getElementById('overlay').style.display = 'block';
}
function renderBuildingRoomsList(rooms) {
    const roomsList = document.getElementById('buildingRoomsList');
    roomsList.innerHTML = '';
    if (rooms.length === 0) {
        roomsList.innerHTML = '<div class="no-rooms">No rooms found in this building.</div>';
        return;
    }
    rooms.forEach(room => {
        const roomItem = document.createElement('div');
        roomItem.className = 'room-item';
        roomItem.dataset.id = room.id;
        roomItem.dataset.name = room.name;
        roomItem.dataset.type = room.roomType || '';
        const roomInfo = document.createElement('div');
        roomInfo.className = 'room-info';
        const roomName = document.createElement('div');
        roomName.className = 'room-item-name';
        roomName.textContent = room.name || 'Unnamed Room';
        const roomDetails = document.createElement('div');
        roomDetails.className = 'room-item-details';
        roomDetails.textContent = `Floor: ${room.floorName || 'Unknown'} | Type: ${room.roomType || 'Standard'}`;
        roomInfo.appendChild(roomName);
        roomInfo.appendChild(roomDetails);
        const requestBtn = document.createElement('button');
        requestBtn.className = 'request-room-access-btn';
        requestBtn.textContent = 'Request Access';
        requestBtn.dataset.roomId = room.id;
        roomItem.appendChild(roomInfo);
        roomItem.appendChild(requestBtn);
        roomsList.appendChild(roomItem);
    });
    document.querySelectorAll('.request-room-access-btn').forEach(button => {
        button.addEventListener('click', function() {
            const roomId = this.dataset.roomId;
            requestRoomAccess(selectedBuildingForRooms.id, roomId);
        });
    });
}
function requestRoomAccess(buildingId, roomId) {
    if (confirm('Are you sure you want to request access to this room?')) {
        window.apiClient.requestRoomAccess(buildingId, roomId, 'room')
            .then(data => {
                if (data.success) {
                    alert('Room access request submitted successfully!');
                } else {
                    alert(data.message || 'Failed to submit room access request.');
                }
            })
            .catch(error => {
                alert(`Error: ${error.message}`);
            });
    }
}
function closeBuildingRoomsModal() {
    document.getElementById('buildingRoomsModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
    selectedBuildingForRooms = null;
}
function filterAccessBuildings(searchTerm) {
    const normalizedSearch = searchTerm.toLowerCase().trim();
    document.querySelectorAll('.building-access-card').forEach(card => {
        const name = card.dataset.name.toLowerCase();
        const description = card.dataset.description.toLowerCase();
        if (name.includes(normalizedSearch) || description.includes(normalizedSearch)) {
            card.style.display = '';
        } else {
            card.style.display = 'none';
        }
    });
}
function filterRoomsInModal(searchTerm) {
    const normalizedSearch = searchTerm.toLowerCase().trim();
    document.querySelectorAll('.room-item').forEach(item => {
        const name = item.dataset.name.toLowerCase();
        const type = item.dataset.type.toLowerCase();
        if (name.includes(normalizedSearch) || type.includes(normalizedSearch)) {
            item.style.display = '';
        } else {
            item.style.display = 'none';
        }
    });
}
function loadPendingRequests() {
    const requestsList = document.getElementById('pendingRequestsList');
    requestsList.innerHTML = '<div class="loading-indicator">Loading pending requests...</div>';
    window.apiClient.getAdminBuildingRequests()
        .then(requests => {
            pendingRequests = requests || [];
            renderPendingRequests();
        })
        .catch(error => {
            requestsList.innerHTML = `<div class="error-message">Failed to load pending requests: ${error.message}</div>`;
        });
}
function renderPendingRequests() {
    const requestsList = document.getElementById('pendingRequestsList');
    requestsList.innerHTML = '';
    if (pendingRequests.length === 0) {
        requestsList.innerHTML = '<div class="no-data">No pending access requests found.</div>';
        return;
    }
    pendingRequests.forEach(request => {
        const requestItem = document.createElement('div');
        requestItem.className = 'pending-request-item';
        requestItem.dataset.id = request.requestId;
        const header = document.createElement('div');
        header.className = 'pending-request-header';
        const userInfo = document.createElement('div');
        userInfo.className = 'pending-request-user';
        if (request.user) {
            userInfo.textContent = `User: ${request.user.name || request.user.email || request.user.id}`;
        } else {
            userInfo.textContent = `User: ID ${request.userId || 'Unknown'}`;
        }
        const dateInfo = document.createElement('div');
        dateInfo.className = 'pending-request-date';
        dateInfo.textContent = request.requestDate ? new Date(request.requestDate).toLocaleString() : 'Pending';
        header.appendChild(userInfo);
        header.appendChild(dateInfo);
        const target = document.createElement('div');
        target.className = 'pending-request-target';
        if (request.roomId) {
            target.textContent = `Requesting access to: ${request.roomName || 'Room'} in ${request.buildingName || 'Building'}`;
        } else {
            target.textContent = `Requesting access to: ${request.buildingName || 'Building'}`;
        }
        const actions = document.createElement('div');
        actions.className = 'pending-request-actions';
        const approveBtn = document.createElement('button');
        approveBtn.className = 'approve-btn';
        approveBtn.textContent = 'Approve';
        approveBtn.dataset.requestId = request.requestId;
        const denyBtn = document.createElement('button');
        denyBtn.className = 'deny-btn';
        denyBtn.textContent = 'Deny';
        denyBtn.dataset.requestId = request.requestId;
        actions.appendChild(approveBtn);
        actions.appendChild(denyBtn);
        requestItem.appendChild(header);
        requestItem.appendChild(target);
        requestItem.appendChild(actions);
        requestsList.appendChild(requestItem);
    });
    document.querySelectorAll('.approve-btn').forEach(button => {
        button.addEventListener('click', function() {
            const requestId = this.dataset.requestId;
            processAccessRequest(requestId, true);
        });
    });
    document.querySelectorAll('.deny-btn').forEach(button => {
        button.addEventListener('click', function() {
            const requestId = this.dataset.requestId;
            processAccessRequest(requestId, false);
        });
    });
}
function processAccessRequest(requestId, approved) {
    const action = approved ? 'approve' : 'deny';
    if (confirm(`Are you sure you want to ${action} this access request?`)) {
        window.apiClient.processAccessRequest(requestId, approved)
            .then(data => {
                if (data.success) {
                    alert(`Request ${approved ? 'approved' : 'denied'} successfully!`);
                    pendingRequests = pendingRequests.filter(request => request.id != requestId);
                    renderPendingRequests();
                } else {
                    alert(data.message || `Failed to ${action} request.`);
                }
            })
            .catch(error => {
                alert(`Error: ${error.message}`);
            });
    }
}
function closeAdminContactsModal() {
    document.getElementById('adminContactsModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}
function formatActionType(actionType) {
    if (!actionType) return '-';
    return actionType
        .replace(/_/g, ' ')
        .split(' ')
        .map(word => word.charAt(0) + word.slice(1).toLowerCase())
        .join(' ');
}
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    return `${(date.getMonth() + 1).toString().padStart(2, '0')}/${date.getDate().toString().padStart(2, '0')}/${date.getFullYear()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}
function addNavigationListeners() {
    document.querySelectorAll('.nav-button').forEach(button => {
        button.addEventListener('click', function() {
            const sectionId = this.dataset.section;
            showContent(sectionId);
            const globalSearchInput = document.getElementById('globalSearch');
            const globalSearchButton = document.getElementById('globalSearchButton');
            if (sectionId === 'access-management') {
                globalSearchInput.disabled = true;
                globalSearchInput.placeholder = 'Search disabled in Access Management';
                globalSearchButton.disabled = true;
            } else {
                globalSearchInput.disabled = false;
                globalSearchInput.placeholder = 'Search buildings';
                globalSearchButton.disabled = false;
            }
        });
    });
    const searchButton = document.getElementById('globalSearchButton');
    if (searchButton) {
        searchButton.addEventListener('click', handleGlobalSearch);
    }
    const logoutButton = document.getElementById('logoutButton');
    if (logoutButton) {
        logoutButton.addEventListener('click', function() {
            window.location.href = '/logout';
        });
    }
    const modalButtons = {
        'openModalBtn': openModal,
        'closeModal': closeModal,
        'closeBuildingEditModal': closeBuildingEditModal,
        'closeRoomEditModal': closeRoomEditModal,
        'closeRoomModal': closeRoomModal,
        'closeAdminContactsModal': closeAdminContactsModal,
        'closeBuildingDetailsModal': closeBuildingDetailsModal
    };
    Object.keys(modalButtons).forEach(buttonId => {
        const button = document.getElementById(buttonId);
        if (button) {
            button.addEventListener('click', modalButtons[buttonId]);
        }
    });
}
function closeRoomModal() {
    document.getElementById('roomModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}
function closeBuildingDetailsModal() {
    document.getElementById('buildingDetailsModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}
function closeRequestAccessModal() {
    document.getElementById('requestAccessModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}
function showContent(sectionId) {
    const contents = document.querySelectorAll('.content');
    const searchInput = document.getElementById('globalSearch');
    switch(sectionId) {
        case 'buildings':
            searchInput.placeholder = 'Search buildings';
            loadBuildings();
            break;
        case 'rooms':
            searchInput.placeholder = 'Search buildings for rooms';
            loadAccessibleRooms();
            break;
        case 'access-management':
            searchInput.placeholder = 'Search buildings for access';
            initAccessManagement();
            break;
        default:
            searchInput.placeholder = 'Search';
    }
    contents.forEach(content => {
        content.classList.remove('active');
        if (content.id === sectionId) {
            content.classList.add('active');
        }
    });
}
function filterBuildings(searchTerm) {
    const normalizedSearch = (searchTerm || '').toLowerCase().trim();
    const buildingItems = document.querySelectorAll('#buildingList .building-item');
    buildingItems.forEach(item => {
        const name = (item.dataset.name || '').toLowerCase();
        const description = (item.dataset.description || '').toLowerCase();
        const nameMatches = name.includes(normalizedSearch);
        const descriptionMatches = description.includes(normalizedSearch);
        item.style.display = (nameMatches || descriptionMatches) ? '' : 'none';
    });
}
function handleGlobalSearch() {
    const searchTerm = document.getElementById('globalSearch').value;
    const activeContent = document.querySelector('.content.active');
    if (activeContent) {
        if (activeContent.id === 'buildings') {
            filterBuildings(searchTerm);
        } else if (activeContent.id === 'rooms') {
            filterRooms(searchTerm);
        }
    }
}
function loadBuildings() {
    const buildingList = document.getElementById('buildingList');
    if (allBuildingsData.length > 0) {
        renderBuildingsList();
        return;
    }
    buildingList.innerHTML = '<div class="loading-indicator">Loading buildings...</div>';
    fetch('/get_buildings')
        .then(response => response.json())
        .then(data => {
            if (data && data.buildings && data.buildings.length > 0) {
                allBuildingsData = data.buildings.map(building => ({
                    id: building[0],
                    name: building[1],
                    floorCount: building[2],
                    description: building[3] || '',
                    adminContacts: building[4] || [],
                    buildingLogs: building[5] || []
                }));
                renderBuildingsList();
            } else {
                buildingList.innerHTML = '<div class="no-buildings">No buildings found. Create a new building to get started.</div>';
            }
        })
        .catch(err => {
            buildingList.innerHTML = '<div class="error-message">Failed to load buildings. Please try again.</div>';
        });
}
function redrawBuilding(buildingId, floorCount) {
    window.location.href = `/draw?building_id=${buildingId}&floors=${floorCount}`;
}
function deleteBuilding(buildingId) {
    if (confirm('Are you sure you want to delete this building? This action cannot be undone.')) {
        const formData = new FormData();
        formData.append('building_id', buildingId);
        fetch('/delete_building', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data && data.message) {
                alert(data.message);
                loadBuildings();
            } else if (data && data.error) {
                alert(`Error: ${data.error}`);
            }
        })
        .catch(err => {
            alert("There was an error deleting the building. Please try again.");
        });
    }
}
function openModal() {
    document.getElementById('buildingModal').style.display = 'flex';
    document.getElementById('overlay').style.display = 'block';
}
function closeModal() {
    document.getElementById('buildingModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}
function openBuildingEditModal(building) {
    document.getElementById('editBuildingId').value = building.id;
    document.getElementById('editBuildingName').value = building.name;
    document.getElementById('editBuildingDescription').value = building.description || '';
    const floorsInput = document.getElementById('editBuildingFloors');
    floorsInput.value = building.floorCount;
    floorsInput.setAttribute('min', building.floorCount);
    document.getElementById('buildingEditModal').style.display = 'flex';
    document.getElementById('overlay').style.display = 'block';
}
function closeBuildingEditModal() {
    document.getElementById('buildingEditModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}
function submitBuildingEdit(event) {
    event.preventDefault();
    const buildingId = document.getElementById('editBuildingId').value;
    const buildingName = document.getElementById('editBuildingName').value;
    const description = document.getElementById('editBuildingDescription').value;
    const floors = document.getElementById('editBuildingFloors').value;
    if (!buildingName) {
        alert('Building name is required');
        return;
    }
    const formData = new FormData();
    formData.append('building_id', buildingId);
    formData.append('building_name', buildingName);
    formData.append('description', description);
    formData.append('floors', floors);
    fetch('/update_building', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.message) {
            alert(data.message);
            closeBuildingEditModal();
            loadBuildings();
        } else if (data.error) {
            alert(`Error: ${data.error}`);
        }
    })
    .catch(err => {
        alert("Error updating building. Please try again.");
    });
}
function openBuildingLogsModal(buildingId, buildingName, logs) {
    document.getElementById('buildingLogsTitle').textContent = `Logs for ${buildingName}`;
    const logsContainer = document.getElementById('buildingLogsContent');
    logsContainer.innerHTML = '';
    if (logs && logs.length > 0) {
        const table = document.createElement('table');
        table.className = 'logs-table';
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        const headers = ['Action', 'Date', 'User', 'Details', 'Floor'];
        headers.forEach(headerText => {
            const th = document.createElement('th');
            th.textContent = headerText;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);
        const tbody = document.createElement('tbody');
        const sortedLogs = [...logs].sort((a, b) => {
            return new Date(b.timestamp) - new Date(a.timestamp);
        });
        sortedLogs.forEach(log => {
            const row = document.createElement('tr');
            const actionCell = document.createElement('td');
            actionCell.className = 'action-type';
            actionCell.textContent = formatActionType(log.actionType);
            row.appendChild(actionCell);
            const dateCell = document.createElement('td');
            dateCell.textContent = formatDate(log.timestamp);
            row.appendChild(dateCell);
            const userCell = document.createElement('td');
            userCell.textContent = `User ${log.userId}`;
            row.appendChild(userCell);
            const detailsCell = document.createElement('td');
            detailsCell.textContent = log.details || '-';
            row.appendChild(detailsCell);
            const floorCell = document.createElement('td');
            floorCell.textContent = log.floorName || '-';
            row.appendChild(floorCell);
            tbody.appendChild(row);
        });
        table.appendChild(tbody);
        logsContainer.appendChild(table);
    } else {
        const noLogsDiv = document.createElement('div');
        noLogsDiv.className = 'no-logs';
        noLogsDiv.textContent = 'No logs available for this building.';
        logsContainer.appendChild(noLogsDiv);
    }
    document.getElementById('buildingLogsModal').style.display = 'flex';
    document.getElementById('overlay').style.display = 'block';
}
function closeBuildingLogsModal() {
    document.getElementById('buildingLogsModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}
function createRoomCard(room) {
    const roomCard = document.createElement('div');
    roomCard.className = 'room-card';
    roomCard.dataset.roomId = room.id;
    roomCard.dataset.buildingId = room.buildingId;
    const roomInfo = document.createElement('div');
    roomInfo.className = 'room-info';
    const roomName = document.createElement('h4');
    roomName.textContent = room.name || 'Unnamed Room';
    roomInfo.appendChild(roomName);
    const floorInfo = document.createElement('p');
    const floorLabel = document.createElement('span');
    floorLabel.className = 'label';
    floorLabel.textContent = 'Floor:';
    const floorValue = document.createElement('span');
    floorValue.textContent = room.floorName || `Floor ${room.floorNumber}` || 'Unknown';
    floorInfo.appendChild(floorLabel);
    floorInfo.appendChild(document.createTextNode(' '));
    floorInfo.appendChild(floorValue);
    roomInfo.appendChild(floorInfo);
    const typeInfo = document.createElement('p');
    const typeLabel = document.createElement('span');
    typeLabel.className = 'label';
    typeLabel.textContent = 'Type:';
    const typeValue = document.createElement('span');
    typeValue.textContent = room.roomType || room.category || 'Standard Room';
    typeInfo.appendChild(typeLabel);
    typeInfo.appendChild(document.createTextNode(' '));
    typeInfo.appendChild(typeValue);
    roomInfo.appendChild(typeInfo);
    if (room.description) {
        roomCard.dataset.description = room.description;
    }
    const roomActions = document.createElement('div');
    roomActions.className = 'room-actions';
    const editButton = document.createElement('button');
    editButton.className = 'edit-room-btn';
    editButton.textContent = 'Edit Details';
    editButton.dataset.roomId = room.id; 
    roomActions.appendChild(editButton);
    roomCard.appendChild(roomInfo);
    roomCard.appendChild(roomActions);
    return roomCard;
}
function addRoomButtonListeners() {
    document.querySelectorAll('.edit-room-btn').forEach(button => {
        button.addEventListener('click', function() {
            const roomId = this.dataset.roomId;
            openRoomEditModal(roomId);
        });
    });
}
let lastAccessibleRoomsLoadTime = 0;
let isAccessibleRoomsLoading = false;
async function loadAccessibleRooms() {
    const now = Date.now();
    const buildingCardsContainer = document.getElementById('buildingCards');
    const loadingIndicator = document.getElementById('buildingCardsLoading');
    if (buildingsData.length > 0 && buildingCardsContainer.children.length === 0) {
        renderRoomsList();
        return;
    }
    if ((now - lastAccessibleRoomsLoadTime < 10000) || isAccessibleRoomsLoading) {
        return;
    }
    try {
        isAccessibleRoomsLoading = true;
        lastAccessibleRoomsLoadTime = now;
        if (buildingCardsContainer.children.length === 0) {
            buildingCardsContainer.innerHTML = '';
            loadingIndicator.style.display = 'block';
        } else {
            const loadingOverlay = document.createElement('div');
            loadingOverlay.className = 'loading-overlay';
            loadingOverlay.id = 'roomsLoadingOverlay';
            loadingOverlay.innerHTML = '<div class="loading-spinner">Updating...</div>';
            document.body.appendChild(loadingOverlay);
        }
        const response = await fetch('/api/protected/rooms/accessible');
        const data = await response.json();
        if (data && data.buildings) {
            buildingsData = data.buildings;
            roomsData = [];
            buildingsData.forEach(building => {
                if (building.rooms && building.rooms.length > 0) {
                    roomsData = roomsData.concat(building.rooms);
                }
            });
            renderRoomsList();
        } else if (buildingCardsContainer.children.length === 0) {
            buildingCardsContainer.innerHTML = 
                '<div class="error">Error loading buildings and rooms. Please try again later.</div>';
        }
    } catch (error) {
        if (buildingCardsContainer.children.length === 0) {
            buildingCardsContainer.innerHTML = 
                '<div class="error">An unexpected error occurred. Please try again later.</div>';
        }
    } finally {
        loadingIndicator.style.display = 'none';
        const loadingOverlay = document.getElementById('roomsLoadingOverlay');
        if (loadingOverlay) {
            loadingOverlay.remove();
        }
        isAccessibleRoomsLoading = false;
    }
}
function filterRooms(searchTerm) {
    const normalizedSearchTerm = searchTerm.toLowerCase().trim();
    document.querySelectorAll('.building-card').forEach(buildingCard => {
        const buildingName = buildingCard.dataset.buildingName.toLowerCase();
        const roomCards = buildingCard.querySelectorAll('.room-card');
        let buildingMatches = buildingName.includes(normalizedSearchTerm);
        let hasMatchingRooms = false;
        if (!normalizedSearchTerm) {
            roomCards.forEach(roomCard => {
                roomCard.style.display = '';
            });
            buildingCard.style.display = '';
            return;
        }
        roomCards.forEach(roomCard => {
            const roomName = roomCard.querySelector('h4').textContent.toLowerCase();
            const roomType = roomCard.querySelector('.label + span').textContent.toLowerCase();
            const roomDescription = roomCard.dataset.description ? roomCard.dataset.description.toLowerCase() : '';
            const roomMatches = roomName.includes(normalizedSearchTerm) || 
                                roomType.includes(normalizedSearchTerm) || 
                                roomDescription.includes(normalizedSearchTerm);
            if (roomMatches) {
                roomCard.style.display = '';
                hasMatchingRooms = true;
            } else {
                roomCard.style.display = 'none';
            }
        });
        buildingCard.style.display = (buildingMatches || hasMatchingRooms) ? '' : 'none';
    });
}
function closeRoomEditModal() {
    document.getElementById('roomEditModal').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}
function validateRoomEditForm() {
    const roomName = document.getElementById('editRoomName').value.trim();
    const email = document.getElementById('editEmail').value.trim();
    if (!roomName) {
        alert('Room name is required');
        return false;
    }
    if (email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            alert('Please enter a valid email address');
            return false;
        }
    }
    return true;
}
async function handleRoomEditFormSubmit(event) {
    event.preventDefault();
    if (!validateRoomEditForm()) {
        return;
    }
    const roomId = document.getElementById('editRoomId').value;
    const roomName = document.getElementById('editRoomName').value.trim();
    const description = document.getElementById('editRoomDescription').value.trim();
    const owner = document.getElementById('editOwner').value.trim();
    const email = document.getElementById('editEmail').value.trim();
    const phone = document.getElementById('editPhone').value.trim();
    const roomType = document.getElementById('editRoomType').value.trim();
    const buildingId = document.getElementById('editRoomBuildingId').value;
    const contactDetails = [owner, email, phone].join('|');
    const roomData = {
        name: roomName,
        description: description,
        contactDetails: contactDetails,
        roomType: roomType,
        buildingId: buildingId 
    };
    try {
        const response = await fetch(`/update_room/${roomId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(roomData)
        });
        const data = await response.json();
        if (data.success) {
            alert('Room details updated successfully');
            closeRoomEditModal();
            loadAccessibleRooms();
        } else {
            alert(data.message || 'Failed to update room details');
        }
    } catch (error) {
        alert('An error occurred while updating room details. Please try again.');
    }
}
function openRoomEditModal(roomId) {
    const room = roomsData.find(r => r.id == roomId);
    if (!room) {
        alert('Room data not found');
        return;
    }
    let owner = '';
    let email = '';
    let phone = '';
    if (room.contactDetails) {
        const parts = room.contactDetails.split(' | ');
        if (parts.length === 1) {
            const fallbackParts = room.contactDetails.split('|');
            if (fallbackParts.length >= 1) owner = fallbackParts[0].trim();
            if (fallbackParts.length >= 2) email = fallbackParts[1].trim();
            if (fallbackParts.length >= 3) phone = fallbackParts[2].trim();
        } else {
            if (parts.length >= 1) owner = parts[0].trim();
            if (parts.length >= 2) email = parts[1].trim();
            if (parts.length >= 3) phone = parts[2].trim();
        }
    }
    const editRoomNameField = document.getElementById('editRoomName');
    const editRoomTypeField = document.getElementById('editRoomType');
    const editRoomDescriptionField = document.getElementById('editRoomDescription');
    const editOwnerField = document.getElementById('editOwner');
    const editEmailField = document.getElementById('editEmail');
    const editPhoneField = document.getElementById('editPhone');
    const editRoomIdField = document.getElementById('editRoomId');
    if (!editRoomNameField || !editRoomTypeField || !editRoomDescriptionField || 
        !editOwnerField || !editEmailField || !editPhoneField || !editRoomIdField) {
        alert('Error: Could not load the edit form properly. Please try again.');
        return;
    }
    editRoomIdField.value = roomId;
    editRoomNameField.value = room.name || '';
    editRoomDescriptionField.value = room.description || '';
    editOwnerField.value = owner;
    editEmailField.value = email;
    editPhoneField.value = phone;
    const roomType = room.roomType || '';
    editRoomTypeField.value = roomType;
    let buildingIdField = document.getElementById('editRoomBuildingId');
    if (!buildingIdField) {
        buildingIdField = document.createElement('input');
        buildingIdField.type = 'hidden';
        buildingIdField.id = 'editRoomBuildingId';
        document.getElementById('roomEditForm').appendChild(buildingIdField);
    }
    buildingIdField.value = room.buildingId;
    document.getElementById('roomEditModal').style.display = 'flex';
    document.getElementById('overlay').style.display = 'block';
}
function loadAccessRequests() {
    const accessRequestsList = document.getElementById('accessRequestsList');
    if (accessRequestsList) {
        accessRequestsList.innerHTML = '<div class="loading-indicator">Loading your access requests...</div>';
    }
    window.apiClient.getUserRequests()
        .then(requests => {
            if (accessRequestsList) {
                if (requests.length === 0) {
                    accessRequestsList.innerHTML = '<div class="no-data">You have no pending access requests.</div>';
                    return;
                }
                accessRequestsList.innerHTML = '';
                requests.forEach(request => {
                    const li = document.createElement('li');
                    li.className = 'access-request-item';
                    const buildingName = document.createElement('div');
                    buildingName.className = 'building-name';
                    buildingName.textContent = request.buildingName || 'Unknown Building';
                    const details = document.createElement('div');
                    details.className = 'request-details';
                    if (request.roomId) {
                        details.textContent = `Room: ${request.roomName}`;
                    } else {
                        details.textContent = `Access Type: ${request.accessType || 'Standard'}`;
                    }
                    const status = document.createElement('div');
                    status.className = `request-status ${request.status.toLowerCase()}`;
                    status.textContent = request.status;
                    li.appendChild(buildingName);
                    li.appendChild(details);
                    li.appendChild(status);
                    accessRequestsList.appendChild(li);
                });
            }
        })
        .catch(error => {
            if (accessRequestsList) {
                accessRequestsList.innerHTML = `<div class="error-message">Failed to load access requests: ${error.message}</div>`;
            }
        });
}
function preloadAllData() {
    const dataPromises = [
        fetch('/get_buildings').then(response => response.json()),
        fetch('/api/protected/rooms/accessible').then(response => response.json())
    ];
    Promise.all(dataPromises)
        .then(([buildingsData, roomsData]) => {
            if (buildingsData && buildingsData.buildings && buildingsData.buildings.length > 0) {
                allBuildingsData = buildingsData.buildings.map(building => ({
                    id: building[0],
                    name: building[1],
                    floorCount: building[2],
                    description: building[3] || ''
                }));
                if (document.querySelector('.content.active#buildings')) {
                    renderBuildingsList();
                }
            }
            if (roomsData && roomsData.buildings) {
                buildingsData = roomsData.buildings;
                roomsData = [];
                buildingsData.forEach(building => {
                    if (building.rooms && building.rooms.length > 0) {
                        roomsData = roomsData.concat(building.rooms);
                    }
                });
                if (document.querySelector('.content.active#rooms')) {
                    renderRoomsList();
                }
            }
            lastAccessibleRoomsLoadTime = Date.now();
        })
        .catch(error => {
        });
}
function renderBuildingsList() {
    const buildingList = document.getElementById('buildingList');
    buildingList.innerHTML = '';
    if (allBuildingsData.length === 0) {
        buildingList.innerHTML = '<div class="no-buildings">No buildings found. Create a new building to get started.</div>';
        return;
    }
    allBuildingsData.forEach(building => {
        const li = document.createElement('li');
        li.className = 'building-item';
        li.dataset.id = building.id;
        li.dataset.name = building.name.toLowerCase();
        li.dataset.description = building.description.toLowerCase();
        const infoSection = document.createElement('div');
        infoSection.className = 'building-info';
        const title = document.createElement('div');
        title.className = 'building-title';
        title.textContent = building.name;
        const descriptionEl = document.createElement('div');
        descriptionEl.className = 'building-description';
        descriptionEl.textContent = building.description || 'No description available';
        const floors = document.createElement('div');
        floors.className = 'building-floors';
        floors.textContent = `${building.floorCount} floors`;
        infoSection.appendChild(title);
        infoSection.appendChild(descriptionEl);
        infoSection.appendChild(floors);
        const actionsSection = document.createElement('div');
        actionsSection.className = 'building-actions';
        const editFloorBtn = document.createElement('button');
        editFloorBtn.className = 'action-button edit-floor-btn';
        editFloorBtn.textContent = 'Edit Floor Plan';
        editFloorBtn.dataset.buildingId = building.id;
        editFloorBtn.dataset.floorCount = building.floorCount;
        const editDetailsBtn = document.createElement('button');
        editDetailsBtn.className = 'action-button edit-details-btn';
        editDetailsBtn.textContent = 'Edit Details';
        editDetailsBtn.dataset.buildingId = building.id;
        const adminListBtn = document.createElement('button');
        adminListBtn.className = 'action-button admin-list-btn';
        adminListBtn.textContent = 'Admin List';
        adminListBtn.dataset.buildingId = building.id;
        adminListBtn.dataset.buildingName = building.name;
        if (building.adminContacts) {
            adminListBtn.dataset.adminContacts = JSON.stringify(building.adminContacts);
        }
        const logsBtn = document.createElement('button');
        logsBtn.className = 'action-button logs-btn';
        logsBtn.textContent = 'Building Logs';
        logsBtn.dataset.buildingId = building.id;
        logsBtn.dataset.buildingName = building.name;
        if (building.buildingLogs) {
            logsBtn.dataset.buildingLogs = JSON.stringify(building.buildingLogs);
        }
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'action-button delete-btn';
        deleteBtn.textContent = 'Delete';
        deleteBtn.dataset.buildingId = building.id;
        actionsSection.appendChild(editFloorBtn);
        actionsSection.appendChild(editDetailsBtn);
        actionsSection.appendChild(adminListBtn);
        actionsSection.appendChild(logsBtn);
        actionsSection.appendChild(deleteBtn);
        li.appendChild(infoSection);
        li.appendChild(actionsSection);
        buildingList.appendChild(li);
    });
    addBuildingButtonListeners();
}
function renderRoomsList() {
    const buildingCardsContainer = document.getElementById('buildingCards');
    const loadingIndicator = document.getElementById('buildingCardsLoading');
    buildingCardsContainer.innerHTML = '';
    loadingIndicator.style.display = 'none';
    if (buildingsData.length === 0) {
        buildingCardsContainer.innerHTML = 
            '<div class="no-data">No accessible buildings found. Request building access to view rooms.</div>';
        return;
    }
    buildingsData.forEach(building => {
        const buildingCard = createBuildingCard(building);
        buildingCardsContainer.appendChild(buildingCard);
    });
    addRoomButtonListeners();
}
function createBuildingCard(building) {
    const buildingCard = document.createElement('div');
    buildingCard.className = 'building-card';
    buildingCard.dataset.buildingId = building.buildingId;
    buildingCard.dataset.buildingName = building.buildingName;
    const buildingCardHeader = document.createElement('div');
    buildingCardHeader.className = 'building-card-header';
    const buildingTitle = document.createElement('h3');
    buildingTitle.textContent = building.buildingName;
    const expandButton = document.createElement('button');
    expandButton.className = 'expand-button';
    expandButton.textContent = '▼';
    expandButton.setAttribute('aria-label', 'Expand building');
    buildingCardHeader.appendChild(buildingTitle);
    buildingCardHeader.appendChild(expandButton);
    const buildingCardContent = document.createElement('div');
    buildingCardContent.className = 'building-card-content';
    const roomsList = document.createElement('div');
    roomsList.className = 'rooms-list';
    if (building.rooms && building.rooms.length > 0) {
        building.rooms.forEach(room => {
            const roomCard = createRoomCard(room);
            roomsList.appendChild(roomCard);
        });
    } else {
        const noRoomsDiv = document.createElement('div');
        noRoomsDiv.className = 'no-data';
        noRoomsDiv.textContent = 'No rooms found in this building';
        roomsList.appendChild(noRoomsDiv);
    }
    buildingCardContent.appendChild(roomsList);
    buildingCard.appendChild(buildingCardHeader);
    buildingCard.appendChild(buildingCardContent);
    buildingCardHeader.addEventListener('click', function() {
        const isExpanded = buildingCardContent.classList.contains('expanded');
        if (isExpanded) {
            buildingCardContent.classList.remove('expanded');
            expandButton.classList.remove('expanded');
            expandButton.textContent = '▼';
            expandedBuildingIds.set(building.buildingId, false);
        } else {
            buildingCardContent.classList.add('expanded');
            expandButton.classList.add('expanded');
            expandButton.textContent = '▲';
            expandedBuildingIds.set(building.buildingId, true);
        }
    });
    if (expandedBuildingIds.get(building.buildingId)) {
        buildingCardContent.classList.add('expanded');
        expandButton.classList.add('expanded');
        expandButton.textContent = '▲';
    }
    return buildingCard;
}
document.addEventListener('DOMContentLoaded', function() {
    addNavigationListeners();
    const globalSearchInput = document.getElementById('globalSearch');
    if (globalSearchInput) {
        globalSearchInput.addEventListener('input', handleGlobalSearch);
    }
    document.querySelectorAll('.modal .form-buttons button[type="button"]').forEach(button => {
        const modalId = button.closest('.modal').id;
        button.addEventListener('click', function() {
            document.getElementById(modalId).style.display = 'none';
            document.getElementById('overlay').style.display = 'none';
        });
    });
    const buildingForm = document.getElementById('buildingForm');
    if (buildingForm) {
        buildingForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const buildingName = document.getElementById('buildingName').value;
            const floors = document.getElementById('floors').value;
            const description = document.getElementById('buildingDescription').value;
            if (!buildingName) {
                alert('Building name is required');
                return;
            }
            const formData = new FormData();
            formData.append('building_name', buildingName);
            formData.append('floors', floors);
            formData.append('description', description);
            fetch('/create_building', {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (response.redirected) {
                    window.location.href = response.url;
                } else {
                    return response.json();
                }
            })
            .then(data => {
                if (data && data.error) {
                    alert(data.error);
                }
            })
            .catch(err => {
                alert('Error creating building. Please try again.');
            });
        });
    }
    const buildingEditForm = document.getElementById('buildingEditForm');
    if (buildingEditForm) {
        buildingEditForm.addEventListener('submit', submitBuildingEdit);
    }
    const roomEditForm = document.getElementById('roomEditForm');
    if (roomEditForm) {
        roomEditForm.addEventListener('submit', handleRoomEditFormSubmit);
    }
    const roomSearchInput = document.getElementById('roomBuildingSearch');
    if (roomSearchInput) {
        roomSearchInput.addEventListener('input', function() {
            filterRooms(this.value);
        });
    }
    const closeAdminModalBtn = document.getElementById('closeAdminContactsModal');
    if (closeAdminModalBtn) {
        closeAdminModalBtn.addEventListener('click', closeAdminContactsModal);
    }
    if (document.getElementById('buildingList')?.children.length > 0) {
        addBuildingButtonListeners();
    }
    if (document.querySelectorAll('.room-card').length > 0) {
        addRoomButtonListeners();
    }
    const closeBuildingLogsBtn = document.getElementById('closeBuildingLogsModal');
    if (closeBuildingLogsBtn) {
        closeBuildingLogsBtn.addEventListener('click', closeBuildingLogsModal);
    }
    if (document.querySelector('.content.active#access-management')) {
        initAccessManagement();
    }
    const navCards = document.querySelectorAll('.nav-card');
    navCards.forEach(card => {
        card.addEventListener('click', function () {
            const section = card.getAttribute('data-section');
            showContent(section);
        });
    });
    loadBuildings();
    if (document.querySelector('.content.active#access-management') && 
    document.querySelector('#building-tenants.access-tab-content.active')) {
    initTenantManagement();
}
});
let allTenants = [];
let currentViewMode = 'tenant';
function loadTenants() {
    const tenantsListContainer = document.getElementById('tenantsListContainer');
    const buildingTenantsContainer = document.getElementById('buildingTenantsContainer');
    document.querySelectorAll('#tenantView .loading-indicator, #buildingView .loading-indicator').forEach(el => {
        el.style.display = 'block';
    });
    tenantsListContainer.innerHTML = '';
    buildingTenantsContainer.innerHTML = '';
    fetch('/api/protected/tenants', {
        headers: {
            'Authorization': `Bearer ${sessionStorage.getItem('auth_token')}`
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        allTenants = data;
        document.querySelectorAll('#tenantView .loading-indicator, #buildingView .loading-indicator').forEach(el => {
            el.style.display = 'none';
        });
        if (allTenants.length === 0) {
            tenantsListContainer.innerHTML = '<div class="no-data">No tenants found in your buildings.</div>';
            buildingTenantsContainer.innerHTML = '<div class="no-data">No tenants found in your buildings.</div>';
            return;
        }
        if (currentViewMode === 'tenant') {
            renderTenantView();
        } else {
            renderBuildingView();
        }
    })
    .catch(error => {
        document.querySelectorAll('#tenantView .loading-indicator, #buildingView .loading-indicator').forEach(el => {
            el.style.display = 'none';
        });
        tenantsListContainer.innerHTML = `<div class="error-message">Failed to load tenants: ${error.message}</div>`;
        buildingTenantsContainer.innerHTML = `<div class="error-message">Failed to load tenants: ${error.message}</div>`;
    });
}
function renderTenantView() {
    const container = document.getElementById('tenantsListContainer');
    container.innerHTML = '';
    const tenantsByUser = {};
    allTenants.forEach(tenant => {
        if (!tenantsByUser[tenant.userId]) {
            tenantsByUser[tenant.userId] = {
                userId: tenant.userId,
                userName: tenant.userName,
                userEmail: tenant.userEmail,
                buildings: {}
            };
        }
        if (!tenantsByUser[tenant.userId].buildings[tenant.buildingId]) {
            tenantsByUser[tenant.userId].buildings[tenant.buildingId] = {
                buildingId: tenant.buildingId,
                buildingName: tenant.buildingName,
                rooms: []
            };
        }
        tenantsByUser[tenant.userId].buildings[tenant.buildingId].rooms.push({
            roomId: tenant.roomId,
            roomName: tenant.roomName,
            floorNumber: tenant.floorNumber,
            floorName: tenant.floorName,
            permissionId: tenant.userRoomPermissionsId
        });
    });
    Object.values(tenantsByUser).forEach(tenant => {
        const tenantCard = document.createElement('div');
        tenantCard.className = 'tenant-card';
        tenantCard.dataset.userId = tenant.userId;
        const tenantHeader = document.createElement('div');
        tenantHeader.className = 'tenant-header';
        const tenantInfo = document.createElement('div');
        tenantInfo.className = 'tenant-info';
        const tenantName = document.createElement('div');
        tenantName.className = 'tenant-name';
        tenantName.textContent = tenant.userName;
        const tenantEmail = document.createElement('div');
        tenantEmail.className = 'tenant-email';
        tenantEmail.textContent = tenant.userEmail;
        tenantInfo.appendChild(tenantName);
        tenantInfo.appendChild(tenantEmail);
        const expandButton = document.createElement('button');
        expandButton.className = 'expand-button';
        expandButton.textContent = '▼';
        expandButton.setAttribute('aria-label', 'Expand tenant details');
        tenantHeader.appendChild(tenantInfo);
        tenantHeader.appendChild(expandButton);
        const tenantContent = document.createElement('div');
        tenantContent.className = 'tenant-content';
        Object.values(tenant.buildings).forEach(building => {
            const buildingCard = document.createElement('div');
            buildingCard.className = 'tenant-building-card';
            const buildingHeader = document.createElement('div');
            buildingHeader.className = 'tenant-building-header';
            const buildingName = document.createElement('div');
            buildingName.className = 'tenant-building-name';
            buildingName.textContent = building.buildingName;
            const expandBuildingButton = document.createElement('button');
            expandBuildingButton.className = 'expand-button';
            expandBuildingButton.textContent = '▼';
            expandBuildingButton.setAttribute('aria-label', 'Expand building rooms');
            buildingHeader.appendChild(buildingName);
            buildingHeader.appendChild(expandBuildingButton);
            const buildingContent = document.createElement('div');
            buildingContent.className = 'tenant-building-content';
            building.rooms.forEach(room => {
                const roomItem = document.createElement('div');
                roomItem.className = 'tenant-room-item';
                const roomInfo = document.createElement('div');
                roomInfo.className = 'tenant-room-info';
                const roomName = document.createElement('div');
                roomName.className = 'tenant-room-name';
                roomName.textContent = room.roomName;
                const roomDetails = document.createElement('div');
                roomDetails.className = 'tenant-room-details';
                roomDetails.textContent = `Floor: ${room.floorName || room.floorNumber}`;
                roomInfo.appendChild(roomName);
                roomInfo.appendChild(roomDetails);
                const revokeButton = document.createElement('button');
                revokeButton.className = 'revoke-access-btn';
                revokeButton.textContent = 'Revoke Access';
                revokeButton.dataset.permissionId = room.permissionId;
                revokeButton.dataset.roomName = room.roomName;
                revokeButton.dataset.userName = tenant.userName;
                roomItem.appendChild(roomInfo);
                roomItem.appendChild(revokeButton);
                buildingContent.appendChild(roomItem);
            });
            buildingCard.appendChild(buildingHeader);
            buildingCard.appendChild(buildingContent);
            buildingHeader.addEventListener('click', function() {
                buildingContent.classList.toggle('expanded');
                expandBuildingButton.textContent = buildingContent.classList.contains('expanded') ? '▲' : '▼';
            });
            tenantContent.appendChild(buildingCard);
        });
        tenantCard.appendChild(tenantHeader);
        tenantCard.appendChild(tenantContent);
        tenantHeader.addEventListener('click', function() {
            tenantContent.classList.toggle('expanded');
            expandButton.textContent = tenantContent.classList.contains('expanded') ? '▲' : '▼';
        });
        container.appendChild(tenantCard);
    });
    document.querySelectorAll('.revoke-access-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.stopPropagation();
            const permissionId = this.dataset.permissionId;
            const roomName = this.dataset.roomName;
            const userName = this.dataset.userName;
            revokeTenantAccess(permissionId, userName, roomName);
        });
    });
}
function renderBuildingView() {
    const container = document.getElementById('buildingTenantsContainer');
    container.innerHTML = '';
    const buildingsWithTenants = {};
    allTenants.forEach(tenant => {
        if (!buildingsWithTenants[tenant.buildingId]) {
            buildingsWithTenants[tenant.buildingId] = {
                buildingId: tenant.buildingId,
                buildingName: tenant.buildingName,
                tenants: {}
            };
        }
        if (!buildingsWithTenants[tenant.buildingId].tenants[tenant.userId]) {
            buildingsWithTenants[tenant.buildingId].tenants[tenant.userId] = {
                userId: tenant.userId,
                userName: tenant.userName,
                userEmail: tenant.userEmail,
                rooms: []
            };
        }
        buildingsWithTenants[tenant.buildingId].tenants[tenant.userId].rooms.push({
            roomId: tenant.roomId,
            roomName: tenant.roomName,
            floorNumber: tenant.floorNumber,
            floorName: tenant.floorName,
            permissionId: tenant.userRoomPermissionsId
        });
    });
    Object.values(buildingsWithTenants).forEach(building => {
        const buildingCard = document.createElement('div');
        buildingCard.className = 'building-tenant-card';
        buildingCard.dataset.buildingId = building.buildingId;
        const buildingHeader = document.createElement('div');
        buildingHeader.className = 'building-tenant-header';
        const buildingName = document.createElement('div');
        buildingName.className = 'building-tenant-name';
        buildingName.textContent = building.buildingName;
        const expandButton = document.createElement('button');
        expandButton.className = 'expand-button';
        expandButton.textContent = '▼';
        expandButton.setAttribute('aria-label', 'Expand building tenants');
        buildingHeader.appendChild(buildingName);
        buildingHeader.appendChild(expandButton);
        const buildingContent = document.createElement('div');
        buildingContent.className = 'building-tenant-content';
        Object.values(building.tenants).forEach(tenant => {
            const tenantCard = document.createElement('div');
            tenantCard.className = 'building-tenant-user-card';
            const tenantHeader = document.createElement('div');
            tenantHeader.className = 'building-tenant-user-header';
            const tenantInfo = document.createElement('div');
            tenantInfo.className = 'building-tenant-user-info';
            const tenantName = document.createElement('div');
            tenantName.className = 'building-tenant-user-name';
            tenantName.textContent = tenant.userName;
            const tenantEmail = document.createElement('div');
            tenantEmail.className = 'building-tenant-user-email';
            tenantEmail.textContent = tenant.userEmail;
            tenantInfo.appendChild(tenantName);
            tenantInfo.appendChild(tenantEmail);
            const expandTenantButton = document.createElement('button');
            expandTenantButton.className = 'expand-button';
            expandTenantButton.textContent = '▼';
            expandTenantButton.setAttribute('aria-label', 'Expand tenant rooms');
            tenantHeader.appendChild(tenantInfo);
            tenantHeader.appendChild(expandTenantButton);
            const tenantContent = document.createElement('div');
            tenantContent.className = 'building-tenant-user-content';
            tenant.rooms.forEach(room => {
                const roomItem = document.createElement('div');
                roomItem.className = 'building-tenant-room-item';
                const roomInfo = document.createElement('div');
                roomInfo.className = 'building-tenant-room-info';
                const roomName = document.createElement('div');
                roomName.className = 'building-tenant-room-name';
                roomName.textContent = room.roomName;
                const roomDetails = document.createElement('div');
                roomDetails.className = 'building-tenant-room-details';
                roomDetails.textContent = `Floor: ${room.floorName || room.floorNumber}`;
                roomInfo.appendChild(roomName);
                roomInfo.appendChild(roomDetails);
                const revokeButton = document.createElement('button');
                revokeButton.className = 'revoke-access-btn';
                revokeButton.textContent = 'Revoke Access';
                revokeButton.dataset.permissionId = room.permissionId;
                revokeButton.dataset.roomName = room.roomName;
                revokeButton.dataset.userName = tenant.userName;
                roomItem.appendChild(roomInfo);
                roomItem.appendChild(revokeButton);
                tenantContent.appendChild(roomItem);
            });
            tenantCard.appendChild(tenantHeader);
            tenantCard.appendChild(tenantContent);
            tenantHeader.addEventListener('click', function() {
                tenantContent.classList.toggle('expanded');
                expandTenantButton.textContent = tenantContent.classList.contains('expanded') ? '▲' : '▼';
            });
            buildingContent.appendChild(tenantCard);
        });
        buildingCard.appendChild(buildingHeader);
        buildingCard.appendChild(buildingContent);
        buildingHeader.addEventListener('click', function() {
            buildingContent.classList.toggle('expanded');
            expandButton.textContent = buildingContent.classList.contains('expanded') ? '▲' : '▼';
        });
        container.appendChild(buildingCard);
    });
    document.querySelectorAll('.revoke-access-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.stopPropagation();
            const permissionId = this.dataset.permissionId;
            const roomName = this.dataset.roomName;
            const userName = this.dataset.userName;
            revokeTenantAccess(permissionId, userName, roomName);
        });
    });
}
function toggleView() {
    const tenantView = document.getElementById('tenantView');
    const buildingView = document.getElementById('buildingView');
    const viewTypeLabel = document.getElementById('viewTypeLabel');
    if (currentViewMode === 'tenant') {
        currentViewMode = 'building';
        tenantView.classList.remove('active');
        buildingView.classList.add('active');
        viewTypeLabel.textContent = 'Building View';
        renderBuildingView();
    } else {
        currentViewMode = 'tenant';
        buildingView.classList.remove('active');
        tenantView.classList.add('active');
        viewTypeLabel.textContent = 'Tenant View';
        renderTenantView();
    }
}
function filterTenants(searchTerm) {
    const normalizedSearch = searchTerm.toLowerCase().trim();
    if (currentViewMode === 'tenant') {
        const tenantCards = document.querySelectorAll('.tenant-card');
        tenantCards.forEach(card => {
            const userName = card.querySelector('.tenant-name').textContent.toLowerCase();
            const userEmail = card.querySelector('.tenant-email').textContent.toLowerCase();
            const tenantMatches = userName.includes(normalizedSearch) || userEmail.includes(normalizedSearch);
            let buildingMatches = false;
            let roomMatches = false;
            const buildingNames = card.querySelectorAll('.tenant-building-name');
            buildingNames.forEach(name => {
                if (name.textContent.toLowerCase().includes(normalizedSearch)) {
                    buildingMatches = true;
                }
            });
            const roomNames = card.querySelectorAll('.tenant-room-name');
            roomNames.forEach(name => {
                if (name.textContent.toLowerCase().includes(normalizedSearch)) {
                    roomMatches = true;
                }
            });
            card.style.display = (tenantMatches || buildingMatches || roomMatches) ? '' : 'none';
        });
    } else {
        const buildingCards = document.querySelectorAll('.building-tenant-card');
        buildingCards.forEach(card => {
            const buildingName = card.querySelector('.building-tenant-name').textContent.toLowerCase();
            const buildingMatches = buildingName.includes(normalizedSearch);
            let tenantMatches = false;
            let roomMatches = false;
            const tenantNames = card.querySelectorAll('.building-tenant-user-name');
            const tenantEmails = card.querySelectorAll('.building-tenant-user-email');
            tenantNames.forEach(name => {
                if (name.textContent.toLowerCase().includes(normalizedSearch)) {
                    tenantMatches = true;
                }
            });
            tenantEmails.forEach(email => {
                if (email.textContent.toLowerCase().includes(normalizedSearch)) {
                    tenantMatches = true;
                }
            });
            const roomNames = card.querySelectorAll('.building-tenant-room-name');
            roomNames.forEach(name => {
                if (name.textContent.toLowerCase().includes(normalizedSearch)) {
                    roomMatches = true;
                }
            });
            card.style.display = (buildingMatches || tenantMatches || roomMatches) ? '' : 'none';
        });
    }
}
function revokeTenantAccess(permissionId, userName, roomName) {
    if (confirm(`Are you sure you want to revoke ${userName}'s access to ${roomName}?`)) {
        fetch(`/api/protected/tenants/${permissionId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${sessionStorage.getItem('auth_token')}`
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`API error: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.status === 'success') {
                alert('Access successfully revoked.');
                loadTenants();
            } else {
                alert(data.message || 'Failed to revoke access.');
            }
        })
        .catch(error => {
            alert(`Error: ${error.message}`);
        });
    }
}
function initTenantManagement() {
    const viewToggle = document.getElementById('viewToggle');
    if (viewToggle) {
        viewToggle.addEventListener('change', toggleView);
    }
    const tenantSearchInput = document.getElementById('tenantSearchInput');
    if (tenantSearchInput) {
        tenantSearchInput.addEventListener('input', function() {
            filterTenants(this.value);
        });
    }
    const tenantSearchBtn = document.getElementById('tenantSearchBtn');
    if (tenantSearchBtn) {
        tenantSearchBtn.addEventListener('click', function() {
            const searchTerm = document.getElementById('tenantSearchInput').value;
            filterTenants(searchTerm);
        });
    }
    loadTenants();
}
function addBuildingButtonListeners() {
    document.querySelectorAll('.edit-floor-btn').forEach(button => {
        button.addEventListener('click', function() {
            const buildingId = this.dataset.buildingId;
            const floorCount = this.dataset.floorCount;
            redrawBuilding(buildingId, floorCount);
        });
    });
    document.querySelectorAll('.logs-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.stopPropagation();
            const buildingId = this.dataset.buildingId;
            const buildingName = this.dataset.buildingName;
            let buildingLogs = [];
            if (this.dataset.buildingLogs) {
                try {
                    buildingLogs = JSON.parse(this.dataset.buildingLogs);
                } catch (error) {
                }
            }
            openBuildingLogsModal(buildingId, buildingName, buildingLogs);
        });
    });
    document.querySelectorAll('.edit-details-btn').forEach(button => {
        button.addEventListener('click', function() {
            const buildingId = this.dataset.buildingId;
            const building = allBuildingsData.find(b => b.id == buildingId);
            if (building) {
                openBuildingEditModal(building);
            }
        });
    });
    function openBuildingLogsModal(buildingId, buildingName, logs) {
    document.getElementById('buildingLogsTitle').textContent = `Logs for ${buildingName}`;
    const logsContainer = document.getElementById('buildingLogsContent');
    logsContainer.innerHTML = '';
    if (logs && logs.length > 0) {
        const table = document.createElement('table');
        table.className = 'logs-table';
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        const headers = ['Action', 'Date', 'User', 'Details', 'Floor'];
        headers.forEach(headerText => {
            const th = document.createElement('th');
            th.textContent = headerText;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);
        const tbody = document.createElement('tbody');
        const sortedLogs = [...logs].sort((a, b) => {
            return new Date(b.timestamp) - new Date(a.timestamp);
        });
        sortedLogs.forEach(log => {
            const row = document.createElement('tr');
            const actionCell = document.createElement('td');
            actionCell.className = 'action-type';
            actionCell.textContent = formatActionType(log.actionType);
            row.appendChild(actionCell);
            const dateCell = document.createElement('td');
            dateCell.textContent = formatDate(log.timestamp);
            row.appendChild(dateCell);
            const userCell = document.createElement('td');
            userCell.textContent = `User ${log.userId}`;
            row.appendChild(userCell);
            const detailsCell = document.createElement('td');
            detailsCell.textContent = log.details || '-';
            row.appendChild(detailsCell);
            const floorCell = document.createElement('td');
            floorCell.textContent = log.floorName || '-';
            row.appendChild(floorCell);
            tbody.appendChild(row);
        });
        table.appendChild(tbody);
        logsContainer.appendChild(table);
    } else {
        const noLogsDiv = document.createElement('div');
        noLogsDiv.className = 'no-logs';
        noLogsDiv.textContent = 'No logs available for this building.';
        logsContainer.appendChild(noLogsDiv);
    }
    document.getElementById('buildingLogsModal').style.display = 'flex';
    document.getElementById('overlay').style.display = 'block';
}
    document.querySelectorAll('.admin-list-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.stopPropagation();
            const buildingId = this.dataset.buildingId;
            const buildingName = this.dataset.buildingName;
            const building = allBuildingsData.find(b => b.id == buildingId);
            document.getElementById('adminContactsBuildingName').textContent = buildingName;
            const adminContactsList = document.getElementById('adminContactsList');
            adminContactsList.innerHTML = '';
            if (building && building.adminContacts && building.adminContacts.length > 0) {
                const adminList = document.createElement('ul');
                adminList.className = 'admin-contacts-list';
                building.adminContacts.forEach(admin => {
                    const adminItem = document.createElement('li');
                    adminItem.className = 'admin-contact-item';
                    const adminName = document.createElement('div');
                    adminName.className = 'admin-name';
                    adminName.textContent = admin.name;
                    const adminEmail = document.createElement('div');
                    adminEmail.className = 'admin-email';
                    adminEmail.textContent = admin.email;
                    adminItem.appendChild(adminName);
                    adminItem.appendChild(adminEmail);
                    adminList.appendChild(adminItem);
                });
                adminContactsList.appendChild(adminList);
            } else {
                adminContactsList.innerHTML = '<div class="no-data">No administrators found for this building.</div>';
            }
            document.getElementById('adminContactsModal').style.display = 'flex';
            document.getElementById('overlay').style.display = 'block';
        });
    });
    document.querySelectorAll('.delete-btn').forEach(button => {
        button.addEventListener('click', function() {
            const buildingId = this.dataset.buildingId;
            deleteBuilding(buildingId);
        });
    });
}