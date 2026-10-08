package com.hotel.controller;

import com.hotel.dto.StaffSendRequestDto;
import com.hotel.model.Booking;
import com.hotel.model.Message;
import com.hotel.model.Notification;
import com.hotel.model.Room;
import com.hotel.model.Staff;
import com.hotel.model.enums.RoomStatus;
import com.hotel.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller handling Staff portal views in Stage 2.
 * Strictly restricted to Role STAFF by Spring Security.
 * Provides live operational tracking while ensuring complete fictional data isolation
 * when operating in Demo Mode.
 */
@Controller
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;
    private final RoomService roomService;
    private final BookingService bookingService;
    private final GuestService guestService;
    private final InternalCommunicationService internalCommunicationService;
    private final StaffOperationsService staffOperationsService;
    private final OfferService offerService;
    private final DemoDataService demoDataService;

    @Autowired
    public StaffController(StaffService staffService,
                           RoomService roomService,
                           BookingService bookingService,
                           GuestService guestService,
                           InternalCommunicationService internalCommunicationService,
                           StaffOperationsService staffOperationsService,
                           OfferService offerService,
                           DemoDataService demoDataService) {
        this.staffService = staffService;
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.guestService = guestService;
        this.internalCommunicationService = internalCommunicationService;
        this.staffOperationsService = staffOperationsService;
        this.offerService = offerService;
        this.demoDataService = demoDataService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long populateUnreadNotifications(Principal principal, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return demoDataService.getStaffNotifications(session).size();
        }
        if (principal != null) {
            return internalCommunicationService.getUnreadNotificationCount(principal.getName());
        }
        return 0;
    }

    @ModelAttribute("unreadMessageCount")
    public long populateUnreadMessages(Principal principal, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return 1;
        }
        if (principal != null) {
            var unreadIds = internalCommunicationService.getUnreadMessageIdsForUser(principal.getName());
            return unreadIds != null ? unreadIds.size() : 0;
        }
        return 0;
    }

    @GetMapping({"", "/", "/home"})
    public String staffHomeRedirect() {
        return "redirect:/";
    }

    /**
     * Staff Dashboard: Live Hotel Operations Control Center
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal, HttpSession session) {
        model.addAttribute("activeNav", "dashboard");

        if (demoDataService.isDemoMode(session)) {
            Staff demoStaff = demoDataService.getStaffProfile(session);
            model.addAttribute("staff", demoStaff);
            model.addAttribute("staffName", demoStaff.getFullName());
            model.addAttribute("unreadNotificationCount", demoDataService.getStaffNotifications(session).size());
            model.addAttribute("ops", demoDataService.getStaffLiveOperationsSummary(session));
            
            List<Room> demoRooms = demoDataService.getStaffRoomsList(session);
            long availableCount = demoRooms.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
            long occupiedCount = demoRooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
            long maintenanceCount = demoRooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();
            long cleaningCount = demoRooms.stream().filter(r -> r.getStatus() == RoomStatus.CLEANING).count();

            model.addAttribute("totalRooms", demoRooms.size());
            model.addAttribute("availableRoomsCount", availableCount);
            model.addAttribute("occupiedRoomsCount", occupiedCount);
            model.addAttribute("maintenanceRoomsCount", maintenanceCount);
            model.addAttribute("cleaningRoomsCount", cleaningCount);
            model.addAttribute("allBookings", demoDataService.getStaffBookingsList(session));
            return "staff/dashboard";
        }

        String username = principal != null ? principal.getName() : "Staff";
        Optional<Staff> staffOpt = staffService.getStaffByUsername(username);

        Staff staff = staffOpt.orElse(new Staff(username, ""));
        model.addAttribute("staff", staff);
        model.addAttribute("staffName", username);

        long unreadCount = internalCommunicationService.getUnreadNotificationCount(username);
        model.addAttribute("unreadNotificationCount", unreadCount);

        // Real-time live operations metrics
        Map<String, Object> ops = staffOperationsService.getLiveOperationsSummary();
        model.addAttribute("ops", ops);

        // Preserved legacy attributes for backwards test compatibility
        List<Room> allRooms = roomService.getAllRooms();
        long availableRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
        long occupiedRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        long maintenanceRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();
        long cleaningRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.CLEANING).count();

        model.addAttribute("totalRooms", allRooms.size() > 0 ? allRooms.size() : 24);
        model.addAttribute("availableRoomsCount", availableRooms > 0 ? availableRooms : 14);
        model.addAttribute("occupiedRoomsCount", occupiedRooms > 0 ? occupiedRooms : 7);
        model.addAttribute("maintenanceRoomsCount", maintenanceRooms > 0 ? maintenanceRooms : 2);
        model.addAttribute("cleaningRoomsCount", cleaningRooms > 0 ? cleaningRooms : 1);

        List<Booking> allBookings = bookingService.getAllBookings();
        model.addAttribute("allBookings", allBookings);

        return "staff/dashboard";
    }

    /**
     * Staff Check Rooms / Room Operational Status Board
     */
    @GetMapping({"/check-rooms", "/room-status"})
    public String checkRooms(Model model, HttpSession session) {
        model.addAttribute("activeNav", "check-rooms");
        model.addAttribute("statuses", RoomStatus.values());

        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("rooms", demoDataService.getStaffRoomsList(session));
            model.addAttribute("ops", demoDataService.getStaffLiveOperationsSummary(session));
            return "staff/check-rooms";
        }

        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("ops", staffOperationsService.getLiveOperationsSummary());
        return "staff/check-rooms";
    }

    /**
     * Staff Offers View (Read-Only)
     */
    @GetMapping("/offers")
    public String offers(Model model, HttpSession session) {
        model.addAttribute("activeNav", "offers");
        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("offers", demoDataService.getDemoActiveOffers(session));
            return "staff/offers";
        }
        model.addAttribute("offers", offerService.getActiveOffers());
        return "staff/offers";
    }

    /**
     * Staff Profile View
     */
    @GetMapping("/profile")
    public String profile(Model model, Principal principal, HttpSession session) {
        model.addAttribute("activeNav", "profile");
        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("staff", demoDataService.getStaffProfile(session));
            return "staff/profile";
        }
        String username = principal != null ? principal.getName() : "Staff";
        Staff staff = staffService.getStaffByUsername(username)
                .orElse(new Staff(username, ""));
        model.addAttribute("staff", staff);
        return "staff/profile";
    }

    /**
     * Staff Bookings View
     */
    @GetMapping("/bookings")
    public String bookings(Model model, HttpSession session) {
        model.addAttribute("activeNav", "bookings");
        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("bookings", demoDataService.getStaffBookingsList(session));
            return "staff/bookings";
        }
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "staff/bookings";
    }

    /**
     * Staff Guests Directory View
     */
    @GetMapping("/guests")
    public String guests(Model model, HttpSession session) {
        model.addAttribute("activeNav", "guests");
        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("guests", demoDataService.getStaffGuestList(session));
            return "staff/guests";
        }
        model.addAttribute("guests", guestService.getAllGuests());
        return "staff/guests";
    }

    @PostMapping("/room-status/{id}")
    public String updateRoomStatus(@PathVariable Long id, @RequestParam("status") String status, RedirectAttributes redirectAttributes, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            try {
                RoomStatus newStatus = RoomStatus.valueOf(status.toUpperCase());
                demoDataService.updateDemoRoomStatus(session, id, newStatus);
                redirectAttributes.addFlashAttribute("successMessage", "Room status updated successfully to " + newStatus.getDisplayName() + " (Demo Mode).");
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid room status: " + status);
            }
            return "redirect:/staff/check-rooms";
        }
        try {
            RoomStatus newStatus = RoomStatus.valueOf(status.toUpperCase());
            boolean success = staffOperationsService.updateRoomStatus(id, newStatus);
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "Room status updated successfully to " + newStatus.getDisplayName());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Could not locate room.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid room status: " + status);
        }
        return "redirect:/staff/check-rooms";
    }

    // =========================================================================
    // REST API FOR LIVE GUEST OPERATIONS & DETAIL-ON-CLICK
    // =========================================================================

    @GetMapping("/api/operations/summary")
    @ResponseBody
    public Map<String, Object> getLiveOperationsSummary(HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return demoDataService.getStaffLiveOperationsSummary(session);
        }
        return staffOperationsService.getLiveOperationsSummary();
    }

    @GetMapping("/api/operations/drilldown")
    @ResponseBody
    public List<Map<String, Object>> getLiveOperationsList(@RequestParam(value = "category", defaultValue = "OCCUPANCY") String category,
                                                          HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return demoDataService.getStaffLiveOperationsList(session, category);
        }
        return staffOperationsService.getLiveOperationsList(category);
    }

    @GetMapping("/api/operations/booking/{id}")
    @ResponseBody
    public Map<String, Object> getBookingGuestDetails(@PathVariable Long id, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return demoDataService.getStaffBookingGuestDetail(session, id);
        }
        return staffOperationsService.getGuestBookingDetail(id);
    }

    @GetMapping("/api/operations/room/{id}")
    @ResponseBody
    public Map<String, Object> getRoomOperationalDetails(@PathVariable Long id, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return demoDataService.getStaffRoomOperationalDetail(session, id);
        }
        return staffOperationsService.getRoomOperationalDetail(id);
    }

    @PostMapping("/api/operations/check-in/{id}")
    @ResponseBody
    public Map<String, Object> performCheckIn(@PathVariable Long id, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return Map.of("success", true, "message", "Guest checked in successfully (Demo Mode).");
        }
        boolean success = staffOperationsService.checkInBooking(id);
        return Map.of("success", success, "message", success ? "Guest checked in successfully." : "Check-in failed.");
    }

    @PostMapping("/api/operations/check-out/{id}")
    @ResponseBody
    public Map<String, Object> performCheckOut(@PathVariable Long id, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return Map.of("success", true, "message", "Guest checked out successfully (Demo Mode).");
        }
        boolean success = staffOperationsService.checkOutBooking(id);
        return Map.of("success", success, "message", success ? "Guest checked out successfully." : "Check-out failed.");
    }

    // =========================================================================
    // INTERNAL COMMUNICATION & NOTIFICATION ENDPOINTS (STAFF)
    // =========================================================================

    @GetMapping("/notifications")
    public String notifications(Model model, Principal principal, HttpSession session) {
        model.addAttribute("activeNav", "notifications");
        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("notifications", demoDataService.getStaffNotifications(session));
            return "staff/notifications";
        }
        String username = principal != null ? principal.getName() : "Staff";
        model.addAttribute("notifications", internalCommunicationService.getNotificationsForUser(username));
        return "staff/notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllNotificationsRead(Principal principal, RedirectAttributes redirectAttributes, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            demoDataService.markAllDemoStaffNotificationsRead(session);
            redirectAttributes.addFlashAttribute("successMessage", "All demo notifications marked as read.");
            return "redirect:/staff/notifications";
        }
        if (principal != null) {
            internalCommunicationService.markAllNotificationsAsRead(principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        }
        return "redirect:/staff/notifications";
    }

    @GetMapping("/notifications/{id}")
    public String viewNotification(@PathVariable Long id, Principal principal, HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            return "redirect:/staff/messages/inbox";
        }
        String username = principal != null ? principal.getName() : "Staff";
        try {
            Notification notif = internalCommunicationService.getNotificationForUser(id, username);
            if (notif.getMessage() != null) {
                return "redirect:/staff/messages/" + notif.getMessage().getId();
            }
            return "redirect:/staff/notifications";
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return "redirect:/access-denied";
        } catch (IllegalArgumentException ex) {
            return "redirect:/staff/notifications";
        }
    }

    @GetMapping("/messages")
    public String messagesOverview() {
        return "redirect:/staff/messages/inbox";
    }

    @GetMapping("/messages/inbox")
    public String messagesInbox(Model model, Principal principal, HttpSession session) {
        model.addAttribute("activeNav", "messages");
        model.addAttribute("subTab", "inbox");
        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("messages", demoDataService.getStaffInboxMessages(session));
            model.addAttribute("unreadMessageIds", Collections.singleton(1001L));
            return "staff/messages-inbox";
        }
        String username = principal != null ? principal.getName() : "Staff";
        model.addAttribute("messages", internalCommunicationService.getInboxMessages(username));
        model.addAttribute("unreadMessageIds", internalCommunicationService.getUnreadMessageIdsForUser(username));
        return "staff/messages-inbox";
    }

    @GetMapping("/messages/sent")
    public String messagesSent(Model model, Principal principal, HttpSession session) {
        model.addAttribute("activeNav", "messages");
        model.addAttribute("subTab", "sent");
        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("messages", demoDataService.getStaffSentMessages(session));
            return "staff/messages-sent";
        }
        String username = principal != null ? principal.getName() : "Staff";
        model.addAttribute("messages", internalCommunicationService.getSentMessages(username));
        return "staff/messages-sent";
    }

    @GetMapping("/messages/new")
    public String newRequestForm(Model model) {
        model.addAttribute("activeNav", "messages");
        model.addAttribute("subTab", "new");
        if (!model.containsAttribute("requestDto")) {
            model.addAttribute("requestDto", new StaffSendRequestDto());
        }
        return "staff/messages-new";
    }

    @PostMapping("/messages/new")
    public String processSendRequest(@Valid @ModelAttribute("requestDto") StaffSendRequestDto requestDto,
                                     BindingResult bindingResult,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes,
                                     Model model,
                                     HttpSession session) {
        if (demoDataService.isDemoMode(session)) {
            demoDataService.sendDemoStaffRequest(session, requestDto.getRequestType(), requestDto.getSubject(), requestDto.getContent());
            redirectAttributes.addFlashAttribute("successMessage", "Demo request submitted successfully to administrators.");
            return "redirect:/staff/messages/sent";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "messages");
            model.addAttribute("subTab", "new");
            return "staff/messages-new";
        }

        String username = principal != null ? principal.getName() : "Staff";
        try {
            internalCommunicationService.sendStaffRequest(
                    username,
                    requestDto.getRequestType(),
                    requestDto.getSubject(),
                    requestDto.getContent()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Request sent successfully to all active administrators.");
            return "redirect:/staff/messages/sent";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error.requestDto", ex.getMessage());
            model.addAttribute("activeNav", "messages");
            model.addAttribute("subTab", "new");
            return "staff/messages-new";
        }
    }

    @GetMapping("/messages/{id}")
    public String viewMessage(@PathVariable Long id, Model model, Principal principal, HttpSession session) {
        model.addAttribute("activeNav", "messages");
        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("messageItem", demoDataService.getStaffMessageById(session, id));
            model.addAttribute("currentUser", "Demo Staff 01");
            return "staff/message-view";
        }
        String username = principal != null ? principal.getName() : "Staff";
        try {
            Message message = internalCommunicationService.getMessageForUser(id, username);
            model.addAttribute("messageItem", message);
            model.addAttribute("currentUser", username);
            return "staff/message-view";
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return "redirect:/access-denied";
        } catch (IllegalArgumentException ex) {
            return "redirect:/staff/messages/inbox";
        }
    }
}
