package com.chitchat.user.service.impl;

import com.chitchat.user.entity.OtpRequest;
import com.chitchat.user.repository.OtpRequestRepository;
import com.chitchat.user.service.OtpRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of OtpRequestService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpRequestServiceImpl implements OtpRequestService {

    private final OtpRequestRepository otpRequestRepository;

    @Override
    @Transactional
    public OtpRequest saveOtpRequest(OtpRequest otpRequest) {
        log.info("Saving OTP request for phone number: {}", otpRequest.getPhoneNumber());
        return otpRequestRepository.save(otpRequest);
    }

    @Override
    public Optional<OtpRequest> findLatestValidOtp(String phoneNumber) {
        log.debug("Finding latest valid OTP for phone number: {}", phoneNumber);
        return otpRequestRepository.findLatestValidOtpByPhoneNumber(phoneNumber, LocalDateTime.now());
    }

    @Override
    public Optional<OtpRequest> findOtpForVerification(String phoneNumber, String otpCode) {
        log.debug("Finding OTP for verification - phone: {}, code: {}", phoneNumber, otpCode);
        return otpRequestRepository.findValidOtpForVerification(phoneNumber, otpCode, LocalDateTime.now());
    }

    @Override
    @Transactional
    public OtpRequest markAsVerified(Long otpRequestId) {
        log.info("Marking OTP request as verified: {}", otpRequestId);
        OtpRequest otpRequest = otpRequestRepository.findById(otpRequestId)
            .orElseThrow(() -> new IllegalArgumentException("OTP request not found: " + otpRequestId));

        otpRequest.setIsVerified(true);
        otpRequest.setVerifiedAt(LocalDateTime.now());

        return otpRequestRepository.save(otpRequest);
    }

    @Override
    @Transactional
    public OtpRequest incrementVerificationAttempts(Long otpRequestId) {
        log.debug("Incrementing verification attempts for OTP request: {}", otpRequestId);
        OtpRequest otpRequest = otpRequestRepository.findById(otpRequestId)
            .orElseThrow(() -> new IllegalArgumentException("OTP request not found: " + otpRequestId));

        otpRequest.setVerificationAttempts(otpRequest.getVerificationAttempts() + 1);
        otpRequest.setLastVerificationAttempt(LocalDateTime.now());

        return otpRequestRepository.save(otpRequest);
    }

    @Override
    public List<OtpRequest> getOtpHistory(String phoneNumber) {
        log.debug("Getting OTP history for phone number: {}", phoneNumber);
        return otpRequestRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber);
    }

    @Override
    public boolean hasExceededRequestLimit(String phoneNumber, int maxRequests, int timeWindowMinutes) {
        log.debug("Checking OTP request limit for phone: {} (max: {}, window: {} min)",
                 phoneNumber, maxRequests, timeWindowMinutes);

        LocalDateTime since = LocalDateTime.now().minusMinutes(timeWindowMinutes);
        long count = otpRequestRepository.countUnverifiedOtpsSince(phoneNumber, since);

        boolean exceeded = count >= maxRequests;
        if (exceeded) {
            log.warn("Phone number {} has exceeded OTP request limit: {} requests in {} minutes",
                    phoneNumber, count, timeWindowMinutes);
        }

        return exceeded;
    }

    @Override
    public Optional<OtpRequest> findById(Long id) {
        return otpRequestRepository.findById(id);
    }

    @Override
    @Transactional
    public OtpRequest updateSmsResult(Long otpRequestId, boolean smsSent, String errorMessage, String twilioMessageSid) {
        log.info("Updating SMS result for OTP request: {} - sent: {}", otpRequestId, smsSent);

        OtpRequest otpRequest = otpRequestRepository.findById(otpRequestId)
            .orElseThrow(() -> new IllegalArgumentException("OTP request not found: " + otpRequestId));

        otpRequest.setSmsSent(smsSent);
        otpRequest.setSmsErrorMessage(errorMessage);
        otpRequest.setTwilioMessageSid(twilioMessageSid);

        return otpRequestRepository.save(otpRequest);
    }

    @Override
    @Transactional
    public void cleanupExpiredOtps() {
        log.info("Starting cleanup of expired OTPs");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // Keep records for 24 hours

        try {
            otpRequestRepository.deleteExpiredOtps(cutoff);
            log.info("Completed cleanup of expired OTPs older than {}", cutoff);
        } catch (Exception e) {
            log.error("Error during OTP cleanup", e);
        }
    }
}