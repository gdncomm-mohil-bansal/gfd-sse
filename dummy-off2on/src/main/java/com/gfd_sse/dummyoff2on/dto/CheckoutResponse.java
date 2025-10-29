package com.gfd_sse.dummyoff2on.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {
    private boolean success;
    private String message;
    private String orderId;
    private String userId;
    private BigDecimal totalAmount;
    private String orderStatus;
}
