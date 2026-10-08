package com.hotel.security;

import com.hotel.model.Admin;
import com.hotel.model.Guest;
import com.hotel.model.Staff;
import com.hotel.model.enums.AccountStatus;
import com.hotel.repository.AdminRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.StaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Custom AuthenticationProvider supporting multi-role authentication:
 * - Guest: allows Username or Email + Password
 * - Staff: allows Staff Name + Password (only Admin-created accounts)
 * - Admin: allows only GAGAN or DARSHAN + Password
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;
    private final GuestRepository guestRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;
    private final HttpServletRequest request;

    @Autowired
    public CustomAuthenticationProvider(AdminRepository adminRepository,
                                        StaffRepository staffRepository,
                                        GuestRepository guestRepository,
                                        PasswordEncoder passwordEncoder,
                                        RateLimiterService rateLimiterService,
                                        HttpServletRequest request) {
        this.adminRepository = adminRepository;
        this.staffRepository = staffRepository;
        this.guestRepository = guestRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiterService = rateLimiterService;
        this.request = request;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName() != null ? authentication.getName().trim() : "";
        String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";

        if (username.isEmpty() || password.isEmpty()) {
            throw new BadCredentialsException("Username and Password are required.");
        }

        String clientIp = request != null ? request.getRemoteAddr() : "";
        if (rateLimiterService.isLoginBlocked(username) || rateLimiterService.isLoginBlocked(clientIp)) {
            throw new org.springframework.security.authentication.LockedException("Too many failed login attempts. Please wait 15 minutes before trying again.");
        }

        String role = request.getParameter("role");
        if (role != null) {
            role = role.trim().toUpperCase();
        }

        Authentication authResult;
        // 1. Explicit Role Selected: ADMIN
        if ("ADMIN".equals(role)) {
            authResult = authenticateAdmin(username, password);
        } else if ("STAFF".equals(role)) {
            authResult = authenticateStaff(username, password);
        } else if ("GUEST".equals(role)) {
            authResult = authenticateGuest(username, password);
        } else {
            // Fallback: If no role parameter was sent, detect by priority
            if ("GAGAN".equalsIgnoreCase(username) || "DARSHAN".equalsIgnoreCase(username)) {
                authResult = authenticateAdmin(username, password);
            } else {
                Optional<Staff> staffOpt = staffRepository.findByUsernameIgnoreCase(username);
                if (staffOpt.isPresent()) {
                    authResult = authenticateStaff(username, password);
                } else {
                    authResult = authenticateGuest(username, password);
                }
            }
        }

        // On successful authentication, reset failed attempts
        rateLimiterService.recordLoginSuccess(username);
        rateLimiterService.recordLoginSuccess(clientIp);

        return authResult;
    }

    private Authentication authenticateAdmin(String username, String password) {
        // Enforce: Only GAGAN and DARSHAN are valid Admin usernames
        if (!"GAGAN".equalsIgnoreCase(username) && !"DARSHAN".equalsIgnoreCase(username)) {
            throw new BadCredentialsException("Unknown Admin username. Only authorized administrators may log in.");
        }

        Admin admin = adminRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BadCredentialsException("Unknown Admin username."));

        if (!admin.isActive() || admin.getStatus() == AccountStatus.INACTIVE) {
            throw new DisabledException("This Admin account has been deactivated.");
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new BadCredentialsException("Invalid Admin password.");
        }

        return new UsernamePasswordAuthenticationToken(
                admin.getUsername(),
                admin.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private Authentication authenticateStaff(String username, String password) {
        Staff staff = staffRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BadCredentialsException("Unknown Staff username. Staff accounts must be created by Admin."));

        if (!staff.isActive() || staff.getStatus() == AccountStatus.INACTIVE) {
            throw new DisabledException("This Staff account is inactive. Please contact the Hotel Administrator.");
        }

        if (!passwordEncoder.matches(password, staff.getPassword())) {
            throw new BadCredentialsException("Invalid Staff password.");
        }

        return new UsernamePasswordAuthenticationToken(
                staff.getUsername(),
                staff.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );
    }

    private Authentication authenticateGuest(String usernameOrEmail, String password) {
        Guest guest = guestRepository.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> guestRepository.findByEmailIgnoreCase(usernameOrEmail))
                .orElseThrow(() -> new BadCredentialsException("Invalid Guest username or email."));

        if (!guest.isActive() || guest.getStatus() == AccountStatus.INACTIVE) {
            throw new DisabledException("This Guest account has been deactivated.");
        }

        if (!passwordEncoder.matches(password, guest.getPassword())) {
            throw new BadCredentialsException("Invalid password. Please check your credentials.");
        }

        return new UsernamePasswordAuthenticationToken(
                guest.getUsername(),
                guest.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
