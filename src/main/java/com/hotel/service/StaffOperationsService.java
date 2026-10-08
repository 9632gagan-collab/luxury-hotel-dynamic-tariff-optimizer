package com.hotel.service;

import com.hotel.model.enums.RoomStatus;
import java.util.List;
import java.util.Map;

/**
 * Service providing live hotel and guest operations telemetry, real-time drilldowns,
 * and operational room status for the Staff portal.
 * Connected directly to real database entities as Single Source of Truth.
 */
public interface StaffOperationsService {

    /**
     * Retrieves live summary metrics and percentage distributions across guests, rooms, and bookings.
     */
    Map<String, Object> getLiveOperationsSummary();

    /**
     * Retrieves filtered operational records for a given category (Detail-on-Click Level 2).
     */
    List<Map<String, Object>> getLiveOperationsList(String category);

    /**
     * Retrieves complete metadata and traveler records for a specific booking (Detail-on-Click Level 3).
     */
    Map<String, Object> getGuestBookingDetail(Long bookingId);

    /**
     * Retrieves complete room operational details including associated guest reservations (Detail-on-Click Level 3).
     */
    Map<String, Object> getRoomOperationalDetail(Long roomId);

    /**
     * Performs staff operational check-in for a reservation.
     */
    boolean checkInBooking(Long bookingId);

    /**
     * Performs staff operational check-out for a reservation.
     */
    boolean checkOutBooking(Long bookingId);

    /**
     * Updates operational room status.
     */
    boolean updateRoomStatus(Long roomId, RoomStatus newStatus);
}
