package com.gfd_sse.dummyoff2on.service;

import com.gfd_sse.dummyoff2on.event.CartEvent;
import com.gfd_sse.dummyoff2on.event.EventType;
import com.gfd_sse.dummyoff2on.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final Map<Long, Product> productDatabase = new HashMap<>();
    private final RedisPublisherService redisPublisher;

    public ProductService(RedisPublisherService redisPublisher) {
        this.redisPublisher = redisPublisher;
    }

    @PostConstruct
    public void initializeProducts() {
        // Initialize with some dummy products
        productDatabase.put(1L, Product.builder()
                .id(1L)
                .name("Laptop")
                .description("High-performance laptop for professionals")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .stockQuantity(50)
                .imageUrl("https://example.com/laptop.jpg")
                .build());

        productDatabase.put(2L, Product.builder()
                .id(2L)
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse with precision tracking")
                .price(new BigDecimal("29.99"))
                .category("Electronics")
                .stockQuantity(200)
                .imageUrl("https://example.com/mouse.jpg")
                .build());

        productDatabase.put(3L, Product.builder()
                .id(3L)
                .name("Mechanical Keyboard")
                .description("RGB mechanical keyboard with blue switches")
                .price(new BigDecimal("89.99"))
                .category("Electronics")
                .stockQuantity(100)
                .imageUrl("https://example.com/keyboard.jpg")
                .build());

        productDatabase.put(4L, Product.builder()
                .id(4L)
                .name("USB-C Hub")
                .description("Multi-port USB-C hub with HDMI and USB 3.0")
                .price(new BigDecimal("49.99"))
                .category("Accessories")
                .stockQuantity(150)
                .imageUrl("https://example.com/usb-hub.jpg")
                .build());

        productDatabase.put(5L, Product.builder()
                .id(5L)
                .name("Noise Cancelling Headphones")
                .description("Premium wireless headphones with active noise cancellation")
                .price(new BigDecimal("249.99"))
                .category("Audio")
                .stockQuantity(75)
                .imageUrl("https://example.com/headphones.jpg")
                .build());

        logger.info("Initialized {} products", productDatabase.size());
    }

    /**
     * Get all products
     */
    public List<Product> getAllProducts() {
        logger.info("Fetching all products");
        return new ArrayList<>(productDatabase.values());
    }

    /**
     * Get product by ID
     */
    public Optional<Product> getProductById(Long productId) {
        logger.info("Fetching product with ID: {}", productId);
        Product product = productDatabase.get(productId);

        // Publish product viewed event
        if (product != null) {
            publishProductViewedEvent(product);
        }

        return Optional.ofNullable(product);
    }

    /**
     * Get products by category
     */
    public List<Product> getProductsByCategory(String category) {
        logger.info("Fetching products in category: {}", category);
        return productDatabase.values().stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .toList();
    }

    /**
     * Check if product is available in stock
     */
    public boolean isProductAvailable(Long productId, Integer quantity) {
        Product product = productDatabase.get(productId);
        if (product == null) {
            return false;
        }
        return product.getStockQuantity() >= quantity;
    }

    /**
     * Update product stock
     */
    public void updateStock(Long productId, Integer quantityChange) {
        Product product = productDatabase.get(productId);
        if (product != null) {
            int newStock = product.getStockQuantity() + quantityChange;
            product.setStockQuantity(Math.max(0, newStock));
            logger.info("Updated stock for product {}: new quantity = {}", productId, product.getStockQuantity());
        }
    }

    /**
     * Publish product viewed event to Redis
     */
    private void publishProductViewedEvent(Product product) {
        try {
            CartEvent event = CartEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(EventType.PRODUCT_VIEWED)
                    .userId("system")
                    .timestamp(System.currentTimeMillis())
                    .message("Product viewed: " + product.getName())
                    .metadata(Map.of(
                            "productId", product.getId(),
                            "productName", product.getName(),
                            "price", product.getPrice()))
                    .build();

            redisPublisher.publishProductEvent(event);
        } catch (Exception e) {
            logger.error("Failed to publish product viewed event", e);
        }
    }
}
