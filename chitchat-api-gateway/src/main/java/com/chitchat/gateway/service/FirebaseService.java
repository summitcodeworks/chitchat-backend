package com.chitchat.gateway.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Firebase service for token validation and user authentication
 */
@Slf4j
@Service
public class FirebaseService {

    @Value("${firebase.project-id:chitchat-9c074}")
    private String projectId;

    private FirebaseAuth firebaseAuth;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("Initializing Firebase for project: {}", projectId);

                // Try to load service account key from resources
                InputStream serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");

                if (serviceAccount != null) {
                    log.info("Using service account key from resources");
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setProjectId(projectId)
                            .build();

                    FirebaseApp.initializeApp(options);
                    this.firebaseAuth = FirebaseAuth.getInstance();
                    log.info("Firebase initialized successfully for project: {}", projectId);
                } else {
                    log.warn("No service account key found, Firebase will not be available");
                    this.firebaseAuth = null;
                }
            } else {
                log.info("Firebase already initialized");
                this.firebaseAuth = FirebaseAuth.getInstance();
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase", e);
            this.firebaseAuth = null;
        }
    }

    /**
     * Verify Firebase ID token and extract user information
     */
    public Map<String, Object> verifyIdToken(String idToken) {
        Map<String, Object> result = new HashMap<>();
        
        if (firebaseAuth == null) {
            log.warn("Firebase not initialized, cannot verify token");
            result.put("valid", false);
            result.put("error", "Firebase not initialized");
            return result;
        }

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            
            result.put("valid", true);
            result.put("uid", decodedToken.getUid());
            result.put("phoneNumber", decodedToken.getClaims().get("phone_number"));
            result.put("email", decodedToken.getEmail());
            result.put("name", decodedToken.getName());
            result.put("picture", decodedToken.getPicture());
            result.put("issuer", decodedToken.getIssuer());
            result.put("audience", decodedToken.getClaims().get("aud"));
            result.put("issuedAt", decodedToken.getClaims().get("iat"));
            result.put("expiresAt", decodedToken.getClaims().get("exp"));
            
            log.info("Firebase token verified for user: {}", decodedToken.getUid());
            return result;
            
        } catch (FirebaseAuthException e) {
            log.error("Firebase token verification failed: {}", e.getMessage());
            result.put("valid", false);
            result.put("error", e.getMessage());
            return result;
        } catch (Exception e) {
            log.error("Unexpected error during Firebase token verification", e);
            result.put("valid", false);
            result.put("error", "Unexpected error");
            return result;
        }
    }

    /**
     * Check if Firebase is available
     */
    public boolean isFirebaseAvailable() {
        return firebaseAuth != null;
    }

    /**
     * Get user information from Firebase UID
     */
    public Map<String, Object> getUserInfo(String uid) {
        Map<String, Object> result = new HashMap<>();
        
        if (firebaseAuth == null) {
            log.warn("Firebase not initialized, cannot get user info");
            result.put("error", "Firebase not initialized");
            return result;
        }

        try {
            var userRecord = firebaseAuth.getUser(uid);
            
            result.put("uid", userRecord.getUid());
            result.put("email", userRecord.getEmail());
            result.put("phoneNumber", userRecord.getPhoneNumber());
            result.put("displayName", userRecord.getDisplayName());
            result.put("photoUrl", userRecord.getPhotoUrl());
            result.put("emailVerified", userRecord.isEmailVerified());
            result.put("disabled", userRecord.isDisabled());
            result.put("creationTime", userRecord.getUserMetadata().getCreationTimestamp());
            result.put("lastSignInTime", userRecord.getUserMetadata().getLastSignInTimestamp());
            
            return result;
            
        } catch (FirebaseAuthException e) {
            log.error("Failed to get user info for UID {}: {}", uid, e.getMessage());
            result.put("error", e.getMessage());
            return result;
        }
    }
}
