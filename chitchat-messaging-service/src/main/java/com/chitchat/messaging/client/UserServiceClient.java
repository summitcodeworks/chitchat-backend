package com.chitchat.messaging.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for calling User Service APIs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {
    
    private final RestTemplate restTemplate;
    private static final String USER_SERVICE_URL = "http://localhost:9102";
    
    /**
     * Get user details by user ID
     */
    public UserDto getUserById(Long userId) {
        try {
            String url = USER_SERVICE_URL + "/api/users/" + userId;
            ApiResponse response = restTemplate.getForObject(url, ApiResponse.class);
            
            if (response != null && response.isSuccess() && response.getData() != null) {
                // Convert LinkedHashMap to UserDto
                Object data = response.getData();
                if (data instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) data;
                    UserDto user = new UserDto();
                    user.setId(userId);
                    user.setName((String) map.get("name"));
                    user.setPhoneNumber((String) map.get("phoneNumber"));
                    user.setAvatarUrl((String) map.get("avatarUrl"));
                    user.setAbout((String) map.get("about"));
                    return user;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error fetching user by ID: {}", userId, e);
            return null;
        }
    }
    
    @Data
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
    
    @Data
    public static class UserDto {
        private Long id;
        private String phoneNumber;
        private String name;
        private String avatarUrl;
        private String about;
    }
}
