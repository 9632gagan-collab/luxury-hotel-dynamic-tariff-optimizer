package com.hotel;

import com.hotel.dto.StaffCreateDto;
import com.hotel.model.Admin;
import com.hotel.model.Message;
import com.hotel.model.Staff;
import com.hotel.model.enums.AccountStatus;
import com.hotel.repository.AdminRepository;
import com.hotel.repository.StaffRepository;
import com.hotel.repository.UserRepository;
import com.hotel.service.InternalCommunicationService;
import com.hotel.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class InternalCommunicationFeatureTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffService staffService;

    @Autowired
    private InternalCommunicationService internalCommunicationService;

    private Staff staffA;
    private Staff staffB;
    private Staff staffC;

    @BeforeEach
    void setupTestData() {
        // Ensure GAGAN and DARSHAN exist as active Admins
        if (!adminRepository.existsByUsernameIgnoreCase("GAGAN")) {
            Admin gagan = new Admin("GAGAN", "adminpass");
            gagan.setStatus(AccountStatus.ACTIVE);
            adminRepository.save(gagan);
        }
        if (!adminRepository.existsByUsernameIgnoreCase("DARSHAN")) {
            Admin darshan = new Admin("DARSHAN", "adminpass");
            darshan.setStatus(AccountStatus.ACTIVE);
            adminRepository.save(darshan);
        }

        // Provision Staff A, Staff B, Staff C if they do not already exist
        staffA = staffRepository.findByUsernameIgnoreCase("COMM_STAFF_A").orElseGet(() -> {
            StaffCreateDto dto = new StaffCreateDto("COMM_STAFF_A", "StaffPass123!", "StaffPass123!");
            return staffService.createStaff(dto);
        });

        staffB = staffRepository.findByUsernameIgnoreCase("COMM_STAFF_B").orElseGet(() -> {
            StaffCreateDto dto = new StaffCreateDto("COMM_STAFF_B", "StaffPass123!", "StaffPass123!");
            return staffService.createStaff(dto);
        });

        staffC = staffRepository.findByUsernameIgnoreCase("COMM_STAFF_C").orElseGet(() -> {
            StaffCreateDto dto = new StaffCreateDto("COMM_STAFF_C", "StaffPass123!", "StaffPass123!");
            return staffService.createStaff(dto);
        });
    }

    // =========================================================================
    // 1. ADMIN DASHBOARD & NOTIFICATION BELL
    // =========================================================================

    @Test
    @DisplayName("Admin Dashboard renders notification bell, count, and new communication options")
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    void testAdminDashboardNotificationBellAndOptions() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("btn-notification-bell")))
                .andExpect(content().string(containsString("Notifications")))
                .andExpect(content().string(containsString("Messages")))
                .andExpect(content().string(containsString("Staff Messages / Requests")));
    }

    // =========================================================================
    // 2. ADMIN MESSAGE TO SINGLE STAFF
    // =========================================================================

    @Test
    @DisplayName("Admin sends message to one Staff: recipient receives it, other staff does not, notification count increments")
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    void testAdminSendMessageToSingleStaff() throws Exception {
        long prevUnreadStaffA = internalCommunicationService.getUnreadNotificationCount(staffA.getUsername());
        long prevUnreadStaffB = internalCommunicationService.getUnreadNotificationCount(staffB.getUsername());

        // Admin sends message targeted specifically to Staff A
        mockMvc.perform(post("/admin/messages/new")
                        .with(csrf())
                        .param("staffRecipientIds", String.valueOf(staffA.getId()))
                        .param("subject", "Schedule Shift for Tonight")
                        .param("content", "Please ensure the presidential suite is prepared for high-profile guests."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/sent"))
                .andExpect(flash().attribute("successMessage", "Message sent successfully."));

        // Verify Staff A received the message and notification count increased by 1
        long newUnreadStaffA = internalCommunicationService.getUnreadNotificationCount(staffA.getUsername());
        long newUnreadStaffB = internalCommunicationService.getUnreadNotificationCount(staffB.getUsername());

        assertEquals(prevUnreadStaffA + 1, newUnreadStaffA, "Staff A unread notification count must increment by 1");
        assertEquals(prevUnreadStaffB, newUnreadStaffB, "Staff B unread notification count must remain unchanged");

        // Verify Staff A inbox contains the message
        List<Message> staffAInbox = internalCommunicationService.getInboxMessages(staffA.getUsername());
        assertTrue(staffAInbox.stream().anyMatch(m -> "Schedule Shift for Tonight".equals(m.getSubject())));

        // Verify Staff B inbox does NOT contain the message
        List<Message> staffBInbox = internalCommunicationService.getInboxMessages(staffB.getUsername());
        assertFalse(staffBInbox.stream().anyMatch(m -> "Schedule Shift for Tonight".equals(m.getSubject())));
    }

    // =========================================================================
    // 3. ADMIN MESSAGE TO MULTIPLE STAFF (STAFF A & STAFF C)
    // =========================================================================

    @Test
    @DisplayName("Admin sends message to Staff A & Staff C: both receive it, Staff B does not")
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    void testAdminSendMessageToMultipleStaff() throws Exception {
        String testSubject = "VIP Royal Welcome Protocol - Batch 2026";

        mockMvc.perform(post("/admin/messages/new")
                        .with(csrf())
                        .param("staffRecipientIds", String.valueOf(staffA.getId()), String.valueOf(staffC.getId()))
                        .param("subject", testSubject)
                        .param("content", "All staff on duty please assemble at lobby entrance for state dignitary arrival."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/sent"));

        // Verify Staff A receives it
        List<Message> staffAInbox = internalCommunicationService.getInboxMessages(staffA.getUsername());
        assertTrue(staffAInbox.stream().anyMatch(m -> testSubject.equals(m.getSubject())));

        // Verify Staff C receives it
        List<Message> staffCInbox = internalCommunicationService.getInboxMessages(staffC.getUsername());
        assertTrue(staffCInbox.stream().anyMatch(m -> testSubject.equals(m.getSubject())));

        // Verify Staff B does NOT receive it
        List<Message> staffBInbox = internalCommunicationService.getInboxMessages(staffB.getUsername());
        assertFalse(staffBInbox.stream().anyMatch(m -> testSubject.equals(m.getSubject())));
    }

    // =========================================================================
    // 4. STAFF DASHBOARD, NOTIFICATION BELL & REQUEST TO ALL ACTIVE ADMINS
    // =========================================================================

    @Test
    @DisplayName("Staff Dashboard displays bell and Messages & Requests options")
    @WithMockUser(username = "COMM_STAFF_A", roles = {"STAFF"})
    void testStaffDashboardOptions() throws Exception {
        mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("btn-notification-bell")))
                .andExpect(content().string(containsString("Messages &amp; Requests")))
                .andExpect(content().string(containsString("Send Request to Admin")));
    }

    @Test
    @DisplayName("Staff request form does NOT contain an Admin selection field and sends to ALL active Admins")
    @WithMockUser(username = "COMM_STAFF_A", roles = {"STAFF"})
    void testStaffRequestDeliveredToAllActiveAdmins() throws Exception {
        // Verify Staff request form contains NO Admin dropdown
        mockMvc.perform(get("/staff/messages/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("select name=\"adminId\""))))
                .andExpect(content().string(not(containsString("select name=\"recipientId\""))))
                .andExpect(content().string(containsString("Direct Administrative Broadcast")));

        long prevUnreadGagan = internalCommunicationService.getUnreadNotificationCount("GAGAN");
        long prevUnreadDarshan = internalCommunicationService.getUnreadNotificationCount("DARSHAN");

        String reqSubject = "Request for Shift Change - Next Weekend";

        // Staff sends request
        mockMvc.perform(post("/staff/messages/new")
                        .with(csrf())
                        .param("requestType", "Leave request")
                        .param("subject", reqSubject)
                        .param("content", "Requesting leave of absence due to urgent family commitment."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/messages/sent"))
                .andExpect(flash().attribute("successMessage", "Request sent successfully to all active administrators."));

        // Both GAGAN and DARSHAN receive the notification
        long newUnreadGagan = internalCommunicationService.getUnreadNotificationCount("GAGAN");
        long newUnreadDarshan = internalCommunicationService.getUnreadNotificationCount("DARSHAN");

        assertEquals(prevUnreadGagan + 1, newUnreadGagan, "Admin GAGAN must receive the notification");
        assertEquals(prevUnreadDarshan + 1, newUnreadDarshan, "Admin DARSHAN must receive the notification");

        // Both GAGAN and DARSHAN receive the message in their staff requests view
        List<Message> gaganRequests = internalCommunicationService.getStaffRequestsForAdmin("GAGAN");
        assertTrue(gaganRequests.stream().anyMatch(m -> reqSubject.equals(m.getSubject())));

        List<Message> darshanRequests = internalCommunicationService.getStaffRequestsForAdmin("DARSHAN");
        assertTrue(darshanRequests.stream().anyMatch(m -> reqSubject.equals(m.getSubject())));
    }

    // =========================================================================
    // 5. READ / UNREAD STATUS & OPENING MESSAGES
    // =========================================================================

    @Test
    @DisplayName("Opening a message marks it as read and reduces unread notification count")
    @WithMockUser(username = "COMM_STAFF_A", roles = {"STAFF"})
    void testMessageReadStatusUpdatesOnOpen() throws Exception {
        // Send a message to Staff A directly via service
        Message message = internalCommunicationService.sendAdminMessage(
                "GAGAN",
                List.of(staffA.getId()),
                "Urgent Keycard Inspection",
                "Please audit all master RFID cards by end of day."
        );

        long unreadBefore = internalCommunicationService.getUnreadNotificationCount(staffA.getUsername());
        assertTrue(unreadBefore > 0);

        // Staff opens the message
        mockMvc.perform(get("/staff/messages/" + message.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Urgent Keycard Inspection")))
                .andExpect(content().string(containsString("Please audit all master RFID cards")));

        // Unread count should decrease by 1
        long unreadAfter = internalCommunicationService.getUnreadNotificationCount(staffA.getUsername());
        assertEquals(unreadBefore - 1, unreadAfter);
    }

    @Test
    @DisplayName("Mark all notifications as read resets notification count to zero")
    @WithMockUser(username = "COMM_STAFF_B", roles = {"STAFF"})
    void testMarkAllNotificationsAsRead() throws Exception {
        // Send two messages to Staff B
        internalCommunicationService.sendAdminMessage("GAGAN", List.of(staffB.getId()), "Notice 1", "Content 1");
        internalCommunicationService.sendAdminMessage("GAGAN", List.of(staffB.getId()), "Notice 2", "Content 2");

        assertTrue(internalCommunicationService.getUnreadNotificationCount(staffB.getUsername()) >= 2);

        // Mark all as read
        mockMvc.perform(post("/staff/notifications/mark-all-read").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/notifications"));

        assertEquals(0, internalCommunicationService.getUnreadNotificationCount(staffB.getUsername()));
    }

    // =========================================================================
    // 6. SECURITY & AUTHORIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Security: Staff cannot view another Staff member's private message")
    @WithMockUser(username = "COMM_STAFF_B", roles = {"STAFF"})
    void testStaffCannotViewOtherStaffMessage() throws Exception {
        // Message sent only to Staff A
        Message msgForA = internalCommunicationService.sendAdminMessage(
                "GAGAN",
                List.of(staffA.getId()),
                "Confidential Performance Review",
                "Internal performance discussion."
        );

        // Staff B attempts to access Staff A's message -> redirected to /access-denied
        mockMvc.perform(get("/staff/messages/" + msgForA.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @DisplayName("Security: Staff cannot access Admin message or notification endpoints")
    @WithMockUser(username = "COMM_STAFF_A", roles = {"STAFF"})
    void testStaffCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/admin/messages/inbox"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/admin/messages/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @DisplayName("Security: Guest cannot access Admin or Staff message/notification endpoints")
    @WithMockUser(username = "guest_user", roles = {"GUEST"})
    void testGuestCannotAccessInternalCommunication() throws Exception {
        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/admin/messages/inbox"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/staff/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/staff/messages/inbox"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @DisplayName("Security: Unauthenticated user redirected to login on accessing messages")
    void testUnauthenticatedRedirect() throws Exception {
        mockMvc.perform(get("/admin/messages/inbox"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/staff/messages/inbox"))
                .andExpect(status().is3xxRedirection());
    }

    // =========================================================================
    // 7. INPUT VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Validation: Admin message without recipients or empty content fails validation")
    @WithMockUser(username = "GAGAN", roles = {"ADMIN"})
    void testAdminMessageValidation() throws Exception {
        // Missing recipients and empty subject
        mockMvc.perform(post("/admin/messages/new")
                        .with(csrf())
                        .param("subject", "")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/messages-new"))
                .andExpect(model().attributeHasFieldErrors("messageDto", "staffRecipientIds", "subject", "content"));
    }

    @Test
    @DisplayName("Validation: Staff request without request type or subject fails validation")
    @WithMockUser(username = "COMM_STAFF_A", roles = {"STAFF"})
    void testStaffRequestValidation() throws Exception {
        mockMvc.perform(post("/staff/messages/new")
                        .with(csrf())
                        .param("requestType", "")
                        .param("subject", "")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/messages-new"))
                .andExpect(model().attributeHasFieldErrors("requestDto", "requestType", "subject", "content"));
    }
}
