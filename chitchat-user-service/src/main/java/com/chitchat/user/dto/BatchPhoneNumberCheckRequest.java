package com.chitchat.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch phone number existence check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPhoneNumberCheckRequest {
    
    @NotEmpty(message = "Phone numbers list cannot be empty")
    private List<String> phoneNumbers;
}
