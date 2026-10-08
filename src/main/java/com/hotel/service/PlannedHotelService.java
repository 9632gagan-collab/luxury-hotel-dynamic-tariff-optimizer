package com.hotel.service;

import com.hotel.model.PlannedHotel;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Service interface for managing Guest Planned Lists & Travel Planner.
 * Demonstrates OOP Abstraction via Java Interfaces.
 * Enforces server-side security where all operations are tied strictly to the authenticated Guest.
 */
public interface PlannedHotelService {

    List<PlannedHotel> getPlannedHotelsForGuest(String username);

    Set<Long> getPlannedHotelIdsForGuest(String username);

    default Set<Long> getPlannedHotelIds(String username) {
        return getPlannedHotelIdsForGuest(username);
    }

    boolean isHotelPlannedByGuest(String username, Long hotelId);

    boolean addHotelToPlannedList(String username, Long hotelId);

    boolean addHotelToPlannedList(String username, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate);

    boolean updatePlannedDates(String username, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate);

    boolean removeHotelFromPlannedList(String username, Long hotelId);

    boolean togglePlannedHotel(String username, Long hotelId);

    long getPlannedCountForGuest(String username);
}
