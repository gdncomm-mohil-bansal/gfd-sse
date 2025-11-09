package com.gfd_sse.dummyoff2onredis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gfd_sse.dummyoff2onredis.event.CartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RedisSubscriberService {

    private static final Logger logger = LoggerFactory.getLogger(RedisSubscriberService.class);

    private final SSEService sseService;
    private final ObjectMapper objectMapper;

    public RedisSubscriberService(SSEService sseService, ObjectMapper objectMapper) {
        this.sseService = sseService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle cart events from Redis
     * Routes events based on sourceId (Front-liner's device ID)
     */
    public void handleCartEvent(String message) {
        logger.info("Received cart event from Redis: {}", message);

        try {
            CartEvent event = objectMapper.readValue(message, CartEvent.class);

            logger.debug("Parsed cart event: eventType={}, sourceId={}",
                    event.getEventType(), event.getSourceId());

            // Route event based on sourceId (device-to-device mapping)
            String sourceId = event.getSourceId();

            if (sourceId != null && !sourceId.trim().isEmpty()) {
                if (sseService.hasActiveConnection(sourceId)) {
                    sseService.sendEventToUser(sourceId, event);
                    logger.info("Forwarded cart event to sourceId: {}", sourceId);
                } else {
                    logger.debug("SourceId {} has no active SSE connection. Event not forwarded.", sourceId);
                }
            } else {
                logger.warn("Received cart event without sourceId. Cannot route to GFD. Event: {}", event);
            }

        } catch (Exception e) {
            logger.error("Error processing cart event from Redis", e);
        }
    }

    /**
     * Handle checkout events from Redis
     * Routes events based on sourceId (Front-liner's device ID)
     */
    public void handleCheckoutEvent(String message) {
        logger.info("Received checkout event from Redis: {}", message);

        try {
            CartEvent event = objectMapper.readValue(message, CartEvent.class);

            logger.debug("Parsed checkout event: eventType={}, sourceId={}",
                    event.getEventType(), event.getSourceId());

            // Route event based on sourceId (device-to-device mapping)
            String sourceId = event.getSourceId();

            if (sourceId != null && !sourceId.trim().isEmpty()) {
                if (sseService.hasActiveConnection(sourceId)) {
                    sseService.sendEventToUser(sourceId, event);
                    logger.info("Forwarded checkout event to sourceId: {}", sourceId);
                } else {
                    logger.debug("SourceId {} has no active SSE connection. Event not forwarded.", sourceId);
                }
            } else {
                logger.warn("Received checkout event without sourceId. Cannot route to GFD. Event: {}", event);
            }

        } catch (Exception e) {
            logger.error("Error processing checkout event from Redis", e);
        }
    }

    /**
     * Handle product events from Redis
     * Routes events based on sourceId (Front-liner's device ID)
     */
    public void handleProductEvent(String message) {
        logger.info("Received product event from Redis: {}", message);

        try {
            CartEvent event = objectMapper.readValue(message, CartEvent.class);

            logger.debug("Parsed product event: eventType={}, sourceId={}",
                    event.getEventType(), event.getSourceId());

            // Route event based on sourceId (device-to-device mapping)
            String sourceId = event.getSourceId();

            if (sourceId != null && !sourceId.trim().isEmpty()) {
                if (sseService.hasActiveConnection(sourceId)) {
                    sseService.sendEventToUser(sourceId, event);
                    logger.info("Forwarded product event to sourceId: {}", sourceId);
                } else {
                    logger.debug("SourceId {} has no active SSE connection. Event not forwarded.", sourceId);
                }
            } else {
                logger.warn("Received product event without sourceId. Cannot route to GFD. Event: {}", event);
            }

        } catch (Exception e) {
            logger.error("Error processing product event from Redis", e);
        }
    }
}
