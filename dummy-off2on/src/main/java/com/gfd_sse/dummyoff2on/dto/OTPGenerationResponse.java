package com.gfd_sse.dummyoff2on.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTPGenerationResponse {
    private boolean success;
    private String otp;
    private String userId;
    private Long expiresAt;
    private String message;
    private String instructions;
}
