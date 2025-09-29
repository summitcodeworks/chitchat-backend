package com.chitchat.user.service.impl;

import com.chitchat.user.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of OTP service using Redis for storage
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String OTP_PREFIX = "otp:";
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Override
    public String generateOtp(String phoneNumber) {
        log.info("Generating OTP for phone number: {}", phoneNumber);
        
        // Generate 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        
        // Store OTP in Redis with expiry
        String key = OTP_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(OTP_EXPIRY_MINUTES));
        
        log.info("OTP generated and stored for phone number: {}", phoneNumber);
        return otp;
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String otp) {
        log.info("Verifying OTP for phone number: {}", phoneNumber);
        
        String key = OTP_PREFIX + phoneNumber;
        String storedOtp = redisTemplate.opsForValue().get(key);
        
        if (storedOtp == null) {
            log.warn("No OTP found for phone number: {}", phoneNumber);
            return false;
        }
        
        boolean isValid = storedOtp.equals(otp);
        
        if (isValid) {
            log.info("OTP verification successful for phone number: {}", phoneNumber);
            // Clear OTP after successful verification
            clearOtp(phoneNumber);
        } else {
            log.warn("OTP verification failed for phone number: {}", phoneNumber);
        }
        
        return isValid;
    }

    @Override
    public boolean hasOtp(String phoneNumber) {
        String key = OTP_PREFIX + phoneNumber;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void clearOtp(String phoneNumber) {
        log.info("Clearing OTP for phone number: {}", phoneNumber);
        String key = OTP_PREFIX + phoneNumber;
        redisTemplate.delete(key);
    }

    @Override
    public String getOtpForTesting(String phoneNumber) {
        log.info("Retrieving OTP for testing for phone number: {}", phoneNumber);
        String key = OTP_PREFIX + phoneNumber;
        return redisTemplate.opsForValue().get(key);
    }
}
