package com.gfd_sse.dummyoff2onredis.service.api;

import com.gfd_sse.dummyoff2onredis.dto.ConnectionRequest;
import com.gfd_sse.dummyoff2onredis.dto.ConnectionResponse;

/**
 * Service interface for managing device connections
 * Handles business logic for connecting/disconnecting GFD devices to Front-liner devices
 */
public interface DeviceConnectionService {

    /**
     * Establish SSE connection between GFD device and Front-liner device
     * 
     * Flow 1: With OTP (First-time connection)
     * - Validates OTP from MongoDB
     * - Checks for device conflicts
     * - Activates device mapping
     * - Creates SSE emitter
     * 
     * Flow 2: Without OTP (Reconnection)
     * - Finds existing device mapping by destinationId
     * - Validates mapping is active
     * - Creates SSE emitter using existing sourceId
     * 
     * @param request Connection request containing destinationId and optional OTP
     * @return ConnectionResponse with sourceId, destinationId, and SSE emitter
     * @throws InvalidOtpException if OTP is invalid or expired
     * @throws DeviceMappingNotFoundException if no mapping found for reconnection
     * @throws DeviceConflictException if device is already connected to different front-liner
     */
    ConnectionResponse connect(ConnectionRequest request);

    /**
     * Disconnect SSE connection for a device
     * 
     * @param destinationId GFD device ID (destination)
     * @return true if disconnection was successful, false if no connection found
     */
    boolean disconnect(String destinationId);

    /**
     * Check if a device has an active connection
     * 
     * @param destinationId GFD device ID (destination)
     * @return true if device has active connection, false otherwise
     */
    boolean isConnected(String destinationId);
}

