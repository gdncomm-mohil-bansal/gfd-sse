package com.gfd_sse.dummyoff2onredis.exception;

/**
 * Exception thrown when OTP is invalid or expired
 */
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }

    public InvalidOtpException(String message, Throwable cause) {
        super(message, cause);
    }
}

