package com.chitchat.notification.client;

import com.chitchat.shared.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for User Service
 */
@FeignClient(name = "chitchat-user-service")
public interface UserServiceClient {
    
    @GetMapping("/api/users/phone/{phoneNumber}")
    ApiResponse<UserDto> getUserByPhoneNumber(@PathVariable String phoneNumber);
    
    class UserDto {
        private Long id;
        private String phoneNumber;
        private String name;
        private String avatarUrl;
        private String deviceToken;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getAvatarUrl() {
            return avatarUrl;
        }
        
        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
        
        public String getDeviceToken() {
            return deviceToken;
        }
        
        public void setDeviceToken(String deviceToken) {
            this.deviceToken = deviceToken;
        }
    }
}
