package com.chitchat.status.dto;

import com.chitchat.status.document.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for status response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusResponse {
    
    private String id;
    private Long userId;
    private String content;
    private String mediaUrl;
    private String thumbnailUrl;
    private Status.StatusType type;
    private Status.StatusPrivacy privacy;
    private LocalDateTime expiresAt;
    private List<StatusViewResponse> views;
    private List<StatusReactionResponse> reactions;
    private LocalDateTime lastActivity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusViewResponse {
        private Long userId;
        private LocalDateTime viewedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusReactionResponse {
        private Long userId;
        private String emoji;
        private LocalDateTime reactedAt;
    }
}
