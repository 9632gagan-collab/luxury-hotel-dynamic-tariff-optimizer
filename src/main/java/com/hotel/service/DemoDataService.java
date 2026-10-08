package com.hotel.service;

import com.hotel.dto.BookingRequestDto;
import com.hotel.dto.StaffCreateDto;
import com.hotel.model.*;
import com.hotel.model.enums.RoomStatus;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service providing completely isolated, session-scoped demonstration sandbox data and state
 * for Admin Demo, Staff Demo, and Guest Demo exploration.
 * Guarantees zero cross-contamination with production/database records and real user accounts.
 */
public interface DemoDataService {

    boolean isDemoMode(HttpSession session);
    void resetDemoSession(HttpSession session);

    // ==========================================
    // DEMO OFFERS SANDBOX
    // ==========================================
    List<Offer> getDemoOffers(HttpSession session);
    List<Offer> getDemoActiveOffers(HttpSession session);
    Optional<Offer> getDemoOfferById(HttpSession session, Long id);
    Offer saveDemoOffer(HttpSession session, Offer offer);
    Offer toggleDemoOfferStatus(HttpSession session, Long id);
    boolean deleteDemoOffer(HttpSession session, Long id);

    // ==========================================
    // ADMIN DEMO DATA & MUTATIONS
    // ==========================================
    Map<String, Object> getAdminGuestActivityOverview(HttpSession session);
    Map<String, Object> getAdminHotelOperationsOverview(HttpSession session);
    Map<String, Object> getAdminMaintenanceOverview(HttpSession session);
    Map<String, Object> getAdminInspectionsOverview(HttpSession session);
    Map<String, Object> getAdminStaffActivityOverview(HttpSession session);
    List<Hotel> getAdminMaintenanceAffectedHotels();
    List<Hotel> getAdminMaintenanceAffectedHotels(HttpSession session);
    List<Map<String, Object>> getAdminRoomsByStatus(HttpSession session, String status);
    List<Map<String, Object>> getAdminGuestFlow(String flowType);
    List<Map<String, Object>> getAdminGuestFlow(HttpSession session, String flowType);
    List<Map<String, Object>> getAdminGuestActivityDetails(String category);
    List<Map<String, Object>> getAdminGuestActivityDetails(HttpSession session, String category);
    List<Staff> getAdminStaffByActivityStatus(HttpSession session, String status);
    Map<String, Object> getAdminDistrictOperationsOverview(String state, String district);
    Map<String, Object> getAdminDistrictOperationsOverview(HttpSession session, String state, String district);
    Map<String, Object> getAdminHotelOperationalDetails(Long hotelId);
    Map<String, Object> getAdminHotelOperationalDetails(HttpSession session, Long hotelId);
    List<MaintenanceRecord> getAdminMaintenanceRecords(Long hotelId);
    List<MaintenanceRecord> getAdminMaintenanceRecords(HttpSession session, Long hotelId);
    List<Inspection> getAdminInspections(String status);
    List<Inspection> getAdminInspections(HttpSession session, String status);
    List<Staff> getAdminStaffList(HttpSession session);
    Staff addDemoStaff(HttpSession session, StaffCreateDto staffDto);
    boolean toggleDemoStaffStatus(HttpSession session, Long staffId);
    List<Guest> getAdminGuestList();
    List<Guest> getAdminGuestList(HttpSession session);
    List<Booking> getAdminBookingsList(HttpSession session);
    Admin getAdminProfile();
    Admin getAdminProfile(HttpSession session);
    List<Notification> getAdminNotifications(HttpSession session);
    void markAllDemoAdminNotificationsRead(HttpSession session);
    List<Message> getAdminInboxMessages(HttpSession session);
    List<Message> getAdminSentMessages(HttpSession session);
    List<Message> getAdminStaffRequests(HttpSession session);
    Message getAdminMessageById(HttpSession session, Long id);
    Message sendDemoAdminMessage(HttpSession session, List<Long> staffRecipientIds, String subject, String content);

    // ==========================================
    // STAFF DEMO DATA & MUTATIONS
    // ==========================================
    Map<String, Object> getStaffLiveOperationsSummary(HttpSession session);
    List<Map<String, Object>> getStaffLiveOperationsList(HttpSession session, String category);
    Map<String, Object> getStaffBookingGuestDetail(Long bookingId);
    Map<String, Object> getStaffBookingGuestDetail(HttpSession session, Long bookingId);
    Map<String, Object> getStaffRoomOperationalDetail(Long roomId);
    Map<String, Object> getStaffRoomOperationalDetail(HttpSession session, Long roomId);
    List<Room> getStaffRoomsList(HttpSession session);
    boolean updateDemoRoomStatus(HttpSession session, Long roomId, RoomStatus newStatus);
    List<Booking> getStaffBookingsList(HttpSession session);
    List<Guest> getStaffGuestList();
    List<Guest> getStaffGuestList(HttpSession session);
    Staff getStaffProfile();
    Staff getStaffProfile(HttpSession session);
    List<Notification> getStaffNotifications(HttpSession session);
    void markAllDemoStaffNotificationsRead(HttpSession session);
    List<Message> getStaffInboxMessages(HttpSession session);
    List<Message> getStaffSentMessages(HttpSession session);
    Message getStaffMessageById(HttpSession session, Long id);
    Message sendDemoStaffRequest(HttpSession session, String subject, String content, String requestType);

    // ==========================================
    // GUEST DEMO DATA & MUTATIONS
    // ==========================================
    Guest getGuestProfile();
    Guest getGuestProfile(HttpSession session);
    List<Booking> getGuestActiveBookings(HttpSession session);
    List<Booking> getGuestPastBookings(HttpSession session);
    Booking createDemoBooking(HttpSession session, BookingRequestDto requestDto);
    boolean cancelDemoBooking(HttpSession session, Long bookingId);
    List<PlannedHotel> getGuestPlannedHotels(HttpSession session);
    Set<Long> getGuestPlannedHotelIds(HttpSession session);
    long getGuestPlannedCount(HttpSession session);
    boolean addGuestPlannedHotel(HttpSession session, Long hotelId, LocalDate checkIn, LocalDate checkOut);
    boolean updateGuestPlannedDates(HttpSession session, Long hotelId, LocalDate checkIn, LocalDate checkOut);
    boolean removeGuestPlannedHotel(HttpSession session, Long hotelId);
    boolean toggleGuestPlannedHotel(HttpSession session, Long hotelId);
}
