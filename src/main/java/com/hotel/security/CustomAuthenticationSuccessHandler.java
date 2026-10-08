package com.hotel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Directs authenticated users to their designated role portal or intended booking destination.
 *
 * Implements Stage 2 Redirect Rules:
 * 1. Admin Login -> Admin Dashboard (/admin/dashboard)
 * 2. Staff Login -> Staff Dashboard (/staff/dashboard)
 * 3. Guest Login after clicking 'Book Now' -> Return to original booking destination (e.g. /bookings/new?roomId=...)
 * 4. Normal Guest Login -> Guest Dashboard (/guest/dashboard) - NEVER Profile
 * 5. Prevents unauthorized redirects to Admin or Staff portals
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RateLimiterService rateLimiterService;

    public CustomAuthenticationSuccessHandler(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("LOGIN_ERROR");
        }

        // Clear failed login attempts upon successful authentication
        rateLimiterService.recordLoginSuccess(authentication.getName());
        rateLimiterService.recordLoginSuccess(request.getRemoteAddr());

        boolean isAdmin = false;
        boolean isStaff = false;
        boolean isGuest = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                isAdmin = true;
            } else if ("ROLE_STAFF".equals(role)) {
                isStaff = true;
            } else if ("ROLE_GUEST".equals(role)) {
                isGuest = true;
            }
        }

        // 1. ADMIN Portal
        if (isAdmin) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            return;
        }

        // 2. STAFF Portal
        if (isStaff) {
            response.sendRedirect(request.getContextPath() + "/staff/dashboard");
            return;
        }

        // 3. GUEST Portal: Check for saved booking redirect first!
        if (isGuest) {
            String targetUrl = null;

            // Check explicit parameter 'redirect' or 'returnUrl'
            String paramRedirect = request.getParameter("redirect");
            if (paramRedirect == null || paramRedirect.isBlank()) {
                paramRedirect = request.getParameter("returnUrl");
            }
            if ((paramRedirect == null || paramRedirect.isBlank()) && session != null) {
                paramRedirect = (String) session.getAttribute("REDIRECT_URL");
                session.removeAttribute("REDIRECT_URL");
            }

            if (paramRedirect != null && isSafeGuestRedirect(paramRedirect)) {
                targetUrl = paramRedirect;
            }

            // If no valid parameter redirect, check Spring Security's saved request cache
            if (targetUrl == null) {
                SavedRequest savedRequest = requestCache.getRequest(request, response);
                if (savedRequest != null) {
                    String savedUrl = savedRequest.getRedirectUrl();
                    if (savedUrl != null) {
                        String contextPath = request.getContextPath();
                        String relativePath = extractRelativePath(savedUrl, contextPath);
                        if (isSafeGuestRedirect(relativePath)) {
                            targetUrl = relativePath;
                            requestCache.removeRequest(request, response);
                        }
                    }
                }
            }

            // Normal guest login fallback (NEVER redirect to Profile!)
            if (targetUrl == null || targetUrl.isBlank()) {
                targetUrl = "/guest/dashboard";
            }

            response.sendRedirect(request.getContextPath() + targetUrl);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/");
    }

    /**
     * Validates that the redirect destination is a safe guest route.
     * Prevents open-redirect attacks and cross-role privilege escalations.
     */
    private boolean isSafeGuestRedirect(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        // Disallow open redirects / protocol-relative URLs / backslash bypass
        if (!url.startsWith("/") || url.startsWith("//") || url.startsWith("/\\") || url.contains("\\")) {
            return false;
        }
        // Strictly prevent unauthorized access to Admin or Staff portals
        if (url.startsWith("/admin") || url.startsWith("/staff")) {
            return false;
        }
        // Do NOT redirect to Profile upon booking action
        if (url.equals("/guest/profile") || url.startsWith("/guest/profile?")) {
            return false;
        }
        // Allowed paths for Guest workflows
        return url.startsWith("/bookings") || url.startsWith("/guest") || url.startsWith("/rooms") || url.startsWith("/hotels");
    }

    private String extractRelativePath(String fullUrl, String contextPath) {
        try {
            URI uri = new URI(fullUrl);
            String path = uri.getPath();
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                String query = uri.getQuery();
                // Strip internal Spring Security 'continue' parameter if present
                query = query.replaceAll("(?i)(&|^)continue(=[^&]*)?", "");
                if (query.startsWith("&")) {
                    query = query.substring(1);
                }
                if (!query.isBlank()) {
                    path += "?" + query;
                }
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }
}
