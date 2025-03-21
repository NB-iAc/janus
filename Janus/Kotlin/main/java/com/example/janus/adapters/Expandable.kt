package com.example.janus.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.janus.R
import com.example.janus.data.MapObject
import com.example.janus.data.MapObjectType
class ExpandableCategoryAdapter(
    private val categorizedRooms: Map<String, List<MapObject>>,
    private val onClick: (MapObject) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_ROOM = 1
    }
    private val items = mutableListOf<Item>()
    private val expandedCategories = mutableSetOf<String>()
    init {
        generateItemsList()
    }
    sealed class Item {
        data class CategoryHeader(val category: String, val itemCount: Int) : Item()
        data class RoomItem(val mapObject: MapObject) : Item()
    }
    private fun generateItemsList() {
        items.clear()
        categorizedRooms.forEach { (category, rooms) ->
            items.add(Item.CategoryHeader(category, rooms.size))
            if (expandedCategories.contains(category)) {
                rooms.forEach { room ->
                    items.add(Item.RoomItem(room))
                }
            }
        }
        notifyDataSetChanged()
    }
    fun expandAllCategories() {
        expandedCategories.addAll(categorizedRooms.keys)
        generateItemsList()
    }
    fun collapseAllCategories() {
        expandedCategories.clear()
        generateItemsList()
    }
    private fun toggleCategory(category: String) {
        if (expandedCategories.contains(category)) {
            expandedCategories.remove(category)
        } else {
            expandedCategories.add(category)
        }
        generateItemsList()
    }
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Item.CategoryHeader -> TYPE_CATEGORY
            is Item.RoomItem -> TYPE_ROOM
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CATEGORY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_header, parent, false)
                CategoryViewHolder(view)
            }
            TYPE_ROOM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.dropdown_item, parent, false)
                RoomViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.CategoryHeader -> (holder as CategoryViewHolder).bind(item)
            is Item.RoomItem -> (holder as RoomViewHolder).bind(item.mapObject)
        }
    }
    override fun getItemCount(): Int = items.size
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryTitleText: TextView = itemView.findViewById(R.id.category_title)
        private val categoryCountText: TextView = itemView.findViewById(R.id.category_count)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expand_icon)
        fun bind(item: Item.CategoryHeader) {
            categoryTitleText.text = item.category
            categoryCountText.text = "(${item.itemCount})"
            val isExpanded = expandedCategories.contains(item.category)
            expandIcon.setImageResource(
                if (isExpanded) R.drawable.expand_less
                else R.drawable.expand_more
            )
            itemView.setOnClickListener {
                toggleCategory(item.category)
            }
        }
    }
    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.item_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.item_description)
        private val floorTextView: TextView = itemView.findViewById(R.id.item_floor)
        private val linearLayout:LinearLayout = itemView.findViewById(R.id.dropdown_layout_container)
        fun bind(mapObject: MapObject) {
            nameTextView.text = mapObject.name
            descriptionTextView.text = mapObject.description
            val imageResource = when (mapObject.objectType) {
                MapObjectType.ROOM -> R.drawable.room
                MapObjectType.ELEVATOR -> R.drawable.room
                MapObjectType.STAIR -> R.drawable.room
                MapObjectType.ESCALATOR -> R.drawable.room
                else -> R.drawable.room
            }
            floorTextView.text = "Floor ${mapObject.floor}"
            linearLayout.setOnClickListener {
                onClick(mapObject)
            }
        }
    }
}