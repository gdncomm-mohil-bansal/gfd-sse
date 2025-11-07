package com.gfd_sse.dummyoff2onredisreplica.dto.response;

import java.math.BigDecimal;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
