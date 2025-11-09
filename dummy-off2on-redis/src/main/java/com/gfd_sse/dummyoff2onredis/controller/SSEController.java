package com.gfd_sse.dummyoff2onredis.controller;

import com.gfd_sse.dummyoff2onredis.model.GfdDeviceMapping;
import com.gfd_sse.dummyoff2onredis.repository.GfdDeviceMappingRepository;
import com.gfd_sse.dummyoff2onredis.service.SSEService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@RestController
@RequestMapping("/api/sse")
public class SSEController {

    private static final Logger logger = LoggerFactory.getLogger(SSEController.class);

    private final SSEService sseService;
    private final GfdDeviceMappingRepository gfdDeviceMappingRepository;

    public SSEController(SSEService sseService, GfdDeviceMappingRepository gfdDeviceMappingRepository) {
        this.sseService = sseService;
        this.gfdDeviceMappingRepository = gfdDeviceMappingRepository;
    }

    /**
     * Establish SSE connection with device-to-device mapping
     * GFD PWA will connect to this endpoint with destinationId (GFD device) and OTP
     * (optional)
     * 
     * Flow 1: With OTP (First-time connection)
     * - GFD provides destinationId and OTP
     * - System validates OTP from MongoDB
     * - Activates device mapping and establishes SSE connection
     * 
     * Flow 2: Without OTP (Reconnection)
     * - GFD provides only destinationId
     * - System checks if destinationId exists in active mapping
     * - Establishes SSE connection using existing sourceId
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> connect(
            @CookieValue(value = "deviceId", required = false) String destinationId,
            @RequestParam(required = false) String otp) {

        logger.info("SSE connection request from GFD device: {} with OTP: {}", destinationId, otp);

        try {
            // Validate destinationId
            if (destinationId == null || destinationId.trim().isEmpty()) {
                logger.warn("SSE connection rejected: Missing destinationId in cookie");
                return ResponseEntity.badRequest()
                        .body("Device ID (destinationId) is required in cookie");
            }

            String sourceId = null;
            GfdDeviceMapping mapping = null;

            // Flow 1: OTP provided - First-time connection
            if (otp != null && !otp.trim().isEmpty()) {
                logger.info("Processing first-time connection with OTP for destinationId: {}", destinationId);

                try {
                    long otpValue = Long.parseLong(otp);

                    // Validate OTP from MongoDB
                    Optional<GfdDeviceMapping> mappingOpt = gfdDeviceMappingRepository.findByOtp(otpValue);

                    if (mappingOpt.isEmpty()) {
                        logger.warn("SSE connection rejected: Invalid or expired OTP");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("Invalid or expired OTP. Please generate a new OTP from Front-liner app.");
                    }

                    mapping = mappingOpt.get();
                    sourceId = mapping.getSourceId();

                    // Update mapping with destination device ID and activate
                    mapping.setDestinationId(destinationId);
                    mapping.setActive(true);
                    // Clear OTP after successful validation (one-time use)
                    mapping.setOtp(0);
                    gfdDeviceMappingRepository.save(mapping);

                    logger.info("Device mapping activated. SourceId: {} connected to DestinationId: {}. OTP cleared.",
                            sourceId, destinationId);

                } catch (NumberFormatException e) {
                    logger.warn("Invalid OTP format: {}", otp);
                    return ResponseEntity.badRequest().body("Invalid OTP format");
                }
            }
            // Flow 2: No OTP - Reconnection with existing device mapping
            else {
                logger.info("Processing reconnection without OTP for destinationId: {}", destinationId);

                // Check if destinationId exists in active mapping
                Optional<GfdDeviceMapping> mappingOpt = gfdDeviceMappingRepository.findByDestinationId(destinationId);

                if (mappingOpt.isEmpty()) {
                    logger.warn("SSE connection rejected: No device mapping found for destinationId: {}",
                            destinationId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("No active device mapping found. Please provide OTP for first-time connection.");
                }

                mapping = mappingOpt.get();

                if (!mapping.isActive()) {
                    logger.warn("SSE connection rejected: Device mapping is not active for destinationId: {}",
                            destinationId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Device mapping is not active. Please reconnect with OTP.");
                }

                sourceId = mapping.getSourceId();
                logger.info("Reconnecting using existing mapping. SourceId: {} -> DestinationId: {}",
                        sourceId, destinationId);
            }

            // Check if sourceId already has an active connection
            if (sseService.hasActiveConnection(sourceId)) {
                logger.info("SourceId {} already has an active connection. Closing old connection.", sourceId);
                sseService.removeEmitter(sourceId);
            }

            // Create SSE emitter for the sourceId
            SseEmitter emitter = sseService.createEmitter(sourceId);

            logger.info("SSE connection established successfully. SourceId: {} -> DestinationId: {}. Returning emitter...",
                    sourceId, destinationId);
            
            // Return the emitter with proper SSE headers
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .header("X-Accel-Buffering", "no") // Disable nginx buffering
                    .body(emitter);

        } catch (Exception e) {
            logger.error("Error establishing SSE connection for device: {}", destinationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to establish SSE connection: " + e.getMessage());
        }
    }

    /**
     * Disconnect SSE connection by deviceId
     */
    @PostMapping("/disconnect")
    public ResponseEntity<String> disconnect(
            @CookieValue(value = "deviceId", required = false) String destinationId) {
        logger.info("Disconnect request for device: {}", destinationId);

        try {
            if (destinationId == null || destinationId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Device ID is required");
            }

            // Find device mapping to get sourceId
            Optional<GfdDeviceMapping> mappingOpt = gfdDeviceMappingRepository.findByDestinationId(destinationId);

            if (mappingOpt.isEmpty()) {
                logger.warn("No device mapping found for destinationId: {}", destinationId);
                return ResponseEntity.ok("No active connection found");
            }

            String sourceId = mappingOpt.get().getSourceId();
            sseService.removeEmitter(sourceId);

            logger.info("SSE connection closed for sourceId: {} (destinationId: {})", sourceId, destinationId);
            return ResponseEntity.ok("SSE connection closed successfully");

        } catch (Exception e) {
            logger.error("Error disconnecting device: {}", destinationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to disconnect: " + e.getMessage());
        }
    }

    /**
     * Check connection status by deviceId
     */
    @GetMapping("/status")
    public ResponseEntity<String> checkStatus(
            @CookieValue(value = "deviceId", required = false) String destinationId) {

        if (destinationId == null || destinationId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Device ID is required");
        }

        // Find device mapping to get sourceId
        Optional<GfdDeviceMapping> mappingOpt = gfdDeviceMappingRepository.findByDestinationId(destinationId);

        if (mappingOpt.isEmpty()) {
            return ResponseEntity.ok("Not connected");
        }

        String sourceId = mappingOpt.get().getSourceId();
        boolean isConnected = sseService.hasActiveConnection(sourceId);
        String status = isConnected ? "Connected" : "Not connected";
        return ResponseEntity.ok(status);
    }

    /**
     * Get active connection count
     */
    @GetMapping("/connections/count")
    public ResponseEntity<Integer> getConnectionCount() {
        int count = sseService.getActiveConnectionCount();
        return ResponseEntity.ok(count);
    }
}
