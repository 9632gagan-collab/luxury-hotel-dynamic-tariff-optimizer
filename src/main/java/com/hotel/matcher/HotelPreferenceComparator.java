package com.hotel.matcher;

import java.util.Comparator;

/**
 * Multi-field Comparator for ranked hotels according to Project P68 Requirement 8:
 * 1. Higher Preference Match Score first (descending).
 * 2. If scores are equal, lower applicable price first (ascending).
 * 3. If still equal, stable deterministic tie-breaker by hotel name, then ID.
 */
public class HotelPreferenceComparator implements Comparator<HotelMatchResult> {

    @Override
    public int compare(HotelMatchResult a, HotelMatchResult b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        // 1. Higher Preference Match Score first (descending)
        int scoreComp = Integer.compare(b.getMatchScore(), a.getMatchScore());
        if (scoreComp != 0) {
            return scoreComp;
        }

        // 2. Lower applicable price first (ascending)
        int priceComp = Double.compare(a.getApplicablePrice(), b.getApplicablePrice());
        if (priceComp != 0) {
            return priceComp;
        }

        // 3. Stable deterministic tie-breaker: hotel name (case-insensitive)
        if (a.getHotel() != null && b.getHotel() != null) {
            String nameA = a.getHotel().getName() != null ? a.getHotel().getName() : "";
            String nameB = b.getHotel().getName() != null ? b.getHotel().getName() : "";
            int nameComp = nameA.compareToIgnoreCase(nameB);
            if (nameComp != 0) {
                return nameComp;
            }

            // Secondary tie-breaker: hotel id
            Long idA = a.getHotel().getId() != null ? a.getHotel().getId() : 0L;
            Long idB = b.getHotel().getId() != null ? b.getHotel().getId() : 0L;
            return Long.compare(idA, idB);
        }

        return 0;
    }
}
