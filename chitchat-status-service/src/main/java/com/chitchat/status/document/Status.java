package com.chitchat.status.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Status document for MongoDB
 */
@Document(collection = "statuses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    
    @Id
    private String id;
    
    private Long userId;
    private String content;
    private String mediaUrl;
    private String thumbnailUrl;
    private StatusType type;
    private StatusPrivacy privacy;
    private LocalDateTime expiresAt;
    
    @Builder.Default
    private List<StatusView> views = new ArrayList<>();
    
    @Builder.Default
    private List<StatusReaction> reactions = new ArrayList<>();
    
    private LocalDateTime lastActivity;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum StatusType {
        TEXT, IMAGE, VIDEO, AUDIO;
        
        @JsonCreator
        public static StatusType fromString(String value) {
            if (value == null) {
                return null;
            }
            
            String upperValue = value.trim().toUpperCase();
            
            switch (upperValue) {
                case "TEXT":
                    return TEXT;
                case "IMAGE":
                case "IMG":
                case "PHOTO":
                    return IMAGE;
                case "VIDEO":
                case "VID":
                    return VIDEO;
                case "AUDIO":
                case "VOICE":
                    return AUDIO;
                default:
                    throw new IllegalArgumentException(
                        "Invalid StatusType value: '" + value + "'. " +
                        "Accepted values are: TEXT, IMAGE, VIDEO, AUDIO"
                    );
            }
        }
        
        @JsonValue
        public String toValue() {
            return this.name();
        }
    }
    
    public enum StatusPrivacy {
        PUBLIC, CONTACTS_ONLY, SELECTED_CONTACTS;
        
        @JsonCreator
        public static StatusPrivacy fromString(String value) {
            if (value == null) {
                return null;
            }
            
            String upperValue = value.trim().toUpperCase();
            
            // Handle common aliases
            switch (upperValue) {
                case "CONTACTS":
                    return CONTACTS_ONLY;
                case "CONTACTS_ONLY":
                    return CONTACTS_ONLY;
                case "PUBLIC":
                    return PUBLIC;
                case "SELECTED_CONTACTS":
                case "SELECTED":
                    return SELECTED_CONTACTS;
                default:
                    throw new IllegalArgumentException(
                        "Invalid StatusPrivacy value: '" + value + "'. " +
                        "Accepted values are: PUBLIC, CONTACTS, CONTACTS_ONLY, SELECTED_CONTACTS"
                    );
            }
        }
        
        @JsonValue
        public String toValue() {
            return this.name();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusView {
        private Long userId;
        private LocalDateTime viewedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusReaction {
        private Long userId;
        private String emoji;
        private LocalDateTime reactedAt;
    }
}
