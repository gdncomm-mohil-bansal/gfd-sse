package com.gfd_sse.dummyoff2onredis.exception;

/**
 * Exception thrown when device is already connected to a different front-liner
 */
public class DeviceConflictException extends RuntimeException {
    public DeviceConflictException(String message) {
        super(message);
    }
}

