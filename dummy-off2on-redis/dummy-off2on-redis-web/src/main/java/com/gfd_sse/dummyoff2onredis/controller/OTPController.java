package com.gfd_sse.dummyoff2onredis.controller;

import com.gfd_sse.dummyoff2onredis.dto.*;
import com.gfd_sse.dummyoff2onredis.service.OTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
public class OTPController {

    private static final Logger logger = LoggerFactory.getLogger(OTPController.class);

    private final OTPService otpService;

    public OTPController(OTPService otpService) {
        this.otpService = otpService;
    }

    /**
     * Validate OTP (for testing/debugging purposes)
     * Note: OTP validation primarily happens during SSE connection establishment
     * OTPs are generated in dummy-off2on service
     */
    @PostMapping("/validate")
    public ResponseEntity<OTPValidationResponse> validateOTP(@RequestBody OTPValidationRequest request) {
        logger.info("OTP validation request (manual) for user: {}", request.getUserId());

        try {
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        OTPValidationResponse.builder()
                                .valid(false)
                                .message("User ID is required")
                                .build());
            }

            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        OTPValidationResponse.builder()
                                .valid(false)
                                .message("OTP is required")
                                .build());
            }

            boolean isValid = otpService.validateOTP(request.getOtp(), request.getUserId());

            OTPValidationResponse response = OTPValidationResponse.builder()
                    .valid(isValid)
                    .userId(request.getUserId())
                    .message(isValid ? "OTP is valid" : "OTP is invalid or expired")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating OTP for user: {}", request.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    OTPValidationResponse.builder()
                            .valid(false)
                            .message("Failed to validate OTP: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Check if OTP exists (for testing/debugging)
     */
    @GetMapping("/exists/{otp}")
    public ResponseEntity<ApiResponse> checkOTPExists(@PathVariable String otp) {
        boolean exists = otpService.otpExists(otp);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message(exists ? "OTP exists in Redis" : "OTP not found or expired")
                .data(exists)
                .build());
    }
}
