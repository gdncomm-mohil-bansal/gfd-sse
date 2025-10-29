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
     */
    public void handleCartEvent(String message) {
        logger.info("Received cart event from Redis: {}", message);

        try {
            CartEvent event = objectMapper.readValue(message, CartEvent.class);

            logger.debug("Parsed cart event: eventType={}, userId={}",
                    event.getEventType(), event.getUserId());

            // Send event to the specific user if they have an active SSE connection
            if (event.getUserId() != null && !event.getUserId().isEmpty()) {
                if (sseService.hasActiveConnection(event.getUserId())) {
                    sseService.sendEventToUser(event.getUserId(), event);
                    logger.info("Forwarded cart event to user: {}", event.getUserId());
                } else {
                    logger.debug("User {} has no active SSE connection. Event not forwarded.",
                            event.getUserId());
                }
            } else {
                logger.warn("Received cart event without userId. Cannot forward to SSE.");
            }

        } catch (Exception e) {
            logger.error("Error processing cart event from Redis", e);
        }
    }

    /**
     * Handle checkout events from Redis
     */
    public void handleCheckoutEvent(String message) {
        logger.info("Received checkout event from Redis: {}", message);

        try {
            CartEvent event = objectMapper.readValue(message, CartEvent.class);

            logger.debug("Parsed checkout event: eventType={}, userId={}",
                    event.getEventType(), event.getUserId());

            // Send event to the specific user if they have an active SSE connection
            if (event.getUserId() != null && !event.getUserId().isEmpty()) {
                if (sseService.hasActiveConnection(event.getUserId())) {
                    sseService.sendEventToUser(event.getUserId(), event);
                    logger.info("Forwarded checkout event to user: {}", event.getUserId());
                } else {
                    logger.debug("User {} has no active SSE connection. Event not forwarded.",
                            event.getUserId());
                }
            } else {
                logger.warn("Received checkout event without userId. Cannot forward to SSE.");
            }

        } catch (Exception e) {
            logger.error("Error processing checkout event from Redis", e);
        }
    }

    /**
     * Handle product events from Redis
     */
    public void handleProductEvent(String message) {
        logger.info("Received product event from Redis: {}", message);

        try {
            CartEvent event = objectMapper.readValue(message, CartEvent.class);

            logger.debug("Parsed product event: eventType={}, userId={}",
                    event.getEventType(), event.getUserId());

            // Send event to the specific user if they have an active SSE connection
            if (event.getUserId() != null && !event.getUserId().isEmpty()) {
                if (sseService.hasActiveConnection(event.getUserId())) {
                    sseService.sendEventToUser(event.getUserId(), event);
                    logger.info("Forwarded product event to user: {}", event.getUserId());
                } else {
                    logger.debug("User {} has no active SSE connection. Event not forwarded.",
                            event.getUserId());
                }
            } else {
                logger.warn("Received product event without userId. Cannot forward to SSE.");
            }

        } catch (Exception e) {
            logger.error("Error processing product event from Redis", e);
        }
    }
}
