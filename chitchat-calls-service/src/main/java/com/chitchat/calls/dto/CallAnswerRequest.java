package com.chitchat.calls.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for answering a call request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallAnswerRequest {
    
    private String calleeSdp;
    private String iceCandidates;
}
