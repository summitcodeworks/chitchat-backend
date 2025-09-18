package com.chitchat.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for analytics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    
    private Long totalUsers;
    private Long activeUsers;
    private Long totalMessages;
    private Long totalCalls;
    private Long totalStatuses;
    private Long totalMediaFiles;
    
    private Map<String, Long> messagesByDay;
    private Map<String, Long> callsByDay;
    private Map<String, Long> usersByDay;
    
    private LocalDateTime generatedAt;
}
