package com.chitchat.calls.service;

import com.chitchat.calls.dto.*;
import com.chitchat.calls.entity.Call;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for call operations
 */
public interface CallService {
    
    CallResponse initiateCall(Long callerId, InitiateCallRequest request);
    
    CallResponse answerCall(String sessionId, Long calleeId, CallAnswerRequest request);
    
    CallResponse rejectCall(String sessionId, Long calleeId, String reason);
    
    CallResponse endCall(String sessionId, Long userId, String reason);
    
    CallResponse getCallBySessionId(String sessionId);
    
    Page<CallResponse> getUserCallHistory(Long userId, Pageable pageable);
    
    List<CallResponse> getMissedCalls(Long userId);
    
    List<CallResponse> getRecentCalls(Long userId, int limit);
    
    void cleanupStuckCalls();
    
    CallResponse updateCallStatus(String sessionId, Call.CallStatus status);
}
