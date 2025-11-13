package com.gfd_sse.dummyoff2onredis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for SSE connection request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRequest {
    /**
     * Destination device ID (GFD PWA device) from cookie
     */
    private String destinationId;

    /**
     * OTP for first-time connection (optional for reconnection)
     */
    private String otp;
}

