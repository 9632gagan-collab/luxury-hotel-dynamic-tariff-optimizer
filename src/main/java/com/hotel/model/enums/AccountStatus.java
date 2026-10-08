package com.hotel.model.enums;

/**
 * Enumeration representing the account operational status.
 * Used for role-based authentication and account lifecycle management in Stage 2.
 */
public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
