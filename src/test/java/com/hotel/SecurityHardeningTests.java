package com.hotel;

import com.hotel.dto.GuestRegistrationDto;
import com.hotel.model.Guest;
import com.hotel.repository.GuestRepository;
import com.hotel.security.RateLimiterService;
import com.hotel.service.GuestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityHardeningTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private GuestService guestService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        rateLimiterService.resetForTesting();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        rateLimiterService.resetForTesting();
    }

    // =========================================================================
    // 1. DIRECT GUEST REGISTRATION & IMMEDIATE LOGIN
    // =========================================================================

    @Test
    @DisplayName("Guest registers directly, password is BCrypt hashed, and guest can log in immediately")
    void testGuestRegistrationDirectAndImmediateLogin() throws Exception {
        String testUser = "direct_guest_" + (System.currentTimeMillis() % 100000);
        String testEmail = testUser + "@luxuryresort.test";
        String rawPassword = "SecurePass123!";

        // 1. Perform Guest Registration
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", testUser)
                        .param("fullName", "Direct VIP Guest")
                        .param("email", testEmail)
                        .param("password", rawPassword)
                        .param("confirmPassword", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true&role=GUEST"));

        // 2. Account must exist in DB with BCrypt hashed password
        Guest createdGuest = guestRepository.findByUsernameIgnoreCase(testUser).orElse(null);
        assertNotNull(createdGuest, "Guest account must exist immediately after registration");
        assertEquals(testEmail, createdGuest.getEmail());
        assertTrue(passwordEncoder.matches(rawPassword, createdGuest.getPassword()),
                "BCrypt password must match raw password");
        assertTrue(createdGuest.getPassword().startsWith("$2a$") || createdGuest.getPassword().startsWith("$2b$"),
                "Password must be securely hashed with BCrypt");

        // 3. Newly registered guest can immediately log in successfully
        mockMvc.perform(post("/login")
                        .param("username", testUser)
                        .param("password", rawPassword)
                        .param("role", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));
    }

    @Test
    @DisplayName("Guest registration rejects password confirmation mismatch")
    void testRegistrationPasswordMismatchRejected() throws Exception {
        String testUser = "mismatch_" + (System.currentTimeMillis() % 100000);
        String testEmail = testUser + "@luxuryresort.test";

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", testUser)
                        .param("email", testEmail)
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "DifferentPass456!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Passwords do not match")));

        assertFalse(guestRepository.existsByUsernameIgnoreCase(testUser));
    }

    @Test
    @DisplayName("Guest registration rejects duplicate username")
    void testRegistrationDuplicateUsernameRejected() throws Exception {
        String testUser = "dup_user_" + (System.currentTimeMillis() % 100000);
        String testEmail1 = testUser + "1@luxuryresort.test";
        String testEmail2 = testUser + "2@luxuryresort.test";

        // Register first time
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", testUser)
                        .param("email", testEmail1)
                        .param("password", "Pass123!")
                        .param("confirmPassword", "Pass123!"))
                .andExpect(status().is3xxRedirection());

        // Register second time with duplicate username
        rateLimiterService.resetForTesting();
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", testUser)
                        .param("email", testEmail2)
                        .param("password", "Pass123!")
                        .param("confirmPassword", "Pass123!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This username is already taken")));
    }

    @Test
    @DisplayName("Guest registration rejects duplicate email")
    void testRegistrationDuplicateEmailRejected() throws Exception {
        String testUser1 = "dup_email1_" + (System.currentTimeMillis() % 100000);
        String testUser2 = "dup_email2_" + (System.currentTimeMillis() % 100000);
        String sharedEmail = "shared_" + (System.currentTimeMillis() % 100000) + "@luxuryresort.test";

        // Register first time
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", testUser1)
                        .param("email", sharedEmail)
                        .param("password", "Pass123!")
                        .param("confirmPassword", "Pass123!"))
                .andExpect(status().is3xxRedirection());

        // Register second time with duplicate email
        rateLimiterService.resetForTesting();
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", testUser2)
                        .param("email", sharedEmail)
                        .param("password", "Pass123!")
                        .param("confirmPassword", "Pass123!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("An account with this email address already exists")));
    }

    // =========================================================================
    // 2. SESSION SECURITY & CACHE HEADERS
    // =========================================================================

    @Test
    @DisplayName("Sensitive authenticated responses include no-cache and no-store headers")
    void testAuthenticatedCacheControlHeaders() throws Exception {
        mockMvc.perform(get("/guest/dashboard")
                        .with(user("test_guest").roles("GUEST")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    @DisplayName("Open redirect attack vectors are rejected by safe redirect validator")
    void testOpenRedirectBypassesBlocked() throws Exception {
        String guestName = "open_redir_guest_" + (System.currentTimeMillis() % 10000);
        guestService.registerGuest(new GuestRegistrationDto(guestName, guestName + "@test.com", "SecretPass123!", "SecretPass123!"));

        // 1. Protocol-relative URL //evil.com
        mockMvc.perform(post("/login")
                        .param("username", guestName)
                        .param("password", "SecretPass123!")
                        .param("role", "GUEST")
                        .param("redirect", "//evil.com/phish"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));

        // 2. Backslash bypass /\evil.com
        mockMvc.perform(post("/login")
                        .param("username", guestName)
                        .param("password", "SecretPass123!")
                        .param("role", "GUEST")
                        .param("redirect", "/\\evil.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));

        // 3. Absolute URL http://malicious.com
        mockMvc.perform(post("/login")
                        .param("username", guestName)
                        .param("password", "SecretPass123!")
                        .param("role", "GUEST")
                        .param("redirect", "http://malicious.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guest/dashboard"));
    }

    @Test
    @DisplayName("Brute force login triggers temporary account lockout rate limiting")
    void testBruteForceLoginLockout() throws Exception {
        String testUser = "brute_login_" + (System.currentTimeMillis() % 10000);
        guestService.registerGuest(new GuestRegistrationDto(testUser, testUser + "@test.com", "CorrectPass123!", "CorrectPass123!"));

        // 5 consecutive failed login attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                    .param("username", testUser)
                    .param("password", "WrongPassword")
                    .param("role", "GUEST"));
        }

        // 6th attempt should be blocked by rate limiter
        mockMvc.perform(post("/login")
                        .param("username", testUser)
                        .param("password", "CorrectPass123!") // Even with correct password
                        .param("role", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true&role=GUEST"));

        // Ensure rate limiter reports blocked
        assertTrue(rateLimiterService.isLoginBlocked(testUser));
    }
}
