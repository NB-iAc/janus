package com.example.janus.data
import android.graphics.PointF
import kotlin.math.pow
import kotlin.math.sqrt
data class searchFloorObject(
    val floor: Int,
    val path: List<Node>
)
data class BuildingMetadata(
    val id: Int,
    val updatedAt: Long
)
data class Node(
    val id: String,
    val position: PointF,
    val floor: Int,
    val isElevationNode: Boolean,
    val neighbors: MutableList<Node> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false
        return this.id == other.id
    }
    override fun hashCode(): Int {
        return id.hashCode()
    }
    override fun toString(): String {
        return id
    }
}
data class BuildingInfo(
    val id: Int,
    val name: String,
    val description: String
)
data class NodeCost(
    val node: Node,
    val costSoFar: Float,
    val estimatedTotalCost: Float,
    val path: List<Node>
)
fun distance(a: Node, b: Node): Float {
    return sqrt((a.position.x - b.position.x).pow(2) + (a.position.y - b.position.y).pow(2))
}
fun heuristic(current: Node, end: Node): Float {
    return distance(current, end)
}
