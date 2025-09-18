package com.chitchat.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating group request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {
    
    @NotBlank(message = "Group name is required")
    private String name;
    
    private String description;
    private String avatarUrl;
    
    @NotEmpty(message = "At least one member is required")
    private List<Long> memberIds;
}
