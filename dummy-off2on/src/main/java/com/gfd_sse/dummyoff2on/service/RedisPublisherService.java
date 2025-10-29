package com.gfd_sse.dummyoff2on.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(RedisPublisherService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.channel.cart-events}")
    private String cartEventsChannel;

    @Value("${redis.channel.product-events}")
    private String productEventsChannel;

    @Value("${redis.channel.checkout-events}")
    private String checkoutEventsChannel;

    public RedisPublisherService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish cart events to Redis channel
     */
    public void publishCartEvent(Object event) {
        try {
            redisTemplate.convertAndSend(cartEventsChannel, event);
            logger.info("Published cart event to channel '{}': {}", cartEventsChannel, event);
        } catch (Exception e) {
            logger.error("Error publishing cart event to Redis", e);
            throw new RuntimeException("Failed to publish cart event", e);
        }
    }

    /**
     * Publish product events to Redis channel
     */
    public void publishProductEvent(Object event) {
        try {
            redisTemplate.convertAndSend(productEventsChannel, event);
            logger.info("Published product event to channel '{}': {}", productEventsChannel, event);
        } catch (Exception e) {
            logger.error("Error publishing product event to Redis", e);
            throw new RuntimeException("Failed to publish product event", e);
        }
    }

    /**
     * Publish checkout events to Redis channel
     */
    public void publishCheckoutEvent(Object event) {
        try {
            redisTemplate.convertAndSend(checkoutEventsChannel, event);
            logger.info("Published checkout event to channel '{}': {}", checkoutEventsChannel, event);
        } catch (Exception e) {
            logger.error("Error publishing checkout event to Redis", e);
            throw new RuntimeException("Failed to publish checkout event", e);
        }
    }

    /**
     * Generic method to publish to any channel
     */
    public void publishToChannel(String channel, Object event) {
        try {
            redisTemplate.convertAndSend(channel, event);
            logger.info("Published event to channel '{}': {}", channel, event);
        } catch (Exception e) {
            logger.error("Error publishing event to Redis channel: {}", channel, e);
            throw new RuntimeException("Failed to publish event to channel: " + channel, e);
        }
    }
}