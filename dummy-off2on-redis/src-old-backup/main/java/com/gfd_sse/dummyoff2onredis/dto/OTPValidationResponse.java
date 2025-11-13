package com.gfd_sse.dummyoff2onredis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTPValidationResponse {
    private boolean valid;
    private String userId;
    private String message;
}
