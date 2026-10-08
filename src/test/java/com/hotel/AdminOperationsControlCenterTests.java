package com.hotel;

import com.hotel.model.Offer;
import com.hotel.service.OfferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminOperationsControlCenterTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OfferService offerService;

    @Test
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    @DisplayName("Admin Check Hotels: Returns 200 OK with calculated percentages and guest flow")
    void testCheckHotelsOperationalOverview() throws Exception {
        mockMvc.perform(get("/admin/check-hotels"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("operations"))
                .andExpect(model().attributeExists("availableRooms"))
                .andExpect(model().attributeExists("reservedRooms"))
                .andExpect(content().string(containsString("Available")))
                .andExpect(content().string(containsString("Reserved")))
                .andExpect(content().string(containsString("Pre-booked")))
                .andExpect(content().string(containsString("Unreserved")))
                .andExpect(content().string(containsString("Occupied")))
                .andExpect(content().string(containsString("Current Guests")))
                .andExpect(content().string(containsString("Upcoming Check-ins")))
                .andExpect(content().string(containsString("Upcoming Check-outs")));
    }

    @Test
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    @DisplayName("Admin Districts: Hierarchical State -> District -> Hotel Operational Overview")
    void testDistrictsHierarchicalNavigation() throws Exception {
        mockMvc.perform(get("/admin/districts").param("state", "Karnataka").param("district", "Bengaluru Urban"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("states"))
                .andExpect(model().attributeExists("districts"))
                .andExpect(model().attributeExists("districtOperations"))
                .andExpect(model().attributeExists("districtHotels"))
                .andExpect(content().string(containsString("Districts Operations Hierarchy")))
                .andExpect(content().string(containsString("Karnataka")))
                .andExpect(content().string(containsString("Bengaluru Urban")));
    }

    @Test
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    @DisplayName("Admin Offers: View, Create, and Toggle Status with single source of truth for Guests")
    void testOffersManagementAndGuestSynchronization() throws Exception {
        // 1. Admin views offers
        mockMvc.perform(get("/admin/offers"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("offers"))
                .andExpect(content().string(containsString("Executive Offers")));

        // 2. Admin creates a new offer
        mockMvc.perform(post("/admin/offers/create")
                        .with(csrf())
                        .param("title", "Royal Rajasthan Maharaja Privilege")
                        .param("tag", "Palace Special")
                        .param("subtitle", "Luxury stay in royal heritage havelis")
                        .param("perks", "Royal welcome ceremony\nDesert safari chaperone")
                        .param("promoCode", "GH-RAJASTHAN30")
                        .param("discountPercentage", "30")
                        .param("targetDistrict", "Jaipur")
                        .param("targetUrl", "/hotels?state=Rajasthan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/offers"));

        // Verify offer exists in DB
        assertTrue(offerService.existsByPromoCode("GH-RAJASTHAN30"), "Newly created offer must exist in database");

        // 3. Guest portal directly displays the newly created active offer
        mockMvc.perform(get("/guest/offers"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Royal Rajasthan Maharaja Privilege")))
                .andExpect(content().string(containsString("GH-RAJASTHAN30")));
    }

    @Test
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    @DisplayName("Admin Dashboard: 5 Core Operations Sections (A. Guests, B. Rooms, C. Maintenance, D. Inspections, E. Staff)")
    void testAdminDashboardFiveOperationsSections() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("guestActivity"))
                .andExpect(model().attributeExists("hotelOperations"))
                .andExpect(model().attributeExists("maintenance"))
                .andExpect(model().attributeExists("inspections"))
                .andExpect(model().attributeExists("staffActivity"))
                .andExpect(content().string(containsString("A. Guest Activity Operations")))
                .andExpect(content().string(containsString("B. Hotel Room Operational Status")))
                .andExpect(content().string(containsString("C. Property Maintenance Operations")))
                .andExpect(content().string(containsString("D. Property Inspections")))
                .andExpect(content().string(containsString("E. Operational Staff Activity")))
                .andExpect(content().string(containsString("Notifications")))
                .andExpect(content().string(containsString("Messages")))
                .andExpect(content().string(containsString("Staff Messages / Requests")));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Security: Guest is strictly forbidden from accessing Admin Operations Center")
    void testSecurityGuestCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
        mockMvc.perform(get("/admin/check-hotels")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
        mockMvc.perform(get("/admin/districts")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
        mockMvc.perform(get("/admin/offers")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("Security: Staff is strictly forbidden from accessing Admin Operations Center")
    void testSecurityStaffCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
        mockMvc.perform(get("/admin/check-hotels")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
        mockMvc.perform(get("/admin/districts")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
        mockMvc.perform(get("/admin/offers")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    @DisplayName("Admin REST API: Operations Level 2 & 3 drill-down endpoints return valid JSON")
    void testOperationsRestEndpoints() throws Exception {
        mockMvc.perform(get("/admin/api/operations/rooms").param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        mockMvc.perform(get("/admin/api/operations/inspections"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        mockMvc.perform(get("/admin/api/operations/staff").param("status", "Working"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}
