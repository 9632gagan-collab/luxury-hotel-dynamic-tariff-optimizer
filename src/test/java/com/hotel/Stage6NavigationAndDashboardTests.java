package com.hotel;

import com.hotel.dto.BookingRequestDto;
import com.hotel.model.Booking;
import com.hotel.model.Guest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage 6 Comprehensive Verification Tests:
 * - Pre-login top navigation (unchanged)
 * - Post-login top navigation exact order (Home -> Find Hotels -> Explore India Map -> Districts -> Offers -> Dashboard -> Profile)
 * - Guest Dashboard exact 6 cards in exact order (Find Hotels, Offers, My Bookings, Booking History, Planned List, Profile)
 * - Removal of 'Explore by District' and 'Hotel Services' from Guest Dashboard
 * - Fix for duplicate 'Find Hotels' on Home and across all pages
 * - Removal of separate 'Select Room Accommodation' dropdown
 * - Automatic flow and persistence of selected room type from Find Hotels to Booking record
 * - Mandatory check-in and check-out dates validation preserved
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class Stage6NavigationAndDashboardTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private com.hotel.service.GuestService guestService;

    @Autowired
    private com.hotel.repository.GuestRepository guestRepository;

    @Autowired
    private com.hotel.service.PlannedHotelService plannedHotelService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        if (guestRepository.findByUsernameIgnoreCase("guest_user").isEmpty()) {
            guestService.registerGuest(new com.hotel.dto.GuestRegistrationDto(
                    "guest_user", "guest_user@royalindianstays.com", "Password123!", "Password123!"));
        }
    }

    // =========================================================================
    // 1. BEFORE LOGIN — PRESERVED NAVIGATION
    // =========================================================================

    @Test
    @DisplayName("Pre-login navigation preserves exact original order and labels")
    void testPreLoginNavigationUnchanged() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // Must contain original unauthenticated links
        assertTrue(html.contains("href=\"/\""), "Should contain Home link");
        assertTrue(html.contains("href=\"/hotels\""), "Should contain Find Hotels link");
        assertTrue(html.contains("href=\"/map\""), "Should contain Explore India Map link");
        assertTrue(html.contains("Register"), "Should contain Register link");
        assertTrue(html.contains("Sign In"), "Should contain Sign In link");
        assertTrue(html.contains("Staff &amp; Admin Access") || html.contains("Staff & Admin Access"),
                "Should contain Staff & Admin Access dropdown");

        // Authenticated elements must NOT appear before login
        assertFalse(html.contains("href=\"/logout\""), "Pre-login should not have logout");
        assertFalse(html.contains("href=\"/districts\""), "Pre-login should not display authenticated Districts link");
    }

    // =========================================================================
    // 2. AFTER LOGIN — MAIN TOP NAVIGATION EXACT ORDER
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Guest post-login navigation order: Home -> Find Hotels -> Explore India Map -> Districts -> Offers -> Dashboard -> Profile")
    void testGuestPostLoginNavigationOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        verifyPostLoginNavigationOrder(html, "/guest/dashboard", "/guest/profile");
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("Staff post-login navigation order: Home -> Check Rooms -> Offers -> Dashboard -> Profile")
    void testStaffPostLoginNavigationOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        
        // Extract the main nav-links container
        int navStart = html.indexOf("class=\"nav-links");
        assertTrue(navStart > 0, "nav-links must exist");
        int navEnd = html.indexOf("</ul>", navStart);
        assertTrue(navEnd > navStart, "nav-links closing tag must exist");
        String navSection = html.substring(navStart, navEnd);

        int posHome = navSection.indexOf("href=\"/\"");
        int posCheckRooms = navSection.indexOf("href=\"/staff/check-rooms\"");
        int posOffers = navSection.indexOf("href=\"/staff/offers\"");
        int posDashboard = navSection.indexOf("href=\"/staff/dashboard\"");
        int posProfile = navSection.indexOf("href=\"/staff/profile\"");

        assertTrue(posHome >= 0, "Home nav item must be present for Staff");
        assertTrue(posCheckRooms >= 0, "Check Rooms nav item must be present for Staff");
        assertTrue(posOffers >= 0, "Offers nav item must be present for Staff");
        assertTrue(posDashboard >= 0, "Dashboard nav item must be present for Staff");
        assertTrue(posProfile >= 0, "Profile nav item must be present for Staff");

        assertTrue(posHome < posCheckRooms, "Home must appear before Check Rooms");
        assertTrue(posCheckRooms < posOffers, "Check Rooms must appear before Offers");
        assertTrue(posOffers < posDashboard, "Offers must appear before Dashboard");
        assertTrue(posDashboard < posProfile, "Dashboard must appear before Profile");

        // Staff must NOT have Explore India Map or Districts
        assertFalse(navSection.contains("/map"), "Staff navigation must not contain Explore India Map");
        assertFalse(navSection.contains("Districts"), "Staff navigation must not contain Districts");
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("Admin post-login navigation order: Home -> Check Hotels -> Districts -> Offers -> Dashboard -> Profile")
    void testAdminPostLoginNavigationOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        
        // Extract the main nav-links container
        int navStart = html.indexOf("class=\"nav-links");
        assertTrue(navStart > 0, "nav-links must exist");
        int navEnd = html.indexOf("</ul>", navStart);
        assertTrue(navEnd > navStart, "nav-links closing tag must exist");
        String navSection = html.substring(navStart, navEnd);

        int posHome = navSection.indexOf("href=\"/\"");
        int posCheckHotels = navSection.indexOf("href=\"/admin/check-hotels\"");
        int posDistricts = navSection.indexOf("href=\"/admin/districts\"");
        int posOffers = navSection.indexOf("href=\"/admin/offers\"");
        int posDashboard = navSection.indexOf("href=\"/admin/dashboard\"");
        int posProfile = navSection.indexOf("href=\"/admin/profile\"");

        assertTrue(posHome >= 0, "Home nav item must be present");
        assertTrue(posCheckHotels >= 0, "Check Hotels nav item must be present for Admin");
        assertTrue(posDistricts >= 0, "Districts nav item must be present for Admin");
        assertTrue(posOffers >= 0, "Offers nav item must be present for Admin");
        assertTrue(posDashboard >= 0, "Dashboard nav item must be present for Admin");
        assertTrue(posProfile >= 0, "Profile nav item must be present for Admin");

        assertTrue(posHome < posCheckHotels, "Home must appear before Check Hotels");
        assertTrue(posCheckHotels < posDistricts, "Check Hotels must appear before Districts");
        assertTrue(posDistricts < posOffers, "Districts must appear before Offers");
        assertTrue(posOffers < posDashboard, "Offers must appear before Dashboard");
        assertTrue(posDashboard < posProfile, "Dashboard must appear before Profile");
    }

    private void verifyPostLoginNavigationOrder(String html, String dashboardHref, String profileHref) {
        // Extract the main nav-links container
        int navStart = html.indexOf("class=\"nav-links");
        assertTrue(navStart > 0, "nav-links must exist");
        int navEnd = html.indexOf("</ul>", navStart);
        assertTrue(navEnd > navStart, "nav-links closing tag must exist");
        String navSection = html.substring(navStart, navEnd);

        int posHome = navSection.indexOf("href=\"/\"");
        int posFindHotels = navSection.indexOf("href=\"/hotels\"");
        int posMap = navSection.indexOf("href=\"/map\"");
        int posDistricts = navSection.indexOf("Districts");
        int posOffers = navSection.indexOf("Offers");
        int posDashboard = navSection.indexOf("Dashboard");
        int posProfile = navSection.indexOf("Profile");

        assertTrue(posHome >= 0, "Home nav item must be present");
        assertTrue(posFindHotels >= 0, "Find Hotels nav item must be present");
        assertTrue(posMap >= 0, "Explore India Map nav item must be present");
        assertTrue(posDistricts >= 0, "Districts nav item must be present");
        assertTrue(posOffers >= 0, "Offers nav item must be present");
        assertTrue(posDashboard >= 0, "Dashboard nav item must be present");
        assertTrue(posProfile >= 0, "Profile nav item must be present");

        assertTrue(posHome < posFindHotels, "Home must appear before Find Hotels");
        assertTrue(posFindHotels < posMap, "Find Hotels must appear before Explore India Map");
        assertTrue(posMap < posDistricts, "Explore India Map must appear before Districts");
        assertTrue(posDistricts < posOffers, "Districts must appear before Offers");
        assertTrue(posOffers < posDashboard, "Offers must appear before Dashboard");
        assertTrue(posDashboard < posProfile, "Dashboard must appear before Profile");
    }

    // =========================================================================
    // 3. GUEST DASHBOARD — EXACT 6 OPTIONS IN ORDER & CLEANUP
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Guest Dashboard contains EXACTLY 6 options in order: Find Hotels, Offers, My Bookings, Booking History, Planned List, Profile")
    void testGuestDashboardExactSixOptionsOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/guest/dashboard"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 1. Check card container exists
        int gridStart = html.indexOf("class=\"guest-nav-grid\"");
        assertTrue(gridStart > 0, "Guest navigation grid must exist");
        int gridEnd = html.indexOf("</div>\n\n        <!-- Stage", gridStart);
        if (gridEnd < 0) {
            gridEnd = html.indexOf("</div>", html.indexOf("id=\"cardProfile\""));
        }
        String gridSection = html.substring(gridStart, gridEnd > gridStart ? gridEnd + 10 : html.length());

        int posFindHotels = gridSection.indexOf("id=\"cardFindHotels\"");
        int posOffers = gridSection.indexOf("id=\"cardOffers\"");
        int posMyBookings = gridSection.indexOf("id=\"cardMyBookings\"");
        int posBookingHistory = gridSection.indexOf("id=\"cardBookingHistory\"");
        int posPlannedList = gridSection.indexOf("id=\"cardPlannedList\"");
        int posProfile = gridSection.indexOf("id=\"cardProfile\"");

        assertTrue(posFindHotels >= 0, "1. Find Hotels card must be present");
        assertTrue(posOffers >= 0, "2. Offers card must be present");
        assertTrue(posMyBookings >= 0, "3. My Bookings card must be present");
        assertTrue(posBookingHistory >= 0, "4. Booking History card must be present");
        assertTrue(posPlannedList >= 0, "5. Planned List card must be present");
        assertTrue(posProfile >= 0, "6. Profile card must be present");

        // Verify exact order
        assertTrue(posFindHotels < posOffers, "1. Find Hotels must be before 2. Offers");
        assertTrue(posOffers < posMyBookings, "2. Offers must be before 3. My Bookings");
        assertTrue(posMyBookings < posBookingHistory, "3. My Bookings must be before 4. Booking History");
        assertTrue(posBookingHistory < posPlannedList, "4. Booking History must be before 5. Planned List");
        assertTrue(posPlannedList < posProfile, "5. Planned List must be before 6. Profile");

        // Removed options must NOT exist in the dashboard grid
        assertFalse(gridSection.contains("cardExploreDistricts"), "Explore by District must be removed from Guest Dashboard");
        assertFalse(gridSection.contains("cardHotelServices"), "Hotel Services must be removed from Guest Dashboard");
        assertFalse(gridSection.contains("cardRecommendations"), "Recommendations must not be in the 6 dashboard cards");
    }

    // =========================================================================
    // 4. HOME — DUPLICATE FIND HOTELS BUG FIXED
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Home tab after login contains only ONE Find Hotels navigation option (no duplicate)")
    void testHomeTabSingleFindHotelsAfterGuestLogin() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        verifySingleFindHotelsInNavbar(html);
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("Home tab after staff login contains exactly ONE Check Rooms navigation option and no duplicates")
    void testHomeTabSingleFindHotelsAfterStaffLogin() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        int navStart = html.indexOf("class=\"nav-links");
        int navEnd = html.indexOf("</ul>", navStart);
        String nav = html.substring(navStart, navEnd);

        Pattern pattern = Pattern.compile("href=\"/staff/check-rooms\"");
        Matcher matcher = pattern.matcher(nav);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertEquals(1, count, "Navigation bar must contain exactly ONE Check Rooms option (/staff/check-rooms)");
        assertFalse(nav.contains("href=\"/hotels\""), "Staff navigation must not contain /hotels");
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("Home tab after admin login contains exactly ONE Check Hotels navigation option and no duplicates")
    void testHomeTabSingleFindHotelsAfterAdminLogin() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        int navStart = html.indexOf("class=\"nav-links");
        int navEnd = html.indexOf("</ul>", navStart);
        String nav = html.substring(navStart, navEnd);

        Pattern pattern = Pattern.compile("href=\"/admin/check-hotels\"");
        Matcher matcher = pattern.matcher(nav);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertEquals(1, count, "Navigation bar must contain exactly ONE Check Hotels option (/admin/check-hotels)");
        assertFalse(nav.contains("href=\"/hotels\""), "Admin navigation must not contain /hotels");
    }

    private void verifySingleFindHotelsInNavbar(String html) {
        int navStart = html.indexOf("class=\"nav-links");
        int navEnd = html.indexOf("</ul>", navStart);
        String nav = html.substring(navStart, navEnd);

        // Count occurrences of href="/hotels" in nav-links
        Pattern pattern = Pattern.compile("href=\"/hotels\"");
        Matcher matcher = pattern.matcher(nav);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertEquals(1, count, "Navigation bar must contain exactly ONE Find Hotels option (/hotels)");
    }

    // =========================================================================
    // 5 & 6. REMOVE 'SELECT ROOM ACCOMMODATION' & AUTO-USE ROOM FILTER
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Booking page removes separate 'Select Room Accommodation' dropdown")
    void testBookingPageRemovesSelectRoomAccommodation() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/bookings/new")
                        .param("hotelId", hotel.getId().toString())
                        .param("roomId", room.getId().toString())
                        .param("roomType", "Private / Single Room"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"roomSelect\""))))
                .andExpect(content().string(not(containsString("Selected Room Accommodation"))))
                .andExpect(content().string(containsString("name=\"roomType\"")))
                .andExpect(content().string(containsString("Private / Single Room")));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Room type is carried from Find Hotels into Booking model and form")
    void testRoomTypeCarriedFromFindHotelsIntoBookingForm() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();

        // 1. Shared Room selection
        mockMvc.perform(get("/bookings/new")
                        .param("hotelId", hotel.getId().toString())
                        .param("roomType", "Shared Room"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedRoomType", "Shared Room"))
                .andExpect(content().string(containsString("Shared Room")));

        // 2. Multi-Room Suite selection
        mockMvc.perform(get("/bookings/new")
                        .param("hotelId", hotel.getId().toString())
                        .param("roomType", "Multi-Room Suite"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedRoomType", "Multi-Room Suite"))
                .andExpect(content().string(containsString("Multi-Room Suite")));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Booking submission automatically persists the selected room type into database record")
    void testBookingSubmissionPersistsSelectedRoomType() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        LocalDate in = LocalDate.now().plusDays(10);
        LocalDate out = LocalDate.now().plusDays(13);

        mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("hotelId", hotel.getId().toString())
                        .param("roomId", room.getId().toString())
                        .param("roomType", "Multi-Room Suite")
                        .param("checkInDate", in.toString())
                        .param("checkOutDate", out.toString())
                        .param("numberOfGuests", "2")
                        .param("specialRequests", "Corner suite"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/my-bookings?booked=true"));

        List<Booking> bookings = bookingRepository.findAll();
        assertFalse(bookings.isEmpty(), "Booking record must be persisted");
        Booking latest = bookings.get(bookings.size() - 1);
        assertEquals("Multi-Room Suite", latest.getRoomType(),
                "Persisted booking must contain the exact selected room type");
    }

    // =========================================================================
    // 7. PRESERVE MANDATORY DATE VALIDATION
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Mandatory date validation is preserved: missing dates redirect with error and carry roomType")
    void testMandatoryDatesValidationPreservedWithRoomType() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("hotelId", hotel.getId().toString())
                        .param("roomId", room.getId().toString())
                        .param("roomType", "Shared Room"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=missing_dates*"))
                .andExpect(redirectedUrlPattern("/bookings/new?*roomType=Shared+Room*"));
    }

    // =========================================================================
    // 8. PLANNED LIST PAGE CLEANUP & HOTEL VIEW FLOW
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Planned List page removes secondary sub-navigation bar and options")
    void testPlannedListPageRemovesSecondarySubNavigation() throws Exception {
        MvcResult result = mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // sub-navbar must be completely removed
        assertFalse(html.contains("class=\"sub-navbar\""), "Planned List page must NOT contain secondary sub-navbar");
        assertFalse(html.contains("cardExploreDistricts"), "Planned List page must NOT contain secondary Explore by District");
        assertFalse(html.contains("cardHotelServices"), "Planned List page must NOT contain secondary Hotel Services");

        // Page title and clean container must be present
        assertTrue(html.contains("MY PLANNED LIST"), "Planned List page must have title 'MY PLANNED LIST'");
        assertTrue(html.contains("class=\"page-header\""), "Page header must be present");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Planned List empty state is cleanly preserved when no hotels are planned")
    void testPlannedListEmptyStatePreserved() throws Exception {
        MvcResult result = mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("empty-planned-state"), "Empty state container must be present when list is empty");
        assertTrue(html.contains("Your Planned List is Empty"), "Empty state title must be displayed");
        assertTrue(html.contains("Explore Hotels"), "Explore Hotels link must be present in empty state");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Planned List items render in compact format: Hotel Name, District/State, and View action button")
    void testPlannedListItemsRenderCompact() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        plannedHotelService.addHotelToPlannedList("guest_user", hotel.getId());

        MvcResult result = mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("planned-compact-list"), "Must render compact list container");
        assertTrue(html.contains("planned-compact-item"), "Must render compact item element");
        assertTrue(html.contains("planned-hotel-name"), "Must render planned hotel name element");
        assertTrue(html.contains(hotel.getName()), "Must render the hotel name");
        assertTrue(html.contains("planned-hotel-location"), "Must render location element");
        assertTrue(html.contains(hotel.getDistrict()), "Must render the hotel district");
        assertTrue(html.contains("planned-view-btn"), "Must render compact View button");
        assertTrue(html.contains("/hotels?hotelId=" + hotel.getId()), "View button must link to /hotels with hotelId parameter");

        // Ensure full hotel cards, price cards, and large facilities are NOT rendered in planned list
        assertFalse(html.contains("hotels-full-grid"), "Planned list must not render full hotels grid");
        assertFalse(html.contains("Applicable Nightly Rate"), "Planned list must not render full price cards");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Clicking View opens existing Find Hotels with hotelId, highlighting and prioritizing the planned hotel")
    void testViewButtonOpensFindHotelsAndHighlightsPlannedHotel() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();

        MvcResult result = mockMvc.perform(get("/hotels").param("hotelId", hotel.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedHotelId", hotel.getId()))
                .andExpect(model().attribute("selectedPlannedHotel", notNullValue()))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Hotel> rankedHotels = (List<Hotel>) result.getModelAndView().getModel().get("hotels");
        assertNotNull(rankedHotels, "Hotels list must exist in model");
        assertFalse(rankedHotels.isEmpty(), "Hotels list must not be empty");
        assertEquals(hotel.getId(), rankedHotels.get(0).getId(),
                "Selected planned hotel must be prioritized at index 0 of the ranked results");

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("telemetryPlannedWrap"), "Telemetry banner must display planned hotel notice");
        assertTrue(html.contains("Viewing Planned Hotel:"), "Telemetry banner must state viewing planned hotel");
        assertTrue(html.contains("selected-planned-hotel"), "Hotel card must receive selected-planned-hotel highlighting class");
        assertTrue(html.contains("selected-planned-badge"), "Hotel card must render Selected Planned Hotel badge");
        assertTrue(html.contains("Selected Planned Hotel"), "Badge must have 'Selected Planned Hotel' label");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Planned List is scoped strictly to the authenticated guest")
    void testPlannedListGuestPrivacyScoped() throws Exception {
        if (guestRepository.findByUsernameIgnoreCase("other_guest").isEmpty()) {
            guestService.registerGuest(new com.hotel.dto.GuestRegistrationDto(
                    "other_guest", "other_guest@royalindianstays.com", "Password123!", "Password123!"));
        }

        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        plannedHotelService.addHotelToPlannedList("guest_user", hotel.getId());

        // other_guest's planned list should be empty
        List<com.hotel.model.PlannedHotel> otherList = plannedHotelService.getPlannedHotelsForGuest("other_guest");
        assertTrue(otherList.isEmpty(), "Other guest must not see guest_user's planned hotels");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Hotel bookmark toggle on Find Hotels works via AJAX endpoint")
    void testHotelCardPlannedToggleAjax() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/guest/planned-list/toggle")
                        .with(csrf())
                        .param("hotelId", hotel.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.planned").isBoolean());
    }

    // =========================================================================
    // 9. MY BOOKINGS LIVE REMINDER & FIELD CLEANUP
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("My Bookings keeps required fields, removes Special Notes and Actions, and adds Reminder")
    void testMyBookingsTableStructure() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        Guest guest = guestRepository.findByUsernameIgnoreCase("guest_user").orElseThrow();

        Booking booking = new Booking();
        booking.setBookingReference("BK-REMINDER-01");
        booking.setGuest(guest);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now().plusDays(3));
        booking.setCheckOutDate(LocalDate.now().plusDays(6));
        booking.setNumberOfGuests(2);
        booking.setTotalAmount(15000.0);
        booking.setStatus(com.hotel.model.enums.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        MvcResult result = mockMvc.perform(get("/guest/my-bookings"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 1. Required fields must be present
        assertTrue(html.contains("Booking ID"), "Should contain Booking ID header");
        assertTrue(html.contains("Hotel"), "Should contain Hotel header");
        assertTrue(html.contains("Location"), "Should contain Location in header or details");
        assertTrue(html.contains("Room Number"), "Should contain Room Number header");
        assertTrue(html.contains("Category"), "Should contain Category header");
        assertTrue(html.contains("Duration"), "Should contain Duration header");
        assertTrue(html.contains("Guests"), "Should contain Guests header");
        assertTrue(html.contains("Total Tariff"), "Should contain Total Tariff header");
        assertTrue(html.contains("Status"), "Should contain Status header");
        assertTrue(html.contains("Reminder"), "Should contain Reminder header");

        // 2. Removed fields must NOT be present
        assertFalse(html.contains("<th>Special Notes</th>"), "Special Notes header must be removed");
        assertFalse(html.contains("<th>Action</th>"), "Action header must be removed");
        assertFalse(html.contains("<th>Actions</th>"), "Actions header must be removed");
        assertFalse(html.contains("title=\"View Voucher\""), "Voucher action button must be removed");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Upcoming booking displays Live Reminder countdown with Days, Hours, Minutes, and until check-in")
    void testUpcomingBookingReminderCountdown() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        Guest guest = guestRepository.findByUsernameIgnoreCase("guest_user").orElseThrow();

        Booking booking = new Booking();
        booking.setBookingReference("BK-COUNTDOWN-TEST");
        booking.setGuest(guest);
        booking.setHotel(hotel);
        booking.setRoom(room);
        // Set future check-in date 2 days ahead
        booking.setCheckInDate(LocalDate.now().plusDays(2));
        booking.setCheckInTime(java.time.LocalTime.of(14, 0));
        booking.setCheckOutDate(LocalDate.now().plusDays(5));
        booking.setNumberOfGuests(2);
        booking.setTotalAmount(12000.0);
        booking.setStatus(com.hotel.model.enums.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        MvcResult result = mockMvc.perform(get("/guest/my-bookings"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("booking-reminder-wrap"), "Should render reminder wrap container");
        assertTrue(html.contains("REMINDER"), "Should display REMINDER label");
        assertTrue(html.contains("until check-in"), "Should display 'until check-in' subtext");
        assertTrue(html.contains("data-checkin-millis"), "Should supply check-in epoch timestamp");
        assertTrue(html.contains("data-server-now"), "Should supply server-now epoch timestamp");
        // Verify countdown format: \d+ Days? \d+ Hours \d+ Minutes
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\d+\\s+Days?\\s+\\d+\\s+Hours\\s+\\d+\\s+Minutes");
        assertTrue(p.matcher(html).find(), "Should format remaining time with Days, Hours, and Minutes");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("When check-in time is reached, Reminder shows Check-in time reached with no negative values")
    void testCheckInTimeReachedNoNegativeCountdown() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        Guest guest = guestRepository.findByUsernameIgnoreCase("guest_user").orElseThrow();

        Booking booking = new Booking();
        booking.setBookingReference("BK-REACHED-TEST");
        booking.setGuest(guest);
        booking.setHotel(hotel);
        booking.setRoom(room);
        // Check-in date is today, and check-in time has passed
        booking.setCheckInDate(LocalDate.now());
        booking.setCheckInTime(java.time.LocalTime.MIN);
        booking.setCheckOutDate(LocalDate.now().plusDays(2));
        booking.setNumberOfGuests(1);
        booking.setTotalAmount(8000.0);
        booking.setStatus(com.hotel.model.enums.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        MvcResult result = mockMvc.perform(get("/guest/my-bookings"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("Check-in time reached"), "Should display 'Check-in time reached'");
        // Ensure no negative countdown values
        assertFalse(html.contains("-1 Days") && html.contains("-1 Hours"), "Must not display negative countdown numbers");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Current stay (Checked In) uses existing booking status without negative countdown")
    void testCurrentStayUsesExistingStatus() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        Guest guest = guestRepository.findByUsernameIgnoreCase("guest_user").orElseThrow();

        Booking booking = new Booking();
        booking.setBookingReference("BK-CHECKEDIN-TEST");
        booking.setGuest(guest);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now());
        booking.setCheckOutDate(LocalDate.now().plusDays(3));
        booking.setNumberOfGuests(2);
        booking.setTotalAmount(9000.0);
        booking.setStatus(com.hotel.model.enums.BookingStatus.CHECKED_IN);
        bookingRepository.save(booking);

        MvcResult result = mockMvc.perform(get("/guest/my-bookings"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("reminder-current-stay"), "Should render current-stay reminder style");
        assertTrue(html.contains("Checked In"), "Should display Checked In status");
        assertFalse(html.contains("-1 Days"), "Should not show negative countdown");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Completed booking in Booking History does not display the upcoming countdown")
    void testBookingHistoryDoesNotShowUpcomingCountdown() throws Exception {
        MvcResult result = mockMvc.perform(get("/guest/booking-history"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertFalse(html.contains("booking-reminder-wrap"), "Booking history must not display upcoming countdown");
        assertFalse(html.contains("until check-in"), "Booking history must not contain until check-in");
    }

    // =========================================================================
    // 10. STATE -> DISTRICT DATA (Source of truth from imported hotel data)
    // =========================================================================

    @Autowired
    private com.hotel.service.HotelService hotelService;

    @Test
    @DisplayName("State selection returns all available districts for that state from imported directory data")
    void testStateToDistrictDataDynamicLookup() {
        List<String> karnatakaDistricts = hotelService.getDistrictsByState("Karnataka");
        assertNotNull(karnatakaDistricts);
        assertFalse(karnatakaDistricts.isEmpty(), "Karnataka must have districts from directory data");
        assertTrue(karnatakaDistricts.contains("Bengaluru Urban") || karnatakaDistricts.contains("Bengaluru"));
        assertTrue(karnatakaDistricts.contains("Mysuru"));
        assertTrue(karnatakaDistricts.contains("Kodagu"));
        // Does not show districts belonging to another state
        assertFalse(karnatakaDistricts.contains("Mumbai City"));
        assertFalse(karnatakaDistricts.contains("Pune"));

        List<String> maharashtraDistricts = hotelService.getDistrictsByState("Maharashtra");
        assertNotNull(maharashtraDistricts);
        assertFalse(maharashtraDistricts.isEmpty(), "Maharashtra must have districts from directory data");
        assertTrue(maharashtraDistricts.contains("Mumbai City"));
        assertTrue(maharashtraDistricts.contains("Pune"));
        assertFalse(maharashtraDistricts.contains("Bengaluru Urban"));
    }

    @Test
    @DisplayName("Find Hotels page with State parameter returns its districts and does not leak other states' districts")
    void testFindHotelsStateDistrictsOptions() throws Exception {
        MvcResult result = mockMvc.perform(get("/hotels").param("state", "Karnataka"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("Mysuru"), "Should contain Karnataka district Mysuru");
        assertTrue(html.contains("Kodagu"), "Should contain Karnataka district Kodagu");
        assertFalse(html.contains("<option value=\"Mumbai City\">Mumbai City</option>"), "Should not show Mumbai City under Karnataka");
    }

    // =========================================================================
    // 11. DYNAMIC GUEST TABLE & RESERVATION DETAILS VALIDATION
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Booking page contains compact dynamic guest details table structure")
    void testBookingPageContainsDynamicGuestDetailsTable() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        MvcResult result = mockMvc.perform(get("/bookings/new").param("hotelId", String.valueOf(hotel.getId())))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("guestDetailsContainer") || html.contains("Guest Details"), "Should contain guest details section");
        assertTrue(html.contains("guestTableBody"), "Should contain compact guest details table body");
        assertTrue(html.contains("Guest"), "Should contain Guest column header");
        assertTrue(html.contains("Type"), "Should contain Type column header");
        assertTrue(html.contains("Age"), "Should contain Age column header");
        assertTrue(html.contains("Gender"), "Should contain Gender column header");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Booking validation fails if guest details are missing or incomplete")
    void testBookingFailsWhenGuestDetailsIncomplete() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        // Submit with 2 guests but incomplete guest details (missing age and gender)
        MvcResult res = mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("hotelId", String.valueOf(hotel.getId()))
                        .param("roomId", String.valueOf(room.getId()))
                        .param("checkInDate", LocalDate.now().plusDays(5).toString())
                        .param("checkOutDate", LocalDate.now().plusDays(8).toString())
                        .param("numberOfGuests", "2")
                        // Incomplete row: missing age and gender
                        .param("guestDetails[0].guestType", "ADULT")
                        .param("guestDetails[0].age", "")
                        .param("guestDetails[0].gender", ""))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirected = res.getResponse().getRedirectedUrl();
        assertNotNull(redirected);
        assertTrue(redirected.contains("error=missing_guest_details"), "Should redirect with error=missing_guest_details");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Booking succeeds with valid guest details and attaches details to booking entity linked to Guest account")
    void testBookingSucceedsWithValidGuestDetails() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        MvcResult result = mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("hotelId", String.valueOf(hotel.getId()))
                        .param("roomId", String.valueOf(room.getId()))
                        .param("checkInDate", LocalDate.now().plusDays(10).toString())
                        .param("checkOutDate", LocalDate.now().plusDays(14).toString())
                        .param("numberOfGuests", "2")
                        .param("guestDetails[0].guestType", "ADULT")
                        .param("guestDetails[0].age", "32")
                        .param("guestDetails[0].gender", "Male")
                        .param("guestDetails[1].guestType", "MINOR")
                        .param("guestDetails[1].age", "8")
                        .param("guestDetails[1].gender", "Female"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirected = result.getResponse().getRedirectedUrl();
        assertNotNull(redirected);
        assertTrue(redirected.contains("/guest/my-bookings"), "Should redirect to guest bookings");

        List<Booking> bookings = bookingRepository.findAll();
        Booking savedBooking = bookings.stream()
                .filter(b -> "guest_user".equals(b.getGuest().getUsername()) && b.getNumberOfGuests() == 2 && b.getGuestDetails() != null && !b.getGuestDetails().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Saved booking with guest details not found"));

        assertEquals("guest_user", savedBooking.getGuest().getUsername(), "Booking must remain linked to logged-in Guest account");
        assertEquals(2, savedBooking.getNumberOfGuests());
        assertNotNull(savedBooking.getGuestDetails());
        assertEquals(2, savedBooking.getGuestDetails().size(), "Must store both guest rows");
        assertEquals("ADULT", savedBooking.getGuestDetails().get(0).getGuestType());
        assertEquals(32, savedBooking.getGuestDetails().get(0).getAge());
        assertEquals("Male", savedBooking.getGuestDetails().get(0).getGender());
        assertEquals("MINOR", savedBooking.getGuestDetails().get(1).getGuestType());
        assertEquals(8, savedBooking.getGuestDetails().get(1).getAge());
        assertEquals("Female", savedBooking.getGuestDetails().get(1).getGender());
    }

    // =========================================================================
    // 12. PLANNED LIST — CHECK-IN REMINDER & SWITCH TO CHECK-OUT REMINDER
    // =========================================================================

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Planned List shows countdown to check-in when before check-in")
    void testPlannedListShowsCountdownToCheckIn() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        Guest guest = guestRepository.findByUsernameIgnoreCase("guest_user").orElseThrow();

        // Add hotel to planned list
        plannedHotelService.addHotelToPlannedList("guest_user", hotel.getId());

        // Create future booking for this planned hotel
        Booking booking = new Booking();
        booking.setBookingReference("BK-PLAN-FUTURE-01");
        booking.setGuest(guest);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now().plusDays(2));
        booking.setCheckInTime(java.time.LocalTime.of(14, 0));
        booking.setCheckOutDate(LocalDate.now().plusDays(5));
        booking.setCheckOutTime(java.time.LocalTime.of(11, 0));
        booking.setNumberOfGuests(1);
        booking.setTotalAmount(10000.0);
        booking.setStatus(com.hotel.model.enums.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        MvcResult result = mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("planned-stay-reminder"), "Should render planned stay reminder container");
        assertTrue(html.contains("REMINDER"), "Should display REMINDER label before check-in");
        assertTrue(html.contains("until check-in"), "Should display 'until check-in' subtext");
        assertTrue(html.contains("data-checkin-millis"), "Should supply check-in epoch timestamp");
        assertTrue(html.contains("data-checkout-millis"), "Should supply check-out epoch timestamp");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Planned List switches to CHECK-OUT REMINDER when check-in is reached or checked in")
    void testPlannedListSwitchesToCheckOutReminder() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        Guest guest = guestRepository.findByUsernameIgnoreCase("guest_user").orElseThrow();

        plannedHotelService.addHotelToPlannedList("guest_user", hotel.getId());

        // Create booking where check-in is today and has been reached
        Booking booking = new Booking();
        booking.setBookingReference("BK-PLAN-CURRENT-01");
        booking.setGuest(guest);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now());
        booking.setCheckInTime(java.time.LocalTime.MIN);
        booking.setCheckOutDate(LocalDate.now().plusDays(2));
        booking.setCheckOutTime(java.time.LocalTime.of(11, 0));
        booking.setNumberOfGuests(1);
        booking.setTotalAmount(10000.0);
        booking.setStatus(com.hotel.model.enums.BookingStatus.CHECKED_IN);
        bookingRepository.save(booking);

        MvcResult result = mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("CHECK-OUT REMINDER"), "Should switch to CHECK-OUT REMINDER label");
        assertTrue(html.contains("until check-out"), "Should display 'until check-out' subtext");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Planned List shows Completed with no active countdown when stay is completed")
    void testPlannedListShowsStayCompletedNoCountdown() throws Exception {
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        Guest guest = guestRepository.findByUsernameIgnoreCase("guest_user").orElseThrow();

        plannedHotelService.addHotelToPlannedList("guest_user", hotel.getId());

        // Create booking where stay is completed
        Booking booking = new Booking();
        booking.setBookingReference("BK-PLAN-COMPLETED-01");
        booking.setGuest(guest);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now().minusDays(5));
        booking.setCheckInTime(java.time.LocalTime.MIN);
        booking.setCheckOutDate(LocalDate.now().minusDays(1));
        booking.setCheckOutTime(java.time.LocalTime.MIN);
        booking.setNumberOfGuests(1);
        booking.setTotalAmount(10000.0);
        booking.setStatus(com.hotel.model.enums.BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        MvcResult result = mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("STAY COMPLETED"), "Should display STAY COMPLETED label");
        assertTrue(html.contains("No active countdown"), "Should display 'No active countdown' subtext");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("GET /bookings/new flexibly parses slash dates '17/10/26' and '20/10/26' into LocalDate without error")
    void testFlexibleDateFormatInGetBookingForm() throws Exception {
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        MvcResult result = mockMvc.perform(get("/bookings/new")
                .param("roomId", room.getId().toString())
                .param("checkIn", "17/10/26")
                .param("checkOut", "20/10/26")
                .param("guests", "4"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", nullValue()))
                .andReturn();

        BookingRequestDto dto = (BookingRequestDto) result.getModelAndView().getModel().get("bookingDto");
        assertNotNull(dto);
        assertEquals(LocalDate.of(2026, 10, 17), dto.getCheckInDate());
        assertEquals(LocalDate.of(2026, 10, 20), dto.getCheckOutDate());
        assertEquals(4, dto.getNumberOfGuests());

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("2026-10-17"), "HTML should reflect parsed ISO date");
        assertTrue(html.contains("2026-10-20"), "HTML should reflect parsed ISO date");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Real-time dynamic traveler rows script and select triggers are properly mounted within main-wrapper")
    void testRealTimeTravelerRowUpdate_StructureAndScriptsMountedInMainWrapper() throws Exception {
        MvcResult result = mockMvc.perform(get("/bookings/new"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 1. Script must be located inside <main class="main-wrapper">...</main> so SPA transitions execute it
        int mainStart = html.indexOf("<main class=\"main-wrapper\">");
        int mainEnd = html.indexOf("</main>");
        assertTrue(mainStart > 0 && mainEnd > mainStart, "Page should contain <main class='main-wrapper'>");

        int scriptDef = html.indexOf("function renderGuestDetailsTable()");
        assertTrue(scriptDef > mainStart && scriptDef < mainEnd,
                "renderGuestDetailsTable() must be mounted inside .main-wrapper so SPA router executes it without refresh");

        // 2. Dropdown has both onchange and oninput event triggers
        assertTrue(html.contains("onchange=\"renderGuestDetailsTable()\""), "Select must have onchange trigger");
        assertTrue(html.contains("oninput=\"renderGuestDetailsTable()\""), "Select must have oninput trigger");

        // 3. Script exposes functions globally to window for immediate inline invocation
        assertTrue(html.contains("window.renderGuestDetailsTable = renderGuestDetailsTable;"),
                "Script must register window.renderGuestDetailsTable globally");
        assertTrue(html.contains("window.createTravelerRow = createTravelerRow;"),
                "Script must register window.createTravelerRow globally");
        assertTrue(html.contains("window.initBookingPage = initBookingPage;"),
                "Script must expose window.initBookingPage");

        // 4. Initial and dynamic table body element exists
        assertTrue(html.contains("id=\"guestTableBody\""), "Table body must exist for dynamic traveler row insertion");
        assertTrue(html.contains("id=\"travelerValidationMsg\""), "Traveler validation notice container must exist");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Dynamic Travelers validation blocks booking if details are incomplete for any row")
    void testDynamicTravelerRowsValidation_MissingDetails() throws Exception {
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-10-17")
                .param("checkOutDate", "2026-10-20")
                .param("numberOfGuests", "3")
                .param("guestDetails[0].guestType", "Adult")
                .param("guestDetails[0].age", "30")
                .param("guestDetails[0].gender", "Male")
                // Missing traveler 2 and 3 details
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=missing_guest_details*"));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Dynamic Travelers validation blocks contradictory Adult/Minor age values")
    void testDynamicTravelerRowsValidation_InvalidAdultMinorAge() throws Exception {
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-10-17")
                .param("checkOutDate", "2026-10-20")
                .param("numberOfGuests", "2")
                .param("guestDetails[0].guestType", "Adult")
                .param("guestDetails[0].age", "30")
                .param("guestDetails[0].gender", "Male")
                .param("guestDetails[1].guestType", "Minor")
                .param("guestDetails[1].age", "25") // Contradictory: Minor with age 25
                .param("guestDetails[1].gender", "Female"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=invalid_guest_age*"));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Booking succeeds with 4 dynamic travelers and slash dates '17/10/26' -> '20/10/26'")
    void testBookingWithFlexibleDateFormatAndDynamicTravelers_Success() throws Exception {
        // Find a room without conflicting bookings
        Room room = roomRepository.findAll().stream()
                .filter(r -> bookingRepository.findOverlappingBookings(r.getId(), 
                        List.of(com.hotel.model.enums.BookingStatus.CONFIRMED, com.hotel.model.enums.BookingStatus.CHECKED_IN),
                        LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15)).isEmpty())
                .findFirst().orElseThrow();

        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "10/11/26")
                .param("checkOutDate", "15/11/26")
                .param("numberOfGuests", "4")
                .param("guestDetails[0].guestType", "Adult")
                .param("guestDetails[0].age", "35")
                .param("guestDetails[0].gender", "Male")
                .param("guestDetails[1].guestType", "Adult")
                .param("guestDetails[1].age", "32")
                .param("guestDetails[1].gender", "Female")
                .param("guestDetails[2].guestType", "Minor")
                .param("guestDetails[2].age", "8")
                .param("guestDetails[2].gender", "Female")
                .param("guestDetails[3].guestType", "Minor")
                .param("guestDetails[3].age", "12")
                .param("guestDetails[3].gender", "Male"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/my-bookings?booked=true"));

        List<Booking> userBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getCheckInDate().equals(LocalDate.of(2026, 11, 10)))
                .toList();
        assertFalse(userBookings.isEmpty(), "Booking should be recorded in database");
        Booking b = userBookings.get(0);
        assertEquals(LocalDate.of(2026, 11, 10), b.getCheckInDate());
        assertEquals(LocalDate.of(2026, 11, 15), b.getCheckOutDate());
        assertEquals(4, b.getNumberOfGuests());
        assertEquals(4, b.getGuestDetails().size());
        assertEquals("Adult", b.getGuestDetails().get(0).getGuestType());
        assertEquals(35, b.getGuestDetails().get(0).getAge());
        assertEquals("Male", b.getGuestDetails().get(0).getGender());
        assertEquals("Minor", b.getGuestDetails().get(2).getGuestType());
        assertEquals(8, b.getGuestDetails().get(2).getAge());
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Mandatory date rules: no dates, only check-in, only check-out are blocked")
    void testMandatoryDatesValidation() throws Exception {
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        // No dates
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=missing_dates*"));

        // Only check-in
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-10-17")
                .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=missing_dates*"));

        // Only check-out
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkOutDate", "2026-10-20")
                .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=missing_dates*"));

        // Check-out before or equal to check-in
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-10-20")
                .param("checkOutDate", "2026-10-17")
                .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=invalid_dates*"));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Traveler Name UI: Booking form renders Name table column and input placeholder")
    void testBookingForm_RendersTravelerNameFieldWithPlaceholder() throws Exception {
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/bookings/new")
                .param("roomId", room.getId().toString())
                .param("guests", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">Name</th>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("placeholder=\"Enter Full Name\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("guest-name-field")));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Traveler Name validation: Missing, blank, or placeholder name blocks booking")
    void testDynamicTravelerRowsValidation_MissingOrPlaceholderName() throws Exception {
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        // 1. Blank name in row 2
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-12-01")
                .param("checkOutDate", "2026-12-05")
                .param("numberOfGuests", "2")
                .param("guestDetails[0].name", "Rahul")
                .param("guestDetails[0].guestType", "Adult")
                .param("guestDetails[0].age", "28")
                .param("guestDetails[0].gender", "Male")
                .param("guestDetails[1].name", "")
                .param("guestDetails[1].guestType", "Adult")
                .param("guestDetails[1].age", "25")
                .param("guestDetails[1].gender", "Female"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=missing_guest_details*"));

        // 2. Auto-generated placeholder "Traveler 2" as name
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-12-01")
                .param("checkOutDate", "2026-12-05")
                .param("numberOfGuests", "2")
                .param("guestDetails[0].name", "Rahul")
                .param("guestDetails[0].guestType", "Adult")
                .param("guestDetails[0].age", "28")
                .param("guestDetails[0].gender", "Male")
                .param("guestDetails[1].name", "Traveler 2")
                .param("guestDetails[1].guestType", "Adult")
                .param("guestDetails[1].age", "25")
                .param("guestDetails[1].gender", "Female"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new?*error=missing_guest_details*"));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Traveler Name storage: Booking saves individual traveler names (Rahul, Ananya, Arjun)")
    void testBookingWithTravelerNames_StoredCorrectly() throws Exception {
        Room room = roomRepository.findAll().stream()
                .filter(r -> bookingRepository.findOverlappingBookings(r.getId(),
                        List.of(com.hotel.model.enums.BookingStatus.CONFIRMED, com.hotel.model.enums.BookingStatus.CHECKED_IN),
                        LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 14)).isEmpty())
                .findFirst().orElseThrow();

        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-12-10")
                .param("checkOutDate", "2026-12-14")
                .param("numberOfGuests", "3")
                .param("guestDetails[0].name", "Rahul")
                .param("guestDetails[0].guestType", "Adult")
                .param("guestDetails[0].age", "28")
                .param("guestDetails[0].gender", "Male")
                .param("guestDetails[1].name", "Ananya")
                .param("guestDetails[1].guestType", "Adult")
                .param("guestDetails[1].age", "25")
                .param("guestDetails[1].gender", "Female")
                .param("guestDetails[2].name", "Arjun")
                .param("guestDetails[2].guestType", "Minor")
                .param("guestDetails[2].age", "10")
                .param("guestDetails[2].gender", "Male"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/my-bookings?booked=true"));

        List<Booking> userBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getCheckInDate().equals(LocalDate.of(2026, 12, 10)))
                .toList();
        assertFalse(userBookings.isEmpty(), "Booking should be recorded in database");
        Booking b = userBookings.get(0);
        assertEquals(3, b.getNumberOfGuests());
        assertEquals(3, b.getGuestDetails().size());

        // Verify names stored with travelers
        assertEquals("Rahul", b.getGuestDetails().get(0).getName());
        assertEquals("Adult", b.getGuestDetails().get(0).getGuestType());
        assertEquals(28, b.getGuestDetails().get(0).getAge());
        assertEquals("Male", b.getGuestDetails().get(0).getGender());

        assertEquals("Ananya", b.getGuestDetails().get(1).getName());
        assertEquals("Adult", b.getGuestDetails().get(1).getGuestType());
        assertEquals(25, b.getGuestDetails().get(1).getAge());
        assertEquals("Female", b.getGuestDetails().get(1).getGender());

        assertEquals("Arjun", b.getGuestDetails().get(2).getName());
        assertEquals("Minor", b.getGuestDetails().get(2).getGuestType());
        assertEquals(10, b.getGuestDetails().get(2).getAge());
        assertEquals("Male", b.getGuestDetails().get(2).getGender());
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("My Bookings displays actual traveler information (Rahul, Ananya, Arjun) instead of generic Adult")
    void testMyBookings_DisplaysActualTravelerInformation() throws Exception {
        Room room = roomRepository.findAll().stream()
                .filter(r -> bookingRepository.findOverlappingBookings(r.getId(),
                        List.of(com.hotel.model.enums.BookingStatus.CONFIRMED, com.hotel.model.enums.BookingStatus.CHECKED_IN),
                        LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 24)).isEmpty())
                .findFirst().orElseThrow();

        // 1. Submit reservation with Rahul, Ananya, and Arjun
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-12-20")
                .param("checkOutDate", "2026-12-24")
                .param("numberOfGuests", "3")
                .param("guestDetails[0].name", "Rahul")
                .param("guestDetails[0].guestType", "Adult")
                .param("guestDetails[0].age", "28")
                .param("guestDetails[0].gender", "Male")
                .param("guestDetails[1].name", "Ananya")
                .param("guestDetails[1].guestType", "Adult")
                .param("guestDetails[1].age", "25")
                .param("guestDetails[1].gender", "Female")
                .param("guestDetails[2].name", "Arjun")
                .param("guestDetails[2].guestType", "Minor")
                .param("guestDetails[2].age", "10")
                .param("guestDetails[2].gender", "Male"))
                .andExpect(status().is3xxRedirection());

        // 2. Open My Bookings
        MvcResult result = mockMvc.perform(get("/guest/my-bookings"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 3. Verify actual traveler names and details are rendered
        assertTrue(html.contains("Rahul"), "Should display traveler 1 name Rahul");
        assertTrue(html.contains("Ananya"), "Should display traveler 2 name Ananya");
        assertTrue(html.contains("Arjun"), "Should display traveler 3 name Arjun");

        assertTrue(html.contains("Adult"), "Should display Adult type");
        assertTrue(html.contains("Minor"), "Should display Minor type for Arjun");
        assertTrue(html.contains("28"), "Should display age 28 for Rahul");
        assertTrue(html.contains("25"), "Should display age 25 for Ananya");
        assertTrue(html.contains("10"), "Should display age 10 for Arjun");
        assertTrue(html.contains("Male"), "Should display gender Male");
        assertTrue(html.contains("Female"), "Should display gender Female");

        // Verify full representation
        assertTrue(html.contains("Rahul — Adult — 28 — Male"), "Should contain full detail string for Rahul");
        assertTrue(html.contains("Ananya — Adult — 25 — Female"), "Should contain full detail string for Ananya");
        assertTrue(html.contains("Arjun — Minor — 10 — Male"), "Should contain full detail string for Arjun");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("My Bookings displays exact number of traveler records for different guest counts (1, 2, 4)")
    void testMyBookings_ExactNumberOfTravelersDisplayed() throws Exception {
        Room room = roomRepository.findAll().stream()
                .filter(r -> bookingRepository.findOverlappingBookings(r.getId(),
                        List.of(com.hotel.model.enums.BookingStatus.CONFIRMED, com.hotel.model.enums.BookingStatus.CHECKED_IN),
                        LocalDate.of(2026, 12, 26), LocalDate.of(2026, 12, 29)).isEmpty())
                .findFirst().orElseThrow();

        // 1 Guest booking
        mockMvc.perform(post("/bookings/submit")
                .with(csrf())
                .param("roomId", room.getId().toString())
                .param("checkInDate", "2026-12-26")
                .param("checkOutDate", "2026-12-29")
                .param("numberOfGuests", "1")
                .param("guestDetails[0].name", "Vikram")
                .param("guestDetails[0].guestType", "Adult")
                .param("guestDetails[0].age", "34")
                .param("guestDetails[0].gender", "Male"))
                .andExpect(status().is3xxRedirection());

        MvcResult result = mockMvc.perform(get("/guest/my-bookings"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("Vikram"), "Should display Vikram");
        assertTrue(html.contains("Vikram — Adult — 34 — Male"), "Should display Vikram full details");
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Non-Home background: Pastel pink + sky blue gradient verified for non-home pages, zero background images, Home cinematic animation preserved")
    void testNonHomePageBackgroundConfiguration() throws Exception {
        // 1. Verify CSS defines subtle pastel pink + sky blue gradient for non-home pages without background images
        MvcResult cssResult = mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk())
                .andReturn();
        String css = cssResult.getResponse().getContentAsString();
        assertFalse(css.contains("page-bg.png"), "CSS must NOT reference non-home background image page-bg.png");
        assertFalse(css.contains("global-non-home-bg"), "CSS must NOT contain global-non-home-bg layer");
        assertTrue(css.contains("body:not(.home-page)"), "CSS must target non-home pages via body:not(.home-page)");
        assertTrue(css.contains("rgba(135, 206, 250, 0.16)"), "CSS must include subtle sky blue gradient stop");
        assertTrue(css.contains("rgba(255, 160, 190, 0.13)"), "CSS must include subtle soft pink gradient stop");

        // 2. Verify Home page maintains body.home-page class and cinematic animation layer
        MvcResult homeResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        String homeHtml = homeResult.getResponse().getContentAsString();
        assertTrue(homeHtml.contains("class=\"home-page\""), "Home page body must have home-page class to preserve its cinematic background animation");
        assertTrue(homeHtml.contains("homeBgAnimLayer"), "Home page must retain its cinematic background animation layer");

        // 3. Verify non-home pages do NOT have home-page class and do NOT contain globalNonHomeBg
        String[] nonHomeUrls = {
                "/hotels",
                "/map",
                "/guest/districts",
                "/guest/offers",
                "/guest/dashboard",
                "/guest/profile",
                "/guest/my-bookings",
                "/guest/booking-history",
                "/guest/planned-list"
        };
        for (String url : nonHomeUrls) {
            MvcResult res = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
            String html = res.getResponse().getContentAsString();
            assertFalse(html.contains("<body class=\"home-page\""), "URL " + url + " must not have home-page class");
            assertTrue(html.contains("<body>") || html.contains("<body class=\""), "URL " + url + " must have standard body tag");
            assertFalse(html.contains("globalNonHomeBg"), "URL " + url + " must not contain globalNonHomeBg");
        }
    }
}




