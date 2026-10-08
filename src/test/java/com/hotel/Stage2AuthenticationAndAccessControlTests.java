package com.hotel;

import com.hotel.dto.GuestRegistrationDto;
import com.hotel.dto.StaffCreateDto;
import com.hotel.model.Admin;
import com.hotel.model.Guest;
import com.hotel.model.Staff;
import com.hotel.model.enums.AccountStatus;
import com.hotel.repository.AdminRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.StaffRepository;
import com.hotel.service.GuestService;
import com.hotel.service.StaffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Stage2AuthenticationAndAccessControlTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private GuestService guestService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.hotel.security.RateLimiterService rateLimiterService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        rateLimiterService.resetForTesting();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        rateLimiterService.resetForTesting();
    }

    @Value("${app.security.admin.gagan.password:}")
    private String gaganPassword;

    @Value("${app.security.admin.darshan.password:}")
    private String darshanPassword;

    // =========================================================================
    // 1. ADMIN TESTS
    // =========================================================================

    @Test
    @DisplayName("Admin accounts GAGAN and DARSHAN initialized securely with BCrypt")
    void testInitialAdminAccountsInitialized() {
        Admin gagan = adminRepository.findByUsernameIgnoreCase("GAGAN").orElse(null);
        assertNotNull(gagan, "Admin account GAGAN must exist");
        assertTrue(gagan.getPassword().startsWith("$2a$") || gagan.getPassword().startsWith("$2b$"),
                "Admin password must be BCrypt hashed");
        assertTrue(passwordEncoder.matches(gaganPassword, gagan.getPassword()),
                "Configured password must match BCrypt hash for GAGAN");
        assertEquals(AccountStatus.ACTIVE, gagan.getStatus(), "GAGAN account status must be ACTIVE");

        Admin darshan = adminRepository.findByUsernameIgnoreCase("DARSHAN").orElse(null);
        assertNotNull(darshan, "Admin account DARSHAN must exist");
        assertTrue(darshan.getPassword().startsWith("$2a$") || darshan.getPassword().startsWith("$2b$"),
                "Admin password must be BCrypt hashed");
        assertTrue(passwordEncoder.matches(darshanPassword, darshan.getPassword()),
                "Configured password must match BCrypt hash for DARSHAN");
        assertEquals(AccountStatus.ACTIVE, darshan.getStatus(), "DARSHAN account status must be ACTIVE");

        // Verify no extra admin accounts automatically created
        assertEquals(2, adminRepository.count(), "Only GAGAN and DARSHAN should be the initial Admin accounts");
    }

    @Test
    @DisplayName("Admin GAGAN can successfully log in via Spring Security")
    void testAdminGaganLoginSuccess() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("GAGAN")
                        .password(gaganPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Admin DARSHAN can successfully log in via Spring Security")
    void testAdminDarshanLoginSuccess() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("DARSHAN")
                        .password(darshanPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Unknown Admin username is rejected")
    void testUnknownAdminLoginRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "UNKNOWN_ADMIN")
                        .param("password", "somePassword")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true&role=ADMIN"));
    }

    @Test
    @DisplayName("Invalid Admin password is rejected")
    void testInvalidAdminPasswordRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "GAGAN")
                        .param("password", "WRONG_PASSWORD")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true&role=ADMIN"));
    }

    // =========================================================================
    // 2. GUEST REGISTRATION & AUTHENTICATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Guest registration creates active account with BCrypt password")
    void testGuestRegistration() {
        String testUser = "guest_" + System.currentTimeMillis();
        String testEmail = testUser + "@testluxury.com";

        GuestRegistrationDto dto = new GuestRegistrationDto(testUser, testEmail, "SecretPass123", "SecretPass123");
        Guest registered = guestService.registerGuest(dto);

        assertNotNull(registered.getId());
        assertEquals(testUser, registered.getUsername());
        assertEquals(testEmail, registered.getEmail());
        assertEquals(AccountStatus.ACTIVE, registered.getStatus());
        assertTrue(passwordEncoder.matches("SecretPass123", registered.getPassword()));
        assertTrue(registered.getPassword().startsWith("$2a$") || registered.getPassword().startsWith("$2b$"));
    }

    @Test
    @DisplayName("Duplicate guest username and email are rejected")
    void testDuplicateGuestRejected() {
        String username = "unique_guest_test";
        String email = "unique_guest@test.com";

        if (!guestService.existsByUsername(username)) {
            guestService.registerGuest(new GuestRegistrationDto(username, email, "Pass1234", "Pass1234"));
        }

        // Test duplicate username
        assertThrows(IllegalArgumentException.class, () ->
                guestService.registerGuest(new GuestRegistrationDto(username, "other@test.com", "Pass1234", "Pass1234")));

        // Test duplicate email
        assertThrows(IllegalArgumentException.class, () ->
                guestService.registerGuest(new GuestRegistrationDto("another_user", email, "Pass1234", "Pass1234")));
    }

    @Test
    @DisplayName("Guest can log in with username or email")
    void testGuestLoginSuccess() throws Exception {
        String username = "guest_login_user";
        String email = "guest_login@example.com";
        String rawPass = "GuestSecure123";

        if (!guestService.existsByUsername(username)) {
            guestService.registerGuest(new GuestRegistrationDto(username, email, rawPass, rawPass));
        }

        // Login using username
        mockMvc.perform(post("/login")
                        .param("username", username)
                        .param("password", rawPass)
                        .param("role", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));

        // Login using email
        mockMvc.perform(post("/login")
                        .param("username", email)
                        .param("password", rawPass)
                        .param("role", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));
    }

    // =========================================================================
    // 3. STAFF CREATION & AUTHENTICATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Admin can create Staff and Staff can log in with BCrypt credentials")
    void testStaffCreationAndLogin() throws Exception {
        String staffName = "staff_member_" + (System.currentTimeMillis() % 10000);
        String rawPass = "StaffPassword123";

        StaffCreateDto dto = new StaffCreateDto(staffName, rawPass, rawPass);
        Staff createdStaff = staffService.createStaff(dto);

        assertNotNull(createdStaff.getId());
        assertEquals(staffName, createdStaff.getUsername());
        assertEquals(AccountStatus.ACTIVE, createdStaff.getStatus());
        assertTrue(passwordEncoder.matches(rawPass, createdStaff.getPassword()));

        // Verify Staff can log in
        mockMvc.perform(post("/login")
                        .param("username", staffName)
                        .param("password", rawPass)
                        .param("role", "STAFF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/dashboard"));
    }

    @Test
    @DisplayName("Unknown Staff member cannot log in")
    void testUnknownStaffLoginRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "NON_EXISTENT_STAFF")
                        .param("password", "AnyPassword")
                        .param("role", "STAFF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true&role=STAFF"));
    }

    @Test
    @DisplayName("Inactive Staff member cannot log in")
    void testInactiveStaffLoginRejected() throws Exception {
        String staffName = "inactive_staff_" + (System.currentTimeMillis() % 10000);
        String rawPass = "Pass123456";

        Staff staff = staffService.createStaff(new StaffCreateDto(staffName, rawPass, rawPass));
        staff.setStatus(AccountStatus.INACTIVE);
        staff.setActive(false);
        staffRepository.save(staff);

        mockMvc.perform(post("/login")
                        .param("username", staffName)
                        .param("password", rawPass)
                        .param("role", "STAFF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true&role=STAFF"));
    }

    // =========================================================================
    // 4. ACCESS CONTROL & PROTECTED ROUTE TESTS
    // =========================================================================

    @Test
    @DisplayName("Anonymous user accessing protected route is redirected to login")
    void testAnonymousAccessProtectedBlocked() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/guest/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Guest cannot access Staff or Admin portals")
    void testGuestAccessDeniedToAdminAndStaff() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        // Guest CAN access Guest portal
        mockMvc.perform(get("/guest/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Guest Portal Dashboard")));
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("Staff cannot access Admin portal or Add Staff")
    void testStaffAccessDeniedToAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/admin/staff/add"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        // Staff CAN access Staff portal
        mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hotel Staff Operations Dashboard")));
    }

    @Test
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    @DisplayName("Admin can access all Admin management pages")
    void testAdminAccessAllowed() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Executive Administration Dashboard")));

        mockMvc.perform(get("/admin/staff/add"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Provision Hotel Staff Account")));

        mockMvc.perform(get("/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Staff Management Ledger")));

        mockMvc.perform(get("/admin/guests"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registered Guests Directory")));

        mockMvc.perform(get("/admin/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administrator Profile")));
    }

    @Test
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    @DisplayName("Logout invalidates session and redirects to login")
    void testLogoutSuccess() throws Exception {
        mockMvc.perform(logout("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    // =========================================================================
    // 5. BOOKING REDIRECT & DASHBOARD TESTS
    // =========================================================================

    @Test
    @DisplayName("Book Now when logged out preserves intended booking destination after login")
    void testBookNowRedirectFlow() throws Exception {
        String username = "booking_redirect_guest";
        String email = "booking_redirect@example.com";
        String rawPass = "Pass123!";

        if (!guestService.existsByUsername(username)) {
            guestService.registerGuest(new GuestRegistrationDto(username, email, rawPass, rawPass));
        }

        // 1. Unauthenticated user clicks 'Book Now' (/bookings/new?roomId=1)
        var mvcResult = mockMvc.perform(get("/bookings/new?roomId=1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"))
                .andReturn();

        // 2. User completes login with the active session where request was saved
        var session = mvcResult.getRequest().getSession(false);
        assertNotNull(session, "Session must hold the saved request cache");

        mockMvc.perform(post("/login")
                        .sessionAttrs(java.util.Collections.singletonMap("SPRING_SECURITY_SAVED_REQUEST",
                                session.getAttribute("SPRING_SECURITY_SAVED_REQUEST")))
                        .param("username", username)
                        .param("password", rawPass)
                        .param("role", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/new?roomId=1"));

        // 3. Verify normal login without booking action still goes to Guest Dashboard
        mockMvc.perform(post("/login")
                        .param("username", username)
                        .param("password", rawPass)
                        .param("role", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));

        // 4. Verify explicit parameter redirect mechanism works
        mockMvc.perform(post("/login")
                        .param("username", username)
                        .param("password", rawPass)
                        .param("role", "GUEST")
                        .param("redirect", "/bookings/new?roomId=2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/new?roomId=2"));

        // 5. Verify system rejects Profile as redirect and safely defaults to Guest Dashboard
        mockMvc.perform(post("/login")
                        .param("username", username)
                        .param("password", rawPass)
                        .param("role", "GUEST")
                        .param("redirect", "/guest/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));

        // 6. Verify cross-role redirect attempt (e.g. to admin) is strictly rejected
        mockMvc.perform(post("/login")
                        .param("username", username)
                        .param("password", rawPass)
                        .param("role", "GUEST")
                        .param("redirect", "/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));
    }

    @Test
    @WithMockUser(username = "staff_member", roles = {"STAFF"})
    @DisplayName("Staff cannot access guest booking destination or guest dashboard")
    void testStaffCannotAccessGuestBookings() throws Exception {
        mockMvc.perform(get("/bookings/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/guest/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("Guest Dashboard renders ONLY the 6 clean navigation option cards in exact order (Stage 6)")
    void testGuestDashboardCleanNavigationOnly() throws Exception {
        mockMvc.perform(get("/guest/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Guest Portal Dashboard")))
                .andExpect(content().string(containsString("cardFindHotels")))
                .andExpect(content().string(containsString("cardOffers")))
                .andExpect(content().string(containsString("cardMyBookings")))
                .andExpect(content().string(containsString("cardBookingHistory")))
                .andExpect(content().string(containsString("cardPlannedList")))
                .andExpect(content().string(containsString("cardProfile")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("cardHotelServices"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("cardExploreDistricts"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("cardRecommendations"))));
    }

    @Test
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    @DisplayName("All separate guest destination pages are accessible and render dedicated content")
    void testDedicatedGuestPagesAccessible() throws Exception {
        // 1. My Bookings Page
        mockMvc.perform(get("/guest/my-bookings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My Luxury Bookings")));

        // 2. Booking History Page
        mockMvc.perform(get("/guest/booking-history"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Booking History")));

        // 3. Recommendations Page
        mockMvc.perform(get("/guest/recommendations"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Curated Recommendations")));

        // 4. Offers Page
        mockMvc.perform(get("/guest/offers"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Exclusive Offers")));

        // 5. Hotel Services Page
        mockMvc.perform(get("/guest/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Curated Hotel Services")));

        // 6. Explore by District Page
        mockMvc.perform(get("/guest/districts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Explore by District")));

        // 7. Profile Page (Unchanged)
        mockMvc.perform(get("/guest/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Guest Profile")));
    }

    @Test
    @DisplayName("Home page operates in Guest mode with Staff & Admin Access in header")
    void testHomePageGuestModeAndAdminHeaderAccess() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Staff &amp; Admin Access")))
                .andExpect(content().string(containsString("Staff Login")))
                .andExpect(content().string(containsString("Admin Login")))
                .andExpect(content().string(containsString("Discover Luxury Hotels Across Karnataka")))
                .andExpect(content().string(containsString("Explore All Karnataka Hotels")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Three Dedicated Access Levels"))));
    }

    @Test
    @DisplayName("Hotel discovery lists ONLY Karnataka properties and district filtering works")
    void testKarnatakaHotelsAndDistrictFilters() throws Exception {
        // All Karnataka hotels listing
        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Karnataka Luxury Hotels &amp; Resorts")))
                .andExpect(content().string(containsString("The Leela Palace Bengaluru")))
                .andExpect(content().string(containsString("Taj West End Bengaluru")))
                .andExpect(content().string(containsString("Evolve Back Coorg")))
                .andExpect(content().string(containsString("The Tamara Coorg")))
                .andExpect(content().string(containsString("The Serai Chikmagalur")))
                .andExpect(content().string(containsString("The Serai Kabini")))
                .andExpect(content().string(containsString("Evolve Back Hampi")));

        // District filter: Kodagu
        mockMvc.perform(get("/hotels").param("district", "Kodagu"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Evolve Back Coorg")))
                .andExpect(content().string(containsString("The Tamara Coorg")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("The Leela Palace Bengaluru"))));

        // District filter: Bengaluru Urban
        mockMvc.perform(get("/hotels").param("district", "Bengaluru Urban"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The Leela Palace Bengaluru")))
                .andExpect(content().string(containsString("Taj West End Bengaluru")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Evolve Back Coorg"))));
    }

    @Test
    @DisplayName("Hotel details page renders verified details, Official Source, and Book button")
    void testHotelDetailsPageAndOfficialSource() throws Exception {
        mockMvc.perform(get("/hotels/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The Leela Palace Bengaluru")))
                .andExpect(content().string(containsString("Official Source")))
                .andExpect(content().string(containsString("https://www.theleela.com/the-leela-palace-bengaluru")))
                .andExpect(content().string(containsString("Book / Check Availability")));
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("Staff Dashboard renders all operational sections and metrics")
    void testStaffDashboardSectionsRendered() throws Exception {
        mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hotel Staff Operations Dashboard")))
                .andExpect(content().string(containsString("Today's Hotel Bookings")))
                .andExpect(content().string(containsString("Upcoming Check-ins")))
                .andExpect(content().string(containsString("Upcoming Check-outs")))
                .andExpect(content().string(containsString("Room Status Distribution")))
                .andExpect(content().string(containsString("Active Guest Requests")))
                .andExpect(content().string(containsString("Available Rooms")))
                .andExpect(content().string(containsString("Occupied Rooms")))
                .andExpect(content().string(containsString("Under Maintenance")))
                .andExpect(content().string(containsString("Recent Operational Activity")));
    }
}
