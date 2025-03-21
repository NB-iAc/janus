package dev.thesis.janus.central.model;

public enum RoomCategory {
    OFFICE("Office", "#FFC107"),
    CONFERENCE("Conference Room", "#8BC34A"),
    LAB("Laboratory", "#03A9F4"),
    LOUNGE("Lounge", "#E91E63"),
    STORAGE("Storage", "#9C27B0"),
    DEFAULT("Default", "#FF0000");

    private final String displayName;
    private final String colorHex;

    RoomCategory(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static RoomCategory fromString(String category) {
        for (RoomCategory roomCategory : RoomCategory.values()) {
            if (roomCategory.displayName.equalsIgnoreCase(category)) {
                return roomCategory;
            }
        }
        return DEFAULT;
    }
}
