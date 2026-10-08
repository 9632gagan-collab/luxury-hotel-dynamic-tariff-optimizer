package com.hotel.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe rate limiter and brute-force throttling service.
 * Protects login and registration endpoints against abuse.
 */
@Service
public class RateLimiterService {

    // Maximum failed login attempts allowed before temporary lockout
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final Duration LOGIN_LOCKOUT_DURATION = Duration.ofMinutes(15);

    // Rate limiting tracking maps
    private final Map<String, LoginAttemptRecord> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> registrationRateLimit = new ConcurrentHashMap<>();

    // =========================================================================
    // 1. LOGIN ATTEMPT LIMITING
    // =========================================================================

    public boolean isLoginBlocked(String usernameOrIp) {
        if (usernameOrIp == null || usernameOrIp.isBlank()) {
            return false;
        }
        String key = usernameOrIp.trim().toLowerCase();
        LoginAttemptRecord record = loginAttempts.get(key);
        if (record == null) {
            return false;
        }
        if (record.isLocked()) {
            if (Instant.now().isAfter(record.getLockoutExpiresAt())) {
                // Lockout expired, clear attempts
                loginAttempts.remove(key);
                return false;
            }
            return true;
        }
        return false;
    }

    public void recordLoginFailure(String usernameOrIp) {
        if (usernameOrIp == null || usernameOrIp.isBlank()) {
            return;
        }
        String key = usernameOrIp.trim().toLowerCase();
        loginAttempts.compute(key, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || now.isAfter(existing.getFirstAttemptAt().plus(LOGIN_LOCKOUT_DURATION))) {
                return new LoginAttemptRecord(1, now, false, null);
            }
            int newCount = existing.getAttempts() + 1;
            boolean locked = newCount >= MAX_LOGIN_ATTEMPTS;
            Instant lockExpires = locked ? now.plus(LOGIN_LOCKOUT_DURATION) : null;
            return new LoginAttemptRecord(newCount, existing.getFirstAttemptAt(), locked, lockExpires);
        });
    }

    public void recordLoginSuccess(String usernameOrIp) {
        if (usernameOrIp != null) {
            loginAttempts.remove(usernameOrIp.trim().toLowerCase());
        }
    }

    // =========================================================================
    // 2. REGISTRATION CLIENT-IP THROTTLING
    // =========================================================================

    public boolean isRegistrationThrottled(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        Instant lastAttempt = registrationRateLimit.get(clientIp);
        if (lastAttempt != null && Instant.now().isBefore(lastAttempt.plusSeconds(3))) {
            return true; // Enforce at least 3 seconds between continuous registration requests from same IP
        }
        return false;
    }

    public void recordRegistrationAttempt(String clientIp) {
        if (clientIp != null && !clientIp.isBlank()) {
            registrationRateLimit.put(clientIp, Instant.now());
        }
    }

    public void resetForTesting() {
        loginAttempts.clear();
        registrationRateLimit.clear();
    }

    // =========================================================================
    // INTERNAL RECORD
    // =========================================================================

    private static class LoginAttemptRecord {
        private final int attempts;
        private final Instant firstAttemptAt;
        private final boolean locked;
        private final Instant lockoutExpiresAt;

        public LoginAttemptRecord(int attempts, Instant firstAttemptAt, boolean locked, Instant lockoutExpiresAt) {
            this.attempts = attempts;
            this.firstAttemptAt = firstAttemptAt;
            this.locked = locked;
            this.lockoutExpiresAt = lockoutExpiresAt;
        }

        public int getAttempts() {
            return attempts;
        }

        public Instant getFirstAttemptAt() {
            return firstAttemptAt;
        }

        public boolean isLocked() {
            return locked;
        }

        public Instant getLockoutExpiresAt() {
            return lockoutExpiresAt;
        }
    }
}
