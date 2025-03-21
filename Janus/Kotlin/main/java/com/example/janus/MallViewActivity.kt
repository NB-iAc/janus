package com.example.janus
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.janus.adapters.ExpandableCategoryAdapter
import com.example.janus.adapters.MapObjectDropdownAdapter
import com.example.janus.data.MapObject
import com.example.janus.data.MapObjectType
import com.example.janus.data.Repository
import com.example.janus.viewmodels.NavigationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
class MallViewActivity : AppCompatActivity() {
    private lateinit var expandableAdapter: ExpandableCategoryAdapter
    private lateinit var viewModel: NavigationViewModel
    private lateinit var dropdownAdapter: MapObjectDropdownAdapter
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var categorySpinner: Spinner
    private lateinit var backButton:ImageView
    private var navigableObjects: MutableList<MapObject> = mutableListOf()
    private var categorizedRooms: Map<String, List<MapObject>> = mapOf()
    private var currentCategory: String = "All Categories"
    private var buildingId: Int = -1
    private lateinit var buildingInfoButton: ImageView
    private var buildingDescription: String = "No description available."
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mall_view)
        viewModel = ViewModelProvider(this)[NavigationViewModel::class.java]
        Repository.initialize(applicationContext)
        buildingInfoButton = findViewById(R.id.building_info_button)
        buildingInfoButton.setOnClickListener {
            showBuildingInfoDialog()
        }
        loadingProgressBar = findViewById(R.id.loading_progress)
        backButton = findViewById(R.id.back_button)
        categorySpinner = findViewById(R.id.category_spinner)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        val mallName = intent.getStringExtra("mall_name") ?: "Default Building Name"
        buildingId = intent.getIntExtra("building_id", -1)
        Log.e("MallViewActivity", "Building ID: $buildingId")
        findViewById<TextView>(R.id.mall_name).text = mallName
        val recyclerView: RecyclerView = findViewById(R.id.stalls_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        expandableAdapter = ExpandableCategoryAdapter(emptyMap()) { mapObject -> onMapObjectSelected(mapObject) }
        recyclerView.adapter = expandableAdapter
        setupCategorySpinner()
        loadBuildingData()
    }
    private fun setupCategorySpinner() {
        val initialCategories = listOf("All Categories")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, initialCategories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                currentCategory = selectedCategory
                filterRoomsByCategory(selectedCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }
    private fun loadBuildingData() {
        loadingProgressBar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (buildingId > 0) {
                    val success = Repository.loadDataFromMiddleware(buildingId)
                    val buildingInfo = Repository.getBuildingInfo(buildingId)
                    if (buildingInfo != null) {
                        buildingDescription = buildingInfo.description
                        Log.d("MallViewActivity", "Retrieved building info: ${buildingInfo.description}")
                    }
                    if (!success) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MallViewActivity,
                                "Failed to load building data. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                        return@launch
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MallViewActivity,
                            "Invalid building. Please select a different building.",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    populateUI()
                    loadingProgressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("MallViewActivity", "Error loading building data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MallViewActivity,
                        "Error loading building data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }
    private fun showBuildingInfoDialog() {
        if (buildingId > 0) {
            Log.d("BuildingInfoDialog", "Fetching info for building ID: $buildingId")
            val buildingInfo = Repository.getBuildingInfo(buildingId)
            Log.d("BuildingInfoDialog", "Retrieved building info: $buildingInfo")
            if (buildingInfo != null) {
                buildingDescription = buildingInfo.description
                Log.d("BuildingInfoDialog", "Updated description: $buildingDescription")
            } else {
                Log.e("BuildingInfoDialog", "buildingInfo is null!")
            }
        } else {
            Log.e("BuildingInfoDialog", "Invalid building ID: $buildingId")
        }
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_building_info)
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))
        val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
        val descriptionTextView = dialog.findViewById<TextView>(R.id.building_description)
        val closeButton = dialog.findViewById<Button>(R.id.close_button)
        val mallName = intent.getStringExtra("mall_name") ?: "Building Information"
        titleTextView.text = mallName
        descriptionTextView.text = buildingDescription
        Log.d("BuildingInfoDialog", "Showing dialog with title: $mallName, description: $buildingDescription")
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
        val displayMetrics = this.resources.displayMetrics
        val dialogHeight = (displayMetrics.heightPixels * 0.7).toInt()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeight)
    }
    private fun populateUI() {
        navigableObjects = Repository.getAllFloors().flatMap { (_, mapObjects) ->
            mapObjects.filter { mapObject ->
                mapObject.objectType == MapObjectType.ROOM ||
                        mapObject.objectType == MapObjectType.ELEVATOR ||
                        mapObject.objectType == MapObjectType.STAIR ||
                        mapObject.objectType == MapObjectType.ESCALATOR
            }
        }.toMutableList()
        Log.d("MallViewActivity", "Total navigable objects loaded: ${navigableObjects.size}")
        organizeRoomsByCategory()
        updateCategorySpinner()
        val searchBar = findViewById<AutoCompleteTextView>(R.id.stalls_dropdown)
        dropdownAdapter = MapObjectDropdownAdapter(this, navigableObjects)
        searchBar.setAdapter(dropdownAdapter)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                dropdownAdapter.filter.filter(editable)
            }
        })
        searchBar.setOnItemClickListener { _, _, position, _ ->
            val selectedMapObject = dropdownAdapter.getItem(position)
            selectedMapObject?.let {
                viewModel.selectedDestination.value = it.name
                Log.d("MallViewActivity", "Selected destination updated in ViewModel: ${it.name}")
                onMapObjectSelected(it)
            }
        }
        filterRoomsByCategory(currentCategory)
    }
    private fun standardizeCategory(category: String): String {
        return category.lowercase(Locale.getDefault())
            .split(" ")
            .joinToString(" ") { word ->
                if (word.isNotEmpty()) {
                    word[0].uppercase() + word.substring(1)
                } else {
                    ""
                }
            }
    }
    private fun organizeRoomsByCategory() {
        val categoryMap = mutableMapOf<String, String>()
        navigableObjects.forEach { mapObject ->
            if (mapObject.objectType == MapObjectType.ROOM) {
                val originalCategory = mapObject.category
                if (!categoryMap.containsKey(originalCategory)) {
                    val standardizedCategory = standardizeCategory(originalCategory)
                    categoryMap[originalCategory] = standardizedCategory
                }
            }
        }
        val standardizedCategoryMap = mutableMapOf<String, MutableList<MapObject>>()
        for (obj in navigableObjects) {
            if (obj.objectType == MapObjectType.ROOM) {
                val originalCategory = obj.category
                val standardizedCategory = categoryMap[originalCategory] ?: standardizeCategory(originalCategory)
                if (!standardizedCategoryMap.containsKey(standardizedCategory)) {
                    standardizedCategoryMap[standardizedCategory] = mutableListOf()
                }
                standardizedCategoryMap[standardizedCategory]?.add(obj)
            }
        }
        standardizedCategoryMap.forEach { (_, rooms) ->
            rooms.sortBy { it.name }
        }
        categorizedRooms = standardizedCategoryMap
        Log.d("MallViewActivity", "Organized rooms into ${standardizedCategoryMap.size} categories")
    }
    private fun updateCategorySpinner() {
        val categories = mutableListOf("All Categories")
        categories.addAll(categorizedRooms.keys.sorted())
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        Log.d("MallViewActivity", "Updated category spinner with ${categories.size} categories")
    }
    private fun filterRoomsByCategory(category: String) {
        if (category == "All Categories") {
            expandableAdapter = ExpandableCategoryAdapter(categorizedRooms) { mapObject -> onMapObjectSelected(mapObject) }
            findViewById<RecyclerView>(R.id.stalls_recyclerview).adapter = expandableAdapter
            expandableAdapter.expandAllCategories()
            Log.d("MallViewActivity", "Showing all ${categorizedRooms.size} categories")
        } else {
            val filteredCategoryMap = mapOf(category to (categorizedRooms[category] ?: emptyList()))
            expandableAdapter = ExpandableCategoryAdapter(filteredCategoryMap) { mapObject -> onMapObjectSelected(mapObject) }
            findViewById<RecyclerView>(R.id.stalls_recyclerview).adapter = expandableAdapter
            expandableAdapter.expandAllCategories()
            Log.d("MallViewActivity", "Filtered to show category: $category")
        }
    }
    private fun getCurrentUserLocation(): String? {
        val lastKnownRoom = Repository.getUserCurrentRoom()
        return if (!lastKnownRoom.isNullOrEmpty()) {
            Log.d("MallViewActivity", "User's last known location: $lastKnownRoom")
            lastKnownRoom
        } else {
            Log.d("MallViewActivity", "User location unknown, defaulting to Building Entrance")
            "Building Entrance"
        }
    }
    private fun onMapObjectSelected(mapObject: MapObject) {
        val fromRoom = Repository.getAllRooms().firstOrNull()
        val fromLocation = fromRoom?.name ?: ""
        val toLocation = mapObject.name
        Log.d("MallViewActivity", "Navigating from: $fromLocation to: $toLocation")
        val intent = Intent(this, NavigationActivity::class.java).apply {
            putExtra("STALL_NAME", mapObject.name)
            putExtra("FROM_LOCATION", fromLocation)
            putExtra("TO_LOCATION", toLocation)
            fromRoom?.let {
                putExtra("FLOOR", it.floor)
            } ?: run {
                putExtra("FLOOR", mapObject.floor)
            }
        }
        startActivity(intent)
    }
}
