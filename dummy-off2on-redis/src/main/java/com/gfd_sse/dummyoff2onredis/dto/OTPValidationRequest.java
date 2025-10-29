package com.gfd_sse.dummyoff2onredis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTPValidationRequest {
    private String otp;
    private String userId;
}
