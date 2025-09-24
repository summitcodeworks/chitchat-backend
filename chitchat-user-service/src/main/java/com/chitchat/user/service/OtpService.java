package com.chitchat.user.service;

/**
 * Service interface for OTP operations
 */
public interface OtpService {
    
    /**
     * Generate and store OTP for the given phone number
     * @param phoneNumber The phone number
     * @return The generated OTP code
     */
    String generateOtp(String phoneNumber);
    
    /**
     * Verify OTP for the given phone number
     * @param phoneNumber The phone number
     * @param otp The OTP to verify
     * @return true if OTP is valid, false otherwise
     */
    boolean verifyOtp(String phoneNumber, String otp);
    
    /**
     * Check if OTP exists for the given phone number
     * @param phoneNumber The phone number
     * @return true if OTP exists, false otherwise
     */
    boolean hasOtp(String phoneNumber);
    
    /**
     * Clear OTP for the given phone number
     * @param phoneNumber The phone number
     */
    void clearOtp(String phoneNumber);
}
