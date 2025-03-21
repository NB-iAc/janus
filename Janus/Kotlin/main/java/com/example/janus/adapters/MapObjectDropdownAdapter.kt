package com.example.janus.adapters
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.example.janus.R
import com.example.janus.data.MapObject
import com.example.janus.data.MapObjectType
class MapObjectDropdownAdapter(
    context: Context,
    private val mapObjects: List<MapObject>
) : ArrayAdapter<MapObject>(context, R.layout.dropdown_item, mapObjects) {
    private val allMapObjects: List<MapObject> = mapObjects.sortedWith(
        compareBy<MapObject> { it.category }
            .thenBy { it.name }
    )
    private var filteredMapObjects: List<MapObject> = allMapObjects
    override fun getCount(): Int = filteredMapObjects.size
    override fun getItem(position: Int): MapObject? = filteredMapObjects.getOrNull(position)
    fun getMapObjectByName(name: String): MapObject? {
        return mapObjects.find { it.name == name }
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.dropdown_item, parent, false)
        val mapObject = getItem(position)
        val nameTextView: TextView = view.findViewById(R.id.item_name)
        val descriptionTextView: TextView = view.findViewById(R.id.item_description)
        nameTextView.text = mapObject?.name ?: ""
        val description = when (mapObject?.objectType) {
            MapObjectType.ROOM -> "${mapObject.category} (Floor ${mapObject.floor})"
            MapObjectType.ELEVATOR -> "Elevator (Floor ${mapObject.floor})"
            MapObjectType.STAIR -> "Stairs (Floor ${mapObject.floor})"
            MapObjectType.ESCALATOR -> "Escalator (Floor ${mapObject.floor})"
            else -> mapObject?.description ?: ""
        }
        descriptionTextView.text = description
        return view
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList = if (constraint.isNullOrEmpty()) {
                    allMapObjects
                } else {
                    val filterPattern = constraint.toString().lowercase().trim()
                    allMapObjects.filter {
                        it.name.lowercase().contains(filterPattern) ||
                                it.category.lowercase().contains(filterPattern)
                    }
                }
                val sortedFilteredList = filteredList.sortedWith(
                    compareBy<MapObject> { it.category }
                        .thenBy { it.name }
                )
                return FilterResults().apply {
                    values = sortedFilteredList
                    count = sortedFilteredList.size
                }
            }
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredMapObjects = results?.values as? List<MapObject> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}