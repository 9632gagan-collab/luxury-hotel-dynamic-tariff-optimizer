package com.hotel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles authentication failures with user-friendly error messages stored in session.
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final RateLimiterService rateLimiterService;

    @Autowired
    public CustomAuthenticationFailureHandler(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String clientIp = request.getRemoteAddr();

        rateLimiterService.recordLoginFailure(username);
        rateLimiterService.recordLoginFailure(clientIp);

        HttpSession session = request.getSession(true);
        String errorMessage;
        if (exception instanceof org.springframework.security.authentication.LockedException) {
            errorMessage = "Too many failed login attempts. Please wait 15 minutes before trying again.";
        } else if (exception instanceof org.springframework.security.authentication.DisabledException) {
            errorMessage = "This account is inactive. Please contact hotel administration.";
        } else if (exception.getMessage() != null && exception.getMessage().contains("Unknown Admin username")) {
            errorMessage = "Unknown Admin username. Only authorized administrators may log in.";
        } else if (exception.getMessage() != null && exception.getMessage().contains("Unknown Staff username")) {
            errorMessage = "Unknown Staff username. Staff accounts must be created by Admin.";
        } else {
            errorMessage = "Invalid credentials. Please verify your details and password.";
        }

        session.setAttribute("LOGIN_ERROR", errorMessage);

        String role = request.getParameter("role");
        String redirectUrl = request.getContextPath() + "/login?error=true";
        if (role != null && !role.isBlank()) {
            redirectUrl += "&role=" + java.net.URLEncoder.encode(role.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }

        String redirect = request.getParameter("redirect");
        if (redirect == null || redirect.isBlank()) {
            redirect = request.getParameter("returnUrl");
        }
        if (redirect != null && !redirect.isBlank()) {
            redirectUrl += "&redirect=" + java.net.URLEncoder.encode(redirect.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }

        response.sendRedirect(redirectUrl);
    }
}
