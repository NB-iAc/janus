package com.example.janus
import android.os.Bundle
import android.util.Log
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.janus.adapters.MapObjectAdapter
import com.example.janus.data.MapObject
import com.example.janus.data.MapObjectType
import com.example.janus.data.Repository
import com.example.janus.map.CustomMapView
import com.example.janus.map.DemoMapFragment
class NavigationActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuIcon: ImageView
    private lateinit var closeMenu: ImageView
    private lateinit var backButton: ImageView
    private lateinit var mapObjectsRecyclerView: RecyclerView
    private lateinit var mapObjectAdapter: MapObjectAdapter
    private lateinit var logoutButton: TextView
    private var currentStartLocation: String? = null
    private var navigableObjects: List<MapObject> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        val stallName = intent.getStringExtra("STALL_NAME") ?: ""
        val fromLocation = intent.getStringExtra("FROM_LOCATION") ?: ""
        val toLocation = intent.getStringExtra("TO_LOCATION") ?: ""
        val fragment = DemoMapFragment.newInstance(fromLocation, toLocation)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment)
            .commit()
        navigableObjects = Repository.getAllFloors().flatMap { (_, mapObjects) ->
            mapObjects.filter { mapObject ->
                mapObject.objectType == MapObjectType.ROOM ||
                        mapObject.objectType == MapObjectType.ELEVATOR ||
                        mapObject.objectType == MapObjectType.STAIR ||
                        mapObject.objectType == MapObjectType.ESCALATOR
            }
        }.sortedBy { it.name }
        drawerLayout = findViewById(R.id.drawer_layout)
        menuIcon = findViewById(R.id.menu_icon)
        closeMenu = findViewById(R.id.close_menu)
        mapObjectsRecyclerView = findViewById(R.id.stallsRecyclerView)
        backButton = findViewById(R.id.back_button)
        logoutButton = findViewById(R.id.nav_logout)
        mapObjectsRecyclerView.layoutManager = LinearLayoutManager(this)
        mapObjectAdapter = MapObjectAdapter(navigableObjects) { selectedMapObject ->
            navigateToDestination(selectedMapObject)
        }
        mapObjectsRecyclerView.adapter = mapObjectAdapter
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        closeMenu.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        logoutButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        val selectedDestinationName = intent.getStringExtra("DESTINATION_NAME")
        val startLocation = intent.getStringExtra("FROM_LOCATION") ?: "Building Entrance"
        currentStartLocation = startLocation
        if (selectedDestinationName != null) {
            updateMapNavigation(startLocation, selectedDestinationName)
        }
        if (savedInstanceState == null) {
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.map_container, DemoMapFragment())
            transaction.commit()
        }
    }
    private fun updateMapNavigation(from: String, to: String) {
        val mapFragment = DemoMapFragment.newInstance(from, to)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
    }
    private fun navigateToDestination(mapObject: MapObject) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.map_container)
        if (currentFragment is DemoMapFragment) {
            val customMapView = currentFragment.view?.findViewById<CustomMapView>(R.id.customMapView)
            customMapView?.let {
                it.setCurrentFloor(mapObject.floor)
                it.focusOnRoomById(mapObject.roomId)
                Log.d("NavigationActivity", "Navigating to: ${mapObject.name} (ID: ${mapObject.roomId}) on floor ${mapObject.floor}")
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
    }
}