package com.gfd_sse.dummyoff2onredisreplica.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gfd_sse.dummyoff2onredisreplica.event.CartEvent;
import com.gfd_sse.dummyoff2onredisreplica.event.EventType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CheckoutService {

  private final ObjectMapper objectMapper;
  private final SseService sseService;

  public CheckoutService(ObjectMapper objectMapper, SseService sseService) {
    this.objectMapper = objectMapper;
    this.sseService = sseService;
  }

  /**
   * Send a checkout event directly to a user.
   * This only works if the user is connected to THIS pod instance.
   */
  public void sendCheckoutEventToUser(String userId) {
    log.info("Creating and sending checkout event for user: {}", userId);

    // Create a test checkout event
    CartEvent event = CartEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType(EventType.CHECKOUT_INITIATED)
        .userId(userId)
        .timestamp(System.currentTimeMillis())
        .message("Checkout initiated from replica service - Testing cross-pod communication")
        .build();

    // Send event to the user if they have an active SSE connection on THIS pod
    if (sseService.hasActiveConnection(userId)) {
      sseService.sendEventToUser(userId, event);
      log.info("Successfully sent checkout event to user: {}", userId);
    } else {
      log.warn("User {} has no active SSE connection on THIS pod. Event not sent.", userId);
    }
  }

  /**
   * Handle checkout event received from Redis Pub/Sub (if implemented).
   * This is for future use when Redis pub/sub is added.
   */
  public void handleCheckoutEvent(String message) {
    log.info("Received checkout event from Redis: {}", message);

    try {
      CartEvent event = objectMapper.readValue(message, CartEvent.class);

      log.debug("Parsed checkout event: eventType={}, userId={}",
          event.getEventType(), event.getUserId());

      // Send event to the specific user if they have an active SSE connection
      if (event.getUserId() != null && !event.getUserId().isEmpty()) {
        if (sseService.hasActiveConnection(event.getUserId())) {
          sseService.sendEventToUser(event.getUserId(), event);
          log.info("Forwarded checkout event to user: {}", event.getUserId());
        } else {
          log.debug("User {} has no active SSE connection. Event not forwarded.",
              event.getUserId());
        }
      } else {
        log.warn("Received checkout event without userId. Cannot forward to SSE.");
      }

    } catch (Exception e) {
      log.error("Error processing checkout event from Redis", e);
    }
  }
}
