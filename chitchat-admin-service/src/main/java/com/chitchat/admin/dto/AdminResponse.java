package com.chitchat.admin.dto;

import com.chitchat.admin.entity.AdminUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for admin response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResponse {
    
    private Long id;
    private String username;
    private String email;
    private AdminUser.AdminRole role;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
