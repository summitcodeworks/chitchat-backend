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
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @Valid @RequestBody SendMessageRequest request) {
        Long senderId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        MessageResponse response = messagingService.sendMessage(senderId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Message sent successfully"));
    }
    
    @GetMapping("/conversation/{receiverId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getConversationMessages(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable Long receiverId,
            @PageableDefault(size = 50) Pageable pageable) {
        Long senderId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        Page<MessageResponse> response = messagingService.getConversationMessages(senderId, receiverId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getGroupMessages(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable String groupId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<MessageResponse> response = messagingService.getGroupMessages(groupId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getUserMessages(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PageableDefault(size = 50) Pageable pageable) {
        Long userId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        Page<MessageResponse> response = messagingService.getUserMessages(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> searchMessages(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @RequestParam String query) {
        Long userId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        List<MessageResponse> response = messagingService.searchMessages(query, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<MessageResponse>> markMessageAsRead(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable String messageId) {
        Long userId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        MessageResponse response = messagingService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message marked as read"));
    }
    
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable String messageId,
            @RequestParam(defaultValue = "false") boolean deleteForEveryone) {
        Long userId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        messagingService.deleteMessage(messageId, userId, deleteForEveryone);
        return ResponseEntity.ok(ApiResponse.success(null, "Message deleted successfully"));
    }
    
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @Valid @RequestBody CreateGroupRequest request) {
        Long adminId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        GroupResponse response = messagingService.createGroup(adminId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Group created successfully"));
    }
    
    @PostMapping("/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<GroupResponse>> addMemberToGroup(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable String groupId,
            @PathVariable Long memberId) {
        Long adminId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        GroupResponse response = messagingService.addMemberToGroup(groupId, adminId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "Member added successfully"));
    }
    
    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<GroupResponse>> removeMemberFromGroup(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable String groupId,
            @PathVariable Long memberId) {
        Long adminId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        GroupResponse response = messagingService.removeMemberFromGroup(groupId, adminId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "Member removed successfully"));
    }
    
    @PutMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroupInfo(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable String groupId,
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        Long adminId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        GroupResponse response = messagingService.updateGroupInfo(groupId, adminId, name, description);
        return ResponseEntity.ok(ApiResponse.success(response, "Group info updated successfully"));
    }
    
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getUserGroups(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType) {
        Long userId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        List<GroupResponse> response = messagingService.getUserGroups(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable String groupId) {
        GroupResponse response = messagingService.getGroupById(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/groups/{groupId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-UID", required = false) String firebaseUidHeader,
            @RequestHeader(value = "X-User-Phone", required = false) String phoneNumberHeader,
            @RequestHeader(value = "X-Token-Type", required = false) String tokenType,
            @PathVariable String groupId) {
        Long userId = extractUserIdFromHeaders(userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        messagingService.leaveGroup(groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Left group successfully"));
    }
    
    private Long extractUserIdFromHeaders(String userIdHeader, String firebaseUidHeader, String phoneNumberHeader, String tokenType) {
        log.info("Extracting user ID from headers - userIdHeader: {}, firebaseUidHeader: {}, phoneNumberHeader: {}, tokenType: {}", 
                userIdHeader, firebaseUidHeader, phoneNumberHeader, tokenType);
        
        if ("jwt".equals(tokenType) && userIdHeader != null) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                log.error("Invalid user ID format: {}", userIdHeader);
                throw new RuntimeException("Invalid user ID format");
            }
        } else if ("firebase".equals(tokenType) && userIdHeader != null) {
            // For Firebase tokens, the API Gateway should have provided the user ID
            log.info("Firebase token detected with user ID: {}", userIdHeader);
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                log.error("Invalid user ID format from API Gateway: {}", userIdHeader);
                throw new RuntimeException("Invalid user ID format from API Gateway");
            }
        } else {
            log.error("Unable to extract user ID from headers");
            throw new RuntimeException("Unable to extract user ID from headers");
        }
    }
}
