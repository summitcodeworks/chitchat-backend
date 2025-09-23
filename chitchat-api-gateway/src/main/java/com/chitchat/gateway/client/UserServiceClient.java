package com.chitchat.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for User Service
 */
@FeignClient(name = "chitchat-user-service")
public interface UserServiceClient {
    
    /**
     * Get user by phone number
     */
    @GetMapping("/api/users/phone/{phoneNumber}")
    UserResponse getUserByPhoneNumber(@PathVariable String phoneNumber);
    
    /**
     * User response DTO
     */
    class UserResponse {
        private Long id;
        private String phoneNumber;
        private String name;
        private String avatarUrl;
        private String about;
        private String lastSeen;
        private Boolean isOnline;
        private String createdAt;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        
        public String getAbout() { return about; }
        public void setAbout(String about) { this.about = about; }
        
        public String getLastSeen() { return lastSeen; }
        public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
        
        public Boolean getIsOnline() { return isOnline; }
        public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
