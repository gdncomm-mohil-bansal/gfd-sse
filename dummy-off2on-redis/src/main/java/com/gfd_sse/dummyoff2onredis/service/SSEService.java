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

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SSEService {

    private static final Logger logger = LoggerFactory.getLogger(SSEService.class);

    @Value("${sse.timeout.minutes}")
    private int sseTimeoutMinutes;

    @Value("${sse.keepalive.interval.seconds}")
    private int keepAliveIntervalSeconds;

    private final ObjectMapper objectMapper;

    // Store SSE emitters by sourceId (Front-liner's device ID)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SSEService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Create and register a new SSE emitter for a device (sourceId)
     * 
     * @param sourceId Front-liner's device ID
     */
    public SseEmitter createEmitter(String sourceId) {
        logger.info("Creating SSE emitter for sourceId: {}", sourceId);

        // Remove existing emitter if present
        removeEmitter(sourceId);

        // Create new emitter with timeout
        long timeout = sseTimeoutMinutes * 60 * 1000L;
        SseEmitter emitter = new SseEmitter(timeout);

        // Setup completion callback
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for sourceId: {}", sourceId);
            emitters.remove(sourceId);
        });

        // Setup timeout callback
        emitter.onTimeout(() -> {
            logger.warn("SSE connection timeout for sourceId: {}", sourceId);
            emitters.remove(sourceId);
        });

        // Setup error callback
        emitter.onError((error) -> {
            logger.error("SSE connection error for sourceId: {}", sourceId, error);
            emitters.remove(sourceId);
        });

        // Store emitter
        emitters.put(sourceId, emitter);

        // Send connection established event
        sendConnectionEstablishedEvent(sourceId);

        logger.info("SSE emitter created and registered for sourceId: {}", sourceId);
        return emitter;
    }

    /**
     * Send event to a specific device (sourceId)
     * 
     * @param sourceId Front-liner's device ID
     */
    public void sendEventToUser(String sourceId, CartEvent event) {
        SseEmitter emitter = emitters.get(sourceId);
        if (emitter == null) {
            logger.debug("No SSE emitter found for sourceId: {}", sourceId);
            return;
        }

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                    .id(event.getEventId())
                    .name(event.getEventType().name())
                    .data(eventJson));

            logger.info("Sent event {} to sourceId {}", event.getEventType(), sourceId);
        } catch (IOException e) {
            logger.error("Error sending event to sourceId: {}", sourceId, e);
            removeEmitter(sourceId);
        } catch (Exception e) {
            logger.error("Unexpected error sending event to sourceId: {}", sourceId, e);
        }
    }

    /**
     * Broadcast event to all connected devices
     */
    public void broadcastEvent(CartEvent event) {
        logger.info("Broadcasting event {} to {} connected devices",
                event.getEventType(), emitters.size());

        emitters.forEach((sourceId, emitter) -> sendEventToUser(sourceId, event));
    }

    /**
     * Remove emitter for a device (sourceId)
     * 
     * @param sourceId Front-liner's device ID
     */
    public void removeEmitter(String sourceId) {
        SseEmitter emitter = emitters.remove(sourceId);
        if (emitter != null) {
            try {
                emitter.complete();
                logger.info("Removed and completed SSE emitter for sourceId: {}", sourceId);
            } catch (Exception e) {
                logger.error("Error completing emitter for sourceId: {}", sourceId, e);
            }
        }
    }

    /**
     * Check if device has an active connection
     * 
     * @param sourceId Front-liner's device ID
     */
    public boolean hasActiveConnection(String sourceId) {
        return emitters.containsKey(sourceId);
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
    private void sendConnectionEstablishedEvent(String sourceId) {
        CartEvent event = CartEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EventType.CONNECTION_ESTABLISHED)
                .sourceId(sourceId)
                .timestamp(System.currentTimeMillis())
                .message("SSE connection established successfully")
                .build();

        sendEventToUser(sourceId, event);
    }

    /**
     * Send heartbeat to all connected devices
     * This keeps the connection alive and helps detect dead connections
     */
    @Scheduled(fixedDelayString = "${sse.keepalive.interval.seconds}000")
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        logger.debug("Sending heartbeat to {} connected devices", emitters.size());

        emitters.forEach((sourceId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
                logger.trace("Heartbeat sent to sourceId: {}", sourceId);
            } catch (IOException e) {
                logger.warn("Failed to send heartbeat to sourceId: {}. Removing connection.", sourceId);
                removeEmitter(sourceId);
            }
        });
    }
}
