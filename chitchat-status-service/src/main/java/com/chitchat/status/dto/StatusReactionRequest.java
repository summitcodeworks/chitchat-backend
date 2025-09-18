package com.chitchat.status.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reacting to status request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusReactionRequest {
    
    @NotBlank(message = "Emoji is required")
    private String emoji;
}
