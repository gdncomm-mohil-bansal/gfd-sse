package com.gfd_sse.dummyoff2onredisreplica.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.gfd_sse.dummyoff2onredisreplica.dto.response.ApiResponse;
import com.gfd_sse.dummyoff2onredisreplica.service.CheckoutService;
import com.gfd_sse.dummyoff2onredisreplica.service.SseService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/replica")
@CrossOrigin(origins = "*")
@Slf4j
public class SseController {

  private final SseService sseService;
  private final CheckoutService checkoutService;

  public SseController(SseService sseService, CheckoutService checkoutService) {
    this.sseService = sseService;
    this.checkoutService = checkoutService;
  }

  /**
   * SSE Connection endpoint for testing.
   * Allows clients to connect to THIS specific pod to receive events.
   * This is for testing cross-pod behavior.
   */
  @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<?> connect(@RequestParam String userId) {
    log.info("SSE connection request from user: {} to REPLICA pod", userId);

    try {
      if (userId == null || userId.trim().isEmpty()) {
        log.warn("SSE connection rejected: Missing userId");
        return ResponseEntity.badRequest()
            .body("User ID is required");
      }

      // Check if user already has an active connection on this pod
      if (sseService.hasActiveConnection(userId)) {
        log.info("User {} already has an active connection on THIS pod. Closing old connection.", userId);
        sseService.removeEmitter(userId);
      }

      // Create SSE emitter
      SseEmitter emitter = sseService.createEmitter(userId);

      log.info("SSE connection established successfully for user: {} on REPLICA pod", userId);
      return ResponseEntity.ok(emitter);

    } catch (Exception e) {
      log.error("Error establishing SSE connection for user: {}", userId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to establish SSE connection: " + e.getMessage());
    }
  }

  /**
   * IMPORTANT: This endpoint can ONLY send events to clients connected to THIS
   * pod.
   * If the client is connected to a different pod, the event will NOT reach them.
   * This is a demonstration/test endpoint to understand SSE behavior across pods.
   */
  @PostMapping("/checkout/{userId}")
  public ResponseEntity<ApiResponse<String>> proceedToCheckout(@PathVariable String userId) {
    log.info("Received checkout event for userId: {}", userId);

    try {
      if (sseService.hasActiveConnection(userId)) {
        log.info("User {} IS connected to THIS pod. Sending event directly.", userId);
        checkoutService.sendCheckoutEventToUser(userId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .message("Event sent successfully - User connected to this pod")
            .data("Event delivered to user: " + userId)
            .build());
      } else {
        sseService.createEmitter(userId);
        checkoutService.sendCheckoutEventToUser(userId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .message("Event sent successfully - User connected to this pod")
            .data("Event delivered to user: " + userId)
            .build());
        //
        // log.warn("User {} is NOT connected to THIS pod. Cannot send event.", userId);
        // log.warn("User is either: (1) Connected to a different pod, or (2) Not
        // connected at all");
        //
        // return ResponseEntity.ok(ApiResponse.<String>builder()
        // .success(false)
        // .message("Cannot send event - User not connected to this pod")
        // .data("User " + userId + " is connected to a different pod or not connected")
        // .build());
      }
    } catch (Exception e) {
      log.error("Error processing checkout for user: {}", userId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.<String>builder()
              .success(false)
              .message("Error: " + e.getMessage())
              .build());
    }
  }

  @PostMapping("/disconnect/{userId}")
  public ResponseEntity<String> disconnect(@PathVariable String userId) {
    log.info("Disconnect request for user: {}", userId);

    try {
      sseService.removeEmitter(userId);
      return ResponseEntity.ok("SSE connection closed successfully on REPLICA pod");
    } catch (Exception e) {
      log.error("Error disconnecting user: {}", userId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to disconnect: " + e.getMessage());
    }
  }

  /**
   * Check if a user is connected to THIS pod
   */
  @GetMapping("/status/{userId}")
  public ResponseEntity<ApiResponse<String>> checkStatus(@PathVariable String userId) {
    boolean isConnected = sseService.hasActiveConnection(userId);
    String status = isConnected ? "Connected to THIS replica pod" : "NOT connected to this replica pod";

    return ResponseEntity.ok(ApiResponse.<String>builder()
        .success(true)
        .message(status)
        .data("User: " + userId + ", Connected: " + isConnected)
        .timestamp(System.currentTimeMillis())
        .build());
  }

  /**
   * Get the count of active connections on THIS pod
   */
  @GetMapping("/connections/count")
  public ResponseEntity<ApiResponse<Integer>> getConnectionCount() {
    int count = sseService.getActiveConnectionCount();

    return ResponseEntity.ok(ApiResponse.<Integer>builder()
        .success(true)
        .message("Active connections on REPLICA pod")
        .data(count)
        .timestamp(System.currentTimeMillis())
        .build());
  }
}
