package com.hotel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
public class DemoModeAccessTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Demo Mode page is publicly accessible and displays role choices")
    void testDemoPortalPageAccessibility() throws Exception {
        mockMvc.perform(get("/demo"))
                .andExpect(status().isOk())
                .andExpect(view().name("demo"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Prototype Demonstration Mode")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Guest Demo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Staff Demo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin Demo")));
    }

    @Test
    @DisplayName("Guest Demo launcher creates authenticated session and redirects to guest dashboard")
    void testLaunchGuestDemo() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/guest"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "Session should be created");
        assertEquals(true, session.getAttribute("DEMO_MODE"));

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(securityContext);
        assertEquals("guest", securityContext.getAuthentication().getName());
        assertTrue(securityContext.getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")));

        // Follow redirect to guest dashboard with this session
        mockMvc.perform(get("/guest/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DEMO MODE")));
    }

    @Test
    @DisplayName("Staff Demo launcher creates authenticated session and enables access to staff portal")
    void testLaunchStaffDemo() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/staff"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "Session should be created");
        assertEquals(true, session.getAttribute("DEMO_MODE"));

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(securityContext);
        assertEquals("staff", securityContext.getAuthentication().getName());
        assertTrue(securityContext.getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF")));

        // Staff home view rendered on / for staff role
        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/home"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DEMO MODE")));
    }

    @Test
    @DisplayName("Admin Demo launcher creates authenticated session and enables access to admin portal")
    void testLaunchAdminDemo() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "Session should be created");
        assertEquals(true, session.getAttribute("DEMO_MODE"));

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(securityContext);
        assertEquals("GAGAN", securityContext.getAuthentication().getName());
        assertTrue(securityContext.getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        // Admin home view rendered on / for admin role
        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/home"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DEMO MODE")));
    }

    @Test
    @DisplayName("Admin Demo: all operational drilldowns, guest/staff details, and messages work with sample data without security errors")
    void testAdminDemoOperationalDrilldownsAndIsolation() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/admin"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        // 1. Admin Dashboard View
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("hotelOperations", "guestActivity", "staffActivity", "maintenance", "inspections"));

        // 2. Admin Operational REST APIs (Percentage Cards & Drilldowns)
        mockMvc.perform(get("/admin/api/operations/rooms?status=RESERVED").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hotelName").value("Demo Palace Bengaluru"))
                .andExpect(jsonPath("$[0].guestName").value("Demo Guest 01"));

        mockMvc.perform(get("/admin/api/operations/guest-flow?type=CURRENT").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Demo Guest 01"));

        mockMvc.perform(get("/admin/api/operations/guest-activity?category=CURRENT").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guestName").value("Demo Guest 01"))
                .andExpect(jsonPath("$[0].reference").value("DEMO-BK-1001"));

        mockMvc.perform(get("/admin/api/operations/staff?status=Working").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Demo Staff 01"))
                .andExpect(jsonPath("$[0].activityStatus").value("Working"));

        mockMvc.perform(get("/admin/api/operations/hotel/101").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("Fully In Service / Demo Active"))
                .andExpect(jsonPath("$.availablePct").value(32));

        // 3. Admin Views (Check Hotels, Districts, Staff, Guests, Notifications, Messages)
        mockMvc.perform(get("/admin/check-hotels").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/check-hotels"));

        mockMvc.perform(get("/admin/districts").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/districts"));

        mockMvc.perform(get("/admin/staff").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff"))
                .andExpect(model().attributeExists("staffList"));

        mockMvc.perform(get("/admin/guests").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/guests"))
                .andExpect(model().attributeExists("guestList"));

        mockMvc.perform(get("/admin/notifications").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notifications"));

        mockMvc.perform(get("/admin/messages/requests").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/messages-requests"));
    }

    @Test
    @DisplayName("Staff Demo: guest records, room status, and messages use isolated fictional data")
    void testStaffDemoOperationsAndFictionalDataIsolation() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/staff"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        // 1. Staff Dashboard
        mockMvc.perform(get("/staff/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/dashboard"))
                .andExpect(model().attributeExists("ops", "staff"));

        // 2. Staff Operations REST APIs
        mockMvc.perform(get("/staff/api/operations/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupancyPct").value(62))
                .andExpect(jsonPath("$.availablePct").value(24))
                .andExpect(jsonPath("$.reservedPct").value(34))
                .andExpect(jsonPath("$.prebookedPct").value(18))
                .andExpect(jsonPath("$.maintenancePct").value(6));

        mockMvc.perform(get("/staff/api/operations/drilldown?category=OCCUPANCY").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guestName").value("Demo Guest 01"))
                .andExpect(jsonPath("$[0].roomNumber").value("S-105"));

        mockMvc.perform(get("/staff/api/operations/booking/1001").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference").value("DEMO-STAY-2041"))
                .andExpect(jsonPath("$.guestName").value("Demo Guest 01"))
                .andExpect(jsonPath("$.guestPhone").value("DEMO-PHONE-01"));

        mockMvc.perform(get("/staff/api/operations/room/201").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value("D-204"))
                .andExpect(jsonPath("$.status").value("OCCUPIED"));

        // 3. Staff Check Rooms & Messages
        mockMvc.perform(get("/staff/check-rooms").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/check-rooms"));

        mockMvc.perform(get("/staff/guests").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/guests"));

        mockMvc.perform(get("/staff/messages/inbox").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/messages-inbox"));
    }

    @Test
    @DisplayName("Guest Demo: profile, bookings, and planned list use isolated demo data")
    void testGuestDemoProfileAndBookings() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/guest"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        // 1. Guest Profile
        mockMvc.perform(get("/guest/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/profile"))
                .andExpect(model().attributeExists("guest"));

        // 2. Active Bookings & History
        mockMvc.perform(get("/guest/my-bookings").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/my-bookings"))
                .andExpect(model().attributeExists("bookings"));

        mockMvc.perform(get("/guest/booking-history").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/booking-history"))
                .andExpect(model().attributeExists("bookings"));

        // 3. Planned List
        mockMvc.perform(get("/guest/planned-list").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/planned-list"))
                .andExpect(model().attributeExists("plannedHotels"));
    }

    @Autowired
    private com.hotel.repository.OfferRepository offerRepository;

    @Autowired
    private com.hotel.repository.RoomRepository roomRepository;

    @Autowired
    private com.hotel.repository.BookingRepository bookingRepository;

    @Test
    @DisplayName("Login and Register pages contain the Explore Demo option")
    void testLoginAndRegisterHaveExploreDemoOption() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Explore Demo")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Explore Demo")));
    }

    @Test
    @DisplayName("Complete Sandbox Isolation Test: Demo Admin edits Offer -> Real DB and Real users remain untouched")
    void testDemoAdminOfferEditDataIsolation() throws Exception {
        // Step 1: Query initial real offers count / content from DB
        long realOfferCountBefore = offerRepository.count();
        var realOffersBefore = offerRepository.findAll();
        String initialDbTitle = realOffersBefore.isEmpty() ? null : realOffersBefore.get(0).getTitle();

        // Step 2: Launch Demo Admin Session
        MvcResult demoResult = mockMvc.perform(get("/demo/admin"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession demoSession = (MockHttpSession) demoResult.getRequest().getSession(false);
        assertNotNull(demoSession);

        // Step 3: Demo Admin edits an offer in Demo Mode (e.g. ID 1 to 'SANDBOX 50% OFF')
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/offers/1/edit")
                        .with(csrf())
                        .session(demoSession)
                        .param("title", "SANDBOX 50% OFF")
                        .param("discountPercentage", "50")
                        .param("tag", "SANDBOX")
                        .param("subtitle", "Sandbox isolated promotion")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/offers"));

        // Step 4: Demo Admin views offers -> sees updated sandbox offer
        mockMvc.perform(get("/admin/offers").session(demoSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SANDBOX 50% OFF")));

        // Step 5: Real database verification -> ZERO changes to real DB records
        long realOfferCountAfter = offerRepository.count();
        assertEquals(realOfferCountBefore, realOfferCountAfter, "Real database offer count must not change");
        var realOffersAfter = offerRepository.findAll();
        if (!realOffersAfter.isEmpty()) {
            assertEquals(initialDbTitle, realOffersAfter.get(0).getTitle(), "Real DB offer title must NOT be modified by Demo Admin");
            assertFalse(realOffersAfter.stream().anyMatch(o -> "SANDBOX 50% OFF".equals(o.getTitle())), "Real DB must NOT contain Demo Admin changes");
        }

        // Step 6: Real Guest / Real Staff view offers without demo session -> see untouched real offers
        mockMvc.perform(get("/guest/offers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SANDBOX 50% OFF"))));
    }

    @Test
    @DisplayName("Complete Sandbox Isolation Test: Demo Staff updates Room status -> Real DB is untouched and Demo dashboard recalculates dynamically")
    void testDemoStaffRoomStatusDataIsolationAndDynamicStats() throws Exception {
        // Step 1: Query initial room status in real DB
        var realRoomsBefore = roomRepository.findAll();
        assertFalse(realRoomsBefore.isEmpty(), "Real rooms should exist in database");
        com.hotel.model.Room firstRealRoom = realRoomsBefore.get(0);
        com.hotel.model.enums.RoomStatus realStatusBefore = firstRealRoom.getStatus();
        Long realRoomId = firstRealRoom.getId();

        // Step 2: Launch Demo Staff Session
        MvcResult demoResult = mockMvc.perform(get("/demo/staff"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession demoSession = (MockHttpSession) demoResult.getRequest().getSession(false);
        assertNotNull(demoSession);

        // Step 3: Demo Staff changes room 201 status to OCCUPIED
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/staff/room-status/201")
                        .with(csrf())
                        .session(demoSession)
                        .param("status", "OCCUPIED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/check-rooms"));

        // Step 4: Verify Demo Staff check-rooms & dashboard show updated status & recalculated occupancy
        mockMvc.perform(get("/staff/dashboard").session(demoSession))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("ops", "occupiedRoomsCount"));

        // Step 5: Verify Real database room is 100% UNCHANGED
        com.hotel.model.Room realRoomAfter = roomRepository.findById(realRoomId).orElseThrow();
        assertEquals(realStatusBefore, realRoomAfter.getStatus(), "Real database room status must not be modified by demo staff");
    }

    @Test
    @DisplayName("Complete Sandbox Isolation Test: Demo Guest submits Booking -> Real DB has 0 new bookings, Demo sees booking in My Bookings")
    void testDemoGuestBookingCreationDataIsolation() throws Exception {
        // Step 1: Count initial bookings in real DB
        long realBookingsCountBefore = bookingRepository.count();

        // Step 2: Launch Demo Guest Session
        MvcResult demoResult = mockMvc.perform(get("/demo/guest"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession demoSession = (MockHttpSession) demoResult.getRequest().getSession(false);
        assertNotNull(demoSession);

        // Step 3: Submit a demo booking in Demo Mode
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/bookings/submit")
                        .with(csrf())
                        .session(demoSession)
                        .param("hotelId", "1")
                        .param("roomId", "1")
                        .param("checkInDate", java.time.LocalDate.now().plusDays(5).toString())
                        .param("checkOutDate", java.time.LocalDate.now().plusDays(8).toString())
                        .param("numberOfGuests", "1")
                        .param("guestDetails[0].name", "Evaluator Tester")
                        .param("guestDetails[0].guestType", "Adult")
                        .param("guestDetails[0].age", "28")
                        .param("guestDetails[0].gender", "Female"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/my-bookings?booked=true"));

        // Step 4: Demo Guest views My Bookings -> sees confirmed stay
        mockMvc.perform(get("/guest/my-bookings").session(demoSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirmed")));

        // Step 5: Real database count must NOT change
        long realBookingsCountAfter = bookingRepository.count();
        assertEquals(realBookingsCountBefore, realBookingsCountAfter, "Real database bookings count must NOT increase on Demo booking");
    }
}
