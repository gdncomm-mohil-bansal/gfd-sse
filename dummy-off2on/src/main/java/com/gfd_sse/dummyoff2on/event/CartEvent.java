package com.gfd_sse.dummyoff2on.event;

import com.gfd_sse.dummyoff2on.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartEvent {
    private String eventId;
    private EventType eventType;
    private String userId;
    private Long timestamp;
    private List<CartItem> cartItems;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private String message;
    private Object metadata;
}
