package com.example.janus
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.janus.data.BuildingInfo
import com.example.janus.data.Repository
import com.example.janus.data.RepositoryDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class SearchActivity : AppCompatActivity() {
    private lateinit var repositoryDataManager: RepositoryDataManager
    private lateinit var searchMall: AutoCompleteTextView
    private lateinit var loadingProgressBar: ProgressBar
    private var buildings = mutableListOf<BuildingInfo>()
    companion object {
        private const val TAG = "SearchActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        searchMall = findViewById(R.id.search_mall)
        loadingProgressBar = findViewById(R.id.loading_progress)
        Repository.initialize(applicationContext)
        repositoryDataManager = RepositoryDataManager.getInstance(applicationContext)
        loadBuildingsFromMiddleware()
        searchMall.setOnItemClickListener { _, _, position, _ ->
            val selectedBuildingName = searchMall.adapter.getItem(position) as String
            val selectedBuilding = buildings.find { it.name == selectedBuildingName }
            if (selectedBuilding != null) {
                selectBuilding(selectedBuilding)
            } else {
                Toast.makeText(this, "Building not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        loadBuildingsFromMiddleware()
    }
    private fun loadBuildingsFromMiddleware() {
        loadingProgressBar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching available buildings from middleware...")
                buildings = repositoryDataManager.getAvailableBuildings().toMutableList()
                withContext(Dispatchers.Main) {
                    if (buildings.isEmpty()) {
                        Toast.makeText(
                            this@SearchActivity,
                            "No buildings available. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.d(TAG, "Successfully loaded ${buildings.size} buildings")
                    }
                    setupBuildingAdapter()
                    loadingProgressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading buildings from middleware: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SearchActivity,
                        "Connection unavailable. Please check your connection and try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadingProgressBar.visibility = View.GONE
                }
            }
        }
    }
    private fun selectBuilding(building: BuildingInfo) {
        loadingProgressBar.visibility = View.VISIBLE
        Repository.storeBuildingInfo(building)
        Log.d(TAG, "Stored building info in Repository: ${building.name}, description: '${building.description}'")
        repositoryDataManager.loadBuildingData(building.id) { success ->
            runOnUiThread {
                if (success) {
                    val intent = Intent(this@SearchActivity, MallViewActivity::class.java)
                    intent.putExtra("mall_name", building.name)
                    intent.putExtra("building_id", building.id)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@SearchActivity,
                        "Failed to load building data. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                loadingProgressBar.visibility = View.GONE
            }
        }
    }
    private fun setupBuildingAdapter() {
        val buildingNames = buildings.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            buildingNames
        )
        searchMall.setAdapter(adapter)
    }
}