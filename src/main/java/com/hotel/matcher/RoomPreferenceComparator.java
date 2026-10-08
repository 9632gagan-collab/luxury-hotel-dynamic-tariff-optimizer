package com.hotel.matcher;

import java.util.Comparator;

/**
 * Multi-field Comparator for ranked rooms according to Project P68:
 * 1. Higher Preference Match Score first (descending).
 * 2. Lower applicable price first (ascending).
 * 3. Stable tie-breaker by room name.
 */
public class RoomPreferenceComparator implements Comparator<RoomMatchResult> {

    @Override
    public int compare(RoomMatchResult a, RoomMatchResult b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        int scoreComp = Integer.compare(b.getMatchScore(), a.getMatchScore());
        if (scoreComp != 0) {
            return scoreComp;
        }

        int priceComp = Double.compare(a.getApplicablePrice(), b.getApplicablePrice());
        if (priceComp != 0) {
            return priceComp;
        }

        String nameA = a.getRoomName() != null ? a.getRoomName() : "";
        String nameB = b.getRoomName() != null ? b.getRoomName() : "";
        return nameA.compareToIgnoreCase(nameB);
    }
}
