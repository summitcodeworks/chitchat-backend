package com.chitchat.status.controller;

import com.chitchat.status.dto.*;
import com.chitchat.status.service.StatusService;
import com.chitchat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for status operations
 */
@Slf4j
@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {
    
    private final StatusService statusService;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<StatusResponse>> createStatus(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateStatusRequest request) {
        Long userId = extractUserIdFromToken(token);
        StatusResponse response = statusService.createStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Status created successfully"));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> getUserStatuses(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        List<StatusResponse> response = statusService.getUserStatuses(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> getActiveStatuses(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        List<StatusResponse> response = statusService.getActiveStatuses(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> getContactsStatuses(
            @RequestHeader("Authorization") String token,
            @RequestParam List<Long> contactIds) {
        Long userId = extractUserIdFromToken(token);
        List<StatusResponse> response = statusService.getContactsStatuses(userId, contactIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/{statusId}/view")
    public ResponseEntity<ApiResponse<StatusResponse>> viewStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String statusId) {
        Long viewerId = extractUserIdFromToken(token);
        StatusResponse response = statusService.viewStatus(statusId, viewerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Status viewed successfully"));
    }
    
    @PostMapping("/{statusId}/react")
    public ResponseEntity<ApiResponse<StatusResponse>> reactToStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String statusId,
            @Valid @RequestBody StatusReactionRequest request) {
        Long userId = extractUserIdFromToken(token);
        StatusResponse response = statusService.reactToStatus(statusId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Reaction added successfully"));
    }
    
    @DeleteMapping("/{statusId}")
    public ResponseEntity<ApiResponse<Void>> deleteStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String statusId) {
        Long userId = extractUserIdFromToken(token);
        statusService.deleteStatus(statusId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Status deleted successfully"));
    }
    
    @GetMapping("/{statusId}")
    public ResponseEntity<ApiResponse<StatusResponse>> getStatusById(
            @RequestHeader("Authorization") String token,
            @PathVariable String statusId) {
        Long userId = extractUserIdFromToken(token);
        StatusResponse response = statusService.getStatusById(statusId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{statusId}/views")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> getStatusViews(
            @RequestHeader("Authorization") String token,
            @PathVariable String statusId) {
        Long userId = extractUserIdFromToken(token);
        List<StatusResponse> response = statusService.getStatusViews(statusId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{statusId}/reactions")
    public ResponseEntity<ApiResponse<List<StatusResponse>>> getStatusReactions(
            @RequestHeader("Authorization") String token,
            @PathVariable String statusId) {
        Long userId = extractUserIdFromToken(token);
        List<StatusResponse> response = statusService.getStatusReactions(statusId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    private Long extractUserIdFromToken(String token) {
        // Extract user ID from JWT token
        // This is a simplified implementation
        // In a real application, you would use a proper JWT service
        return 1L; // Placeholder
    }
}
