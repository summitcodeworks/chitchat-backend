package com.chitchat.messaging.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for User Service
 */
@FeignClient(name = "chitchat-user-service")
public interface UserServiceClient {
    
    /**
     * Get user ID by Firebase UID
     */
    @GetMapping("/api/users/firebase-uid/{firebaseUid}")
    Long getUserIdByFirebaseUid(@PathVariable String firebaseUid, @RequestHeader("Authorization") String token);
}
