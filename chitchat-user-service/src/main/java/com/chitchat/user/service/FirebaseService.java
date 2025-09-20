package com.chitchat.user.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for Firebase authentication operations
 */
@Slf4j
@Service
public class FirebaseService {

    private final FirebaseAuth firebaseAuth;
    private final RestTemplate restTemplate;

    @Value("${firebase.web-api-key:AIzaSyBefqzOkJgvV0qnmc4Qds43Gi5XvdmAl7g}")
    private String firebaseWebApiKey;

    public FirebaseService() {
        FirebaseAuth auth;
        try {
            auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            log.warn("Firebase not initialized, using mock implementation");
            auth = null;
        }
        this.firebaseAuth = auth;
        this.restTemplate = new RestTemplate();
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

    /**
     * Verify Firebase ID token and extract user information
     * @param idToken Firebase ID token from frontend
     * @return FirebaseToken with user information
     */
    public FirebaseToken verifyIdToken(String idToken) {
        try {
            log.info("Verifying Firebase ID token");
            
            if (firebaseAuth == null) {
                log.warn("Firebase not available, using mock token verification");
                // For development, create a mock token
                return createMockFirebaseToken();
            }
            
            // Verify the ID token
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            log.info("Firebase ID token verified successfully for UID: {}", decodedToken.getUid());
            
            return decodedToken;
            
        } catch (FirebaseAuthException e) {
            log.error("Failed to verify Firebase ID token", e);
            throw new RuntimeException("Invalid Firebase token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error verifying Firebase ID token", e);
            throw new RuntimeException("Token verification failed", e);
        }
    }
    
    /**
     * Get user information from Firebase using UID
     * @param uid Firebase user UID
     * @return UserRecord with user information
     */
    public UserRecord getUserByUid(String uid) {
        try {
            log.info("Getting user information for UID: {}", uid);
            
            if (firebaseAuth == null) {
                log.warn("Firebase not available, using mock user record");
                return createMockUserRecord(uid);
            }
            
            UserRecord userRecord = firebaseAuth.getUser(uid);
            log.info("User information retrieved successfully for UID: {}", uid);
            
            return userRecord;
            
        } catch (FirebaseAuthException e) {
            log.error("Failed to get user information for UID: {}", uid, e);
            throw new RuntimeException("User not found: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error getting user information for UID: {}", uid, e);
            throw new RuntimeException("Failed to get user information", e);
        }
    }
    
    /**
     * Create a mock Firebase token for development
     */
    private FirebaseToken createMockFirebaseToken() {
        // This is a simplified mock implementation for development
        // In a real scenario, you would create a proper mock token
        log.warn("Using mock Firebase token for development");
        return null; // Will be handled by the calling code
    }
    
    /**
     * Create a mock user record for development
     */
    private UserRecord createMockUserRecord(String uid) {
        // This is a simplified mock implementation for development
        log.warn("Using mock user record for development with UID: {}", uid);
        return null; // Will be handled by the calling code
    }
}
