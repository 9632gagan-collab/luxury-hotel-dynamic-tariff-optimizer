package com.hotel.controller;

import com.hotel.dto.GuestRegistrationDto;
import com.hotel.dto.UserLoginDto;
import com.hotel.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller handling public landing pages, authentication portal, guest registration,
 * and access denied views in Stage 2.
 */
@Controller
public class HomeController {

    private final RoomService roomService;
    private final GuestService guestService;
    private final HotelService hotelService;
    private final com.hotel.security.RateLimiterService rateLimiterService;
    private final StaffService staffService;
    private final AdminService adminService;
    private final PlannedHotelService plannedHotelService;
    private final AdminOperationsService adminOperationsService;
    private final InternalCommunicationService internalCommunicationService;
    private final StaffOperationsService staffOperationsService;
    private final DemoDataService demoDataService;

    @Autowired
    public HomeController(RoomService roomService,
                          GuestService guestService,
                          HotelService hotelService,
                          com.hotel.security.RateLimiterService rateLimiterService,
                          StaffService staffService,
                          AdminService adminService,
                          PlannedHotelService plannedHotelService,
                          AdminOperationsService adminOperationsService,
                          InternalCommunicationService internalCommunicationService,
                          StaffOperationsService staffOperationsService,
                          DemoDataService demoDataService) {
        this.roomService = roomService;
        this.guestService = guestService;
        this.hotelService = hotelService;
        this.rateLimiterService = rateLimiterService;
        this.staffService = staffService;
        this.adminService = adminService;
        this.plannedHotelService = plannedHotelService;
        this.adminOperationsService = adminOperationsService;
        this.internalCommunicationService = internalCommunicationService;
        this.staffOperationsService = staffOperationsService;
        this.demoDataService = demoDataService;
    }

    @GetMapping("/")
    public String home(Model model,
                       java.security.Principal principal,
                       org.springframework.security.core.Authentication authentication,
                       HttpSession session) {

        // Dedicated Lightweight Admin Home (Admins Only)
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            if (demoDataService.isDemoMode(session)) {
                model.addAttribute("pageTitle", "Admin Operations Home (Demo Mode) - Royal Indian Stays");
                model.addAttribute("activeNav", "home");
                model.addAttribute("adminUsername", "Demo Admin");
                model.addAttribute("adminName", "Demo Administrator");
                model.addAttribute("hotelOps", demoDataService.getAdminHotelOperationsOverview(session));
                model.addAttribute("staffOps", demoDataService.getAdminStaffActivityOverview(session));
                model.addAttribute("maintOps", demoDataService.getAdminMaintenanceOverview(session));
                model.addAttribute("staffRequestsCount", 1);
                model.addAttribute("unreadNotificationCount", demoDataService.getAdminNotifications(session).size());
                model.addAttribute("unreadMessageCount", 1);
                return "admin/home";
            }

            String adminUsername = principal != null ? principal.getName() : "ADMIN";
            String adminName = adminService.getAdminByUsername(adminUsername)
                    .map(com.hotel.model.Admin::getFullName)
                    .orElse(adminUsername);

            model.addAttribute("pageTitle", "Admin Operations Home - Royal Indian Stays");
            model.addAttribute("activeNav", "home");
            model.addAttribute("adminUsername", adminUsername);
            model.addAttribute("adminName", adminName);

            // 1. Hotel / Room Operations snapshot (real calculation)
            java.util.Map<String, Object> hotelOps = adminOperationsService.getHotelOperationsOverview();
            model.addAttribute("hotelOps", hotelOps);

            // 2. Staff Activity snapshot (real calculation)
            java.util.Map<String, Object> staffOps = adminOperationsService.getStaffActivityOverview();
            model.addAttribute("staffOps", staffOps);

            // 3. Maintenance snapshot (real calculation)
            java.util.Map<String, Object> maintOps = adminOperationsService.getMaintenanceOverview();
            model.addAttribute("maintOps", maintOps);

            // 4. Pending Staff Requests & Communications
            java.util.List<com.hotel.model.Message> staffRequests = internalCommunicationService.getStaffRequestsForAdmin(adminUsername);
            model.addAttribute("staffRequestsCount", staffRequests != null ? staffRequests.size() : 0);

            long unreadNotifs = internalCommunicationService.getUnreadNotificationCount(adminUsername);
            model.addAttribute("unreadNotificationCount", unreadNotifs);

            java.util.Set<Long> unreadMsgIds = internalCommunicationService.getUnreadMessageIdsForUser(adminUsername);
            model.addAttribute("unreadMessageCount", unreadMsgIds != null ? unreadMsgIds.size() : 0);

            return "admin/home";
        }

        // Dedicated Lightweight Staff Home (Staff Only)
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_STAFF".equals(a.getAuthority()))) {
            if (demoDataService.isDemoMode(session)) {
                model.addAttribute("pageTitle", "Staff Operations Home (Demo Mode) - Royal Indian Stays");
                model.addAttribute("activeNav", "home");
                model.addAttribute("staffUsername", "demo_staff");
                model.addAttribute("staffName", "Demo Staff 01");
                model.addAttribute("ops", demoDataService.getStaffLiveOperationsSummary(session));
                model.addAttribute("unreadNotificationCount", demoDataService.getStaffNotifications(session).size());
                model.addAttribute("unreadMessageCount", 1);
                return "staff/home";
            }

            String staffUsername = principal != null ? principal.getName() : "Staff";
            String staffName = staffService.getStaffByUsername(staffUsername)
                    .map(com.hotel.model.Staff::getFullName)
                    .filter(s -> !s.isBlank())
                    .orElse(staffUsername);

            model.addAttribute("pageTitle", "Staff Operations Home - Royal Indian Stays");
            model.addAttribute("activeNav", "home");
            model.addAttribute("staffUsername", staffUsername);
            model.addAttribute("staffName", staffName);

            // Live Operational Data from StaffOperationsService
            java.util.Map<String, Object> ops = staffOperationsService.getLiveOperationsSummary();
            model.addAttribute("ops", ops);

            long unreadNotifs = internalCommunicationService.getUnreadNotificationCount(staffUsername);
            model.addAttribute("unreadNotificationCount", unreadNotifs);

            java.util.Set<Long> unreadMsgIds = internalCommunicationService.getUnreadMessageIdsForUser(staffUsername);
            model.addAttribute("unreadMessageCount", unreadMsgIds != null ? unreadMsgIds.size() : 0);

            return "staff/home";
        }

        // Standard Guest/Public Home (Unauthenticated, Guests)
        model.addAttribute("pageTitle", "Royal Indian Stays - Discover Luxury Hotels Across India");
        model.addAttribute("featuredRooms", roomService.getAllRooms());
        model.addAttribute("featuredHotels", hotelService.getFeaturedKarnatakaHotels());
        model.addAttribute("allIndiaHotels", hotelService.getFeaturedHotels());
        model.addAttribute("allStates", hotelService.getAllStates());
        model.addAttribute("stateHotelCounts", hotelService.getHotelCountByState());
        model.addAttribute("totalHotels", hotelService.getTotalHotelCount());

        if (demoDataService.isDemoMode(session)) {
            model.addAttribute("plannedHotelIds", demoDataService.getGuestPlannedHotelIds(session));
        } else {
            String username = principal != null ? principal.getName() : null;
            model.addAttribute("plannedHotelIds", plannedHotelService.getPlannedHotelIdsForGuest(username));
        }

        model.addAttribute("activeNav", "home");
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            @RequestParam(value = "role", required = false) String role,
                            @RequestParam(value = "redirect", required = false) String redirect,
                            HttpServletRequest request,
                            Model model) {
        model.addAttribute("pageTitle", "Portal Login - Luxury Hotel System");
        model.addAttribute("loginDto", new UserLoginDto());
        model.addAttribute("selectedRole", role != null ? role.toUpperCase() : "GUEST");

        if (redirect != null && !redirect.isBlank()) {
            model.addAttribute("redirectUrl", redirect.trim());
            HttpSession session = request.getSession(true);
            session.setAttribute("REDIRECT_URL", redirect.trim());
        }

        if (error != null) {
            HttpSession session = request.getSession(false);
            String sessionError = (session != null) ? (String) session.getAttribute("LOGIN_ERROR") : null;
            if (sessionError != null) {
                model.addAttribute("errorMessage", sessionError);
                session.removeAttribute("LOGIN_ERROR");
            } else {
                model.addAttribute("errorMessage", "Invalid credentials. Please verify your details.");
            }
        }

        if (logout != null) {
            model.addAttribute("successMessage", "You have been successfully logged out.");
        }

        if (registered != null) {
            model.addAttribute("successMessage", "Your luxury guest account was created successfully! Please sign in.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("pageTitle", "Guest Registration - Royal Indian Stays");
        model.addAttribute("registrationDto", new GuestRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerGuest(@Valid @ModelAttribute("registrationDto") GuestRegistrationDto registrationDto,
                                BindingResult bindingResult,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        // 1. Check registration rate limiting by client IP
        if (rateLimiterService.isRegistrationThrottled(request.getRemoteAddr())) {
            model.addAttribute("errorMessage", "Too many registration requests. Please wait a moment before trying again.");
            model.addAttribute("pageTitle", "Guest Registration - Royal Indian Stays");
            return "register";
        }
        rateLimiterService.recordRegistrationAttempt(request.getRemoteAddr());

        // 2. Check bean validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Guest Registration - Royal Indian Stays");
            return "register";
        }

        // 3. Check password match
        if (!registrationDto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.registrationDto", "Passwords do not match.");
            model.addAttribute("pageTitle", "Guest Registration - Royal Indian Stays");
            return "register";
        }

        // 4. Check duplicate username across all roles
        if (guestService.existsByUsername(registrationDto.getUsername()) ||
            staffService.existsByUsername(registrationDto.getUsername()) ||
            adminService.existsByUsername(registrationDto.getUsername())) {
            bindingResult.rejectValue("username", "error.registrationDto", "This username is already taken. Please choose another.");
            model.addAttribute("pageTitle", "Guest Registration - Royal Indian Stays");
            return "register";
        }

        // 5. Check duplicate email across existing guests
        if (guestService.existsByEmail(registrationDto.getEmail())) {
            bindingResult.rejectValue("email", "error.registrationDto", "An account with this email address already exists. Please sign in or use another email.");
            model.addAttribute("pageTitle", "Guest Registration - Royal Indian Stays");
            return "register";
        }

        try {
            guestService.registerGuest(registrationDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your luxury guest account was created successfully! Please sign in.");
            return "redirect:/login?registered=true&role=GUEST";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Guest Registration - Royal Indian Stays");
            return "register";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Access Denied - 403 Forbidden");
        return "access-denied";
    }

    @GetMapping("/districts")
    public String districtsRedirect() {
        return "redirect:/guest/districts";
    }

    @GetMapping("/offers")
    public String offersRedirect() {
        return "redirect:/guest/offers";
    }
}
