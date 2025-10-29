package com.gfd_sse.dummyoff2onredis.controller;

import com.gfd_sse.dummyoff2onredis.service.SSEService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    private final SSEService sseService;

    public HealthController(SSEService sseService) {
        this.sseService = sseService;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "dummy-off2on-redis");
        health.put("timestamp", System.currentTimeMillis());
        health.put("activeConnections", sseService.getActiveConnectionCount());
        return ResponseEntity.ok(health);
    }

    /**
     * Welcome endpoint
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> welcome() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to Off2On Redis Subscriber Service");
        response.put("version", "1.0.0");
        response.put("description", "SSE service for GFD PWA with OTP authentication");
        return ResponseEntity.ok(response);
    }
}
