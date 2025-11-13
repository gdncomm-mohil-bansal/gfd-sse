package com.gfd_sse.dummyoff2onredis.controller;

import com.gfd_sse.dummyoff2onredis.dto.ApiResponse;
import com.gfd_sse.dummyoff2onredis.exception.InvalidOtpException;


import io.swagger.v3.oas.annotations.Hidden;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Hidden @ControllerAdvice public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handle illegal argument exceptions
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex,
      WebRequest request) {
    logger.error("Illegal argument exception: ", ex);
    ApiResponse response =
        ApiResponse.builder().success(false).message("Invalid request: " + ex.getMessage()).build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * Handle specific business logic exception for invalid OTPs.
   * Returns 401 Unauthorized.
   */
  @ExceptionHandler(InvalidOtpException.class)
  public ResponseEntity<ApiResponse> handleInvalidOtpException(InvalidOtpException ex,
      WebRequest request) {
    logger.warn("Invalid OTP Exception: {}", ex.getMessage());
    ApiResponse response =
        ApiResponse.builder().success(false).message("Authentication failed: " + ex.getMessage())
            .build();
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  /**
   * A final catch-all for any other unexpected exceptions.
   * Returns 500 Internal Server Error.
   */
  @ExceptionHandler(Exception.class) public ResponseEntity<ApiResponse> handleGlobalException(
      Exception ex, WebRequest request) {
    logger.error("An unexpected error occurred: ", ex);
    ApiResponse response =
        ApiResponse.builder().success(false).message("An internal server error occurred.").build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
