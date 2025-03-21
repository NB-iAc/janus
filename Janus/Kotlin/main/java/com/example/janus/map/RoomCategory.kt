package com.example.janus.map
import android.graphics.Color
enum class RoomCategory(val displayName: String, val color: Int) {
    OFFICE("Office", Color.parseColor("#FFC107")),
    CONFERENCE("Conference Room", Color.parseColor("#8BC34A")),
    LAB("Laboratory", Color.parseColor("#03A9F4")),
    LOUNGE("Lounge", Color.parseColor("#E91E63")),
    STORAGE("Storage", Color.parseColor("#9C27B0")),
    DEFAULT("Default", Color.RED);
    companion object {
        fun fromString(category: String?): RoomCategory {
            return values().find { it.displayName.equals(category, ignoreCase = true) }
                ?: DEFAULT
        }
    }
}
