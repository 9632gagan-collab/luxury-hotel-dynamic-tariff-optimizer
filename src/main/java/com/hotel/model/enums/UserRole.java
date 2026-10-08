package com.hotel.model.enums;

/**
 * Enumeration representing system user roles in the Luxury Hotel Management System.
 * Demonstrates basic Java Enum usage with associated role display names.
 */
public enum UserRole {
    GUEST("Guest / Customer"),
    STAFF("Hotel Staff Member"),
    ADMIN("System Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
