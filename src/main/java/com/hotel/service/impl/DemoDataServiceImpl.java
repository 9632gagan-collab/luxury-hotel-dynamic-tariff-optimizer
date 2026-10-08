package com.hotel.service.impl;

import com.hotel.dto.BookingRequestDto;
import com.hotel.dto.StaffCreateDto;
import com.hotel.model.*;
import com.hotel.model.enums.*;
import com.hotel.repository.HotelRepository;
import com.hotel.service.DemoDataService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete, session-isolated Demo Sandbox implementation.
 * Ensures that any change made by an evaluator in Demo Mode (editing offers, updating room statuses,
 * creating bookings, adding staff, sending demo messages) exists ONLY within that evaluator's
 * HTTP session and NEVER touches the production PostgreSQL/H2 database or real accounts.
 */
@Service
public class DemoDataServiceImpl implements DemoDataService {

    private final HotelRepository hotelRepository;

    @Autowired
    public DemoDataServiceImpl(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public boolean isDemoMode(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute("DEMO_MODE"));
    }

    @Override
    public void resetDemoSession(HttpSession session) {
        if (session == null) return;
        session.setAttribute("DEMO_OFFERS", createInitialDemoOffers());
        session.setAttribute("DEMO_ROOMS", createInitialDemoRooms());
        session.setAttribute("DEMO_BOOKINGS_ACTIVE", createInitialActiveBookings());
        session.setAttribute("DEMO_BOOKINGS_PAST", createInitialPastBookings());
        session.setAttribute("DEMO_STAFF_LIST", createInitialDemoStaff());
        session.setAttribute("DEMO_ADMIN_NOTIFICATIONS", createInitialAdminNotifications());
        session.setAttribute("DEMO_STAFF_NOTIFICATIONS", createInitialStaffNotifications());
        session.setAttribute("DEMO_ADMIN_INBOX", createInitialAdminInbox());
        session.setAttribute("DEMO_ADMIN_SENT", createInitialAdminSent());
        session.setAttribute("DEMO_STAFF_INBOX", createInitialStaffInbox());
        session.setAttribute("DEMO_STAFF_SENT", createInitialStaffSent());
        session.setAttribute("DEMO_PLANNED_LIST", getDefaultDemoPlannedHotels());
    }

    private Hotel createDemoHotel(Long id, String name, String district, String city) {
        Hotel demoH = new Hotel(name, district, city, "Luxury Palace", "9876543210", "https://royalindianstays.com", "https://images.unsplash.com/photo-1566073771259-6a8506099945");
        demoH.setId(id != null ? id : 101L);
        demoH.setState("Karnataka");
        demoH.setAddress("Palace Road");
        demoH.setPostalCode("560001");
        demoH.setEmail("info@demo.com");
        demoH.setStartingPricePerNight(12000.0);
        demoH.setRating(4.9);
        demoH.setShortDescription("Heritage luxury property");
        return demoH;
    }

    // =========================================================================
    // DEMO OFFERS SANDBOX
    // =========================================================================

    private List<Offer> createInitialDemoOffers() {
        List<Offer> list = new ArrayList<>();
        Offer o1 = new Offer(
                "Kodagu & Western Ghats Serenade",
                "Karnataka Season",
                "Valid across Coorg & Chikkamagaluru resorts",
                "25% tariff reduction on 3+ night bookings\nComplimentary coffee estate plantation walking tour\nDaily artisanal buffet breakfast & high tea\n20% discount on Ayurvedic spa treatments",
                "GH-COORG25",
                25.0,
                "Kodagu",
                "/hotels?district=Kodagu"
        );
        o1.setId(101L);
        o1.setActive(true);

        Offer o2 = new Offer(
                "Royal Mysuru & Hampi Heritage",
                "Heritage Escape",
                "Immerse in royal palaces & UNESCO monuments",
                "Complimentary palace heritage tour chaperone\nRoyal candlelit private dinner for two\nGuaranteed late checkout until 3:00 PM\nWelcome basket with Mysore Pak & royal fragrances",
                "GH-HERITAGE",
                20.0,
                "Mysuru",
                "/hotels?district=Mysuru"
        );
        o2.setId(102L);
        o2.setActive(true);

        Offer o3 = new Offer(
                "Bengaluru Executive & Weekend Haven",
                "Metropolitan Retreat",
                "City center opulence with executive club privileges",
                "Complimentary upgrade to Executive Suite (subject to availability)\nAccess to exclusive 24th-floor Executive Club Lounge\nAirport luxury sedan transfers included\nHigh-speed fiber connectivity & private meeting room access",
                "GH-BLRDEAL",
                15.0,
                "Bengaluru Urban",
                "/hotels?district=Bengaluru+Urban"
        );
        o3.setId(103L);
        o3.setActive(true);

        Offer o4 = new Offer(
                "Coastal Karavali & Western Sunset Escape",
                "Coastal Getaway",
                "Unwind alongside pristine Arabian Sea shorelines",
                "Exclusive private beach cabana for sunset viewing\nComplimentary coastal seafood barbeque dining experience\nWater sports voucher (Jet ski / Parasailing)\nSignature coconut oil Ayurvedic head massage",
                "GH-COASTAL",
                30.0,
                "Dakshina Kannada",
                "/hotels?district=Dakshina+Kannada"
        );
        o4.setId(104L);
        o4.setActive(true);

        list.add(o1);
        list.add(o2);
        list.add(o3);
        list.add(o4);
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Offer> getDemoOffers(HttpSession session) {
        if (session == null) return createInitialDemoOffers();
        List<Offer> list = (List<Offer>) session.getAttribute("DEMO_OFFERS");
        if (list == null) {
            list = createInitialDemoOffers();
            session.setAttribute("DEMO_OFFERS", list);
        }
        return list;
    }

    @Override
    public List<Offer> getDemoActiveOffers(HttpSession session) {
        return getDemoOffers(session).stream()
                .filter(Offer::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Offer> getDemoOfferById(HttpSession session, Long id) {
        if (id == null) return Optional.empty();
        return getDemoOffers(session).stream()
                .filter(o -> id.equals(o.getId()))
                .findFirst();
    }

    @Override
    public Offer saveDemoOffer(HttpSession session, Offer offer) {
        if (offer == null) return null;
        List<Offer> list = new ArrayList<>(getDemoOffers(session));
        if (offer.getId() != null) {
            for (int i = 0; i < list.size(); i++) {
                if (offer.getId().equals(list.get(i).getId())) {
                    Offer existing = list.get(i);
                    existing.setTitle(offer.getTitle());
                    existing.setTag(offer.getTag());
                    existing.setSubtitle(offer.getSubtitle());
                    existing.setPerks(offer.getPerks());
                    existing.setPromoCode(offer.getPromoCode());
                    existing.setDiscountPercentage(offer.getDiscountPercentage());
                    existing.setTargetDistrict(offer.getTargetDistrict());
                    existing.setTargetUrl(offer.getTargetUrl());
                    existing.setActive(offer.isActive());
                    if (session != null) session.setAttribute("DEMO_OFFERS", list);
                    return existing;
                }
            }
        }
        long nextId = list.stream().mapToLong(o -> o.getId() != null ? o.getId() : 0).max().orElse(100) + 1;
        offer.setId(nextId);
        list.add(offer);
        if (session != null) session.setAttribute("DEMO_OFFERS", list);
        return offer;
    }

    @Override
    public Offer toggleDemoOfferStatus(HttpSession session, Long id) {
        if (id == null) return null;
        List<Offer> list = new ArrayList<>(getDemoOffers(session));
        for (Offer o : list) {
            if (id.equals(o.getId())) {
                o.setActive(!o.isActive());
                if (session != null) session.setAttribute("DEMO_OFFERS", list);
                return o;
            }
        }
        return null;
    }

    @Override
    public boolean deleteDemoOffer(HttpSession session, Long id) {
        if (id == null) return false;
        List<Offer> list = new ArrayList<>(getDemoOffers(session));
        boolean removed = list.removeIf(o -> id.equals(o.getId()));
        if (session != null) session.setAttribute("DEMO_OFFERS", list);
        return removed;
    }

    // =========================================================================
    // DEMO ROOMS & DYNAMIC STATISTICS SANDBOX
    // =========================================================================

    private List<Room> createInitialDemoRooms() {
        Room r1 = new Room("D-204", RoomCategory.DELUXE, RoomStatus.OCCUPIED, 4500.0, 2, 2, "Deluxe king room", "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
        r1.setId(201L);

        Room r2 = new Room("S-105", RoomCategory.SUITE, RoomStatus.AVAILABLE, 8500.0, 1, 4, "Executive suite with balcony", "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80");
        r2.setId(202L);

        Room r3 = new Room("D-311", RoomCategory.DELUXE, RoomStatus.AVAILABLE, 4800.0, 3, 2, "Garden facing deluxe room", "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80");
        r3.setId(203L);

        Room r4 = new Room("M-101", RoomCategory.DELUXE, RoomStatus.MAINTENANCE, 4500.0, 1, 2, "Standard room undergoing inspection", "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80");
        r4.setId(204L);

        Room r5 = new Room("S-202", RoomCategory.SUITE, RoomStatus.RESERVED, 8500.0, 2, 4, "Grand luxury suite", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80");
        r5.setId(205L);

        Room r6 = new Room("D-205", RoomCategory.DELUXE, RoomStatus.AVAILABLE, 4500.0, 2, 2, "Premium courtyard deluxe", "https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=800&q=80");
        r6.setId(206L);

        return new ArrayList<>(List.of(r1, r2, r3, r4, r5, r6));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Room> getStaffRoomsList(HttpSession session) {
        if (session == null) return createInitialDemoRooms();
        List<Room> list = (List<Room>) session.getAttribute("DEMO_ROOMS");
        if (list == null) {
            list = createInitialDemoRooms();
            session.setAttribute("DEMO_ROOMS", list);
        }
        return list;
    }

    @Override
    public boolean updateDemoRoomStatus(HttpSession session, Long roomId, RoomStatus newStatus) {
        if (roomId == null || newStatus == null) return false;
        List<Room> rooms = getStaffRoomsList(session);
        for (Room r : rooms) {
            if (roomId.equals(r.getId())) {
                r.setStatus(newStatus);
                if (session != null) session.setAttribute("DEMO_ROOMS", rooms);
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // ADMIN DEMO
    // =========================================================================

    @Override
    public Map<String, Object> getAdminGuestActivityOverview(HttpSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", 15);
        map.put("currentGuestsCount", 4L);
        map.put("currentGuestsPct", 28);
        map.put("upcomingGuestsCount", 7L);
        map.put("upcomingGuestsPct", 44);
        map.put("completedStaysCount", 3L);
        map.put("completedStaysPct", 22);
        map.put("pendingCount", 1L);
        map.put("pendingPct", 6);
        return map;
    }

    @Override
    public Map<String, Object> getAdminHotelOperationsOverview(HttpSession session) {
        List<Room> rooms = getStaffRoomsList(session);
        int total = Math.max(rooms.size(), 6);
        long occupied = rooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        long available = rooms.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
        long reserved = rooms.stream().filter(r -> r.getStatus() == RoomStatus.RESERVED).count();
        long prebooked = rooms.stream().filter(r -> r.getStatus() == RoomStatus.CLEANING).count();
        long maintenance = rooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();

        // Calculate dynamic percentages or default to baseline if unchanged
        int occPct = (int) Math.round((occupied * 100.0) / total);
        int availPct = (int) Math.round((available * 100.0) / total);
        int resPct = (int) Math.round((reserved * 100.0) / total);
        int prePct = (int) Math.round((prebooked * 100.0) / total);
        int maintPct = (int) Math.round((maintenance * 100.0) / total);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalInventory", 44);
        map.put("occupancyPct", occPct > 0 ? occPct : 68);
        map.put("occupiedPct", occPct > 0 ? occPct : 68);
        map.put("occupiedCount", occupied > 0 ? occupied : 30L);
        map.put("availablePct", availPct > 0 ? availPct : 22);
        map.put("availableCount", available > 0 ? available : 10L);
        map.put("reservedPct", resPct > 0 ? resPct : 41);
        map.put("reservedCount", reserved > 0 ? reserved : 18L);
        map.put("prebookedPct", prePct > 0 ? prePct : 17);
        map.put("prebookedCount", prebooked > 0 ? prebooked : 7L);
        map.put("unreservedPct", 10);
        map.put("unreservedCount", 4L);
        map.put("maintenancePct", maintPct > 0 ? maintPct : 8);
        map.put("maintenanceCount", maintenance > 0 ? maintenance : 4L);
        map.put("currentGuests", 12L);
        map.put("upcomingCheckins", 8L);
        map.put("upcomingCheckIns", 8L);
        map.put("upcomingCheckouts", 5L);
        map.put("upcomingCheckOuts", 5L);
        return map;
    }

    @Override
    public Map<String, Object> getAdminMaintenanceOverview(HttpSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("maintenancePct", 8);
        map.put("activeCount", 3L);
        map.put("totalRecords", 6);
        return map;
    }

    @Override
    public Map<String, Object> getAdminInspectionsOverview(HttpSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", 4);
        map.put("completedCount", 2L);
        map.put("completedPct", 40);
        map.put("inspectedCount", 1L);
        map.put("inspectedPct", 30);
        map.put("pendingCount", 1L);
        map.put("pendingPct", 20);
        map.put("dueCount", 1L);
        map.put("duePct", 10);
        return map;
    }

    @Override
    public Map<String, Object> getAdminStaffActivityOverview(HttpSession session) {
        List<Staff> staffList = getAdminStaffList(session);
        int total = Math.max(staffList.size(), 5);
        long working = staffList.stream().filter(s -> "Working".equalsIgnoreCase(s.getActivityStatus())).count();
        long onLeave = staffList.stream().filter(s -> "On Leave".equalsIgnoreCase(s.getActivityStatus())).count();
        long notWorking = staffList.stream().filter(s -> "Not Working".equalsIgnoreCase(s.getActivityStatus())).count();

        int workPct = (int) Math.round((working * 100.0) / total);
        int leavePct = (int) Math.round((onLeave * 100.0) / total);
        int notWorkPct = (int) Math.round((notWorking * 100.0) / total);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", total);
        map.put("workingCount", working > 0 ? working : 3L);
        map.put("workingPct", workPct > 0 ? workPct : 60);
        map.put("onLeaveCount", onLeave > 0 ? onLeave : 1L);
        map.put("onLeavePct", leavePct > 0 ? leavePct : 20);
        map.put("notWorkingCount", notWorking > 0 ? notWorking : 1L);
        map.put("notWorkingPct", notWorkPct > 0 ? notWorkPct : 20);
        return map;
    }

    @Override
    public List<Hotel> getAdminMaintenanceAffectedHotels() {
        List<Hotel> allHotels = hotelRepository.findAll();
        if (!allHotels.isEmpty()) {
            return allHotels.stream().limit(3).collect(Collectors.toList());
        }
        Hotel h = createDemoHotel(101L, "Demo Palace Bengaluru", "Bengaluru Urban", "Bengaluru");
        return List.of(h);
    }

    @Override
    public List<Hotel> getAdminMaintenanceAffectedHotels(HttpSession session) {
        return getAdminMaintenanceAffectedHotels();
    }

    @Override
    public List<Map<String, Object>> getAdminRoomsByStatus(HttpSession session, String status) {
        List<Map<String, Object>> list = new ArrayList<>();
        String s = status != null ? status.trim().toUpperCase() : "AVAILABLE";

        if ("RESERVED".equals(s)) {
            list.add(createRoomRow("DEMO-BK-1001", "Demo Palace Bengaluru", "D-204", "Demo Guest 01", "10 Oct 2026 to 12 Oct 2026", "Confirmed Reservation", "₹12,000"));
            list.add(createRoomRow("DEMO-BK-1002", "Demo Palace Mysuru", "S-105", "Demo Guest 02", "14 Oct 2026 to 17 Oct 2026", "Confirmed Reservation", "₹18,500"));
            list.add(createRoomRow("DEMO-BK-1003", "Demo Resort Coorg", "V-302", "Demo Guest 03", "18 Oct 2026 to 21 Oct 2026", "Confirmed Reservation", "₹24,000"));
        } else if ("PRE-BOOKED".equals(s) || "PREBOOKED".equals(s)) {
            list.add(createRoomRow("DEMO-BK-1004", "Demo Palace Bengaluru", "D-205", "Demo Guest 04", "18 Oct 2026 to 20 Oct 2026", "Pre-Booked / Awaiting Confirmation", "₹10,000"));
            list.add(createRoomRow("DEMO-BK-1005", "Demo Heritage Hampi", "S-201", "Demo Guest 05", "22 Oct 2026 to 24 Oct 2026", "Pre-Booked / Awaiting Confirmation", "₹14,000"));
        } else if ("OCCUPIED".equals(s)) {
            list.add(createRoomRow("DEMO-BK-1001", "Demo Palace Bengaluru", "D-204", "Demo Guest 01", "10 Oct 2026 to 12 Oct 2026", "Occupied / In-House Guest", "₹12,000"));
            list.add(createRoomRow("DEMO-BK-1006", "Demo Palace Bengaluru", "S-301", "Demo Guest 06", "09 Oct 2026 to 13 Oct 2026", "Occupied / In-House Guest", "₹22,000"));
        } else {
            // AVAILABLE / UNRESERVED
            list.add(createRoomRow("ROOM-D-101", "Demo Palace Bengaluru", "D-101", "Ready for Allocation", "Floor 1 • Max 2 Guests", "Available / Clean", "₹4,500/night"));
            list.add(createRoomRow("ROOM-S-105", "Demo Palace Bengaluru", "S-105", "Ready for Allocation", "Floor 1 • Max 4 Guests", "Available / Clean", "₹8,500/night"));
            list.add(createRoomRow("ROOM-PH-01", "Demo Palace Bengaluru", "PH-01", "Ready for Allocation", "Floor 4 • Max 6 Guests", "Available / Clean", "₹18,000/night"));
        }
        return list;
    }

    private Map<String, Object> createRoomRow(String ref, String hotel, String room, String guest, String dates, String status, String amount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reference", ref);
        m.put("hotelName", hotel);
        m.put("roomNumber", room);
        m.put("guestName", guest);
        m.put("dates", dates);
        m.put("status", status);
        m.put("amount", amount);
        return m;
    }

    @Override
    public List<Map<String, Object>> getAdminGuestFlow(String flowType) {
        return getAdminGuestFlow(null, flowType);
    }

    @Override
    public List<Map<String, Object>> getAdminGuestFlow(HttpSession session, String flowType) {
        List<Map<String, Object>> list = new ArrayList<>();
        String type = flowType != null ? flowType.trim().toUpperCase() : "CURRENT";

        if ("CURRENT".equals(type) || "CURRENT_GUESTS".equals(type)) {
            list.add(createFlowItem("Demo Guest 01", "Demo Palace Bengaluru", "Room D-204 • Stay until 12 Oct 2026", "In-House", "badge-success"));
            list.add(createFlowItem("Demo Guest 06", "Demo Palace Bengaluru", "Room S-301 • Stay until 13 Oct 2026", "In-House", "badge-success"));
        } else if ("CHECKINS".equals(type) || "CHECK_INS".equals(type)) {
            list.add(createFlowItem("Demo Guest 02", "Demo Palace Mysuru", "Check-in: 14 Oct 2026 (14:00)", "Arriving Soon", "badge-info"));
            list.add(createFlowItem("Demo Guest 04", "Demo Palace Bengaluru", "Check-in: 18 Oct 2026 (14:00)", "Arriving Soon", "badge-info"));
        } else {
            // CHECKOUTS
            list.add(createFlowItem("Demo Guest 01", "Demo Palace Bengaluru", "Checkout: 12 Oct 2026 (11:00)", "Departure Pending", "badge-warning"));
        }
        return list;
    }

    private Map<String, Object> createFlowItem(String title, String sub, String meta, String badge, String badgeClass) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("sub", sub);
        m.put("meta", meta);
        m.put("badge", badge);
        m.put("badgeClass", badgeClass);
        return m;
    }

    @Override
    public List<Map<String, Object>> getAdminGuestActivityDetails(String category) {
        return getAdminGuestActivityDetails(null, category);
    }

    @Override
    public List<Map<String, Object>> getAdminGuestActivityDetails(HttpSession session, String category) {
        List<Map<String, Object>> list = new ArrayList<>();
        String cat = category != null ? category.trim().toUpperCase() : "CURRENT";

        if ("CURRENT".equals(cat)) {
            Map<String, Object> g1 = new LinkedHashMap<>();
            g1.put("reference", "DEMO-BK-1001");
            g1.put("guestName", "Demo Guest 01");
            g1.put("email", "demo.guest01@example.com");
            g1.put("hotelName", "Demo Palace Bengaluru");
            g1.put("roomNumber", "D-204");
            g1.put("dates", "10 Oct 2026 to 12 Oct 2026");
            g1.put("guests", "2 Guests");
            g1.put("amount", "₹12,000");
            g1.put("status", "Checked In");
            g1.put("guestType", "Adult");
            g1.put("age", 32);
            g1.put("gender", "Female");
            list.add(g1);
        } else if ("UPCOMING".equals(cat)) {
            Map<String, Object> g2 = new LinkedHashMap<>();
            g2.put("reference", "DEMO-BK-1002");
            g2.put("guestName", "Demo Guest 02");
            g2.put("email", "demo.guest02@example.com");
            g2.put("hotelName", "Demo Palace Mysuru");
            g2.put("roomNumber", "S-105");
            g2.put("dates", "14 Oct 2026 to 17 Oct 2026");
            g2.put("guests", "3 Guests");
            g2.put("amount", "₹18,500");
            g2.put("status", "Upcoming Reservation");
            g2.put("guestType", "Adult");
            g2.put("age", 41);
            g2.put("gender", "Female");
            list.add(g2);
        } else if ("COMPLETED".equals(cat)) {
            Map<String, Object> g3 = new LinkedHashMap<>();
            g3.put("reference", "DEMO-BK-1003");
            g3.put("guestName", "Demo Guest 03");
            g3.put("email", "demo.guest03@example.com");
            g3.put("hotelName", "Demo Resort Coorg");
            g3.put("roomNumber", "V-302");
            g3.put("dates", "05 Oct 2026 to 08 Oct 2026");
            g3.put("guests", "2 Guests");
            g3.put("amount", "₹24,000");
            g3.put("status", "Completed Stay");
            g3.put("guestType", "Adult");
            g3.put("age", 35);
            g3.put("gender", "Male");
            list.add(g3);
        } else if ("PENDING".equals(cat)) {
            Map<String, Object> g4 = new LinkedHashMap<>();
            g4.put("reference", "DEMO-BK-1004");
            g4.put("guestName", "Demo Guest 04");
            g4.put("email", "demo.guest04@example.com");
            g4.put("hotelName", "Demo Palace Bengaluru");
            g4.put("roomNumber", "D-205");
            g4.put("dates", "18 Oct 2026 to 20 Oct 2026");
            g4.put("guests", "1 Guest");
            g4.put("amount", "₹10,000");
            g4.put("status", "Pending Confirmation");
            g4.put("guestType", "Adult");
            g4.put("age", 29);
            g4.put("gender", "Male");
            list.add(g4);
        }
        return list;
    }

    @Override
    public List<Staff> getAdminStaffByActivityStatus(HttpSession session, String status) {
        List<Staff> all = getAdminStaffList(session);
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return all;
        }
        String s = status.trim();
        return all.stream()
                .filter(st -> s.equalsIgnoreCase(st.getActivityStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getAdminDistrictOperationsOverview(String state, String district) {
        return getAdminDistrictOperationsOverview(null, state, district);
    }

    @Override
    public Map<String, Object> getAdminDistrictOperationsOverview(HttpSession session, String state, String district) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("state", state != null ? state : "Karnataka");
        map.put("district", district != null ? district : "Bengaluru Urban");
        map.put("totalHotels", 3);
        map.put("totalRooms", 44);
        map.put("availableRooms", 14);
        map.put("availableCount", 14);
        map.put("availablePct", 32);
        map.put("reservedPct", 41);
        map.put("reservedCount", 18);
        map.put("prebookedPct", 17);
        map.put("prebookedCount", 7);
        map.put("occupiedPct", 10);
        map.put("occupiedCount", 5);
        map.put("unreservedPct", 32);
        map.put("unreservedCount", 14);
        return map;
    }

    @Override
    public Map<String, Object> getAdminHotelOperationalDetails(Long hotelId) {
        return getAdminHotelOperationalDetails(null, hotelId);
    }

    @Override
    public Map<String, Object> getAdminHotelOperationalDetails(HttpSession session, Long hotelId) {
        Map<String, Object> map = new LinkedHashMap<>();
        Hotel h = hotelRepository.findById(hotelId).orElseGet(() -> createDemoHotel(hotelId, "Demo Palace Bengaluru", "Bengaluru Urban", "Bengaluru"));
        map.put("hotel", h);
        map.put("operationalStatus", "Fully In Service / Demo Active");
        map.put("availablePct", 32);
        map.put("availableCount", 14L);
        map.put("reservedPct", 41);
        map.put("reservedCount", 18L);
        map.put("prebookedPct", 17);
        map.put("prebookedCount", 7L);
        map.put("occupiedPct", 10);
        map.put("occupiedCount", 5L);
        map.put("maintenancePct", 5);
        map.put("unreservedCount", 14L);

        List<Map<String, Object>> categories = new ArrayList<>();
        categories.add(Map.of("name", "Deluxe Room", "total", 14, "available", 6, "tariff", 4500.0));
        categories.add(Map.of("name", "Executive Suite", "total", 8, "available", 3, "tariff", 8500.0));
        categories.add(Map.of("name", "Presidential Penthouse", "total", 4, "available", 1, "tariff", 18000.0));
        map.put("categories", categories);

        return map;
    }

    @Override
    public List<MaintenanceRecord> getAdminMaintenanceRecords(Long hotelId) {
        return getAdminMaintenanceRecords(null, hotelId);
    }

    @Override
    public List<MaintenanceRecord> getAdminMaintenanceRecords(HttpSession session, Long hotelId) {
        Hotel h = hotelRepository.findById(hotelId).orElseGet(() -> createDemoHotel(hotelId, "Demo Palace Bengaluru", "Bengaluru Urban", "Bengaluru"));

        MaintenanceRecord m1 = new MaintenanceRecord(h, "Suite D-204", "HVAC Cooling Servicing", "IN_PROGRESS", LocalDate.now().minusDays(1), "Demo Technician 01", "Seasonal compressor lubrication and filter calibration");
        m1.setId(1001L);
        MaintenanceRecord m2 = new MaintenanceRecord(h, "Room S-105", "Smart Keyless Lock Firmware", "SCHEDULED", LocalDate.now().plusDays(1), "Demo Technician 02", "Firmware synchronization with central desk");
        m2.setId(1002L);
        return List.of(m1, m2);
    }

    @Override
    public List<Inspection> getAdminInspections(String status) {
        return getAdminInspections(null, status);
    }

    @Override
    public List<Inspection> getAdminInspections(HttpSession session, String status) {
        Hotel h = getAdminMaintenanceAffectedHotels().get(0);
        Inspection i1 = new Inspection(h, "Entire Property", "Demo Auditor 01", LocalDate.now().minusDays(2), "COMPLETED", "98%", "Standard luxury hygiene audit passed with excellence.");
        i1.setId(2001L);
        Inspection i2 = new Inspection(h, "Culinary Wing", "Demo Auditor 02", LocalDate.now().minusDays(1), "INSPECTED", "96%", "HACCP compliance verified.");
        i2.setId(2002L);
        Inspection i3 = new Inspection(h, "Wellness & Spa", "Demo Auditor 01", LocalDate.now().plusDays(2), "DUE", "Pending", "Scheduled bi-weekly sanitation review.");
        i3.setId(2003L);
        Inspection i4 = new Inspection(h, "Fire & Life Safety", "Demo Auditor 03", LocalDate.now().plusDays(4), "PENDING", "Pending", "Automated sprinkler pressure check.");
        i4.setId(2004L);

        List<Inspection> list = List.of(i1, i2, i3, i4);
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return list;
        }
        return list.stream().filter(i -> status.equalsIgnoreCase(i.getStatus())).collect(Collectors.toList());
    }

    // =========================================================================
    // DEMO STAFF ROSTER
    // =========================================================================

    private List<Staff> createInitialDemoStaff() {
        Staff s1 = new Staff("demo_staff_01", "", "Demo Staff 01", "demo.staff01@example.com", "DEMO-PHONE-01", "DEMO-STF-01", "Front Desk", "Operations Lead", "Morning (07:00 - 15:30)");
        s1.setId(301L);
        s1.setActivityStatus("Working");
        s1.setStatus(AccountStatus.ACTIVE);
        s1.setActive(true);

        Staff s2 = new Staff("demo_staff_02", "", "Demo Staff 02", "demo.staff02@example.com", "DEMO-PHONE-02", "DEMO-STF-02", "Guest Relations", "Senior Concierge", "Morning (07:00 - 15:30)");
        s2.setId(302L);
        s2.setActivityStatus("On Leave");
        s2.setStatus(AccountStatus.ACTIVE);
        s2.setActive(true);

        Staff s3 = new Staff("demo_staff_03", "", "Demo Staff 03", "demo.staff03@example.com", "DEMO-PHONE-03", "DEMO-STF-03", "Engineering", "Maintenance Specialist", "Night (23:00 - 07:30)");
        s3.setId(303L);
        s3.setActivityStatus("Not Working");
        s3.setStatus(AccountStatus.INACTIVE);
        s3.setActive(false);

        Staff s4 = new Staff("demo_staff_04", "", "Demo Staff 04", "demo.staff04@example.com", "DEMO-PHONE-04", "DEMO-STF-04", "Housekeeping", "Floor Supervisor", "Evening (15:00 - 23:30)");
        s4.setId(304L);
        s4.setActivityStatus("Working");
        s4.setStatus(AccountStatus.ACTIVE);
        s4.setActive(true);

        Staff s5 = new Staff("demo_staff_05", "", "Demo Staff 05", "demo.staff05@example.com", "DEMO-PHONE-05", "DEMO-STF-05", "Culinary", "Sous Chef", "General (09:00 - 18:00)");
        s5.setId(305L);
        s5.setActivityStatus("Working");
        s5.setStatus(AccountStatus.ACTIVE);
        s5.setActive(true);

        return new ArrayList<>(List.of(s1, s2, s3, s4, s5));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Staff> getAdminStaffList(HttpSession session) {
        if (session == null) return createInitialDemoStaff();
        List<Staff> list = (List<Staff>) session.getAttribute("DEMO_STAFF_LIST");
        if (list == null) {
            list = createInitialDemoStaff();
            session.setAttribute("DEMO_STAFF_LIST", list);
        }
        return list;
    }

    @Override
    public Staff addDemoStaff(HttpSession session, StaffCreateDto staffDto) {
        if (staffDto == null) return null;
        List<Staff> list = new ArrayList<>(getAdminStaffList(session));
        long nextId = list.stream().mapToLong(s -> s.getId() != null ? s.getId() : 0).max().orElse(300) + 1;
        String username = staffDto.getStaffName() != null ? staffDto.getStaffName().toLowerCase().replaceAll("\\s+", "_") : "demo_staff_" + nextId;
        String fullName = staffDto.getStaffName() != null ? staffDto.getStaffName() : "Demo Staff " + nextId;
        String email = username + "@example.com";
        String phone = "DEMO-PHONE-" + nextId;
        String empId = "DEMO-STF-" + nextId;
        String dept = "Operations";
        String desig = "Staff";
        String shift = "General (09:00 - 18:00)";

        Staff s = new Staff(username, "", fullName, email, phone, empId, dept, desig, shift);
        s.setId(nextId);
        s.setActivityStatus("Working");
        s.setStatus(AccountStatus.ACTIVE);
        s.setActive(true);
        list.add(s);
        if (session != null) session.setAttribute("DEMO_STAFF_LIST", list);
        return s;
    }

    @Override
    public boolean toggleDemoStaffStatus(HttpSession session, Long staffId) {
        if (staffId == null) return false;
        List<Staff> list = new ArrayList<>(getAdminStaffList(session));
        for (Staff s : list) {
            if (staffId.equals(s.getId())) {
                boolean active = s.isActive();
                s.setActive(!active);
                s.setStatus(!active ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
                s.setActivityStatus(!active ? "Working" : "Not Working");
                if (session != null) session.setAttribute("DEMO_STAFF_LIST", list);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Guest> getAdminGuestList() {
        return getAdminGuestList(null);
    }

    @Override
    public List<Guest> getAdminGuestList(HttpSession session) {
        Guest g1 = new Guest("demo_guest_01", "", "Demo Guest 01", "demo.guest01@example.com");
        g1.setId(401L);
        g1.setPhoneNumber("DEMO-PHONE-G1");
        g1.setStatus(AccountStatus.ACTIVE);
        g1.setActive(true);

        Guest g2 = new Guest("demo_guest_02", "", "Demo Guest 02", "demo.guest02@example.com");
        g2.setId(402L);
        g2.setPhoneNumber("DEMO-PHONE-G2");
        g2.setStatus(AccountStatus.ACTIVE);
        g2.setActive(true);

        Guest g3 = new Guest("demo_guest_03", "", "Demo Guest 03", "demo.guest03@example.com");
        g3.setId(403L);
        g3.setPhoneNumber("DEMO-PHONE-G3");
        g3.setStatus(AccountStatus.ACTIVE);
        g3.setActive(true);

        Guest g4 = new Guest("demo_guest_04", "", "Demo Guest 04", "demo.guest04@example.com");
        g4.setId(404L);
        g4.setPhoneNumber("DEMO-PHONE-G4");
        g4.setStatus(AccountStatus.ACTIVE);
        g4.setActive(true);

        return List.of(g1, g2, g3, g4);
    }

    @Override
    public List<Booking> getAdminBookingsList(HttpSession session) {
        return getGuestActiveBookings(session);
    }

    @Override
    public Admin getAdminProfile() {
        Admin a = new Admin("Demo Admin", "");
        a.setId(501L);
        a.setFullName("Demo Administrator");
        a.setEmail("demo.admin@example.com");
        a.setAdminLevel("EXECUTIVE_ADMIN");
        a.setStatus(AccountStatus.ACTIVE);
        a.setActive(true);
        return a;
    }

    @Override
    public Admin getAdminProfile(HttpSession session) {
        return getAdminProfile();
    }

    // =========================================================================
    // DEMO NOTIFICATIONS & MESSAGES (ADMIN)
    // =========================================================================

    private List<Notification> createInitialAdminNotifications() {
        Admin admin = getAdminProfile();
        Staff staff = getStaffProfile();

        Notification n1 = new Notification();
        n1.setId(601L);
        n1.setRecipient(admin);
        n1.setSender(staff);
        n1.setNotificationType("STAFF_REQUEST");
        n1.setSubject("New Staff Request from Demo Staff 01");
        n1.setPreview("Demo request: Room 204 housekeeping required.");

        Notification n2 = new Notification();
        n2.setId(602L);
        n2.setRecipient(admin);
        n2.setSender(admin);
        n2.setNotificationType("AUDIT");
        n2.setSubject("Quality Audit Completed");
        n2.setPreview("Hygiene audit score 98% recorded for Demo Palace Bengaluru.");

        return new ArrayList<>(List.of(n1, n2));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Notification> getAdminNotifications(HttpSession session) {
        if (session == null) return createInitialAdminNotifications();
        List<Notification> list = (List<Notification>) session.getAttribute("DEMO_ADMIN_NOTIFICATIONS");
        if (list == null) {
            list = createInitialAdminNotifications();
            session.setAttribute("DEMO_ADMIN_NOTIFICATIONS", list);
        }
        return list;
    }

    @Override
    public void markAllDemoAdminNotificationsRead(HttpSession session) {
        if (session != null) {
            session.setAttribute("DEMO_ADMIN_NOTIFICATIONS", new ArrayList<Notification>());
        }
    }

    private List<Message> createInitialAdminInbox() {
        Staff staff = getStaffProfile();
        Message m = new Message();
        m.setId(701L);
        m.setSender(staff);
        m.setSenderRole(UserRole.STAFF);
        m.setRequestType("STAFF_REQUEST");
        m.setSubject("Demo request: Room 204 housekeeping required.");
        m.setContent("Please dispatch housekeeping team for priority turnaround in Suite D-204.");
        return new ArrayList<>(List.of(m));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Message> getAdminInboxMessages(HttpSession session) {
        if (session == null) return createInitialAdminInbox();
        List<Message> list = (List<Message>) session.getAttribute("DEMO_ADMIN_INBOX");
        if (list == null) {
            list = createInitialAdminInbox();
            session.setAttribute("DEMO_ADMIN_INBOX", list);
        }
        return list;
    }

    private List<Message> createInitialAdminSent() {
        Admin admin = getAdminProfile();
        Message m = new Message();
        m.setId(702L);
        m.setSender(admin);
        m.setSenderRole(UserRole.ADMIN);
        m.setRequestType("ANNOUNCEMENT");
        m.setSubject("Demo operational broadcast");
        m.setContent("All operational leads please verify check-in logs for today's VIP arrivals.");
        return new ArrayList<>(List.of(m));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Message> getAdminSentMessages(HttpSession session) {
        if (session == null) return createInitialAdminSent();
        List<Message> list = (List<Message>) session.getAttribute("DEMO_ADMIN_SENT");
        if (list == null) {
            list = createInitialAdminSent();
            session.setAttribute("DEMO_ADMIN_SENT", list);
        }
        return list;
    }

    @Override
    public List<Message> getAdminStaffRequests(HttpSession session) {
        return getAdminInboxMessages(session);
    }

    @Override
    public Message getAdminMessageById(HttpSession session, Long id) {
        for (Message m : getAdminInboxMessages(session)) {
            if (id != null && id.equals(m.getId())) return m;
        }
        for (Message m : getAdminSentMessages(session)) {
            if (id != null && id.equals(m.getId())) return m;
        }
        Staff staff = getStaffProfile();
        Message m = new Message();
        m.setId(id != null ? id : 701L);
        m.setSender(staff);
        m.setSenderRole(UserRole.STAFF);
        m.setRequestType("STAFF_REQUEST");
        m.setSubject("Demo request: Room 204 housekeeping required.");
        m.setContent("Please dispatch housekeeping team for priority turnaround in Suite D-204.");
        return m;
    }

    @Override
    public Message sendDemoAdminMessage(HttpSession session, List<Long> staffRecipientIds, String subject, String content) {
        Admin admin = getAdminProfile();
        Message m = new Message();
        m.setId(System.currentTimeMillis() % 100000);
        m.setSender(admin);
        m.setSenderRole(UserRole.ADMIN);
        m.setRequestType("ANNOUNCEMENT");
        m.setSubject(subject != null ? subject : "Demo broadcast from Admin");
        m.setContent(content != null ? content : "Demo broadcast content");

        List<Message> sent = new ArrayList<>(getAdminSentMessages(session));
        sent.add(0, m);
        if (session != null) session.setAttribute("DEMO_ADMIN_SENT", sent);
        return m;
    }

    // =========================================================================
    // STAFF DEMO
    // =========================================================================

    @Override
    public Map<String, Object> getStaffLiveOperationsSummary(HttpSession session) {
        List<Room> rooms = getStaffRoomsList(session);
        long occupied = rooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        long available = rooms.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
        long reserved = rooms.stream().filter(r -> r.getStatus() == RoomStatus.RESERVED).count();
        long maintenance = rooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();
        long cleaning = rooms.stream().filter(r -> r.getStatus() == RoomStatus.CLEANING).count();

        // Baseline operational statistics matching hotel level overview
        int occPct = 62;
        int availPct = 24;
        int resPct = 34;
        int prePct = 18;
        int maintPct = 6;

        if (occupied > 1) {
            occPct = Math.min(100, 62 + (int)((occupied - 1) * 6));
            availPct = Math.max(0, 24 - (int)((occupied - 1) * 6));
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalInventory", 24);
        map.put("occupancyPct", occPct);
        map.put("occupiedCount", 15L + (occupied - 1));
        map.put("availablePct", availPct);
        map.put("availableCount", available > 0 ? available : 6L);
        map.put("reservedPct", resPct);
        map.put("reservedCount", 8L);
        map.put("prebookedPct", prePct);
        map.put("prebookedCount", 4L);
        map.put("unreservedPct", 24);
        map.put("unreservedCount", 6L);
        map.put("maintenancePct", maintPct);
        map.put("maintenanceCount", 2L);
        map.put("cleaningCount", 1L);

        map.put("currentGuests", 14L);
        map.put("todayArrivals", 12L);
        map.put("todayDepartures", 8L);
        map.put("upcomingGuests", 18L);
        map.put("completedStays", 10L);
        map.put("cancelledBookings", 1L);
        map.put("guestRequestsCount", 3L);
        return map;
    }

    @Override
    public List<Map<String, Object>> getStaffLiveOperationsList(HttpSession session, String category) {
        List<Map<String, Object>> list = new ArrayList<>();
        String cat = category != null ? category.trim().toUpperCase() : "OCCUPANCY";

        switch (cat) {
            case "OCCUPANCY":
            case "OCCUPIED":
            case "CURRENT_GUESTS":
                list.add(createStaffBookingItem(1001L, "DEMO-STAY-2041", "Demo Guest 01", "DEMO-PHONE-01", "Demo Palace Bengaluru", "S-105", "Executive Suite", "2026-10-10", "2026-10-12", 2, "CHECKED_IN", "Checked In", "In Residence / Active Stay", "Demo request: Extra pillows requested"));
                break;
            case "AVAILABLE":
            case "UNRESERVED":
                list.add(createStaffRoomItem(201L, "RM-D-101", "Demo Palace Bengaluru", "D-101", "Deluxe Room", 2, "AVAILABLE", "Available", "Ready for Guest Assignment"));
                list.add(createStaffRoomItem(202L, "RM-S-105", "Demo Palace Bengaluru", "S-105", "Executive Suite", 4, "AVAILABLE", "Available", "Ready for Guest Assignment"));
                break;
            case "RESERVED":
                list.add(createStaffBookingItem(1002L, "DEMO-STAY-2042", "Demo Guest 02", "DEMO-PHONE-02", "Demo Palace Bengaluru", "D-311", "Deluxe Room", "2026-10-14", "2026-10-17", 2, "CONFIRMED", "Confirmed", "Confirmed Reservation", "Airport transfer required"));
                break;
            case "PREBOOKED":
            case "PENDING":
                list.add(createStaffBookingItem(1003L, "DEMO-STAY-2043", "Demo Guest 03", "DEMO-PHONE-03", "Demo Palace Bengaluru", "D-205", "Deluxe Room", "2026-10-18", "2026-10-20", 1, "PENDING", "Pending", "Pending Confirmation", "Awaiting card pre-auth"));
                break;
            case "TODAY_ARRIVALS":
            case "ARRIVALS":
                list.add(createStaffBookingItem(1004L, "DEMO-STAY-2044", "Demo Guest 04", "DEMO-PHONE-04", "Demo Palace Bengaluru", "S-202", "Executive Suite", "2026-10-10", "2026-10-13", 2, "CONFIRMED", "Confirmed", "Arriving Today", "Early check-in requested"));
                break;
            case "TODAY_DEPARTURES":
            case "DEPARTURES":
                list.add(createStaffBookingItem(1001L, "DEMO-STAY-2041", "Demo Guest 01", "DEMO-PHONE-01", "Demo Palace Bengaluru", "S-105", "Executive Suite", "2026-10-08", "2026-10-10", 2, "CHECKED_IN", "Checked In", "Departing Today", "Late checkout approved"));
                break;
            case "UPCOMING_GUESTS":
            case "UPCOMING":
                list.add(createStaffBookingItem(1002L, "DEMO-STAY-2042", "Demo Guest 02", "DEMO-PHONE-02", "Demo Palace Bengaluru", "D-311", "Deluxe Room", "2026-10-14", "2026-10-17", 2, "CONFIRMED", "Confirmed", "Upcoming Arrival", "Vegetarian meal preference"));
                break;
            case "MAINTENANCE":
                list.add(createStaffRoomItem(203L, "RM-M-101", "Demo Palace Bengaluru", "M-101", "Standard Room", 2, "MAINTENANCE", "Maintenance", "Under Active Maintenance"));
                break;
            case "GUEST_REQUESTS":
            case "REQUESTS":
                list.add(createStaffBookingItem(1001L, "DEMO-STAY-2041", "Demo Guest 01", "DEMO-PHONE-01", "Demo Palace Bengaluru", "S-105", "Executive Suite", "2026-10-10", "2026-10-12", 2, "CHECKED_IN", "Checked In", "Special Concierge Request", "Demo request: Room 204 housekeeping required."));
                break;
            default:
                list.add(createStaffBookingItem(1001L, "DEMO-STAY-2041", "Demo Guest 01", "DEMO-PHONE-01", "Demo Palace Bengaluru", "S-105", "Executive Suite", "2026-10-10", "2026-10-12", 2, "CHECKED_IN", "Checked In", "Active Stay", "None"));
                break;
        }
        return list;
    }

    private Map<String, Object> createStaffBookingItem(Long id, String ref, String guest, String phone, String hotel, String room, String cat, String checkIn, String checkOut, int guests, String status, String statusDisplay, String opNote, String specialReq) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "BOOKING");
        m.put("id", id);
        m.put("reference", ref);
        m.put("guestName", guest);
        m.put("guestPhone", phone);
        m.put("hotelName", hotel);
        m.put("roomNumber", room);
        m.put("category", cat);
        m.put("checkIn", checkIn);
        m.put("checkOut", checkOut);
        m.put("guests", guests);
        m.put("status", status);
        m.put("statusDisplay", statusDisplay);
        m.put("operationalNote", opNote);
        m.put("specialRequests", specialReq);
        return m;
    }

    private Map<String, Object> createStaffRoomItem(Long id, String ref, String hotel, String room, String cat, int guests, String status, String statusDisplay, String opNote) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "ROOM");
        m.put("id", id);
        m.put("reference", ref);
        m.put("hotelName", hotel);
        m.put("roomNumber", room);
        m.put("category", cat);
        m.put("guests", guests);
        m.put("status", status);
        m.put("statusDisplay", statusDisplay);
        m.put("operationalNote", opNote);
        m.put("specialRequests", "");
        return m;
    }

    @Override
    public Map<String, Object> getStaffBookingGuestDetail(Long bookingId) {
        return getStaffBookingGuestDetail(null, bookingId);
    }

    @Override
    public Map<String, Object> getStaffBookingGuestDetail(HttpSession session, Long bookingId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookingId", bookingId != null ? bookingId : 1001L);
        m.put("bookingReference", "DEMO-STAY-2041");
        m.put("guestName", "Demo Guest 01");
        m.put("guestEmail", "demo.guest01@example.com");
        m.put("guestPhone", "DEMO-PHONE-01");
        m.put("guestType", "Adult");
        m.put("guestUsername", "demo_guest_01");

        List<Map<String, Object>> travelers = new ArrayList<>();
        travelers.add(Map.of("name", "Demo Guest 01", "guestType", "Adult", "age", 29, "gender", "Male"));
        travelers.add(Map.of("name", "Demo Guest Companion", "guestType", "Adult", "age", 27, "gender", "Female"));
        m.put("travelers", travelers);

        m.put("hotelName", "Demo Palace Bengaluru");
        m.put("city", "Bengaluru");
        m.put("district", "Bengaluru Urban");
        m.put("state", "Karnataka");
        m.put("address", "Palace Grounds");

        m.put("roomId", 201L);
        m.put("roomNumber", "S-105");
        m.put("floorNumber", 1);
        m.put("roomCategory", "Executive Suite");
        m.put("roomStatus", "Occupied");
        m.put("roomRate", 8500.0);

        m.put("checkInDate", "2026-10-10");
        m.put("checkInTime", "14:00");
        m.put("checkOutDate", "2026-10-12");
        m.put("checkOutTime", "11:00");
        m.put("numberOfGuests", 2);
        m.put("totalAmount", 17000.0);
        m.put("bookingStatus", "CHECKED_IN");
        m.put("bookingStatusDisplay", "Checked In");
        m.put("currentStayStatus", "Currently In Residence");
        m.put("specialRequests", "Demo request: Room 204 housekeeping required.");

        return m;
    }

    @Override
    public Map<String, Object> getStaffRoomOperationalDetail(Long roomId) {
        return getStaffRoomOperationalDetail(null, roomId);
    }

    @Override
    public Map<String, Object> getStaffRoomOperationalDetail(HttpSession session, Long roomId) {
        List<Room> rooms = getStaffRoomsList(session);
        Room found = rooms.stream().filter(r -> roomId != null && roomId.equals(r.getId())).findFirst().orElse(null);

        String roomNum = found != null ? found.getRoomNumber() : "D-204";
        String status = found != null && found.getStatus() != null ? found.getStatus().name() : "OCCUPIED";
        String statusDisplay = found != null && found.getStatus() != null ? found.getStatus().getDisplayName() : "Occupied";
        String cat = found != null && found.getCategory() != null ? found.getCategory().getDisplayName() : "Deluxe Room";

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roomId", roomId != null ? roomId : 201L);
        m.put("roomNumber", roomNum);
        m.put("floorNumber", 2);
        m.put("category", cat);
        m.put("status", status);
        m.put("statusDisplay", statusDisplay);
        m.put("basePrice", 4500.0);
        m.put("imageUrl", "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
        m.put("description", "Deluxe luxury suite with city view and ergonomic workspace.");
        m.put("hotelName", "Demo Palace Bengaluru");
        m.put("city", "Bengaluru");
        m.put("district", "Bengaluru Urban");
        m.put("state", "Karnataka");

        m.put("hasGuest", "OCCUPIED".equalsIgnoreCase(status));
        m.put("bookingId", 1001L);
        m.put("bookingReference", "DEMO-STAY-2041");
        m.put("guestName", "Demo Guest 01");
        m.put("guestPhone", "DEMO-PHONE-01");
        m.put("checkInDate", "2026-10-10");
        m.put("checkOutDate", "2026-10-12");
        m.put("numberOfGuests", 2);
        m.put("specialRequests", "Demo request: Room 204 housekeeping required.");
        m.put("bookingStatus", "CHECKED_IN");

        return m;
    }

    @Override
    public List<Booking> getStaffBookingsList(HttpSession session) {
        return getGuestActiveBookings(session);
    }

    @Override
    public List<Guest> getStaffGuestList() {
        return getStaffGuestList(null);
    }

    @Override
    public List<Guest> getStaffGuestList(HttpSession session) {
        return getAdminGuestList(session);
    }

    @Override
    public Staff getStaffProfile() {
        Staff s = new Staff("demo_staff", "", "Demo Staff 01", "demo.staff@example.com", "DEMO-PHONE-STAFF", "DEMO-STF-01", "Front Desk", "Operations Lead", "Morning (07:00 - 15:30)");
        s.setId(801L);
        s.setActivityStatus("Working");
        s.setStatus(AccountStatus.ACTIVE);
        s.setActive(true);
        return s;
    }

    @Override
    public Staff getStaffProfile(HttpSession session) {
        return getStaffProfile();
    }

    private List<Notification> createInitialStaffNotifications() {
        Staff staff = getStaffProfile();
        Admin admin = getAdminProfile();
        Notification n = new Notification();
        n.setId(901L);
        n.setRecipient(staff);
        n.setSender(admin);
        n.setNotificationType("DESK");
        n.setSubject("Daily Shift Handover");
        n.setPreview("All 12 scheduled arrivals verified for morning check-in.");
        return new ArrayList<>(List.of(n));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Notification> getStaffNotifications(HttpSession session) {
        if (session == null) return createInitialStaffNotifications();
        List<Notification> list = (List<Notification>) session.getAttribute("DEMO_STAFF_NOTIFICATIONS");
        if (list == null) {
            list = createInitialStaffNotifications();
            session.setAttribute("DEMO_STAFF_NOTIFICATIONS", list);
        }
        return list;
    }

    @Override
    public void markAllDemoStaffNotificationsRead(HttpSession session) {
        if (session != null) {
            session.setAttribute("DEMO_STAFF_NOTIFICATIONS", new ArrayList<Notification>());
        }
    }

    private List<Message> createInitialStaffInbox() {
        Admin admin = getAdminProfile();
        Message m = new Message();
        m.setId(1001L);
        m.setSender(admin);
        m.setSenderRole(UserRole.ADMIN);
        m.setRequestType("ANNOUNCEMENT");
        m.setSubject("Demo broadcast from Admin");
        m.setContent("VIP guest arrival in Suite D-204. Please ensure priority check-in.");
        return new ArrayList<>(List.of(m));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Message> getStaffInboxMessages(HttpSession session) {
        if (session == null) return createInitialStaffInbox();
        List<Message> list = (List<Message>) session.getAttribute("DEMO_STAFF_INBOX");
        if (list == null) {
            list = createInitialStaffInbox();
            session.setAttribute("DEMO_STAFF_INBOX", list);
        }
        return list;
    }

    private List<Message> createInitialStaffSent() {
        Staff staff = getStaffProfile();
        Message m = new Message();
        m.setId(1002L);
        m.setSender(staff);
        m.setSenderRole(UserRole.STAFF);
        m.setRequestType("STAFF_REQUEST");
        m.setSubject("Demo request: Room 204 housekeeping required.");
        m.setContent("Demo request: Room 204 housekeeping required.");
        return new ArrayList<>(List.of(m));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Message> getStaffSentMessages(HttpSession session) {
        if (session == null) return createInitialStaffSent();
        List<Message> list = (List<Message>) session.getAttribute("DEMO_STAFF_SENT");
        if (list == null) {
            list = createInitialStaffSent();
            session.setAttribute("DEMO_STAFF_SENT", list);
        }
        return list;
    }

    @Override
    public Message getStaffMessageById(HttpSession session, Long id) {
        for (Message m : getStaffInboxMessages(session)) {
            if (id != null && id.equals(m.getId())) return m;
        }
        for (Message m : getStaffSentMessages(session)) {
            if (id != null && id.equals(m.getId())) return m;
        }
        Admin admin = getAdminProfile();
        Message m = new Message();
        m.setId(id != null ? id : 1001L);
        m.setSender(admin);
        m.setSenderRole(UserRole.ADMIN);
        m.setRequestType("STAFF_REQUEST");
        m.setSubject("Demo request: Room 204 housekeeping required.");
        m.setContent("Please ensure priority turnaround for Suite D-204.");
        return m;
    }

    @Override
    public Message sendDemoStaffRequest(HttpSession session, String subject, String content, String requestType) {
        Staff staff = getStaffProfile();
        Message m = new Message();
        m.setId(System.currentTimeMillis() % 100000);
        m.setSender(staff);
        m.setSenderRole(UserRole.STAFF);
        m.setRequestType(requestType != null ? requestType : "STAFF_REQUEST");
        m.setSubject(subject != null ? subject : "Demo staff operational request");
        m.setContent(content != null ? content : "Demo request content");

        List<Message> sent = new ArrayList<>(getStaffSentMessages(session));
        sent.add(0, m);
        if (session != null) session.setAttribute("DEMO_STAFF_SENT", sent);

        List<Message> adminInbox = new ArrayList<>(getAdminInboxMessages(session));
        adminInbox.add(0, m);
        if (session != null) session.setAttribute("DEMO_ADMIN_INBOX", adminInbox);

        return m;
    }

    // =========================================================================
    // GUEST DEMO
    // =========================================================================

    @Override
    public Guest getGuestProfile() {
        Guest g = new Guest("demo_guest", "", "Demo Guest", "demo.guest@example.com");
        g.setId(9901L);
        g.setPhoneNumber("DEMO-PHONE-GUEST");
        g.setStatus(AccountStatus.ACTIVE);
        g.setActive(true);
        return g;
    }

    @Override
    public Guest getGuestProfile(HttpSession session) {
        return getGuestProfile();
    }

    private List<Booking> createInitialActiveBookings() {
        Hotel h = hotelRepository.findAll().stream().findFirst().orElseGet(() -> createDemoHotel(101L, "Demo Palace Bengaluru", "Bengaluru Urban", "Bengaluru"));
        Guest g = getGuestProfile();
        Room r = new Room("204", RoomCategory.DELUXE, RoomStatus.OCCUPIED, 4500.0, 2, 2, "Deluxe King Room", "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
        r.setId(501L);

        Booking b = new Booking("DEMO-BK-1001", g, h, r, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), 2, 9000.0);
        b.setId(8801L);
        b.setStatus(BookingStatus.CONFIRMED);
        b.setRoomType("Deluxe Room");
        b.setBookingDate(LocalDateTime.now().minusDays(3));

        List<BookingGuestDetail> details = List.of(
                new BookingGuestDetail("Demo Guest", "Adult", 32, "Female")
        );
        b.setGuestDetails(details);
        return new ArrayList<>(List.of(b));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Booking> getGuestActiveBookings(HttpSession session) {
        if (session == null) return createInitialActiveBookings();
        List<Booking> list = (List<Booking>) session.getAttribute("DEMO_BOOKINGS_ACTIVE");
        if (list == null) {
            list = createInitialActiveBookings();
            session.setAttribute("DEMO_BOOKINGS_ACTIVE", list);
        }
        return list;
    }

    private List<Booking> createInitialPastBookings() {
        Hotel h = hotelRepository.findAll().stream().skip(1).findFirst().orElseGet(() -> createDemoHotel(102L, "Demo Heritage Mysuru", "Mysuru", "Mysuru"));
        Guest g = getGuestProfile();
        Room r = new Room("102", RoomCategory.SUITE, RoomStatus.AVAILABLE, 8500.0, 1, 4, "Heritage Suite", "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80");
        r.setId(502L);

        Booking b = new Booking("DEMO-BK-0982", g, h, r, LocalDate.now().minusDays(60), LocalDate.now().minusDays(57), 2, 25500.0);
        b.setId(8802L);
        b.setStatus(BookingStatus.COMPLETED);
        b.setRoomType("Heritage Suite");
        b.setBookingDate(LocalDateTime.now().minusDays(65));
        return new ArrayList<>(List.of(b));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Booking> getGuestPastBookings(HttpSession session) {
        if (session == null) return createInitialPastBookings();
        List<Booking> list = (List<Booking>) session.getAttribute("DEMO_BOOKINGS_PAST");
        if (list == null) {
            list = createInitialPastBookings();
            session.setAttribute("DEMO_BOOKINGS_PAST", list);
        }
        return list;
    }

    @Override
    public Booking createDemoBooking(HttpSession session, BookingRequestDto requestDto) {
        if (requestDto == null) return null;
        List<Booking> list = new ArrayList<>(getGuestActiveBookings(session));
        long nextId = list.stream().mapToLong(b -> b.getId() != null ? b.getId() : 0).max().orElse(8800) + 1;
        String ref = "DEMO-BK-" + nextId;

        Hotel h = requestDto.getHotelId() != null ?
                hotelRepository.findById(requestDto.getHotelId()).orElseGet(() -> createDemoHotel(requestDto.getHotelId(), "Demo Palace Bengaluru", "Bengaluru Urban", "Bengaluru")) :
                createDemoHotel(101L, "Demo Palace Bengaluru", "Bengaluru Urban", "Bengaluru");

        Guest g = getGuestProfile();
        Room r = new Room(requestDto.getRoomId() != null ? String.valueOf(requestDto.getRoomId()) : "101", RoomCategory.DELUXE, RoomStatus.OCCUPIED, 4500.0, 1, 2, "Deluxe Room", "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
        r.setId(requestDto.getRoomId() != null ? requestDto.getRoomId() : 501L);

        LocalDate checkIn = requestDto.getCheckInDate() != null ? requestDto.getCheckInDate() : LocalDate.now().plusDays(1);
        LocalDate checkOut = requestDto.getCheckOutDate() != null ? requestDto.getCheckOutDate() : LocalDate.now().plusDays(3);
        int guests = requestDto.getNumberOfGuests() > 0 ? requestDto.getNumberOfGuests() : 2;
        long nights = (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) ? java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut) : 2;
        double amount = nights * 4500.0;

        Booking b = new Booking(ref, g, h, r, checkIn, checkOut, guests, amount);
        b.setId(nextId);
        b.setStatus(BookingStatus.CONFIRMED);
        b.setRoomType(requestDto.getRoomType() != null ? requestDto.getRoomType() : "Deluxe Room");
        b.setBookingDate(LocalDateTime.now());
        if (requestDto.getGuestDetails() != null) {
            b.setGuestDetails(new ArrayList<>(requestDto.getGuestDetails()));
        }

        list.add(0, b);
        if (session != null) session.setAttribute("DEMO_BOOKINGS_ACTIVE", list);
        return b;
    }

    @Override
    public boolean cancelDemoBooking(HttpSession session, Long bookingId) {
        if (bookingId == null) return false;
        List<Booking> list = new ArrayList<>(getGuestActiveBookings(session));
        for (Booking b : list) {
            if (bookingId.equals(b.getId())) {
                b.setStatus(BookingStatus.CANCELLED);
                if (session != null) session.setAttribute("DEMO_BOOKINGS_ACTIVE", list);
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // PLANNED LIST SANDBOX
    // =========================================================================

    @Override
    @SuppressWarnings("unchecked")
    public List<PlannedHotel> getGuestPlannedHotels(HttpSession session) {
        if (session == null) return getDefaultDemoPlannedHotels();
        List<PlannedHotel> list = (List<PlannedHotel>) session.getAttribute("DEMO_PLANNED_LIST");
        if (list == null) {
            list = getDefaultDemoPlannedHotels();
            session.setAttribute("DEMO_PLANNED_LIST", list);
        }
        return list;
    }

    private List<PlannedHotel> getDefaultDemoPlannedHotels() {
        List<Hotel> all = hotelRepository.findAll();
        Guest demoGuest = getGuestProfile();
        List<PlannedHotel> list = new ArrayList<>();

        if (!all.isEmpty()) {
            Hotel h1 = all.get(0);
            PlannedHotel p1 = new PlannedHotel(demoGuest, h1, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
            p1.setId(9901L);
            list.add(p1);

            if (all.size() > 1) {
                Hotel h2 = all.get(1);
                PlannedHotel p2 = new PlannedHotel(demoGuest, h2, LocalDate.now().plusDays(15), LocalDate.now().plusDays(18));
                p2.setId(9902L);
                list.add(p2);
            }
        }
        return list;
    }

    @Override
    public Set<Long> getGuestPlannedHotelIds(HttpSession session) {
        return getGuestPlannedHotels(session).stream()
                .map(p -> p.getHotel() != null ? p.getHotel().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public long getGuestPlannedCount(HttpSession session) {
        return getGuestPlannedHotels(session).size();
    }

    @Override
    public boolean addGuestPlannedHotel(HttpSession session, Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        if (hotelId == null) return false;
        List<PlannedHotel> list = new ArrayList<>(getGuestPlannedHotels(session));
        for (PlannedHotel p : list) {
            if (p.getHotel() != null && hotelId.equals(p.getHotel().getId())) {
                p.setPlannedCheckInDate(checkIn);
                p.setPlannedCheckOutDate(checkOut);
                if (session != null) session.setAttribute("DEMO_PLANNED_LIST", list);
                return false;
            }
        }
        hotelRepository.findById(hotelId).ifPresent(h -> {
            PlannedHotel p = new PlannedHotel(getGuestProfile(), h, checkIn, checkOut);
            p.setId((long) (list.size() + 1000));
            list.add(p);
            if (session != null) session.setAttribute("DEMO_PLANNED_LIST", list);
        });
        return true;
    }

    @Override
    public boolean updateGuestPlannedDates(HttpSession session, Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        if (hotelId == null) return false;
        List<PlannedHotel> list = new ArrayList<>(getGuestPlannedHotels(session));
        for (PlannedHotel p : list) {
            if (p.getHotel() != null && hotelId.equals(p.getHotel().getId())) {
                p.setPlannedCheckInDate(checkIn);
                p.setPlannedCheckOutDate(checkOut);
                if (session != null) session.setAttribute("DEMO_PLANNED_LIST", list);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeGuestPlannedHotel(HttpSession session, Long hotelId) {
        if (hotelId == null) return false;
        List<PlannedHotel> list = new ArrayList<>(getGuestPlannedHotels(session));
        boolean removed = list.removeIf(p -> p.getHotel() != null && hotelId.equals(p.getHotel().getId()));
        if (session != null) session.setAttribute("DEMO_PLANNED_LIST", list);
        return removed;
    }

    @Override
    public boolean toggleGuestPlannedHotel(HttpSession session, Long hotelId) {
        if (hotelId == null) return false;
        Set<Long> ids = getGuestPlannedHotelIds(session);
        if (ids.contains(hotelId)) {
            removeGuestPlannedHotel(session, hotelId);
            return false;
        } else {
            addGuestPlannedHotel(session, hotelId, null, null);
            return true;
        }
    }
}
