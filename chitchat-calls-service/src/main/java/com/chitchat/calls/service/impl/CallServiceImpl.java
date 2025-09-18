package com.chitchat.calls.service.impl;

import com.chitchat.calls.dto.*;
import com.chitchat.calls.entity.Call;
import com.chitchat.calls.repository.CallRepository;
import com.chitchat.calls.service.CallService;
import com.chitchat.calls.service.NotificationService;
import com.chitchat.shared.exception.ChitChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of CallService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallServiceImpl implements CallService {
    
    private final CallRepository callRepository;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    @Transactional
    public CallResponse initiateCall(Long callerId, InitiateCallRequest request) {
        log.info("Initiating call from user {} to user {}", callerId, request.getCalleeId());
        
        // Check if there's already an active call between these users
        List<Call> activeCalls = callRepository.findByUserIdAndStatus(callerId, Call.CallStatus.RINGING);
        if (!activeCalls.isEmpty()) {
            throw new ChitChatException("User already has an active call", HttpStatus.CONFLICT, "ACTIVE_CALL_EXISTS");
        }
        
        // Generate unique session ID
        String sessionId = UUID.randomUUID().toString();
        
        // Create call record
        Call call = Call.builder()
                .sessionId(sessionId)
                .callerId(callerId)
                .calleeId(request.getCalleeId())
                .callType(request.getCallType())
                .status(Call.CallStatus.INITIATED)
                .callerSdp(request.getCallerSdp())
                .build();
        
        call = callRepository.save(call);
        
        // Send push notification to callee
        notificationService.sendCallNotification(request.getCalleeId(), callerId, request.getCallType(), sessionId);
        
        // Publish call event to Kafka
        publishCallEvent(call, "CALL_INITIATED");
        
        log.info("Call initiated successfully with session ID: {}", sessionId);
        
        return mapToCallResponse(call);
    }
    
    @Override
    @Transactional
    public CallResponse answerCall(String sessionId, Long calleeId, CallAnswerRequest request) {
        log.info("Answering call with session ID: {}", sessionId);
        
        Call call = callRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ChitChatException("Call not found", HttpStatus.NOT_FOUND, "CALL_NOT_FOUND"));
        
        // Check if user is the callee
        if (!call.getCalleeId().equals(calleeId)) {
            throw new ChitChatException("Unauthorized to answer this call", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        // Check if call is in correct status
        if (call.getStatus() != Call.CallStatus.RINGING) {
            throw new ChitChatException("Call is not in ringing status", HttpStatus.BAD_REQUEST, "INVALID_CALL_STATUS");
        }
        
        // Update call with callee's SDP and ICE candidates
        call.setCalleeSdp(request.getCalleeSdp());
        call.setIceCandidates(request.getIceCandidates());
        call.setStatus(Call.CallStatus.ANSWERED);
        call.setStartedAt(LocalDateTime.now());
        
        call = callRepository.save(call);
        
        // Publish call event to Kafka
        publishCallEvent(call, "CALL_ANSWERED");
        
        log.info("Call answered successfully with session ID: {}", sessionId);
        
        return mapToCallResponse(call);
    }
    
    @Override
    @Transactional
    public CallResponse rejectCall(String sessionId, Long calleeId, String reason) {
        log.info("Rejecting call with session ID: {}", sessionId);
        
        Call call = callRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ChitChatException("Call not found", HttpStatus.NOT_FOUND, "CALL_NOT_FOUND"));
        
        // Check if user is the callee
        if (!call.getCalleeId().equals(calleeId)) {
            throw new ChitChatException("Unauthorized to reject this call", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        // Update call status
        call.setStatus(Call.CallStatus.REJECTED);
        call.setRejectionReason(reason);
        call.setEndedAt(LocalDateTime.now());
        
        call = callRepository.save(call);
        
        // Publish call event to Kafka
        publishCallEvent(call, "CALL_REJECTED");
        
        log.info("Call rejected successfully with session ID: {}", sessionId);
        
        return mapToCallResponse(call);
    }
    
    @Override
    @Transactional
    public CallResponse endCall(String sessionId, Long userId, String reason) {
        log.info("Ending call with session ID: {}", sessionId);
        
        Call call = callRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ChitChatException("Call not found", HttpStatus.NOT_FOUND, "CALL_NOT_FOUND"));
        
        // Check if user is part of this call
        if (!call.getCallerId().equals(userId) && !call.getCalleeId().equals(userId)) {
            throw new ChitChatException("Unauthorized to end this call", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        // Calculate duration if call was answered
        if (call.getStartedAt() != null) {
            long duration = java.time.Duration.between(call.getStartedAt(), LocalDateTime.now()).getSeconds();
            call.setDuration(duration);
        }
        
        // Update call status
        call.setStatus(Call.CallStatus.ENDED);
        call.setEndReason(reason);
        call.setEndedAt(LocalDateTime.now());
        
        call = callRepository.save(call);
        
        // Publish call event to Kafka
        publishCallEvent(call, "CALL_ENDED");
        
        log.info("Call ended successfully with session ID: {}", sessionId);
        
        return mapToCallResponse(call);
    }
    
    @Override
    public CallResponse getCallBySessionId(String sessionId) {
        Call call = callRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ChitChatException("Call not found", HttpStatus.NOT_FOUND, "CALL_NOT_FOUND"));
        
        return mapToCallResponse(call);
    }
    
    @Override
    public Page<CallResponse> getUserCallHistory(Long userId, Pageable pageable) {
        Page<Call> calls = callRepository.findByUserId(userId, pageable);
        return calls.map(this::mapToCallResponse);
    }
    
    @Override
    public List<CallResponse> getMissedCalls(Long userId) {
        List<Call> missedCalls = callRepository.findMissedCallsByUserId(userId);
        return missedCalls.stream()
                .map(this::mapToCallResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CallResponse> getRecentCalls(Long userId, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Call> recentCalls = callRepository.findRecentCallsByUserId(userId, since);
        
        return recentCalls.stream()
                .limit(limit)
                .map(this::mapToCallResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void cleanupStuckCalls() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        List<Call> stuckCalls = callRepository.findStuckCalls(cutoffTime);
        
        for (Call call : stuckCalls) {
            call.setStatus(Call.CallStatus.FAILED);
            call.setEndReason("Call timeout");
            call.setEndedAt(LocalDateTime.now());
            callRepository.save(call);
            
            log.info("Cleaned up stuck call with session ID: {}", call.getSessionId());
        }
    }
    
    @Override
    @Transactional
    public CallResponse updateCallStatus(String sessionId, Call.CallStatus status) {
        Call call = callRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ChitChatException("Call not found", HttpStatus.NOT_FOUND, "CALL_NOT_FOUND"));
        
        call.setStatus(status);
        
        if (status == Call.CallStatus.RINGING) {
            // Mark as missed for callee if not answered within timeout
            // This would be handled by a scheduled task
        }
        
        call = callRepository.save(call);
        
        return mapToCallResponse(call);
    }
    
    private void publishCallEvent(Call call, String eventType) {
        // Publish call event to Kafka for real-time updates
        kafkaTemplate.send("call-events", eventType, call);
    }
    
    private CallResponse mapToCallResponse(Call call) {
        return CallResponse.builder()
                .id(call.getId())
                .sessionId(call.getSessionId())
                .callerId(call.getCallerId())
                .calleeId(call.getCalleeId())
                .callType(call.getCallType())
                .status(call.getStatus())
                .startedAt(call.getStartedAt())
                .endedAt(call.getEndedAt())
                .duration(call.getDuration())
                .callerSdp(call.getCallerSdp())
                .calleeSdp(call.getCalleeSdp())
                .iceCandidates(call.getIceCandidates())
                .rejectionReason(call.getRejectionReason())
                .endReason(call.getEndReason())
                .createdAt(call.getCreatedAt())
                .updatedAt(call.getUpdatedAt())
                .build();
    }
}
