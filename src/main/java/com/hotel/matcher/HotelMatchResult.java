package com.hotel.matcher;

import com.hotel.model.Hotel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the deterministic Preference Match evaluation for a Hotel.
 * Carries:
 * - The original Hotel entity
 * - Calculated Preference Match Score percentage (e.g. 94)
 * - Concise, evidence-based match reasons and missed criteria
 * - Best suited room option
 * - Applicable price
 */
public class HotelMatchResult {

    private final Hotel hotel;
    private final int matchScore;
    private final double applicablePrice;
    private final RoomMatchResult bestMatchedRoom;
    private final List<String> matchReasons;
    private final List<String> missedCriteria;
    private final String matchTier;

    public HotelMatchResult(Hotel hotel, int matchScore, double applicablePrice,
                            RoomMatchResult bestMatchedRoom,
                            List<String> matchReasons, List<String> missedCriteria) {
        this.hotel = hotel;
        this.matchScore = Math.max(0, Math.min(100, matchScore));
        this.applicablePrice = applicablePrice;
        this.bestMatchedRoom = bestMatchedRoom;
        this.matchReasons = matchReasons != null ? Collections.unmodifiableList(new ArrayList<>(matchReasons)) : Collections.emptyList();
        this.missedCriteria = missedCriteria != null ? Collections.unmodifiableList(new ArrayList<>(missedCriteria)) : Collections.emptyList();

        if (this.matchScore >= 90) {
            this.matchTier = "Top Match";
        } else if (this.matchScore >= 80) {
            this.matchTier = "Strong Match";
        } else if (this.matchScore >= 70) {
            this.matchTier = "Good Match";
        } else {
            this.matchTier = "Partial Match";
        }
    }

    public Hotel getHotel() {
        return hotel;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public String getMatchScoreFormatted() {
        return matchScore + "% Match";
    }

    public double getApplicablePrice() {
        return applicablePrice;
    }

    public String getFormattedPrice() {
        return "₹" + String.format("%,.0f", applicablePrice);
    }

    public RoomMatchResult getBestMatchedRoom() {
        return bestMatchedRoom;
    }

    public String getRecommendedRoomName() {
        return bestMatchedRoom != null ? bestMatchedRoom.getRoomName() : "Deluxe Room";
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public List<String> getMissedCriteria() {
        return missedCriteria;
    }

    public String getMatchTier() {
        return matchTier;
    }
}
