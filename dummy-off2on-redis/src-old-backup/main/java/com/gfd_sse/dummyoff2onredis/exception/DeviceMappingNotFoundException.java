package com.gfd_sse.dummyoff2onredis.exception;

/**
 * Exception thrown when device mapping is not found
 */
public class DeviceMappingNotFoundException extends RuntimeException {
    public DeviceMappingNotFoundException(String message) {
        super(message);
    }
}

