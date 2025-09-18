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
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    public String verifyPhoneNumber(String phoneNumber) {
        try {
            // In a real implementation, you would verify the phone number with Firebase
            // For now, we'll simulate the verification
            log.info("Verifying phone number with Firebase: {}", phoneNumber);
            
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
        } catch (FirebaseAuthException e) {
            log.error("Firebase verification failed for phone number: {}", phoneNumber, e);
            throw new RuntimeException("Firebase verification failed", e);
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
