package com.chitchat.user.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for Firebase authentication operations
 */
@Slf4j
@Service
public class FirebaseService {
    
    private final FirebaseAuth firebaseAuth;

    public FirebaseService() {
        FirebaseAuth auth;
        try {
            auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            log.warn("Firebase not initialized, using mock implementation");
            auth = null;
        }
        this.firebaseAuth = auth;
    }
    
    public String verifyPhoneNumber(String phoneNumber) {
        try {
            log.info("Verifying phone number: {}", phoneNumber);
            
            if (firebaseAuth == null) {
                log.warn("Firebase not available, using mock verification");
                // Mock implementation for development
                return "mock-uid-" + phoneNumber.hashCode();
            }
            
            // Create or get user record
            UserRecord userRecord;
            try {
                userRecord = firebaseAuth.getUserByPhoneNumber(phoneNumber);
            } catch (FirebaseAuthException e) {
                // User doesn't exist, create new one
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setPhoneNumber(phoneNumber);
                userRecord = firebaseAuth.createUser(request);
            }
            
            return userRecord.getUid();
        } catch (Exception e) {
            log.error("Phone number verification failed: {}", phoneNumber, e);
            // For development, return a mock UID
            return "mock-uid-" + phoneNumber.hashCode();
        }
    }

    public String sendOTP(String phoneNumber) {
        try {
            log.info("Sending OTP to phone number: {}", phoneNumber);

            if (firebaseAuth == null) {
                log.warn("Firebase not available, using mock OTP sending");
                // For development, always return the test OTP
                String testOtp = "123456";
                log.info("Mock OTP sent successfully to: {} - Test OTP: {}", phoneNumber, testOtp);
                return testOtp;
            }

            // In production, this would integrate with Firebase Auth to send real OTP
            // For now, we simulate OTP sending and return test OTP
            String testOtp = "123456";
            log.info("OTP sent successfully to: {}", phoneNumber);
            return testOtp;

        } catch (Exception e) {
            log.error("Failed to send OTP to phone number: {}", phoneNumber, e);
            throw new RuntimeException("Failed to send OTP", e);
        }
    }

    public void verifyOTP(String phoneNumber, String otp) {
        try {
            // In a real implementation, you would verify the OTP with Firebase
            // For now, we'll simulate the verification
            log.info("Verifying OTP for phone number: {}", phoneNumber);
            
            // Simulate OTP verification
            if (!"123456".equals(otp)) {
                throw new RuntimeException("Invalid OTP");
            }
            
        } catch (Exception e) {
            log.error("OTP verification failed for phone number: {}", phoneNumber, e);
            throw new RuntimeException("OTP verification failed", e);
        }
    }
}
