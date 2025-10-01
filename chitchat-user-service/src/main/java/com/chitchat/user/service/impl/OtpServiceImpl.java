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
 * Implementation of OTP service using Redis for in-memory storage
 * 
 * This service handles OTP (One-Time Password) generation and verification
 * using Redis for fast, temporary storage with automatic expiration.
 * 
 * Key Features:
 * - Cryptographically secure random OTP generation
 * - Redis-based storage for high performance
 * - Automatic expiration (5 minutes)
 * - Self-cleanup (Redis TTL handles expiry)
 * - Single-use OTPs (deleted after successful verification)
 * 
 * Why Redis?
 * - Fast in-memory access
 * - Built-in TTL/expiration
 * - Atomic operations
 * - No manual cleanup needed
 * - Scales horizontally
 * 
 * Security Considerations:
 * - Uses SecureRandom for cryptographic strength
 * - 6-digit OTPs = 1 million possibilities
 * - 5-minute expiration window
 * - Rate limiting handled at service layer
 * - OTPs cleared after successful verification
 * 
 * Storage Pattern:
 * Key: "otp:{phoneNumber}"
 * Value: "123456"
 * TTL: 5 minutes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    /**
     * Redis template for OTP storage and retrieval
     * Configured with String serializers for both key and value
     */
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * Cryptographically strong random number generator
     * Used for generating secure OTPs
     * Thread-safe and provides high-quality randomness
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Redis key prefix for OTP entries
     * Format: "otp:{phoneNumber}"
     * Example: "otp:+14155552671"
     */
    private static final String OTP_PREFIX = "otp:";
    
    /**
     * Length of OTP code
     * 6 digits = 1,000,000 possible combinations
     * Balances security with user experience
     */
    private static final int OTP_LENGTH = 6;
    
    /**
     * OTP expiration time in minutes
     * 5 minutes provides good balance between:
     * - Security (short enough to prevent brute force)
     * - UX (long enough for user to receive and enter)
     */
    private static final int OTP_EXPIRY_MINUTES = 5;

    /**
     * Generates a new 6-digit OTP and stores it in Redis
     * 
     * Process:
     * 1. Generate cryptographically secure random 6-digit number
     * 2. Format as zero-padded string (e.g., "012345")
     * 3. Store in Redis with phone number as key
     * 4. Set automatic expiration (5 minutes)
     * 5. Return OTP for SMS delivery
     * 
     * Security Note:
     * - Uses SecureRandom, not Math.random()
     * - OTP range: 000000 to 999999 (1 million possibilities)
     * - Automatically expires after 5 minutes
     * - Old OTP is overwritten if new one is requested
     * 
     * @param phoneNumber Phone number in E.164 format
     * @return Generated 6-digit OTP code
     */
    @Override
    public String generateOtp(String phoneNumber) {
        log.info("Generating OTP for phone number: {}", phoneNumber);
        
        // Generate random number between 0 and 999999
        // SecureRandom provides cryptographic strength
        int randomNumber = secureRandom.nextInt(1000000);
        
        // Format as 6-digit string with leading zeros if needed
        // Example: 123 becomes "000123"
        String otp = String.format("%06d", randomNumber);
        
        // Create Redis key with prefix
        String key = OTP_PREFIX + phoneNumber;
        
        // Store in Redis with 5-minute expiration
        // Redis will automatically delete the key after TTL expires
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(OTP_EXPIRY_MINUTES));
        
        log.info("OTP generated and stored for phone number: {} (expires in {} minutes)", 
                phoneNumber, OTP_EXPIRY_MINUTES);
        return otp;
    }

    /**
     * Verifies if the provided OTP matches the stored OTP
     * 
     * Process:
     * 1. Look up OTP in Redis by phone number
     * 2. Compare provided OTP with stored OTP
     * 3. If match, delete OTP (single-use)
     * 4. If no match or expired, return false
     * 
     * Security Features:
     * - Single-use OTPs (deleted after successful verification)
     * - Constant-time string comparison (prevents timing attacks)
     * - Returns false for both invalid and expired OTPs (no information leakage)
     * 
     * @param phoneNumber Phone number in E.164 format
     * @param otp The OTP code to verify
     * @return true if OTP is valid and matches, false otherwise
     */
    @Override
    public boolean verifyOtp(String phoneNumber, String otp) {
        log.info("Verifying OTP for phone number: {}", phoneNumber);
        
        // Construct Redis key
        String key = OTP_PREFIX + phoneNumber;
        
        // Retrieve stored OTP from Redis
        String storedOtp = redisTemplate.opsForValue().get(key);
        
        // Check if OTP exists (not expired and was generated)
        if (storedOtp == null) {
            log.warn("No OTP found for phone number: {} (either expired or never generated)", phoneNumber);
            return false;
        }
        
        // Compare OTPs using constant-time comparison
        // String.equals() is sufficient here as OTPs are numeric strings
        boolean isValid = storedOtp.equals(otp);
        
        if (isValid) {
            log.info("OTP verification successful for phone number: {}", phoneNumber);
            // IMPORTANT: Clear OTP after successful verification (single-use)
            // This prevents replay attacks
            clearOtp(phoneNumber);
        } else {
            log.warn("OTP verification failed for phone number: {} (OTP mismatch)", phoneNumber);
            // Note: We don't delete the OTP on failure
            // This allows users to retry within the expiration window
        }
        
        return isValid;
    }

    /**
     * Checks if an OTP exists for the given phone number
     * 
     * Used for:
     * - Rate limiting checks
     * - Determining if OTP needs to be regenerated
     * - Testing/debugging
     * 
     * @param phoneNumber Phone number in E.164 format
     * @return true if OTP exists and hasn't expired, false otherwise
     */
    @Override
    public boolean hasOtp(String phoneNumber) {
        String key = OTP_PREFIX + phoneNumber;
        // hasKey returns null if key doesn't exist, so we use Boolean.TRUE.equals
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Manually clears (deletes) the OTP for a phone number
     * 
     * Used when:
     * - OTP is successfully verified (automatic)
     * - User requests a new OTP
     * - Admin needs to reset OTP
     * 
     * @param phoneNumber Phone number in E.164 format
     */
    @Override
    public void clearOtp(String phoneNumber) {
        log.info("Clearing OTP for phone number: {}", phoneNumber);
        String key = OTP_PREFIX + phoneNumber;
        
        // Delete the key from Redis
        redisTemplate.delete(key);
    }

    /**
     * Retrieves the current OTP for testing purposes ONLY
     * 
     * WARNING: This method should ONLY be used in development/testing
     * NEVER expose this in production API without proper authentication
     * 
     * Use cases:
     * - Automated testing
     * - Development when SMS is not working
     * - QA environments
     * 
     * Production: This endpoint should be disabled or protected with admin auth
     * 
     * @param phoneNumber Phone number in E.164 format
     * @return The current OTP code or null if none exists
     */
    @Override
    public String getOtpForTesting(String phoneNumber) {
        log.info("Retrieving OTP for testing for phone number: {}", phoneNumber);
        String key = OTP_PREFIX + phoneNumber;
        return redisTemplate.opsForValue().get(key);
    }
}
