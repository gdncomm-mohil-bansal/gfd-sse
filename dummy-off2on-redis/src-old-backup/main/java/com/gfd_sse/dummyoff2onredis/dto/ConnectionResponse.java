package com.gfd_sse.dummyoff2onredis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * DTO for SSE connection response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {
     * Source device ID (Front-liner's device)
     */
    private String sourceId;

    /**
     * Destination device ID (GFD PWA device)
     */
    private String destinationId;

    /**
     * SSE emitter for the connection
     */
    private SseEmitter emitter;

    /**
     * Whether this is a new connection or reconnection
     */
    private boolean isReconnection;
}

