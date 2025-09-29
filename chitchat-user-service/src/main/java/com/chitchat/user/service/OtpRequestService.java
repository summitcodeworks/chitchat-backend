package com.chitchat.user.service;

import com.chitchat.user.entity.OtpRequest;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for OTP request management
 */
public interface OtpRequestService {

    /**
     * Save OTP request with full context
     */
    OtpRequest saveOtpRequest(OtpRequest otpRequest);

    /**
     * Find the latest valid OTP for a phone number
     */
    Optional<OtpRequest> findLatestValidOtp(String phoneNumber);

    /**
     * Find OTP for verification
     */
    Optional<OtpRequest> findOtpForVerification(String phoneNumber, String otpCode);

    /**
     * Mark OTP as verified
     */
    OtpRequest markAsVerified(Long otpRequestId);

    /**
     * Increment verification attempts
     */
    OtpRequest incrementVerificationAttempts(Long otpRequestId);

    /**
     * Get all OTP requests for a phone number
     */
    List<OtpRequest> getOtpHistory(String phoneNumber);

    /**
     * Check if phone number has exceeded OTP request limits
     */
    boolean hasExceededRequestLimit(String phoneNumber, int maxRequests, int timeWindowMinutes);

    /**
     * Get OTP request by ID
     */
    Optional<OtpRequest> findById(Long id);

    /**
     * Update OTP request with SMS sending result
     */
    OtpRequest updateSmsResult(Long otpRequestId, boolean smsSent, String errorMessage, String twilioMessageSid);

    /**
     * Cleanup expired OTPs
     */
    void cleanupExpiredOtps();
}