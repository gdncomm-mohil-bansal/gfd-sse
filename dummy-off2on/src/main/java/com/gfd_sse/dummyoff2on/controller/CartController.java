package com.gfd_sse.dummyoff2on.controller;

import com.gfd_sse.dummyoff2on.dto.*;
import com.gfd_sse.dummyoff2on.model.CartItem;
import com.gfd_sse.dummyoff2on.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * POST /api/cart/add - Add product to cart
     */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<AddToCartResponse>> addToCart(@RequestBody AddToCartRequest request) {
        logger.info("Received request to add product {} to cart for user {}",
                request.getProductId(), request.getUserId());

        // Validate request
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID is required"));
        }
        if (request.getProductId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Product ID is required"));
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Quantity must be greater than 0"));
        }

        try {
            AddToCartResponse response = cartService.addToCart(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response, "Product added to cart successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Error adding product to cart", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to add product to cart: " + e.getMessage()));
        }
    }

    /**
     * GET /api/cart/{userId} - Get cart for user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<CartItem>>> getCart(@PathVariable String userId) {
        logger.info("Received request to fetch cart for user: {}", userId);
        try {
            List<CartItem> cart = cartService.getCart(userId);
            return ResponseEntity.ok(ApiResponse.success(cart, "Cart fetched successfully"));
        } catch (Exception e) {
            logger.error("Error fetching cart", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch cart: " + e.getMessage()));
        }
    }

    /**
     * POST /api/cart/checkout - Checkout cart
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@RequestBody CheckoutRequest request) {
        logger.info("Received checkout request for user: {}", request.getUserId());

        // Validate request
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID is required"));
        }

        try {
            CheckoutResponse response = cartService.checkout(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response, "Checkout completed successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Error processing checkout", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to process checkout: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/cart/{userId} - Clear cart
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> clearCart(@PathVariable String userId) {
        logger.info("Received request to clear cart for user: {}", userId);
        try {
            cartService.clearCart(userId);
            return ResponseEntity.ok(ApiResponse.success("Cart cleared", "Cart cleared successfully"));
        } catch (Exception e) {
            logger.error("Error clearing cart", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to clear cart: " + e.getMessage()));
        }
    }

    /**
     * GET /api/cart/health - Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK", "Cart service is healthy"));
    }
}
