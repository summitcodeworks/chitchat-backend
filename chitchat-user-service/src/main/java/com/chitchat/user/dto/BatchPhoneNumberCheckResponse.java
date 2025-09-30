package com.chitchat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch phone number existence check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPhoneNumberCheckResponse {
    private List<PhoneNumberCheckResponse> results;
    private int totalChecked;
    private int foundCount;
    private int notFoundCount;
    private String message;
}
