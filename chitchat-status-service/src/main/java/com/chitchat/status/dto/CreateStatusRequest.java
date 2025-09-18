package com.chitchat.status.dto;

import com.chitchat.status.document.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating status request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStatusRequest {
    
    @NotBlank(message = "Status content is required")
    private String content;
    
    private String mediaUrl;
    private String thumbnailUrl;
    
    @NotNull(message = "Status type is required")
    private Status.StatusType type;
    
    @NotNull(message = "Status privacy is required")
    private Status.StatusPrivacy privacy;
    
    private List<Long> selectedContacts; // For SELECTED_CONTACTS privacy
}
