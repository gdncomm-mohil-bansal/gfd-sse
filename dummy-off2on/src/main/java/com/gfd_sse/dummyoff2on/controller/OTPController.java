package com.gfd_sse.dummyoff2on.controller;

import com.gfd_sse.dummyoff2on.dto.ApiResponse;
import com.gfd_sse.dummyoff2on.dto.OTPGenerationRequest;
import com.gfd_sse.dummyoff2on.dto.OTPGenerationResponse;
import com.gfd_sse.dummyoff2on.service.OTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "*")
public class OTPController {

    private static final Logger logger = LoggerFactory.getLogger(OTPController.class);

    private final OTPService otpService;

    public OTPController(OTPService otpService) {
        this.otpService = otpService;
    }

    /**
     * Generate OTP for Front-liner to share with GFD
     * Front-liner PWA calls this endpoint
     */
    @PostMapping("/generate")
    public ResponseEntity<OTPGenerationResponse> generateOTP(@RequestBody OTPGenerationRequest request) {
        logger.info("OTP generation request for Front-liner user: {}", request.getUserId());

        try {
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        OTPGenerationResponse.builder()
                                .success(false)
                                .message("User ID is required")
                                .build());
            }

            String otp = otpService.generateOTP(request.getUserId());
            Long expiresAt = otpService.getOTPExpiration(otp);

            OTPGenerationResponse response = OTPGenerationResponse.builder()
                    .success(true)
                    .otp(otp)
                    .userId(request.getUserId())
                    .expiresAt(expiresAt)
                    .message("OTP generated successfully")
                    .instructions("Share this OTP with GFD to monitor your session. Valid for 5 minutes.")
                    .build();

            logger.info("OTP generated successfully for Front-liner user: {}", request.getUserId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating OTP for user: {}", request.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    OTPGenerationResponse.builder()
                            .success(false)
                            .message("Failed to generate OTP: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Check if OTP exists (for internal testing)
     */
    @GetMapping("/exists/{otp}")
    public ResponseEntity<ApiResponse> checkOTPExists(@PathVariable String otp) {
        boolean exists = otpService.otpExists(otp);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message(exists ? "OTP exists" : "OTP not found or expired")
                .data(exists)
                .build());
    }
}
