package com.chitchat.calls.controller;

import com.chitchat.calls.dto.*;
import com.chitchat.calls.service.CallService;
import com.chitchat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for call operations
 */
@Slf4j
@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {
    
    private final CallService callService;
    
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<CallResponse>> initiateCall(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody InitiateCallRequest request) {
        Long callerId = extractUserIdFromToken(token);
        CallResponse response = callService.initiateCall(callerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Call initiated successfully"));
    }
    
    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<ApiResponse<CallResponse>> answerCall(
            @RequestHeader("Authorization") String token,
            @PathVariable String sessionId,
            @Valid @RequestBody CallAnswerRequest request) {
        Long calleeId = extractUserIdFromToken(token);
        CallResponse response = callService.answerCall(sessionId, calleeId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Call answered successfully"));
    }
    
    @PostMapping("/{sessionId}/reject")
    public ResponseEntity<ApiResponse<CallResponse>> rejectCall(
            @RequestHeader("Authorization") String token,
            @PathVariable String sessionId,
            @RequestParam(required = false) String reason) {
        Long calleeId = extractUserIdFromToken(token);
        CallResponse response = callService.rejectCall(sessionId, calleeId, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Call rejected successfully"));
    }
    
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<CallResponse>> endCall(
            @RequestHeader("Authorization") String token,
            @PathVariable String sessionId,
            @RequestParam(required = false) String reason) {
        Long userId = extractUserIdFromToken(token);
        CallResponse response = callService.endCall(sessionId, userId, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Call ended successfully"));
    }
    
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<CallResponse>> getCallBySessionId(
            @RequestHeader("Authorization") String token,
            @PathVariable String sessionId) {
        CallResponse response = callService.getCallBySessionId(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<CallResponse>>> getUserCallHistory(
            @RequestHeader("Authorization") String token,
            Pageable pageable) {
        Long userId = extractUserIdFromToken(token);
        Page<CallResponse> response = callService.getUserCallHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/missed")
    public ResponseEntity<ApiResponse<List<CallResponse>>> getMissedCalls(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        List<CallResponse> response = callService.getMissedCalls(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<CallResponse>>> getRecentCalls(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = extractUserIdFromToken(token);
        List<CallResponse> response = callService.getRecentCalls(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    private Long extractUserIdFromToken(String token) {
        // Extract user ID from JWT token
        // This is a simplified implementation
        // In a real application, you would use a proper JWT service
        return 1L; // Placeholder
    }
}
