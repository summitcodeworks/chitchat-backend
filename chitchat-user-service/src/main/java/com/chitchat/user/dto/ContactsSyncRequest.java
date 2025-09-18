package com.chitchat.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for contacts synchronization request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactsSyncRequest {
    
    @NotEmpty(message = "Phone numbers list cannot be empty")
    private List<String> phoneNumbers;
}
