package com.hotel.model.enums;

/**
 * Enumeration representing luxury room categories.
 * Demonstrates OOP principles in Enums: private fields, constructors, and accessor methods.
 */
public enum RoomCategory {
    DELUXE("Deluxe Room", 4500.00, 2, "Spacious room with king bed, marble bathroom, and city view."),
    SUITE("Executive Suite", 8500.00, 4, "Premium suite with separate living lounge, balcony, and jacuzzi."),
    PRESIDENTIAL("Presidential Suite", 18000.00, 6, "Ultra-luxury penthouse suite with panoramic view, private butler, and VIP amenities.");

    private final String displayName;
    private final double basePricePerNight;
    private final int defaultMaxOccupancy;
    private final String description;

    RoomCategory(String displayName, double basePricePerNight, int defaultMaxOccupancy, String description) {
        this.displayName = displayName;
        this.basePricePerNight = basePricePerNight;
        this.defaultMaxOccupancy = defaultMaxOccupancy;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBasePricePerNight() {
        return basePricePerNight;
    }

    public int getDefaultMaxOccupancy() {
        return defaultMaxOccupancy;
    }

    public String getDescription() {
        return description;
    }
}
