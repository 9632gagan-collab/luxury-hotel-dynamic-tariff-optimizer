package com.hotel.model.enums;

/**
 * Enumeration representing the operational and availability status of a hotel room.
 * Demonstrates enum properties and domain helper logic.
 */
public enum RoomStatus {
    AVAILABLE("Available", true, "badge-success"),
    OCCUPIED("Occupied", false, "badge-danger"),
    RESERVED("Reserved", false, "badge-warning"),
    CLEANING("Housekeeping / Cleaning", false, "badge-info"),
    MAINTENANCE("Under Maintenance", false, "badge-secondary"),
    OUT_OF_SERVICE("Out of Service", false, "badge-dark");

    private final String displayName;
    private final boolean bookable;
    private final String badgeClass;

    RoomStatus(String displayName, boolean bookable, String badgeClass) {
        this.displayName = displayName;
        this.bookable = bookable;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBookable() {
        return bookable;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
