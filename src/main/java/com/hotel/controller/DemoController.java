package com.hotel.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller handling public prototype demonstration mode.
 * Provides instant, credential-free role exploration for evaluators and presenters
 * while maintaining existing backend authorization and security boundaries.
 */
@Controller
@RequestMapping("/demo")
public class DemoController {

    private final com.hotel.service.DemoDataService demoDataService;
    private final org.springframework.security.web.context.SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public DemoController(com.hotel.service.DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public String demoSelectionPage(Model model) {
        model.addAttribute("pageTitle", "Prototype Demonstration Portal - Royal Indian Stays");
        model.addAttribute("activeNav", "demo");
        return "demo";
    }

    @GetMapping("/guest")
    public String launchGuestDemo(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        authenticateAsDemoUser("guest", "ROLE_GUEST", request, response);
        return "redirect:/guest/dashboard";
    }

    @GetMapping("/staff")
    public String launchStaffDemo(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        authenticateAsDemoUser("staff", "ROLE_STAFF", request, response);
        return "redirect:/";
    }

    @GetMapping("/admin")
    public String launchAdminDemo(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        authenticateAsDemoUser("GAGAN", "ROLE_ADMIN", request, response);
        return "redirect:/";
    }

    private void authenticateAsDemoUser(String username, String role, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
        securityContextRepository.saveContext(context, request, response);
        session.setAttribute("DEMO_MODE", true);
        session.setAttribute("DEMO_ROLE", role);
        demoDataService.resetDemoSession(session);
    }
}
