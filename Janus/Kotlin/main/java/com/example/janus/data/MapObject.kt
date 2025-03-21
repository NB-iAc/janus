package com.example.janus.data
import android.graphics.PointF
data class MapObject(
    val objectType: MapObjectType,
    val points: List<PointF>,
    val name: String = "",
    var roomId: String = "",
    val floor: Int,
    var isSelected: Boolean = false,
    val category: String = "Room",
    val roomType: String = "Room",
    val contactDetails: String = "",
    var entranceNode: Node? = null,
    val floors: List<Pair<Int, Node>>? = null,
    val description: String = "Default Description"
) {
    override fun toString(): String {
        return name
    }
}