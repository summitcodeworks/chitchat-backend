package com.chitchat.status.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
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
    private List<StatusView> views;
    private List<StatusReaction> reactions;
    private LocalDateTime lastActivity;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum StatusType {
        TEXT, IMAGE, VIDEO, AUDIO
    }
    
    public enum StatusPrivacy {
        PUBLIC, CONTACTS_ONLY, SELECTED_CONTACTS
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
