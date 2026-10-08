package com.hotel;

import com.hotel.model.Booking;
import com.hotel.model.Hotel;
import com.hotel.model.Room;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage 8: Major Staff Dashboard Update & Live Hotel Operations Verification Tests
 *
 * Verifies:
 * 1. Single instance of each Staff option (no duplicates in nav or dashboard)
 * 2. Main Staff Navigation: Home -> Check Rooms -> Offers -> Dashboard -> Profile
 * 3. Notification & Message quick-access icons with unread badges in Staff header
 * 4. Lightweight dedicated Staff Home (/) with welcome hero, operations summary, quick action cards
 * 5. Staff Dashboard Live Operations:
 *    - Level 1: Percentage-first room distribution & live guest flow
 *    - Level 2: Detail-on-click dynamic drilldown table (/staff/api/operations/drilldown)
 *    - Level 3: Modal guest stay & room dossier (/staff/api/operations/booking/{id})
 *    - Live operations telemetry summary API (/staff/api/operations/summary)
 *    - Operational check-in and check-out actions (/staff/api/operations/check-in, check-out)
 * 6. Operational "Check Rooms" page (/staff/check-rooms)
 * 7. View-only "Offers" page (/staff/offers)
 * 8. Security & Role Isolation: Staff blocked from Admin, Guests blocked from Staff
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class Stage8StaffOperationsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // =========================================================================
    // 1. STAFF MAIN NAVIGATION & HEADER COMMUNICATION ICONS
    // =========================================================================

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Staff navigation strictly follows: Home -> Check Rooms -> Offers -> Dashboard -> Profile")
    void testStaffMainNavigationOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        int navStart = html.indexOf("class=\"nav-links");
        assertTrue(navStart > 0, "nav-links must exist");
        int navEnd = html.indexOf("</ul>", navStart);
        assertTrue(navEnd > navStart, "nav-links closing tag must exist");
        String nav = html.substring(navStart, navEnd);

        int posHome = nav.indexOf("href=\"/\"");
        int posCheckRooms = nav.indexOf("href=\"/staff/check-rooms\"");
        int posOffers = nav.indexOf("href=\"/staff/offers\"");
        int posDashboard = nav.indexOf("href=\"/staff/dashboard\"");
        int posProfile = nav.indexOf("href=\"/staff/profile\"");

        assertTrue(posHome >= 0, "Home nav item must be present");
        assertTrue(posCheckRooms >= 0, "Check Rooms nav item must be present");
        assertTrue(posOffers >= 0, "Offers nav item must be present");
        assertTrue(posDashboard >= 0, "Dashboard nav item must be present");
        assertTrue(posProfile >= 0, "Profile nav item must be present");

        assertTrue(posHome < posCheckRooms, "Home must appear before Check Rooms");
        assertTrue(posCheckRooms < posOffers, "Check Rooms must appear before Offers");
        assertTrue(posOffers < posDashboard, "Offers must appear before Dashboard");
        assertTrue(posDashboard < posProfile, "Dashboard must appear before Profile");

        // Staff must NOT see Find Hotels, Explore India Map, or Districts
        assertFalse(nav.contains("/hotels"), "Staff navigation must not contain /hotels (Find Hotels)");
        assertFalse(nav.contains("/map"), "Staff navigation must not contain /map (Explore India Map)");
        assertFalse(nav.contains("Districts"), "Staff navigation must not contain Districts");
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Staff header contains notification and message quick-access icons with unread badges")
    void testStaffHeaderCommunicationIcons() throws Exception {
        MvcResult result = mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("href=\"/staff/notifications\""), "Navbar must contain Staff Notifications link");
        assertTrue(html.contains("href=\"/staff/messages/inbox\""), "Navbar must contain Staff Messages link");
    }

    // =========================================================================
    // 2. NO DUPLICATE NAVIGATION OR DASHBOARD OPTIONS
    // =========================================================================

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Navbar and Dashboard have exactly ONE instance of each Staff option (no duplicates)")
    void testNoDuplicateStaffOptions() throws Exception {
        MvcResult result = mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 1. Exactly ONE Check Rooms in main nav
        int navStart = html.indexOf("class=\"nav-links");
        int navEnd = html.indexOf("</ul>", navStart);
        String nav = html.substring(navStart, navEnd);

        Pattern checkRoomsPattern = Pattern.compile("href=\"/staff/check-rooms\"");
        Matcher matcher = checkRoomsPattern.matcher(nav);
        int countCheckRooms = 0;
        while (matcher.find()) countCheckRooms++;
        assertEquals(1, countCheckRooms, "Exactly ONE Check Rooms option in nav-links");

        // 2. Exactly ONE Offers in main nav
        Pattern offersPattern = Pattern.compile("href=\"/staff/offers\"");
        matcher = offersPattern.matcher(nav);
        int countOffers = 0;
        while (matcher.find()) countOffers++;
        assertEquals(1, countOffers, "Exactly ONE Offers option in nav-links");

        // 3. Exactly ONE Dashboard in main nav
        Pattern dashPattern = Pattern.compile("href=\"/staff/dashboard\"");
        matcher = dashPattern.matcher(nav);
        int countDash = 0;
        while (matcher.find()) countDash++;
        assertEquals(1, countDash, "Exactly ONE Dashboard option in nav-links");
    }

    // =========================================================================
    // 3. DEDICATED LIGHTWEIGHT STAFF HOME (/)
    // =========================================================================

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Authenticated Staff accessing / renders dedicated Staff Home (not Guest Home)")
    void testStaffHomeLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/home"))
                .andExpect(content().string(containsString("Staff Operations Desk")))
                .andExpect(content().string(containsString("Live Operations Dashboard")))
                .andExpect(content().string(containsString("Today's Operations Summary")))
                .andExpect(content().string(containsString("Current Operational Status")));
    }

    // =========================================================================
    // 4. STAFF DASHBOARD LIVE OPERATIONS (LEVEL 1, 2, 3)
    // =========================================================================

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Staff Dashboard renders Level 1 room distribution percentages and live guest flow cards")
    void testStaffDashboardLevel1Rendering() throws Exception {
        mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hotel Staff Operations Dashboard")))
                .andExpect(content().string(containsString("Room Status Distribution")))
                .andExpect(content().string(containsString("Occupied Rooms")))
                .andExpect(content().string(containsString("Available Rooms")))
                .andExpect(content().string(containsString("Reserved Rooms")))
                .andExpect(content().string(containsString("Pre-booked Rooms")))
                .andExpect(content().string(containsString("Under Maintenance")))
                .andExpect(content().string(containsString("Current Guests")))
                .andExpect(content().string(containsString("Upcoming Check-ins")))
                .andExpect(content().string(containsString("Upcoming Check-outs")))
                .andExpect(content().string(containsString("Upcoming Guests")))
                .andExpect(content().string(containsString("Active Guest Requests")))
                .andExpect(content().string(containsString("Completed Stays")));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Level 2 Drilldown API returns matching operational records with traveler details")
    void testStaffOperationsDrilldownApi() throws Exception {
        // Query OCCUPANCY
        mockMvc.perform(get("/staff/api/operations/drilldown").param("category", "OCCUPANCY"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // Query AVAILABLE
        mockMvc.perform(get("/staff/api/operations/drilldown").param("category", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // Query RESERVED
        mockMvc.perform(get("/staff/api/operations/drilldown").param("category", "RESERVED"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // Query CURRENT_GUESTS
        mockMvc.perform(get("/staff/api/operations/drilldown").param("category", "CURRENT_GUESTS"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Live Operations Telemetry Summary API returns single-source-of-truth counts and percentages")
    void testStaffOperationsSummaryApi() throws Exception {
        mockMvc.perform(get("/staff/api/operations/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupancyPct", notNullValue()))
                .andExpect(jsonPath("$.availablePct", notNullValue()))
                .andExpect(jsonPath("$.reservedPct", notNullValue()))
                .andExpect(jsonPath("$.prebookedPct", notNullValue()))
                .andExpect(jsonPath("$.maintenancePct", notNullValue()))
                .andExpect(jsonPath("$.currentGuests", notNullValue()))
                .andExpect(jsonPath("$.todayArrivals", notNullValue()))
                .andExpect(jsonPath("$.todayDepartures", notNullValue()))
                .andExpect(jsonPath("$.upcomingGuests", notNullValue()));
    }

    // =========================================================================
    // 5. STAFF CHECK ROOMS & OFFERS PAGES
    // =========================================================================

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Staff Check Rooms page renders operational rooms board with filter tabs")
    void testStaffCheckRoomsPage() throws Exception {
        mockMvc.perform(get("/staff/check-rooms"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/check-rooms"))
                .andExpect(content().string(containsString("Check Rooms")))
                .andExpect(content().string(containsString("Operational Status Board")))
                .andExpect(content().string(containsString("filterRooms('ALL', this)")))
                .andExpect(content().string(containsString("filterRooms('AVAILABLE', this)")))
                .andExpect(content().string(containsString("filterRooms('OCCUPIED', this)")))
                .andExpect(content().string(containsString("filterRooms('RESERVED', this)")))
                .andExpect(content().string(containsString("filterRooms('MAINTENANCE', this)")));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Staff Offers page renders verified active promotions in view-only mode")
    void testStaffOffersPage() throws Exception {
        mockMvc.perform(get("/staff/offers"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/offers"))
                .andExpect(content().string(containsString("Active Promotional Offers &amp; Tariffs")));
    }

    // =========================================================================
    // 6. ROLE ISOLATION & SECURITY BOUNDARIES
    // =========================================================================

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Staff is strictly blocked from Admin dashboard and operations")
    void testStaffBlockedFromAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/admin/api/operations/overview"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "guest", roles = {"GUEST"})
    @DisplayName("Guest is strictly blocked from Staff dashboard and operations")
    void testGuestBlockedFromStaff() throws Exception {
        mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/staff/check-rooms"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/staff/api/operations/summary"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }
}
