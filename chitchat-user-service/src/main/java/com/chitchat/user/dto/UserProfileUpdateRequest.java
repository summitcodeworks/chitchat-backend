package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user profile update request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String avatarUrl;
    
    private String about;
}
