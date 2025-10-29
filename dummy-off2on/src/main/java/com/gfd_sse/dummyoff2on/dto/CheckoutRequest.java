package com.gfd_sse.dummyoff2on.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private String userId;
    private String paymentMethod;
    private String shippingAddress;
}
