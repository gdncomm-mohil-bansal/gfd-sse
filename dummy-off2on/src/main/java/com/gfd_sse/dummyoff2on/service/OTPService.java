package com.gfd_sse.dummyoff2on.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class OTPService {

    private static final Logger logger = LoggerFactory.getLogger(OTPService.class);

    @Value("${otp.expiration.minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.length}")
    private int otpLength;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis key prefix for OTPs
    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_USER_PREFIX = "otp:user:";

    public OTPService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate a new OTP for a user and store in Redis
     */
    public String generateOTP(String userId) {
        logger.info("Generating OTP for user: {}", userId);

        // Generate random 6-digit OTP
        int otp = 100000 + secureRandom.nextInt(900000);
        String otpString = String.valueOf(otp);

        // Remove old OTP if exists
        String oldOtp = (String) redisTemplate.opsForValue().get(OTP_USER_PREFIX + userId);
        if (oldOtp != null) {
            redisTemplate.delete(OTP_PREFIX + oldOtp);
            logger.debug("Deleted old OTP for user: {}", userId);
        }

        // Store OTP → userId mapping in Redis with expiration
        redisTemplate.opsForValue().set(
                OTP_PREFIX + otpString,
                userId,
                otpExpirationMinutes,
                TimeUnit.MINUTES);

        // Store userId → OTP mapping for cleanup
        redisTemplate.opsForValue().set(
                OTP_USER_PREFIX + userId,
                otpString,
                otpExpirationMinutes,
                TimeUnit.MINUTES);

        logger.info("Generated OTP for user {}: {} (expires in {} minutes)",
                userId, otpString, otpExpirationMinutes);
        return otpString;
    }

    /**
     * Get expiration time for an OTP (for display purposes)
     */
    public Long getOTPExpiration(String otp) {
        Long ttl = redisTemplate.getExpire(OTP_PREFIX + otp, TimeUnit.MILLISECONDS);
        if (ttl != null && ttl > 0) {
            return System.currentTimeMillis() + ttl;
        }
        return null;
    }

    /**
     * Check if OTP exists (for internal use)
     */
    public boolean otpExists(String otp) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(OTP_PREFIX + otp));
    }
}
