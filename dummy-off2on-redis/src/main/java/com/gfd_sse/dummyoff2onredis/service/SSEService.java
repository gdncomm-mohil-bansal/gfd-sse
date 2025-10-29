package com.gfd_sse.dummyoff2onredis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gfd_sse.dummyoff2onredis.event.CartEvent;
import com.gfd_sse.dummyoff2onredis.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SSEService {

    private static final Logger logger = LoggerFactory.getLogger(SSEService.class);

    @Value("${sse.timeout.minutes}")
    private int sseTimeoutMinutes;

    @Value("${sse.keepalive.interval.seconds}")
    private int keepAliveIntervalSeconds;

    private final ObjectMapper objectMapper;

    // Store SSE emitters by userId
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SSEService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Create and register a new SSE emitter for a user
     */
    public SseEmitter createEmitter(String userId) {
        logger.info("Creating SSE emitter for user: {}", userId);

        // Remove existing emitter if present
        removeEmitter(userId);

        // Create new emitter with timeout
        long timeout = sseTimeoutMinutes * 60 * 1000L;
        SseEmitter emitter = new SseEmitter(timeout);

        // Setup completion callback
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for user: {}", userId);
            emitters.remove(userId);
        });

        // Setup timeout callback
        emitter.onTimeout(() -> {
            logger.warn("SSE connection timeout for user: {}", userId);
            emitters.remove(userId);
        });

        // Setup error callback
        emitter.onError((error) -> {
            logger.error("SSE connection error for user: {}", userId, error);
            emitters.remove(userId);
        });

        // Store emitter
        emitters.put(userId, emitter);

        // Send connection established event
        sendConnectionEstablishedEvent(userId);

        logger.info("SSE emitter created and registered for user: {}", userId);
        return emitter;
    }

    /**
     * Send event to a specific user
     */
    public void sendEventToUser(String userId, CartEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            logger.debug("No SSE emitter found for user: {}", userId);
            return;
        }

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                    .id(event.getEventId())
                    .name(event.getEventType().name())
                    .data(eventJson));

            logger.info("Sent event {} to user {}", event.getEventType(), userId);
        } catch (IOException e) {
            logger.error("Error sending event to user: {}", userId, e);
            removeEmitter(userId);
        } catch (Exception e) {
            logger.error("Unexpected error sending event to user: {}", userId, e);
        }
    }

    /**
     * Broadcast event to all connected users
     */
    public void broadcastEvent(CartEvent event) {
        logger.info("Broadcasting event {} to {} connected users",
                event.getEventType(), emitters.size());

        emitters.forEach((userId, emitter) -> sendEventToUser(userId, event));
    }

    /**
     * Remove emitter for a user
     */
    public void removeEmitter(String userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
                logger.info("Removed and completed SSE emitter for user: {}", userId);
            } catch (Exception e) {
                logger.error("Error completing emitter for user: {}", userId, e);
            }
        }
    }

    /**
     * Check if user has an active connection
     */
    public boolean hasActiveConnection(String userId) {
        return emitters.containsKey(userId);
    }

    /**
     * Get count of active connections
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }

    /**
     * Send connection established event
     */
    private void sendConnectionEstablishedEvent(String userId) {
        CartEvent event = CartEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EventType.CONNECTION_ESTABLISHED)
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .message("SSE connection established successfully")
                .build();

        sendEventToUser(userId, event);
    }

    /**
     * Send heartbeat to all connected clients
     * This keeps the connection alive and helps detect dead connections
     */
    @Scheduled(fixedDelayString = "${sse.keepalive.interval.seconds}000")
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        logger.debug("Sending heartbeat to {} connected users", emitters.size());

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
                logger.trace("Heartbeat sent to user: {}", userId);
            } catch (IOException e) {
                logger.warn("Failed to send heartbeat to user: {}. Removing connection.", userId);
                removeEmitter(userId);
            }
        });
    }
}
