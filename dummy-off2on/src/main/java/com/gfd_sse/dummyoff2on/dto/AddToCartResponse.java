package com.gfd_sse.dummyoff2on.dto;

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
public class AddToCartResponse {
    private boolean success;
    private String message;
    private String userId;
    private List<CartItem> cartItems;
    private BigDecimal totalAmount;
    private Integer totalItems;
}
