package com.chitchat.messaging.service;

import com.chitchat.messaging.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for messaging operations
 */
public interface MessagingService {
    
    MessageResponse sendMessage(Long senderId, SendMessageRequest request);
    
    Page<MessageResponse> getConversationMessages(Long userId1, Long userId2, Pageable pageable);
    
    Page<MessageResponse> getGroupMessages(String groupId, Pageable pageable);
    
    Page<MessageResponse> getUserMessages(Long userId, Pageable pageable);
    
    List<MessageResponse> searchMessages(String query, Long userId);
    
    MessageResponse markMessageAsRead(String messageId, Long userId);
    
    void markMessageAsDelivered(String messageId);
    
    void deleteMessage(String messageId, Long userId, boolean deleteForEveryone);
    
    GroupResponse createGroup(Long adminId, CreateGroupRequest request);
    
    GroupResponse addMemberToGroup(String groupId, Long adminId, Long memberId);
    
    GroupResponse removeMemberFromGroup(String groupId, Long adminId, Long memberId);
    
    GroupResponse updateGroupInfo(String groupId, Long adminId, String name, String description);
    
    List<GroupResponse> getUserGroups(Long userId);
    
    GroupResponse getGroupById(String groupId);
    
    void leaveGroup(String groupId, Long userId);
}
