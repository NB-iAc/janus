package com.example.janus.data
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
private const val TAG = "Repository"
object Repository {
    private val nodes = mutableListOf<Node>()
    private val floorMapObjects = mutableMapOf<Int, MutableList<MapObject>>()
    private val rooms = mutableListOf<MapObject>()
    private var currentBuildingId: Int = -1
    private var isDataLoaded = false
    private val buildingInfoCache = mutableMapOf<Int, BuildingInfo>()
    private val buildingCache = mutableMapOf<Int, Long>()
    private var apiClient: JanusApiClient? = null
    fun initialize(context: Context) {
        apiClient = JanusApiClient(context)
    }
    suspend fun loadDataFromMiddleware(buildingId: Int): Boolean {
        Log.d(TAG, "loadDataFromMiddleware called for buildingId: $buildingId")
        if (apiClient == null) {
            Log.e(TAG, "API client not initialized. Call initialize() with context first.")
            return false
        }
        try {
            val needsRefresh = shouldRefreshBuildingData(buildingId)
            if (!needsRefresh && isDataLoaded && currentBuildingId == buildingId) {
                Log.d(TAG, "Using cached data for building $buildingId")
                return true
            }
            Log.d(TAG, "Fetching fresh data for building $buildingId")
            val success = apiClient!!.loadBuildingData(buildingId)
            if (success) {
                buildingCache[buildingId] = System.currentTimeMillis()
                return true
            } else {
                Log.e(TAG, "Failed to load building data from middleware")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from middleware: ${e.message}", e)
            return false
        }
    }
    private suspend fun shouldRefreshBuildingData(buildingId: Int): Boolean {
        if (!buildingCache.containsKey(buildingId)) {
            return true
        }
        try {
            val buildingInfo = apiClient?.getBuildingMetadata(buildingId)
            if (buildingInfo != null && buildingInfo.updatedAt > 0) {
                val cachedTimestamp = buildingCache[buildingId] ?: 0
                return buildingInfo.updatedAt > cachedTimestamp
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking building metadata: ${e.message}", e)
        }
        return false
    }
    fun loadDataFromDatabase(buildingId: Int, callback: (Boolean) -> Unit = {}) {
        Log.d(TAG, "loadDataFromDatabase called for buildingId: $buildingId")
        GlobalScope.launch(Dispatchers.IO) {
            val success = loadDataFromMiddleware(buildingId)
            withContext(Dispatchers.Main) {
                callback(success)
            }
        }
    }
    fun storeBuildingInfo(buildingInfo: BuildingInfo) {
        buildingInfoCache[buildingInfo.id] = buildingInfo
        Log.d(TAG, "Stored building info: ${buildingInfo.id}, ${buildingInfo.name}, ${buildingInfo.description}")
    }
    fun getBuildingInfo(buildingId: Int? = null): BuildingInfo? {
        val id = buildingId ?: currentBuildingId
        if (id <= 0) {
            Log.w(TAG, "Invalid building ID: $id")
            return null
        }
        val cachedInfo = buildingInfoCache[id]
        if (cachedInfo != null) {
            Log.d(TAG, "Retrieved building info from cache: ${cachedInfo.description}")
            return cachedInfo
        }
        Log.w(TAG, "Building info not found in cache, creating default: $id")
        return BuildingInfo(
            id = id,
            name = "Building $id",
            description = "No description available for building $id"
        )
    }
    fun clearCache() {
        buildingCache.clear()
    }
    fun clearCacheForBuilding(buildingId: Int) {
        buildingCache.remove(buildingId)
    }
    fun getAllRooms(): List<MapObject> {
        if (!isDataLoaded) {
            Log.w(TAG, "Data not loaded yet.")
        }
        return rooms
    }
    fun getFloorMapObjects(floor: Int): List<MapObject> {
        if (!isDataLoaded) {
            Log.w(TAG, "Data not loaded yet.")
        }
        Log.d(TAG, "getFloorMapObjects called for floor=$floor")
        return floorMapObjects[floor] ?: emptyList()
    }
    fun getAllFloors(): Map<Int, MutableList<MapObject>> {
        if (!isDataLoaded) {
            Log.w(TAG, "Data not loaded yet.")
        }
        return floorMapObjects
    }
    fun getNodes(): List<Node> {
        if (!isDataLoaded) {
            Log.w(TAG, "Data not loaded yet.")
        }
        Log.d(TAG, "getNodes called, total nodes=${nodes.size}")
        return nodes
    }
    fun getRoomByName(name: String): MapObject? {
        if (!isDataLoaded) {
            Log.w(TAG, "Data not loaded yet.")
        }
        Log.d("RepositoryDebug", "Searching for MapObject with name: '$name'")
        var foundObject = rooms.find {
            it.name.trim().equals(name.trim(), ignoreCase = true)
        }
        if (foundObject == null) {
            for ((floor, objects) in floorMapObjects) {
                foundObject = objects.find {
                    it.name.trim().equals(name.trim(), ignoreCase = true) &&
                            (it.objectType == MapObjectType.ROOM ||
                                    it.objectType == MapObjectType.ELEVATOR ||
                                    it.objectType == MapObjectType.STAIR ||
                                    it.objectType == MapObjectType.ESCALATOR)
                }
                if (foundObject != null) break
            }
        }
        if (foundObject == null) {
            Log.e("RepositoryDebug", "MapObject not found: '$name'")
        } else {
            Log.d("RepositoryDebug", "Found MapObject: '${foundObject.name}' (${foundObject.objectType}, Floor ${foundObject.floor})")
        }
        return foundObject
    }
    fun getCurrentBuildingId(): Int {
        return currentBuildingId
    }
    private var userCurrentRoom: String? = null
    fun setUserCurrentRoom(roomName: String) {
        userCurrentRoom = roomName
        Log.d("Repository", "User location updated: $roomName")
    }
    fun getUserCurrentRoom(): String? {
        Log.d("Repository", "Retrieving user location: $userCurrentRoom")
        return userCurrentRoom
    }
    fun getAvailableBuildings(): List<BuildingInfo> {
        return emptyList()
    }
    fun setAllNodes(nodeList: List<Node>) {
        nodes.clear()
        nodes.addAll(nodeList)
        isDataLoaded = true
        Log.d(TAG, "Set all nodes. Total: ${nodes.size}")
    }
    fun setAllFloorMapObjects(floorObjectsMap: Map<Int, List<MapObject>>) {
        floorMapObjects.clear()
        for ((floor, objects) in floorObjectsMap) {
            floorMapObjects[floor] = objects.toMutableList()
        }
        refreshRoomsList()
        isDataLoaded = true
        Log.d(TAG, "Set all floor objects. Total floors: ${floorMapObjects.size}")
    }
    fun setCurrentBuildingId(id: Int) {
        currentBuildingId = id
        Log.d(TAG, "Set current building ID to $id")
    }
    fun clearData() {
        nodes.clear()
        floorMapObjects.clear()
        rooms.clear()
        isDataLoaded = false
        Log.d(TAG, "Repository data cleared")
    }
    private fun refreshRoomsList() {
        rooms.clear()
        if (floorMapObjects.containsKey(1)) {
            rooms.addAll(floorMapObjects[1]!!.filter {
                it.objectType == MapObjectType.ROOM ||
                        it.objectType == MapObjectType.ELEVATOR ||
                        it.objectType == MapObjectType.STAIR ||
                        it.objectType == MapObjectType.ESCALATOR
            })
        } else {
            for (floor in floorMapObjects.keys.sorted()) {
                rooms.addAll(floorMapObjects[floor]!!.filter {
                    it.objectType == MapObjectType.ROOM ||
                            it.objectType == MapObjectType.ELEVATOR ||
                            it.objectType == MapObjectType.STAIR ||
                            it.objectType == MapObjectType.ESCALATOR
                })
                break
            }
        }
        Log.d(TAG, "Refreshed rooms list. Total rooms: ${rooms.size}")
    }
}