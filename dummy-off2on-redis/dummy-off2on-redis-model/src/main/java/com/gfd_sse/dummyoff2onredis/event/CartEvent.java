package com.gfd_sse.dummyoff2onredis.event;

import com.gfd_sse.dummyoff2onredis.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cart event model for SSE streaming to GFD PWA
 * 
 * Event routing for GFD:
 * - sourceId is REQUIRED: Front-liner's device ID used for routing events to
 * GFD PWA
 * - userId is OPTIONAL: Can be included for additional context but NOT used for
 * routing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartEvent {
    private String eventId;
    private EventType eventType;
    private String userId; // Optional: Not used for GFD event routing
    private String sourceId; // Required: Front-liner's device ID for routing events to GFD PWA
    private Long timestamp;
    private List<CartItem> cartItems;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private String message;
    private Object metadata;
}
