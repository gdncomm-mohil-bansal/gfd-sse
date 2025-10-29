package com.gfd_sse.dummyoff2on.service;

import com.gfd_sse.dummyoff2on.dto.AddToCartRequest;
import com.gfd_sse.dummyoff2on.dto.AddToCartResponse;
import com.gfd_sse.dummyoff2on.dto.CheckoutRequest;
import com.gfd_sse.dummyoff2on.dto.CheckoutResponse;
import com.gfd_sse.dummyoff2on.event.CartEvent;
import com.gfd_sse.dummyoff2on.event.EventType;
import com.gfd_sse.dummyoff2on.model.CartItem;
import com.gfd_sse.dummyoff2on.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final ProductService productService;
    private final RedisPublisherService redisPublisher;

    // In-memory cart storage: userId -> List of CartItems
    private final Map<String, List<CartItem>> userCarts = new ConcurrentHashMap<>();

    public CartService(ProductService productService, RedisPublisherService redisPublisher) {
        this.productService = productService;
        this.redisPublisher = redisPublisher;
    }

    /**
     * Add item to cart
     */
    public AddToCartResponse addToCart(AddToCartRequest request) {
        logger.info("Adding product {} to cart for user {}", request.getProductId(), request.getUserId());

        try {
            // Validate product exists
            Optional<Product> productOpt = productService.getProductById(request.getProductId());
            if (productOpt.isEmpty()) {
                return createErrorResponse(request.getUserId(), "Product not found");
            }

            Product product = productOpt.get();

            // Check stock availability
            if (!productService.isProductAvailable(request.getProductId(), request.getQuantity())) {
                return createErrorResponse(request.getUserId(), "Insufficient stock available");
            }

            // Get or create user cart
            List<CartItem> cart = userCarts.computeIfAbsent(request.getUserId(), k -> new ArrayList<>());

            // Check if product already exists in cart
            Optional<CartItem> existingItem = cart.stream()
                    .filter(item -> item.getProductId().equals(request.getProductId()))
                    .findFirst();

            if (existingItem.isPresent()) {
                // Update quantity
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + request.getQuantity());
                item.setSubtotal(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
            } else {
                // Add new item
                CartItem newItem = CartItem.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .price(product.getPrice())
                        .quantity(request.getQuantity())
                        .subtotal(product.getPrice().multiply(new BigDecimal(request.getQuantity())))
                        .build();
                cart.add(newItem);
            }

            // Calculate totals
            BigDecimal totalAmount = calculateTotalAmount(cart);
            Integer totalItems = calculateTotalItems(cart);

            AddToCartResponse response = AddToCartResponse.builder()
                    .success(true)
                    .message("Product added to cart successfully")
                    .userId(request.getUserId())
                    .cartItems(new ArrayList<>(cart))
                    .totalAmount(totalAmount)
                    .totalItems(totalItems)
                    .build();

            // Publish cart event to Redis
            publishCartAddedEvent(request.getUserId(), product, request.getQuantity(), cart, totalAmount, totalItems);

            logger.info("Successfully added product {} to cart for user {}", request.getProductId(),
                    request.getUserId());
            return response;

        } catch (Exception e) {
            logger.error("Error adding product to cart", e);
            return createErrorResponse(request.getUserId(), "Failed to add product to cart: " + e.getMessage());
        }
    }

    /**
     * Get cart for user
     */
    public List<CartItem> getCart(String userId) {
        return userCarts.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * Clear cart for user
     */
    public void clearCart(String userId) {
        userCarts.remove(userId);
        logger.info("Cleared cart for user {}", userId);
    }

    /**
     * Checkout cart
     */
    public CheckoutResponse checkout(CheckoutRequest request) {
        logger.info("Processing checkout for user {}", request.getUserId());

        try {
            List<CartItem> cart = userCarts.get(request.getUserId());
            if (cart == null || cart.isEmpty()) {
                return CheckoutResponse.builder()
                        .success(false)
                        .message("Cart is empty")
                        .userId(request.getUserId())
                        .build();
            }

            // Calculate total
            BigDecimal totalAmount = calculateTotalAmount(cart);

            // Generate order ID
            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Simulate order processing
            CheckoutResponse response = CheckoutResponse.builder()
                    .success(true)
                    .message("Order placed successfully")
                    .orderId(orderId)
                    .userId(request.getUserId())
                    .totalAmount(totalAmount)
                    .orderStatus("CONFIRMED")
                    .build();

            // Publish checkout event
            publishCheckoutEvent(request.getUserId(), orderId, cart, totalAmount, true);

            // Clear cart after successful checkout
            clearCart(request.getUserId());

            logger.info("Successfully processed checkout for user {}. Order ID: {}", request.getUserId(), orderId);
            return response;

        } catch (Exception e) {
            logger.error("Error processing checkout", e);

            // Publish failed checkout event
            publishCheckoutEvent(request.getUserId(), null, null, BigDecimal.ZERO, false);

            return CheckoutResponse.builder()
                    .success(false)
                    .message("Checkout failed: " + e.getMessage())
                    .userId(request.getUserId())
                    .orderStatus("FAILED")
                    .build();
        }
    }

    /**
     * Calculate total amount in cart
     */
    private BigDecimal calculateTotalAmount(List<CartItem> cart) {
        return cart.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total items in cart
     */
    private Integer calculateTotalItems(List<CartItem> cart) {
        return cart.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Create error response
     */
    private AddToCartResponse createErrorResponse(String userId, String message) {
        return AddToCartResponse.builder()
                .success(false)
                .message(message)
                .userId(userId)
                .cartItems(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .totalItems(0)
                .build();
    }

    /**
     * Publish cart item added event to Redis
     */
    private void publishCartAddedEvent(String userId, Product product, Integer quantity,
            List<CartItem> cart, BigDecimal totalAmount, Integer totalItems) {
        try {
            CartEvent event = CartEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(EventType.CART_ITEM_ADDED)
                    .userId(userId)
                    .timestamp(System.currentTimeMillis())
                    .cartItems(new ArrayList<>(cart))
                    .totalAmount(totalAmount)
                    .totalItems(totalItems)
                    .message("Added " + quantity + " x " + product.getName() + " to cart")
                    .metadata(Map.of(
                            "productId", product.getId(),
                            "productName", product.getName(),
                            "quantity", quantity,
                            "price", product.getPrice()))
                    .build();

            redisPublisher.publishCartEvent(event);
            logger.info("Published cart event for user {}", userId);
        } catch (Exception e) {
            logger.error("Failed to publish cart event", e);
        }
    }

    /**
     * Publish checkout event to Redis
     */
    private void publishCheckoutEvent(String userId, String orderId, List<CartItem> cart,
            BigDecimal totalAmount, boolean success) {
        try {
            CartEvent event = CartEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(success ? EventType.CHECKOUT_COMPLETED : EventType.CHECKOUT_FAILED)
                    .userId(userId)
                    .timestamp(System.currentTimeMillis())
                    .cartItems(cart != null ? new ArrayList<>(cart) : new ArrayList<>())
                    .totalAmount(totalAmount)
                    .totalItems(cart != null ? calculateTotalItems(cart) : 0)
                    .message(success ? "Checkout completed successfully" : "Checkout failed")
                    .metadata(success ? Map.of("orderId", orderId, "status", "CONFIRMED") : Map.of("status", "FAILED"))
                    .build();

            redisPublisher.publishCheckoutEvent(event);
            logger.info("Published checkout event for user {}", userId);
        } catch (Exception e) {
            logger.error("Failed to publish checkout event", e);
        }
    }
}
