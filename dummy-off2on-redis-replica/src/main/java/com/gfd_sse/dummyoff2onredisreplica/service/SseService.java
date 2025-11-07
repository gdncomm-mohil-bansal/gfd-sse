package com.gfd_sse.dummyoff2onredisreplica.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gfd_sse.dummyoff2onredisreplica.event.CartEvent;
import com.gfd_sse.dummyoff2onredisreplica.event.EventType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SseService {

  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

  @Value("${sse.timeout.minutes}")
  private int sseTimeoutMinutes;

  private final ObjectMapper objectMapper;

  public SseService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public SseEmitter createEmitter(String userId) {
    log.info("Creating SSE emitter for user: {}", userId);

    removeEmitter(userId);

    long timeout = sseTimeoutMinutes * 60 * 1000L;
    SseEmitter emitter = new SseEmitter(timeout);

    emitter.onCompletion(() -> {
      log.info("SSE connection completed for user: {}", userId);
      emitters.remove(userId);
    });

    emitter.onTimeout(() -> {
      log.warn("SSE connection timeout for user: {}", userId);
      emitters.remove(userId);
    });

    emitter.onError((error) -> {
      log.error("SSE connection error for user: {}", userId, error);
      emitters.remove(userId);
    });

    emitters.put(userId, emitter);

    // Send connection established event
    // sendConnectionEstablishedEvent(userId);

    log.info("SSE emitter created and registered for user: {}", userId);
    return emitter;
  }

  public void sendEventToUser(String userId, CartEvent event) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter == null) {
      log.debug("No SSE emitter found for user: {}", userId);
      return;
    }

    try {
      String eventJson = objectMapper.writeValueAsString(event);
      emitter.send(SseEmitter.event().id(event.getEventId()).name(event.getEventType().name())
          .data(eventJson));

      log.info("Sent event {} to user {}", event.getEventType(), userId);
    } catch (IOException e) {
      log.error("Error sending event to user: {}", userId, e);
      removeEmitter(userId);
    } catch (Exception e) {
      log.error("Unexpected error sending event to user: {}", userId, e);
    }
  }

  public boolean hasActiveConnection(String userId) {
    return emitters.containsKey(userId);
  }

  public void removeEmitter(String userId) {
    SseEmitter emitter = emitters.remove(userId);
    if (emitter != null) {
      try {
        emitter.complete();
        log.info("Removed and completed SSE emitter for user: {}", userId);
      } catch (Exception e) {
        log.error("Error completing emitter for user: {}", userId, e);
      }
    }
  }

  /**
   * Get the count of active connections on this pod
   */
  public int getActiveConnectionCount() {
    return emitters.size();
  }

  /**
   * Send connection established event to a user
   */
  private void sendConnectionEstablishedEvent(String userId) {
    CartEvent event = CartEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType(EventType.CONNECTION_ESTABLISHED)
        .userId(userId)
        .timestamp(System.currentTimeMillis())
        .message("SSE connection established successfully on REPLICA pod")
        .build();

    sendEventToUser(userId, event);
  }
}
