package com.gfd_sse.dummyoff2onredis.controller;

import com.gfd_sse.dummyoff2onredis.dto.ConnectionRequest;
import com.gfd_sse.dummyoff2onredis.dto.ConnectionResponse;
import com.gfd_sse.dummyoff2onredis.service.SSEService;
import com.gfd_sse.dummyoff2onredis.service.api.DeviceConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for SSE (Server-Sent Events) connections
 * Handles connection and disconnection requests from GFD PWA
 */
@RestController @RequestMapping("/api/sse") public class SSEController {

  private static final Logger logger = LoggerFactory.getLogger(SSEController.class);

  private final DeviceConnectionService deviceConnectionService;
  private final SSEService sseService;

  public SSEController(DeviceConnectionService deviceConnectionService, SSEService sseService) {
    this.deviceConnectionService = deviceConnectionService;
    this.sseService = sseService;
  }

  /**
   * Establish SSE connection with device-to-device mapping
   * GFD PWA will connect to this endpoint with destinationId (GFD device) and OTP (optional)
   * <p>
   * Flow 1: With OTP (First-time connection)
   * - GFD provides destinationId and OTP
   * - System validates OTP from MongoDB
   * - Activates device mapping and establishes SSE connection
   * <p>
   * Flow 2: Without OTP (Reconnection)
   * - GFD provides only destinationId
   * - System checks if destinationId exists in active mapping
   * - Establishes SSE connection using existing sourceId
   */
  @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<?> connect(
      @CookieValue(value = "deviceId", required = false) String destinationId,
      @RequestParam(required = false) String otp) {

    logger.info("SSE connection request from GFD device: {} with OTP: {}", destinationId,
        otp != null ? "provided" : "not provided");

    try {
      // Build connection request
      ConnectionRequest request =
          ConnectionRequest.builder().destinationId(destinationId).otp(otp).build();

      // Process connection through service layer
      ConnectionResponse response = deviceConnectionService.connect(request);

      logger.info("SSE connection established successfully. SourceId: {} -> DestinationId: {}. "
              + "Is reconnection: {}", response.getSourceId(), response.getDestinationId(),
          response.isReconnection());

      // Return the emitter with proper SSE headers
      return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
          .header("Cache-Control", "no-cache").header("Connection", "keep-alive")
          .header("X-Accel-Buffering", "no") // Disable nginx buffering
          .body(response.getEmitter());

    } catch (IllegalArgumentException e) {
      logger.warn("SSE connection rejected: {}", e.getMessage());
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      logger.error("Error establishing SSE connection for device: {}", destinationId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to establish SSE connection: " + e.getMessage());
    }
  }

  /**
   * Disconnect SSE connection by deviceId
   */
  @PostMapping("/disconnect") public ResponseEntity<String> disconnect(
      @CookieValue(value = "deviceId", required = false) String destinationId) {
    logger.info("Disconnect request for device: {}", destinationId);

    try {
      boolean disconnected = deviceConnectionService.disconnect(destinationId);

      if (disconnected) {
        logger.info("SSE connection closed successfully for destinationId: {}", destinationId);
        return ResponseEntity.ok("SSE connection closed successfully");
      } else {
        logger.warn("No active connection found for destinationId: {}", destinationId);
        return ResponseEntity.ok("No active connection found");
      }

    } catch (Exception e) {
      logger.error("Error disconnecting device: {}", destinationId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to disconnect: " + e.getMessage());
    }
  }

  /**
   * Check connection status by deviceId
   */
  @GetMapping("/status") public ResponseEntity<String> checkStatus(
      @CookieValue(value = "deviceId", required = false) String destinationId) {

    if (destinationId == null || destinationId.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("Device ID is required");
    }

    boolean isConnected = deviceConnectionService.isConnected(destinationId);
    String status = isConnected ? "Connected" : "Not connected";
    return ResponseEntity.ok(status);
  }

  /**
   * Get active connection count
   */
  @GetMapping("/connections/count") public ResponseEntity<Integer> getConnectionCount() {
    int count = sseService.getActiveConnectionCount();
    return ResponseEntity.ok(count);
  }
}
