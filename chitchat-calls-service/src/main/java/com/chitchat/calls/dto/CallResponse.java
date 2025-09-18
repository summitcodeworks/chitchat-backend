package com.chitchat.calls.dto;

import com.chitchat.calls.entity.Call;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for call response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallResponse {
    
    private Long id;
    private String sessionId;
    private Long callerId;
    private Long calleeId;
    private Call.CallType callType;
    private Call.CallStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long duration;
    private String callerSdp;
    private String calleeSdp;
    private String iceCandidates;
    private String rejectionReason;
    private String endReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
