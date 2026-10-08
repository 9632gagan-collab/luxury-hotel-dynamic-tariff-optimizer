package com.hotel;

import com.hotel.dto.GuestRegistrationDto;
import com.hotel.model.Guest;
import com.hotel.model.Hotel;
import com.hotel.model.PlannedHotel;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.PlannedHotelRepository;
import com.hotel.service.GuestService;
import com.hotel.service.PlannedHotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlannedListFeatureTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlannedHotelService plannedHotelService;

    @Autowired
    private PlannedHotelRepository plannedHotelRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private GuestService guestService;

    @Autowired
    private HotelRepository hotelRepository;

    private Guest guest1;
    private Guest guest2;
    private Hotel testHotel1;
    private Hotel testHotel2;

    @BeforeEach
    void setUp() {
        // Find existing or register test guests
        guest1 = guestRepository.findByUsername("guest1").orElse(null);
        if (guest1 == null) {
            guest1 = guestService.registerGuest(new GuestRegistrationDto("guest1", "guest1@royalindianstays.com", "Password123!", "Password123!"));
        }

        guest2 = guestRepository.findByUsername("guest2").orElse(null);
        if (guest2 == null) {
            guest2 = guestService.registerGuest(new GuestRegistrationDto("guest2", "guest2@royalindianstays.com", "Password123!", "Password123!"));
        }

        assertNotNull(guest1, "Guest 1 must be registered for testing");
        assertNotNull(guest2, "Guest 2 must be registered for testing");

        List<Hotel> hotels = hotelRepository.findAll();
        assertTrue(hotels.size() >= 2, "At least 2 hotels must exist for testing");
        testHotel1 = hotels.get(0);
        testHotel2 = hotels.get(1);

        // Clear any previous planned hotels for clean test runs
        plannedHotelRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Guest Dashboard contains Planned List option card and link")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testGuestDashboardContainsPlannedList() throws Exception {
        mockMvc.perform(get("/guest/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Planned List")))
                .andExpect(content().string(containsString("/guest/planned-list")))
                .andExpect(content().string(containsString("cardPlannedList")));
    }

    @Test
    @DisplayName("2. Planned List opens as a separate dedicated page")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testPlannedListOpensAsSeparatePage() throws Exception {
        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/planned-list"))
                .andExpect(content().string(containsString("MY PLANNED LIST")));
    }

    @Test
    @DisplayName("3. Verify Travel Planning Bookmark icon is used, NO heart icon")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testIconIsBookmarkAndNotHeart() throws Exception {
        // In hotels view
        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fa-bookmark")))
                .andExpect(content().string(not(containsString("fa-heart"))));

        // In hotel details view
        mockMvc.perform(get("/hotels/" + testHotel1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fa-bookmark")))
                .andExpect(content().string(not(containsString("fa-heart"))));

        // In recommendations view
        mockMvc.perform(get("/guest/recommendations"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fa-bookmark")))
                .andExpect(content().string(not(containsString("fa-heart"))));
    }

    @Test
    @DisplayName("4. Hotel cards show the new planning icon with tooltip")
    void testHotelCardsShowPlanningIcon() throws Exception {
        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("btn-planned-toggle")))
                .andExpect(content().string(containsString("Add to Planned List")));
    }

    @Test
    @DisplayName("5. Hotel Details shows the same planning icon and button")
    void testHotelDetailsShowsPlanningIcon() throws Exception {
        mockMvc.perform(get("/hotels/" + testHotel1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("btnPlannedDetails")))
                .andExpect(content().string(containsString("Add to Planned List")));
    }

    @Test
    @DisplayName("6 & 7. Logged-in Guest can add hotel and it appears in Planned List")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testLoggedInGuestCanAddHotelAndItAppearsInPlannedList() throws Exception {
        // Add via toggle AJAX endpoint
        mockMvc.perform(post("/guest/planned-list/toggle")
                        .with(csrf())
                        .param("hotelId", String.valueOf(testHotel1.getId()))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.planned").value(true))
                .andExpect(jsonPath("$.message").value("Added to your Planned List"));

        // Verify hotel appears in Planned List page
        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(testHotel1.getName())))
                .andExpect(content().string(containsString(testHotel1.getState())))
                .andExpect(content().string(containsString("Remove from Planned List")));

        // Verify via service
        assertTrue(plannedHotelService.isHotelPlannedByGuest("guest1", testHotel1.getId()));
        assertEquals(1, plannedHotelService.getPlannedCountForGuest("guest1"));
    }

    @Test
    @DisplayName("8. Clicking again removes the hotel from the Planned List")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testClickingAgainRemovesHotelFromPlannedList() throws Exception {
        // First add
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());
        assertTrue(plannedHotelService.isHotelPlannedByGuest("guest1", testHotel1.getId()));

        // Toggle to remove
        mockMvc.perform(post("/guest/planned-list/toggle")
                        .with(csrf())
                        .param("hotelId", String.valueOf(testHotel1.getId()))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.planned").value(false))
                .andExpect(jsonPath("$.message").value("Removed from your Planned List"));

        assertFalse(plannedHotelService.isHotelPlannedByGuest("guest1", testHotel1.getId()));
        assertEquals(0, plannedHotelService.getPlannedCountForGuest("guest1"));
    }

    @Test
    @DisplayName("9. Duplicate entries are prevented in Planned List")
    void testDuplicateEntriesArePrevented() {
        boolean firstAdd = plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());
        assertTrue(firstAdd, "First add should succeed");

        boolean secondAdd = plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());
        assertFalse(secondAdd, "Second add should be prevented and return false");

        assertEquals(1, plannedHotelService.getPlannedCountForGuest("guest1"));
    }

    @Test
    @DisplayName("10. Planned List persists in database")
    void testPlannedListPersistence() {
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());
        plannedHotelService.addHotelToPlannedList("guest1", testHotel2.getId());

        List<PlannedHotel> list = plannedHotelRepository.findByGuestUsernameOrderByPlannedAtDesc("guest1");
        assertEquals(2, list.size());
        assertEquals("guest1", list.get(0).getGuest().getUsername());
        assertNotNull(list.get(0).getPlannedAt());
    }

    @Test
    @DisplayName("11. Unauthenticated visitor accessing /guest/planned-list is redirected to Login")
    void testUnauthenticatedVisitorRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login**"));
    }

    @Test
    @DisplayName("12. After login, redirect to /guest/planned-list/add adds hotel and returns to intended page")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testPostLoginAddAndRedirect() throws Exception {
        mockMvc.perform(get("/guest/planned-list/add")
                        .param("hotelId", String.valueOf(testHotel1.getId()))
                        .param("returnUrl", "/hotels/" + testHotel1.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hotels/" + testHotel1.getId()))
                .andExpect(flash().attribute("plannedMessage", "Added to your Planned List"));

        assertTrue(plannedHotelService.isHotelPlannedByGuest("guest1", testHotel1.getId()));
    }

    @Test
    @DisplayName("13. Security: Guest can see ONLY their own Planned List")
    void testGuestIsolationSecurity() {
        // Guest 1 adds Hotel 1
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());
        // Guest 2 adds Hotel 2
        plannedHotelService.addHotelToPlannedList("guest2", testHotel2.getId());

        Set<Long> guest1Planned = plannedHotelService.getPlannedHotelIds("guest1");
        Set<Long> guest2Planned = plannedHotelService.getPlannedHotelIds("guest2");

        assertTrue(guest1Planned.contains(testHotel1.getId()));
        assertFalse(guest1Planned.contains(testHotel2.getId()));

        assertTrue(guest2Planned.contains(testHotel2.getId()));
        assertFalse(guest2Planned.contains(testHotel1.getId()));
    }

    @Test
    @DisplayName("14. Empty Planned List state renders properly with Explore Hotels button")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testEmptyPlannedListState() throws Exception {
        // Ensure no hotels planned
        plannedHotelRepository.deleteAll();

        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your Planned List is Empty")))
                .andExpect(content().string(containsString("Save hotels you are planning to visit and find them here later.")))
                .andExpect(content().string(containsString("Explore Hotels")));
    }

    @Test
    @DisplayName("15. Google Maps navigation is preserved on Planned List items")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testGoogleMapsNavigationOnPlannedList() throws Exception {
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());

        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("View on Google Maps")))
                .andExpect(content().string(containsString("google.com/maps")));
    }

    @Test
    @DisplayName("16. Existing booking flow link works from Planned List")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testBookingLinkFromPlannedList() throws Exception {
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());

        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/bookings/new?hotelId=" + testHotel1.getId())));
    }

    @Test
    @DisplayName("17. Terminology check - 'Planned List' used everywhere, no 'Wishlist' or 'Favorites'")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testTerminologyStrictlyFollowed() throws Exception {
        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsStringIgnoringCase("Wishlist"))))
                .andExpect(content().string(not(containsStringIgnoringCase("Favorites"))))
                .andExpect(content().string(not(containsStringIgnoringCase("Favourites"))));
    }

    @Test
    @DisplayName("18. Stage 7 - Planned Stay shows 'Dates not planned' when no dates attached")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testDatesNotPlannedWhenEmpty() throws Exception {
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());

        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Planned Stay:")))
                .andExpect(content().string(containsString("Dates not planned")));
    }

    @Test
    @DisplayName("19. Stage 7 - Planned Stay formats attached dates correctly")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testPlannedStayDatesFormatting() throws Exception {
        java.time.LocalDate inDate = java.time.LocalDate.of(2026, 10, 10);
        java.time.LocalDate outDate = java.time.LocalDate.of(2026, 10, 12);
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId(), inDate, outDate);

        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("10 Oct → 12 Oct")));
    }

    @Test
    @DisplayName("20. Stage 7 - AJAX endpoint updates planned dates dynamically")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testUpdatePlannedDatesAjax() throws Exception {
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());

        mockMvc.perform(post("/guest/planned-list/update-dates")
                        .with(csrf())
                        .param("hotelId", String.valueOf(testHotel1.getId()))
                        .param("checkIn", "2026-11-05")
                        .param("checkOut", "2026-11-08")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.formattedStay").value("5 Nov → 8 Nov"))
                .andExpect(jsonPath("$.checkIn").value("2026-11-05"))
                .andExpect(jsonPath("$.checkOut").value("2026-11-08"));

        // Verify entity persisted in database
        PlannedHotel updated = plannedHotelRepository.findByGuestUsernameAndHotelId("guest1", testHotel1.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(java.time.LocalDate.of(2026, 11, 5), updated.getPlannedCheckInDate());
        assertEquals(java.time.LocalDate.of(2026, 11, 8), updated.getPlannedCheckOutDate());
    }

    @Test
    @DisplayName("21. Stage 7 - Clearing planned dates resets to 'Dates not planned'")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testClearPlannedDatesAjax() throws Exception {
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId(),
                java.time.LocalDate.of(2026, 10, 10), java.time.LocalDate.of(2026, 10, 12));

        mockMvc.perform(post("/guest/planned-list/update-dates")
                        .with(csrf())
                        .param("hotelId", String.valueOf(testHotel1.getId()))
                        .param("checkIn", "")
                        .param("checkOut", "")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.formattedStay").value("Dates not planned"));

        PlannedHotel updated = plannedHotelRepository.findByGuestUsernameAndHotelId("guest1", testHotel1.getId()).orElse(null);
        assertNotNull(updated);
        assertNull(updated.getPlannedCheckInDate());
        assertNull(updated.getPlannedCheckOutDate());
    }

    @Test
    @DisplayName("22. Stage 7 - Travel Planner shows simple status 'Planned'")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testStatusIsPlannedForUnbookedHotel() throws Exception {
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());

        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Status:")))
                .andExpect(content().string(containsString("Planned")));
    }

    @Test
    @DisplayName("23. Stage 7 - View Hotel button links to exact hotel details")
    @WithMockUser(username = "guest1", roles = {"GUEST"})
    void testViewHotelButtonLinksToExactHotel() throws Exception {
        plannedHotelService.addHotelToPlannedList("guest1", testHotel1.getId());

        mockMvc.perform(get("/guest/planned-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("View Hotel")))
                .andExpect(content().string(containsString("/hotels/" + testHotel1.getId())));
    }
}
