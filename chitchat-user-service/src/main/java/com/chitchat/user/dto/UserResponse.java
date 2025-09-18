package com.chitchat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String phoneNumber;
    private String name;
    private String avatarUrl;
    private String about;
    private LocalDateTime lastSeen;
    private Boolean isOnline;
    private LocalDateTime createdAt;
}
