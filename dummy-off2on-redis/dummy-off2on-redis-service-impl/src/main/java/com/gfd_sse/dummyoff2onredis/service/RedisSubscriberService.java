package com.gfd_sse.dummyoff2onredis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gfd_sse.dummyoff2onredis.event.CartEvent;
import com.gfd_sse.dummyoff2onredis.event.EventType;
import com.gfd_sse.dummyoff2onredis.repository.GfdDeviceMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RedisSubscriberService {

    private static final Logger logger = LoggerFactory.getLogger(RedisSubscriberService.class);

    private final SSEService sseService;
    private final ObjectMapper objectMapper;
    private final GfdDeviceMappingRepository gfdDeviceMappingRepository;

    public RedisSubscriberService(SSEService sseService, ObjectMapper objectMapper, 
                                  GfdDeviceMappingRepository gfdDeviceMappingRepository) {
        this.sseService = sseService;
        this.objectMapper = objectMapper;
        this.gfdDeviceMappingRepository = gfdDeviceMappingRepository;
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

            // Handle disconnect events specially
            if (event.getEventType() == EventType.GFD_DISCONNECT_REQUESTED) {
                handleDisconnectEvent(event);
                return;
            }

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

    /**
     * Handle GFD disconnect event
     * Closes SSE connection and updates database mapping
     * 
     * @param event The disconnect event with sourceId
     */
    private void handleDisconnectEvent(CartEvent event) {
        String sourceId = event.getSourceId();
        logger.info("Processing GFD_DISCONNECT_REQUESTED event for sourceId: {}", sourceId);

        if (sourceId == null || sourceId.trim().isEmpty()) {
            logger.warn("Received disconnect event without sourceId. Cannot process.");
            return;
        }

        try {
            // Send disconnect confirmation to GFD before closing connection
            if (sseService.hasActiveConnection(sourceId)) {
                logger.info("Sending disconnect confirmation to sourceId: {}", sourceId);
                sseService.sendDisconnectConfirmation(sourceId);
                
                // Give some time for the message to be sent before closing
                Thread.sleep(100);
            }

            // Close SSE connection
            sseService.disconnectBySourceId(sourceId);
            logger.info("SSE connection closed for sourceId: {}", sourceId);

            // Update database mapping to inactive
            gfdDeviceMappingRepository.findBySourceId(sourceId).ifPresent(mapping -> {
                mapping.setActive(false);
                gfdDeviceMappingRepository.save(mapping);
                logger.info("Updated GfdDeviceMapping to inactive for sourceId: {}", sourceId);
            });

            logger.info("Successfully processed GFD disconnect for sourceId: {}", sourceId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while processing disconnect for sourceId: {}", sourceId, e);
        } catch (Exception e) {
            logger.error("Error processing disconnect event for sourceId: {}", sourceId, e);
        }
    }
}
