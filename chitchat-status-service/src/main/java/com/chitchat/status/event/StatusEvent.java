package com.chitchat.status.event;

import com.chitchat.status.document.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event DTO for status events published to Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusEvent {
    
    private String eventType;
    private String statusId;
    private Long userId;
    private Status.StatusType type;
    private Status.StatusPrivacy privacy;
    private LocalDateTime timestamp;
    private String content;
    private String mediaUrl;
    private LocalDateTime expiresAt;
    
    public static StatusEvent fromStatus(Status status, String eventType) {
        return StatusEvent.builder()
                .eventType(eventType)
                .statusId(status.getId())
                .userId(status.getUserId())
                .type(status.getType())
                .privacy(status.getPrivacy())
                .content(status.getContent())
                .mediaUrl(status.getMediaUrl())
                .expiresAt(status.getExpiresAt())
                .timestamp(LocalDateTime.now())
                .build();
    }
}

