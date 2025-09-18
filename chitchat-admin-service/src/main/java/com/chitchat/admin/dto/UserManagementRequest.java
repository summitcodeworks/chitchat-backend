package com.chitchat.admin.dto;

import com.chitchat.admin.entity.AdminUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user management request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Action is required")
    private UserAction action;
    
    private String reason;
    
    public enum UserAction {
        BAN, UNBAN, SUSPEND, UNSUSPEND, DELETE
    }
}
