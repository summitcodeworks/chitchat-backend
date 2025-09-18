package com.chitchat.messaging.controller;

import com.chitchat.messaging.dto.*;
import com.chitchat.messaging.service.MessagingService;
import com.chitchat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for messaging operations
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessagingController {
    
    private final MessagingService messagingService;
    
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SendMessageRequest request) {
        Long senderId = extractUserIdFromToken(token);
        MessageResponse response = messagingService.sendMessage(senderId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Message sent successfully"));
    }
    
    @GetMapping("/conversation/{userId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getConversationMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @PageableDefault(size = 50) Pageable pageable) {
        Long currentUserId = extractUserIdFromToken(token);
        Page<MessageResponse> response = messagingService.getConversationMessages(currentUserId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getGroupMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable String groupId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<MessageResponse> response = messagingService.getGroupMessages(groupId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getUserMessages(
            @RequestHeader("Authorization") String token,
            @PageableDefault(size = 50) Pageable pageable) {
        Long userId = extractUserIdFromToken(token);
        Page<MessageResponse> response = messagingService.getUserMessages(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> searchMessages(
            @RequestHeader("Authorization") String token,
            @RequestParam String query) {
        Long userId = extractUserIdFromToken(token);
        List<MessageResponse> response = messagingService.searchMessages(query, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<MessageResponse>> markMessageAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable String messageId) {
        Long userId = extractUserIdFromToken(token);
        MessageResponse response = messagingService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message marked as read"));
    }
    
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @RequestHeader("Authorization") String token,
            @PathVariable String messageId,
            @RequestParam(defaultValue = "false") boolean deleteForEveryone) {
        Long userId = extractUserIdFromToken(token);
        messagingService.deleteMessage(messageId, userId, deleteForEveryone);
        return ResponseEntity.ok(ApiResponse.success(null, "Message deleted successfully"));
    }
    
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateGroupRequest request) {
        Long adminId = extractUserIdFromToken(token);
        GroupResponse response = messagingService.createGroup(adminId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Group created successfully"));
    }
    
    @PostMapping("/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<GroupResponse>> addMemberToGroup(
            @RequestHeader("Authorization") String token,
            @PathVariable String groupId,
            @PathVariable Long memberId) {
        Long adminId = extractUserIdFromToken(token);
        GroupResponse response = messagingService.addMemberToGroup(groupId, adminId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "Member added successfully"));
    }
    
    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<GroupResponse>> removeMemberFromGroup(
            @RequestHeader("Authorization") String token,
            @PathVariable String groupId,
            @PathVariable Long memberId) {
        Long adminId = extractUserIdFromToken(token);
        GroupResponse response = messagingService.removeMemberFromGroup(groupId, adminId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "Member removed successfully"));
    }
    
    @PutMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroupInfo(
            @RequestHeader("Authorization") String token,
            @PathVariable String groupId,
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        Long adminId = extractUserIdFromToken(token);
        GroupResponse response = messagingService.updateGroupInfo(groupId, adminId, name, description);
        return ResponseEntity.ok(ApiResponse.success(response, "Group info updated successfully"));
    }
    
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getUserGroups(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        List<GroupResponse> response = messagingService.getUserGroups(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(
            @RequestHeader("Authorization") String token,
            @PathVariable String groupId) {
        GroupResponse response = messagingService.getGroupById(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/groups/{groupId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @RequestHeader("Authorization") String token,
            @PathVariable String groupId) {
        Long userId = extractUserIdFromToken(token);
        messagingService.leaveGroup(groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Left group successfully"));
    }
    
    private Long extractUserIdFromToken(String token) {
        // Extract user ID from JWT token
        // This is a simplified implementation
        // In a real application, you would use a proper JWT service
        return 1L; // Placeholder
    }
}
