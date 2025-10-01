package com.chitchat.user.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Admin SDK configuration for user service
 * 
 * Initializes Firebase Admin SDK for server-side Firebase operations:
 * - Firebase Authentication token verification
 * - Push notifications via FCM
 * - Firebase Realtime Database access
 * - Cloud Firestore operations
 * 
 * Firebase Features Used:
 * 1. Authentication: Verify Firebase ID tokens from clients
 * 2. Cloud Messaging (FCM): Send push notifications
 * 3. Admin SDK: Server-side user management
 * 
 * Configuration:
 * - Service Account: JSON key file for authentication
 * - Project ID: Firebase project identifier
 * - Credentials: Google Cloud credentials for API access
 * 
 * Service Account Key:
 * Location: src/main/resources/firebase-service-account.json
 * Contains:
 * - Project ID
 * - Private key
 * - Client email
 * - Token URIs
 * 
 * Security:
 * - Service account key must be kept secret
 * - Never commit key to version control
 * - Use environment variables or secrets manager in production
 * - Rotate keys periodically
 * 
 * Graceful Degradation:
 * - If Firebase key not found, service continues without Firebase
 * - Allows development without Firebase setup
 * - Falls back to SMS/OTP only authentication
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    /**
     * Firebase project ID
     * 
     * Identifies which Firebase project to connect to.
     * Default: chitchat-9c074
     * Override via application.yml: firebase.project-id
     */
    @Value("${firebase.project-id:chitchat-9c074}")
    private String projectId;

    /**
     * Initializes Firebase Admin SDK after Spring context creation
     * 
     * This method runs automatically after bean construction.
     * 
     * Initialization Process:
     * 1. Check if Firebase already initialized (prevent re-initialization)
     * 2. Load service account key from classpath resources
     * 3. Create GoogleCredentials from service account
     * 4. Build FirebaseOptions with credentials and project ID
     * 5. Initialize FirebaseApp singleton
     * 
     * Error Handling:
     * - If service account not found: Logs warning, continues without Firebase
     * - If initialization fails: Logs error, continues (graceful degradation)
     * - Service can still operate with SMS/OTP only
     * 
     * @PostConstruct annotation ensures this runs once after bean creation
     * 
     * Throws: No exceptions thrown - errors are logged and handled gracefully
     */
    @PostConstruct
    public void initializeFirebase() {
        try {
            // Check if Firebase is already initialized
            // FirebaseApp.getApps() returns list of initialized apps
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("Initializing Firebase for project: {}", projectId);

                // Try to load Firebase service account key from classpath
                // Expected location: src/main/resources/firebase-service-account.json
                InputStream serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");

                FirebaseOptions options;
                if (serviceAccount != null) {
                    log.info("Using service account key from resources");
                    
                    // Build Firebase options with credentials
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))  // Authenticate with Google
                            .setProjectId(projectId)                                       // Set Firebase project
                            .build();
                } else {
                    // Service account key not found - this is OK for development
                    log.warn("No service account key found, skipping Firebase initialization");
                    log.warn("Continuing without Firebase initialization - Firebase auth will not work");
                    log.warn("SMS/OTP authentication will still function normally");
                    return; // Exit early without throwing exception
                }

                // Initialize the Firebase App singleton
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully for project: {}", projectId);
            } else {
                // Firebase already initialized (multiple calls to @PostConstruct or manual init)
                log.info("Firebase already initialized");
            }
        } catch (Exception e) {
            // Log error but don't crash the application
            // This allows development without Firebase setup
            log.error("Failed to initialize Firebase", e);
            log.warn("Continuing without Firebase initialization - Firebase features disabled");
            log.warn("SMS/OTP authentication will still work");
            
            // In production, you might want to throw exception instead
            // For development, graceful degradation is better
        }
    }

}
