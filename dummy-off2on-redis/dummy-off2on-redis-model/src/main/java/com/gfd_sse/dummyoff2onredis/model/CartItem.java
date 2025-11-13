package com.gfd_sse.dummyoff2onredis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Cart item model for SSE events to GFD PWA
 * Contains only non-sensitive, display-relevant information
 * aligned with industry standards and Off2OnCartItem structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    
    // Item identifiers
    private String itemSku;                 // Item SKU code
    private String orderItemId;             // Order item identifier
    private String itemKeyId;               // Unique cart item key
    private String productSku;              // Product SKU
    private String productCode;             // Product code
    
    // Product information
    private String productName;             // Product/item name
    private String brandName;               // Brand name
    private String imageUrl;                // Product image URL
    
    // Pricing information (using BigDecimal for precision)
    private BigDecimal price;               // Unit offer price
    private BigDecimal listPrice;           // Original list price
    private BigDecimal subtotal;            // Total for this line item (price * quantity)
    private BigDecimal discount;            // Discount amount
    
    // Quantity
    private Integer quantity;               // Item quantity
    
    // Merchant information
    private String merchantCode;            // Merchant code
    private String merchantName;            // Merchant name
    
    // Stock and availability
    private Boolean outOfStock;             // Stock availability flag
    private Integer totalStock;             // Total available stock
}
