package com.gfd_sse.dummyoff2onredis.config;

import com.gfd_sse.dummyoff2onredis.service.RedisSubscriberService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${redis.channel.cart-events}")
    private String cartEventsChannel;

    @Value("${redis.channel.product-events}")
    private String productEventsChannel;

    @Value("${redis.channel.checkout-events}")
    private String checkoutEventsChannel;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter cartEventsListener,
            MessageListenerAdapter checkoutEventsListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Subscribe to cart events
        container.addMessageListener(cartEventsListener, new PatternTopic(cartEventsChannel));

        // Subscribe to checkout events
        container.addMessageListener(checkoutEventsListener, new PatternTopic(checkoutEventsChannel));

        return container;
    }

    @Bean
    public MessageListenerAdapter cartEventsListener(RedisSubscriberService subscriberService) {
        return new MessageListenerAdapter(subscriberService, "handleCartEvent");
    }

    @Bean
    public MessageListenerAdapter checkoutEventsListener(RedisSubscriberService subscriberService) {
        return new MessageListenerAdapter(subscriberService, "handleCheckoutEvent");
    }
}
