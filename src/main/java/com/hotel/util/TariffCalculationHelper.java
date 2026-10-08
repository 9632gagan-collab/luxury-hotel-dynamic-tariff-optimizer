package com.hotel.util;

/**
 * Foundation helper class for Luxury Hotel Dynamic Tariff calculations.
 * Prepared for Stage 2 implementation of dynamic pricing algorithms.
 */
public final class TariffCalculationHelper {

    private TariffCalculationHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Placeholder formula demonstrating how dynamic pricing works:
     * Adjusted Tariff = Base Price * Occupancy Multiplier
     * In Stage 2, this will dynamically evaluate occupancy rates:
     * - Occupancy &lt; 40%: Discount factor (0.90x)
     * - Occupancy 40% - 70%: Standard base tariff (1.00x)
     * - Occupancy 70% - 90%: High demand surge (1.25x)
     * - Occupancy &gt; 90%: Peak demand surge (1.50x)
     */
    public static double calculateEstimatedTariff(double basePrice, double occupancyPercentage) {
        double multiplier = 1.0;
        if (occupancyPercentage > 90.0) {
            multiplier = 1.50;
        } else if (occupancyPercentage >= 70.0) {
            multiplier = 1.25;
        } else if (occupancyPercentage < 40.0) {
            multiplier = 0.90;
        }
        return Math.round(basePrice * multiplier * 100.0) / 100.0;
    }
}
