package com.example.janus.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.janus.R
import com.example.janus.data.MapObject
class MapObjectAdapter(
    private var mapObjects: List<MapObject>,
    private val onClick: (MapObject) -> Unit
) : RecyclerView.Adapter<MapObjectAdapter.MapObjectViewHolder>(), Filterable {
    private var filteredMapObjects: List<MapObject> = mapObjects
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapObjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.dropdown_item, parent, false)
        return MapObjectViewHolder(view)
    }
    override fun onBindViewHolder(holder: MapObjectViewHolder, position: Int) {
        val mapObject = filteredMapObjects[position]
        holder.bind(mapObject, onClick)
    }
    override fun getItemCount(): Int = filteredMapObjects.size
    fun updateList(newList: List<MapObject>) {
        mapObjects = newList
        filteredMapObjects = newList
        notifyDataSetChanged()
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList = if (constraint.isNullOrEmpty()) {
                    mapObjects
                } else {
                    mapObjects.filter {
                        it.name.contains(constraint, ignoreCase = true)
                    }
                }
                return FilterResults().apply { values = filteredList }
            }
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredMapObjects = results?.values as? List<MapObject> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
    class MapObjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.item_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.item_description)
        private val floorTextView: TextView = itemView.findViewById(R.id.item_floor)
        fun bind(mapObject: MapObject, onClick: (MapObject) -> Unit) {
            nameTextView.text = mapObject.name
            descriptionTextView.text = mapObject.description
            floorTextView.text = "Floor ${mapObject.floor}"
            itemView.setOnClickListener { onClick(mapObject) }
        }
    }
}