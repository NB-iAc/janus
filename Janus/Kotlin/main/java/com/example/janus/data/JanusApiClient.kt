package com.example.janus.data
import android.content.Context
import android.graphics.PointF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.random.Random
class JanusApiClient(private val context: Context) {
    companion object {
        private const val TAG = "JanusApiClient"
        private const val BASE_URL = "https://redacted.link"
        private const val CONNECT_TIMEOUT = 15000
        private const val READ_TIMEOUT = 30000
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 10L
    }
    private val requestTimestamps = mutableMapOf<String, Long>()
    private val MIN_REQUEST_INTERVAL = 5000
    suspend fun getBuildingMetadata(buildingId: Int): BuildingMetadata? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting metadata for building ID: $buildingId")
        val endpoint = "buildings/$buildingId"
        val requestKey = "metadata_$buildingId"
        if (shouldThrottleRequest(requestKey)) {
            Log.d(TAG, "Throttling metadata request for building $buildingId")
            return@withContext null
        }
        try {
            val response = makeRequest(endpoint, "GET", null, "building metadata")
            if (response != null) {
                val updatedAtStr = response.getString("updatedAt")
                val updatedAt = try {
                    LocalDateTime.parse(updatedAtStr)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing updatedAt timestamp: ${e.message}", e)
                    System.currentTimeMillis()
                }
                return@withContext BuildingMetadata(
                    id = response.getInt("id"),
                    updatedAt = updatedAt
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting building metadata: ${e.message}", e)
        }
        return@withContext null
    }
    suspend fun loadBuildingData(buildingId: Int): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting loadBuildingData request for buildingId: $buildingId")
        val requestKey = "load_building_$buildingId"
        if (shouldThrottleRequest(requestKey)) {
            Log.d(TAG, "Throttling loadBuildingData request for building $buildingId")
            return@withContext false
        }
        updateRequestTimestamp(requestKey)
        var connection: HttpURLConnection? = null
        try {
            val urlString = "$BASE_URL/building-data/$buildingId"
            Log.d(TAG, "Connecting to URL: $urlString")
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            val responseCode = connection.responseCode
            Log.d(TAG, "Received response code: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                inputStream.close()
                val responseText = response.toString()
                val truncatedResponse = if (responseText.length > 200)
                    responseText.substring(0, 200) + "..."
                else
                    responseText
                Log.d(TAG, "Received data (preview): $truncatedResponse")
                try {
                    val jsonResponse = JSONObject(responseText)
                    Log.d(TAG, "JSON parsed successfully, processing building data...")
                    parseAndSaveBuildingData(jsonResponse, buildingId)
                    Log.d(TAG, "Building data parsed and saved successfully")
                    return@withContext true
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing JSON response: ${e.javaClass.simpleName} - ${e.message}", e)
                    return@withContext false
                }
            } else {
                val errorStream = connection.errorStream
                val errorMessage = if (errorStream != null) {
                    val errorReader = BufferedReader(InputStreamReader(errorStream))
                    val errorResponse = StringBuilder()
                    var line: String?
                    while (errorReader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }
                    errorReader.close()
                    errorStream.close()
                    errorResponse.toString()
                } else {
                    "No error details available"
                }
                Log.e(TAG, "Error loading building data. Response code: $responseCode, Error: $errorMessage")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during loadBuildingData: ${e.javaClass.simpleName} - ${e.message}", e)
            return@withContext false
        } finally {
            connection?.disconnect()
            Log.d(TAG, "Connection closed")
        }
    }
    suspend fun getBuildings(): List<BuildingInfo> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting available buildings from middleware")
        var connection: HttpURLConnection? = null
        try {
            val urlString = "$BASE_URL/buildings"
            Log.d(TAG, "Connecting to URL: $urlString")
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            val responseCode = connection.responseCode
            Log.d(TAG, "Received response code: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                inputStream.close()
                val responseText = response.toString()
                Log.d(TAG, "Response: $responseText")
                try {
                    val jsonArray = JSONArray(responseText)
                    val buildings = mutableListOf<BuildingInfo>()
                    for (i in 0 until jsonArray.length()) {
                        val buildingJson = jsonArray.getJSONObject(i)
                        buildings.add(
                            BuildingInfo(
                                id = buildingJson.getInt("id"),
                                name = buildingJson.getString("name"),
                                description = buildingJson.optString("description", "")
                            )
                        )
                    }
                    Log.d(TAG, "Successfully retrieved ${buildings.size} buildings")
                    return@withContext buildings
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing JSON response: ${e.message}", e)
                    return@withContext emptyList<BuildingInfo>()
                }
            } else {
                val errorStream = connection.errorStream
                val errorMessage = if (errorStream != null) {
                    val errorReader = BufferedReader(InputStreamReader(errorStream))
                    val errorResponse = StringBuilder()
                    var line: String?
                    while (errorReader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }
                    errorReader.close()
                    errorStream.close()
                    errorResponse.toString()
                } else {
                    "No error details available"
                }
                Log.e(TAG, "Error getting buildings. Response code: $responseCode, Error: $errorMessage")
                return@withContext emptyList<BuildingInfo>()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting buildings: ${e.message}", e)
            return@withContext emptyList<BuildingInfo>()
        } finally {
            connection?.disconnect()
        }
    }
    private suspend fun makeRequest(
        endpoint: String,
        method: String,
        jsonPayload: JSONObject?,
        entityName: String
    ): JSONObject? {
        val urlString = "$BASE_URL$endpoint"
        Log.d(TAG, "Making $method request to: $urlString")
        if (jsonPayload != null) {
            Log.d(TAG, "Payload: $jsonPayload")
        }
        var lastException: Exception? = null
        var lastResponseCode = -1
        var lastErrorMessage = ""
        for (attempt in 1..MAX_RETRIES) {
            var connection: HttpURLConnection? = null
            try {
                if (attempt > 1) {
                    Log.d(TAG, "Retry attempt $attempt for $entityName")
                    delay(RETRY_DELAY_MS * attempt + Random.nextLong(50))
                }
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.setRequestProperty("Content-Type", "application/json")
                if (jsonPayload != null) {
                    connection.doOutput = true
                    val outputStream = connection.outputStream
                    val writer = OutputStreamWriter(outputStream)
                    writer.write(jsonPayload.toString())
                    writer.flush()
                    writer.close()
                    outputStream.close()
                }
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")
                lastResponseCode = responseCode
                if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_CREATED) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    inputStream.close()
                    val responseText = response.toString()
                    Log.d(TAG, "Response: $responseText")
                    try {
                        return JSONObject(responseText)
                    } catch (e: Exception) {
                        Log.w(TAG, "Received success response but could not parse JSON: ${e.message}")
                        return JSONObject()
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorMessage = if (errorStream != null) {
                        val errorReader = BufferedReader(InputStreamReader(errorStream))
                        val errorResponse = StringBuilder()
                        var line: String?
                        while (errorReader.readLine().also { line = it } != null) {
                            errorResponse.append(line)
                        }
                        errorReader.close()
                        errorStream.close()
                        errorResponse.toString()
                    } else {
                        "No error details available"
                    }
                    lastErrorMessage = errorMessage
                    Log.e(TAG, "Request failed with response code: $responseCode, Error: $errorMessage")
                    if (responseCode >= 400 && responseCode < 500) {
                        if (responseCode == 409 && method == "POST") {
                            Log.d(TAG, "Got conflict for POST. Entity might already exist.")
                        } else {
                            Log.d(TAG, "Not retrying due to client error: $responseCode")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Exception during request (attempt $attempt): ${e.javaClass.simpleName} - ${e.message}", e)
            } finally {
                connection?.disconnect()
            }
        }
        Log.e(TAG, "All $MAX_RETRIES attempts failed for $entityName. Last response code: $lastResponseCode, Last error: $lastErrorMessage")
        if (lastException != null) {
            Log.e(TAG, "Last exception: ${lastException.message}", lastException)
        }
        return null
    }
    private fun parseAndSaveBuildingData(jsonResponse: JSONObject, buildingId: Int) {
        Repository.clearData()
        val buildingJson = jsonResponse.getJSONObject("building")
        val buildingName = buildingJson.getString("name")
        Log.d(TAG, "Parsing building: $buildingName")
        val floorsJson = jsonResponse.getJSONArray("floors")
        val floorMap = mutableMapOf<Long, Int>()
        for (i in 0 until floorsJson.length()) {
            val floorJson = floorsJson.getJSONObject(i)
            val floorId = floorJson.getLong("id")
            val floorNumber = floorJson.getInt("floorNumber")
            floorMap[floorId] = floorNumber
        }
        val nodesJson = jsonResponse.getJSONArray("nodes")
        val nodeMap = mutableMapOf<String, Node>()
        for (i in 0 until nodesJson.length()) {
            val nodeJson = nodesJson.getJSONObject(i)
            val nodeId = nodeJson.getString("id")
            val x = nodeJson.getDouble("x").toFloat()
            val y = nodeJson.getDouble("y").toFloat()
            val floorId = nodeJson.getLong("floorId")
            val floorNumber = floorMap[floorId] ?: 1
            val isElevationNode = nodeJson.getBoolean("elevationNode")
            val node = Node(
                id = nodeId,
                position = PointF(x, y),
                floor = floorNumber,
                isElevationNode = isElevationNode
            )
            nodeMap[nodeId] = node
        }
        val connectionsJson = jsonResponse.getJSONArray("connections")
        for (i in 0 until connectionsJson.length()) {
            val connectionJson = connectionsJson.getJSONObject(i)
            val sourceNodeId = connectionJson.getString("sourceNodeId")
            val targetNodeId = connectionJson.getString("targetNodeId")
            val bidirectional = connectionJson.getBoolean("bidirectional")
            val sourceNode = nodeMap[sourceNodeId]
            val targetNode = nodeMap[targetNodeId]
            if (sourceNode != null && targetNode != null) {
                sourceNode.neighbors.add(targetNode)
                if (bidirectional) {
                    targetNode.neighbors.add(sourceNode)
                }
            }
        }
        val mapObjectsByFloor = jsonResponse.getJSONObject("mapObjectsByFloor")
        val floorMapObjects = mutableMapOf<Int, MutableList<MapObject>>()
        for (floorIdStr in mapObjectsByFloor.keys()) {
            val floorId = floorIdStr.toLong()
            val floorNumber = floorMap[floorId] ?: continue
            val objectsJson = mapObjectsByFloor.getJSONArray(floorIdStr)
            val objectsList = mutableListOf<MapObject>()
            for (j in 0 until objectsJson.length()) {
                val mapObjectJson = objectsJson.getJSONObject(j)
                val objectType = MapObjectType.valueOf(mapObjectJson.getString("objectType"))
                val name = mapObjectJson.optString("name", "")
                val roomId = mapObjectJson.optString("roomId", UUID.randomUUID().toString())
                val category = mapObjectJson.optString("category", "DEFAULT")
                val contactDetails = mapObjectJson.optString("contactDetails", "")
                val roomType = mapObjectJson.optString("roomType", "")
                val description = mapObjectJson.optString("description", "")
                val entranceNodeId = mapObjectJson.optString("entranceNodeId", null)
                val pointsJson = mapObjectJson.getJSONArray("points")
                val points = mutableListOf<PointF>()
                for (k in 0 until pointsJson.length()) {
                    val pointJson = pointsJson.getJSONObject(k)
                    val pointX = pointJson.getDouble("x").toFloat()
                    val pointY = pointJson.getDouble("y").toFloat()
                    points.add(PointF(pointX, pointY))
                }
                val mapObject = MapObject(
                    objectType = objectType,
                    points = points,
                    name = name,
                    roomId = roomId,
                    floor = floorNumber,
                    category = category,
                    contactDetails = contactDetails,
                    roomType = roomType,
                    entranceNode = if (entranceNodeId != null && entranceNodeId != "null") nodeMap[entranceNodeId] else null,
                    description = description
                )
                objectsList.add(mapObject)
            }
            floorMapObjects[floorNumber] = objectsList
        }
        Repository.setAllNodes(nodeMap.values.toList())
        Repository.setAllFloorMapObjects(floorMapObjects)
        Repository.setCurrentBuildingId(buildingId)
        Log.d(TAG, "Building data parsed and saved to repository")
    }
    private fun shouldThrottleRequest(requestKey: String): Boolean {
        val lastRequestTime = requestTimestamps[requestKey] ?: 0
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastRequestTime
        return timeDifference < MIN_REQUEST_INTERVAL
    }
    private fun updateRequestTimestamp(requestKey: String) {
        requestTimestamps[requestKey] = System.currentTimeMillis()
    }
}