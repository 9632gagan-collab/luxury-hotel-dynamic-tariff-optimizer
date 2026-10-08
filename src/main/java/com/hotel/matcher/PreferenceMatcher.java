package com.hotel.matcher;

import com.hotel.model.Hotel;
import com.hotel.model.Room;
import java.util.List;

/**
 * Service contract for the Stage 2 Smart Hotel & Room Preference Matcher.
 * Evaluates guest search criteria against actual verified property attributes,
 * generates deterministic Match Scores and concise explanations, and returns
 * ranked results ordered by multi-field criteria.
 */
public interface PreferenceMatcher {

    /**
     * Matches and ranks a collection of hotels against guest criteria.
     * Orders results using {@link HotelPreferenceComparator}.
     */
    List<HotelMatchResult> matchAndRankHotels(List<Hotel> hotels, PreferenceCriteria criteria);

    /**
     * Evaluates a single hotel against guest criteria.
     */
    HotelMatchResult matchHotel(Hotel hotel, PreferenceCriteria criteria);

    /**
     * Matches and ranks individual room entities against guest criteria.
     * Preserves Project P68 room inventory recommendation capabilities.
     */
    List<RoomMatchResult> matchAndRankRooms(List<Room> rooms, PreferenceCriteria criteria);

    /**
     * Evaluates a single room against guest criteria.
     */
    RoomMatchResult matchRoom(Room room, PreferenceCriteria criteria);
}
