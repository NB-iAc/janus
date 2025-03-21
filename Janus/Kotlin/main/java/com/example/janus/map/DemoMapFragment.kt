package com.example.janus.map
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.janus.R
import com.example.janus.adapters.MapObjectDropdownAdapter
import com.example.janus.data.BuildingInfo
import com.example.janus.data.MapObject
import com.example.janus.data.MapObjectType
import com.example.janus.data.Repository
import com.example.janus.viewmodels.NavigationViewModel
private const val TAG = "DemoMapFragment"
class DemoMapFragment : Fragment(R.layout.fragment_demo_map), OnRoomClickListener {
    private val viewModel: NavigationViewModel by viewModels()
    private lateinit var customMapView: CustomMapView
    private lateinit var buttonFloor1: Button
    private lateinit var buttonFloor2: Button
    private lateinit var navigateButton: Button
    private var currentStartPoint: MapObject? = null
    private var currentEndPoint: MapObject? = null
    private lateinit var fromSelect: AutoCompleteTextView
    private lateinit var toSelect: AutoCompleteTextView
    private lateinit var buildingSpinner: Spinner
    private lateinit var context: Context
    private var buildings = listOf<BuildingInfo>()
    private var isDataLoaded = false
    private var navigableObjects = listOf<MapObject>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customMapView = view.findViewById(R.id.customMapView)
        buttonFloor1 = view.findViewById(R.id.btn_up)
        buttonFloor2 = view.findViewById(R.id.btn_down)
        fromSelect = view.findViewById(R.id.fromSelect)
        toSelect = view.findViewById(R.id.toSelect)
        navigateButton = view.findViewById(R.id.btn_nav)
        setupMapData()
        viewModel.selectedDestination.observe(viewLifecycleOwner) { selectedDestination ->
            selectedDestination?.let {
                toSelect.setText(it)
                setNavigationPoints(fromSelect.text.toString(), it)
            }
        }
        val floor = arguments?.getInt("FLOOR")
        val nodeX = arguments?.getFloat("nodeX")
        val nodeY = arguments?.getFloat("nodeY")
        customMapView.setPos(nodeX,nodeY)
        if (floor != null) {
            customMapView.setCurrentFloor(floor)
        } else {
            customMapView.setCurrentFloor(1)
        }
        buttonFloor1.setOnClickListener { customMapView.incrementCurrentFloor() }
        buttonFloor2.setOnClickListener { customMapView.decrementCurrentFloor() }
        val fromLocation = arguments?.getString("FROM")
        val toLocation = arguments?.getString("TO")
        if (!fromLocation.isNullOrEmpty() && !toLocation.isNullOrEmpty()) {
            Log.d(TAG, "Navigating from $fromLocation to $toLocation")
            waitForDataAndInitializeNavigation(fromLocation, toLocation)
        } else {
            Log.e(TAG, "Missing FROM or TO location")
        }
        navigateButton.setOnClickListener {
            Log.d("NavigationDebug", "Navigate button clicked")
            runNavCheck()
        }
    }
    private fun waitForDataAndInitializeNavigation(fromLocation: String, toLocation: String) {
        if (isDataLoaded) {
            initializeNavigation(fromLocation, toLocation)
            return
        }
        view?.postDelayed({
            waitForDataAndInitializeNavigation(fromLocation, toLocation)
        }, 200)
    }
    private fun initializeNavigation(fromLocation: String, toLocation: String) {
        val startPoint = Repository.getRoomByName(fromLocation)
        val endPoint = Repository.getRoomByName(toLocation)
        currentStartPoint = startPoint
        currentEndPoint = endPoint
        if (startPoint != null) {
            fromSelect.setText(startPoint.name)
            customMapView.setFromSelect(startPoint.roomId)
        }
        if (endPoint != null) {
            toSelect.setText(endPoint.name)
            customMapView.setToSelect(endPoint.roomId)
        }
        if (startPoint != null && endPoint != null) {
            runNavCheck()
        } else {
            Log.e(TAG, "Failed to initialize navigation: startPoint=$startPoint, endPoint=$endPoint")
        }
    }
    private fun setNavigationPoints(from: String, to: String) {
        if (!isDataLoaded) return
        val startPoint = Repository.getRoomByName(from)
        val endPoint = Repository.getRoomByName(to)
        if (startPoint == null || endPoint == null) {
            Log.d("NavigationDebug", "Setting Navigation Points: from=$from, to=$to")
            Log.e(TAG, "Error: Start or End location not found in repository")
            return
        }
        currentStartPoint = startPoint
        currentEndPoint = endPoint
        fromSelect.setText(startPoint.name)
        toSelect.setText(endPoint.name)
        customMapView.setFromSelect(startPoint.roomId)
        customMapView.setToSelect(endPoint.roomId)
        runNavCheck()
    }
    companion object {
        fun newInstance(from: String, to: String): DemoMapFragment {
            val fragment = DemoMapFragment()
            val args = Bundle()
            args.putString("FROM", from)
            args.putString("TO", to)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onAttach(context: Context) {
        this.context = context
        super.onAttach(context)
    }
    private fun setupBuildingSpinner() {
        buildings = Repository.getAvailableBuildings()
        if (buildings.isEmpty()) {
            buildings = listOf(
                BuildingInfo(
                Repository.getCurrentBuildingId(),
                "Main Building",
                "Default building"
            )
            )
        }
        val buildingNames = buildings.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, buildingNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        buildingSpinner.adapter = adapter
        val currentBuildingId = Repository.getCurrentBuildingId()
        val currentBuildingIndex = buildings.indexOfFirst { it.id == currentBuildingId }
        if (currentBuildingIndex >= 0) {
            buildingSpinner.setSelection(currentBuildingIndex)
        }
        buildingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBuildingId = buildings[position].id
                if (selectedBuildingId != Repository.getCurrentBuildingId()) {
                    loadBuildingData(selectedBuildingId)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }
    private fun loadBuildingData(buildingId: Int) {
        Toast.makeText(context, "Loading building data...", Toast.LENGTH_SHORT).show()
        Repository.loadDataFromDatabase(buildingId) { success ->
            activity?.runOnUiThread {
                if (success) {
                    setupMapData()
                    Toast.makeText(context, "Building data loaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to load building data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun setupMapData() {
        val graphNodes = Repository.getNodes()
        customMapView.setAllFloorMapObjects(Repository.getAllFloors())
        customMapView.setGraphNodes(graphNodes)
        customMapView.setCurrentFloor(1)
        customMapView.setOnRoomClickListener(this)
        navigableObjects = Repository.getAllFloors().flatMap { (_, mapObjects) ->
            mapObjects.filter { mapObject ->
                mapObject.objectType == MapObjectType.ROOM ||
                        mapObject.objectType == MapObjectType.ELEVATOR ||
                        mapObject.objectType == MapObjectType.STAIR ||
                        mapObject.objectType == MapObjectType.ESCALATOR
            }
        }
        val dropdownAdapter = MapObjectDropdownAdapter(requireContext(), navigableObjects)
        fromSelect.setAdapter(dropdownAdapter)
        toSelect.setAdapter(dropdownAdapter)
        fromSelect.setOnItemClickListener { _, _, position, _ ->
            val selectedMapObject = dropdownAdapter.getItem(position)
            if (selectedMapObject != null) {
                currentStartPoint = selectedMapObject
                Log.d("NavigationDebug", "Selected Start Point: ${currentStartPoint?.name}")
                if (currentEndPoint != null) {
                    runNavCheck()
                }
            }
        }
        toSelect.setOnItemClickListener { _, _, position, _ ->
            val selectedMapObject = dropdownAdapter.getItem(position)
            if (selectedMapObject != null) {
                currentEndPoint = selectedMapObject
                Log.d("NavigationDebug", "Selected End Point: ${currentEndPoint?.name}")
                if (currentStartPoint != null) {
                    runNavCheck()
                }
            }
        }
        isDataLoaded = true
    }
    override fun onRoomClicked(room: MapObject) {
        Log.d(TAG, "onRoomClicked: ${room.name} (id=${room.roomId})")
        AlertDialog.Builder(requireContext())
            .setTitle(room.name)
            .setMessage(
                "Room ID: ${room.roomId}\nCategory: ${room.category}" +
                        "\nContact: ${room.contactDetails}\nType: ${room.roomType}" +
                        "\nFloor: ${room.floor}"
            )
            .setPositiveButton("View More Details") { dialog, _ ->
                openRoomDetailFragment(room)
                dialog.dismiss()
            }
            .setNegativeButton("Set Start Point") { dialog, _ ->
                currentStartPoint = room
                fromSelect.setText(room.name)
                runNavCheck()
                dialog.dismiss()
            }
            .setNeutralButton("Set End Point") { dialog, _ ->
                currentEndPoint = room
                toSelect.setText(room.name)
                runNavCheck()
                dialog.dismiss()
            }
            .show()
    }
    private fun runNavCheck() {
        val startPoint = currentStartPoint ?: return
        val endPoint = currentEndPoint ?: return
        val startNode = startPoint.entranceNode
        val endNode = endPoint.entranceNode
        if (startNode == null || endNode == null) {
            Log.e(TAG, "Error: One of the locations has no entrance node.")
            return
        }
        Log.d("NavigationDebug", "Start Node Floor: ${startNode.floor}")
        Log.d("NavigationDebug", "End Node Floor: ${endNode.floor}")
        if (customMapView.getCurrentFloor() != startPoint.floor) {
            customMapView.setCurrentFloor(startPoint.floor)
        }
        customMapView.setFromSelect(currentStartPoint!!.roomId)
        customMapView.setToSelect(currentEndPoint!!.roomId)
        if (startPoint.floor != endPoint.floor) {
            Log.d("NavigationDebug", "Showing path between nodes with different elevation")
            customMapView.showPathBetweenNodesDifferentElevation(startNode, endNode, startPoint.floor, endPoint.floor)
        } else {
            Log.d("NavigationDebug", "Showing path between nodes on same floor")
            customMapView.showPathBetweenNodes(startNode, endNode)
        }
    }
    private fun openRoomDetailFragment(room: MapObject) {
        Log.d(TAG, "Opening RoomDetailFragment for room ID: ${room.roomId}")
        val fragment = RoomDetailFragment.newInstance(room.roomId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment, "RoomDetail")
            .addToBackStack("RoomDetail")
            .commit()
    }
}