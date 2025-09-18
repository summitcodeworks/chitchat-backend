package com.chitchat.status.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for viewing status request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusViewRequest {
    
    private String statusId;
    private Long viewerId;
}
