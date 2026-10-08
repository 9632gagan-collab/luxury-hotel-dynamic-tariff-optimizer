package com.hotel.matcher;

import com.hotel.model.enums.RoomCategory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captures room-level matching results for a hotel room option.
 * Satisfies Requirement 9: Room-Level Matching using DELUXE, SUITE, PRESIDENTIAL categories.
 */
public class RoomMatchResult {

    private final String roomName;
    private final RoomCategory category;
    private final int maxOccupancy;
    private final double applicablePrice;
    private final boolean suitableForOccupancy;
    private final boolean suitableForRoomType;
    private final boolean withinBudget;
    private final int matchScore;
    private final List<String> matchReasons;

    public RoomMatchResult(String roomName, RoomCategory category, int maxOccupancy,
                           double applicablePrice, boolean suitableForOccupancy,
                           boolean suitableForRoomType, boolean withinBudget,
                           int matchScore, List<String> matchReasons) {
        this.roomName = roomName;
        this.category = category;
        this.maxOccupancy = maxOccupancy;
        this.applicablePrice = applicablePrice;
        this.suitableForOccupancy = suitableForOccupancy;
        this.suitableForRoomType = suitableForRoomType;
        this.withinBudget = withinBudget;
        this.matchScore = matchScore;
        this.matchReasons = matchReasons != null ? Collections.unmodifiableList(new ArrayList<>(matchReasons)) : Collections.emptyList();
    }

    public String getRoomName() {
        return roomName;
    }

    public RoomCategory getCategory() {
        return category;
    }

    public int getMaxOccupancy() {
        return maxOccupancy;
    }

    public double getApplicablePrice() {
        return applicablePrice;
    }

    public boolean isSuitableForOccupancy() {
        return suitableForOccupancy;
    }

    public boolean isSuitableForRoomType() {
        return suitableForRoomType;
    }

    public boolean isWithinBudget() {
        return withinBudget;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }
}
