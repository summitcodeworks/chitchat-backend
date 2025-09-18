package com.chitchat.calls.dto;

import com.chitchat.calls.entity.Call;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for initiating a call request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateCallRequest {
    
    @NotNull(message = "Callee ID is required")
    private Long calleeId;
    
    @NotNull(message = "Call type is required")
    private Call.CallType callType;
    
    private String callerSdp;
}
