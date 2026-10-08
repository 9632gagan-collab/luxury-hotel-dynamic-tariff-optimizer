package com.hotel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Lightweight, secure Health Check Endpoint for Railway, Docker, and uptime monitoring.
 * Maps both /health and /actuator/health without exposing sensitive internals.
 */
@RestController
public class HealthCheckController {

    private final Instant startTime = Instant.now();

    @GetMapping({"/health", "/actuator/health"})
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Royal Indian Stays",
            "uptimeSeconds", Duration.between(startTime, Instant.now()).getSeconds()
        ));
    }
}
