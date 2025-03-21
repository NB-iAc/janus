package com.example.janus.data
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class RepositoryDataManager(private val context: Context) {
    companion object {
        private const val TAG = "RepositoryDataManager"
        @Volatile
        private var INSTANCE: RepositoryDataManager? = null
        fun getInstance(context: Context): RepositoryDataManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RepositoryDataManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    private val apiClient = JanusApiClient(context)
    fun loadBuildingData(buildingId: Int, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Starting building data load process for buildingId: $buildingId")
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to load data from middleware server...")
                val success = Repository.loadDataFromMiddleware(buildingId)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Log.d(TAG, "Successfully loaded building data")
                        callback(true)
                    } else {
                        Log.e(TAG, "Failed to load building data from middleware")
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading building data: ${e.javaClass.simpleName} - ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
    suspend fun getAvailableBuildings(): List<BuildingInfo> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting available buildings from middleware")
            val buildings = apiClient.getBuildings()
            Log.d(TAG, "Middleware returned ${buildings.size} buildings")
            return@withContext buildings
        } catch (e: Exception) {
            Log.e(TAG, "Error getting buildings from middleware", e)
            return@withContext emptyList<BuildingInfo>()
        }
    }
}