package com.hotel.model.enums;

/**
 * Enumeration representing the lifecycle state of a guest room booking.
 */
public enum BookingStatus {
    PENDING("Pending Confirmation"),
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked In"),
    COMPLETED("Completed"),
    CHECKED_OUT("Checked Out"),
    CANCELLED("Cancelled");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == CONFIRMED || this == CHECKED_IN || this == PENDING;
    }

    public boolean isCompletedOrPast() {
        return this == COMPLETED || this == CHECKED_OUT || this == CANCELLED;
    }
}
