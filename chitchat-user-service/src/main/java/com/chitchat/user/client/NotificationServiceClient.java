package com.chitchat.user.client;

import com.chitchat.shared.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for Notification Service
 */
@FeignClient(name = "chitchat-notification-service")
public interface NotificationServiceClient {
    
    @PostMapping("/api/notifications/send-by-phone")
    ApiResponse<Void> sendNotificationByPhone(@RequestBody SendNotificationByPhoneDto request);
    
    class SendNotificationByPhoneDto {
        private String phoneNumber;
        private String title;
        private String body;
        private String type;
        private String imageUrl;
        private String actionUrl;
        private Map<String, Object> data;
        
        public SendNotificationByPhoneDto() {
        }
        
        public SendNotificationByPhoneDto(String phoneNumber, String title, String body, String type) {
            this.phoneNumber = phoneNumber;
            this.title = title;
            this.body = body;
            this.type = type;
        }
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setBody(String body) {
            this.body = body;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getImageUrl() {
            return imageUrl;
        }
        
        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
        
        public String getActionUrl() {
            return actionUrl;
        }
        
        public void setActionUrl(String actionUrl) {
            this.actionUrl = actionUrl;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}
