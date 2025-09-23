package com.chitchat.notification.config;

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
 * Firebase configuration for notification service
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id:chitchat-9c074}")
    private String projectId;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("Initializing Firebase for project: {}", projectId);

                // Try to load service account key from resources
                InputStream serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");

                FirebaseOptions options;
                if (serviceAccount != null) {
                    log.info("Using service account key from resources");
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setProjectId(projectId)
                            .build();
                } else {
                    log.warn("No service account key found, skipping Firebase initialization");
                    log.warn("Continuing without Firebase initialization - notifications will be mocked");
                    return; // Exit early without throwing exception
                }

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully for project: {}", projectId);
            } else {
                log.info("Firebase already initialized");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase", e);
            // For development, we'll continue without Firebase
            log.warn("Continuing without Firebase initialization - notifications will be mocked");
        }
    }

}
