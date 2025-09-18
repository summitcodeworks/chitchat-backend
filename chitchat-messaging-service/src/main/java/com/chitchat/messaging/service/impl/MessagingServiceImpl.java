package com.chitchat.messaging.service.impl;

import com.chitchat.messaging.document.Group;
import com.chitchat.messaging.document.Message;
import com.chitchat.messaging.dto.*;
import com.chitchat.messaging.repository.GroupRepository;
import com.chitchat.messaging.repository.MessageRepository;
import com.chitchat.messaging.service.MessagingService;
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
import java.util.stream.Collectors;

/**
 * Implementation of MessagingService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingServiceImpl implements MessagingService {
    
    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        log.info("Sending message from user {} to {}", senderId, request.getRecipientId());
        
        Message message = Message.builder()
                .senderId(senderId)
                .recipientId(request.getRecipientId())
                .groupId(request.getGroupId())
                .content(request.getContent())
                .type(request.getType())
                .status(Message.MessageStatus.SENT)
                .mediaUrl(request.getMediaUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .replyToMessageId(request.getReplyToMessageId())
                .mentions(request.getMentions())
                .scheduledAt(request.getScheduledAt())
                .build();
        
        message = messageRepository.save(message);
        
        // Publish message event to Kafka for real-time delivery
        publishMessageEvent(message);
        
        log.info("Message sent successfully with ID: {}", message.getId());
        
        return mapToMessageResponse(message);
    }
    
    @Override
    public Page<MessageResponse> getConversationMessages(Long userId1, Long userId2, Pageable pageable) {
        Page<Message> messages = messageRepository.findConversationMessages(userId1, userId2, pageable);
        return messages.map(this::mapToMessageResponse);
    }
    
    @Override
    public Page<MessageResponse> getGroupMessages(String groupId, Pageable pageable) {
        Page<Message> messages = messageRepository.findGroupMessages(groupId, pageable);
        return messages.map(this::mapToMessageResponse);
    }
    
    @Override
    public Page<MessageResponse> getUserMessages(Long userId, Pageable pageable) {
        // Get user's groups
        List<Group> userGroups = groupRepository.findByUserId(userId);
        List<String> groupIds = userGroups.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
        
        Page<Message> messages = messageRepository.findUserMessages(userId, groupIds, pageable);
        return messages.map(this::mapToMessageResponse);
    }
    
    @Override
    public List<MessageResponse> searchMessages(String query, Long userId) {
        List<Message> messages = messageRepository.searchMessages(query, userId);
        return messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public MessageResponse markMessageAsRead(String messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ChitChatException("Message not found", HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND"));
        
        // Check if user is the recipient
        if (!message.getRecipientId().equals(userId) && !isUserInGroup(userId, message.getGroupId())) {
            throw new ChitChatException("Unauthorized to mark message as read", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        message.setStatus(Message.MessageStatus.READ);
        message.setReadAt(LocalDateTime.now());
        message = messageRepository.save(message);
        
        // Publish read receipt event
        publishReadReceiptEvent(message);
        
        return mapToMessageResponse(message);
    }
    
    @Override
    @Transactional
    public void markMessageAsDelivered(String messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ChitChatException("Message not found", HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND"));
        
        message.setStatus(Message.MessageStatus.DELIVERED);
        message.setDeliveredAt(LocalDateTime.now());
        messageRepository.save(message);
    }
    
    @Override
    @Transactional
    public void deleteMessage(String messageId, Long userId, boolean deleteForEveryone) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ChitChatException("Message not found", HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND"));
        
        // Check if user is the sender
        if (!message.getSenderId().equals(userId)) {
            throw new ChitChatException("Only sender can delete message", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        if (deleteForEveryone) {
            messageRepository.delete(message);
            // Publish delete event for all recipients
            publishDeleteMessageEvent(message);
        } else {
            // Mark as deleted for sender only (implement soft delete)
            message.setStatus(Message.MessageStatus.FAILED);
            messageRepository.save(message);
        }
    }
    
    @Override
    @Transactional
    public GroupResponse createGroup(Long adminId, CreateGroupRequest request) {
        log.info("Creating group '{}' by user {}", request.getName(), adminId);
        
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .avatarUrl(request.getAvatarUrl())
                .adminId(adminId)
                .lastActivity(LocalDateTime.now())
                .settings(Group.GroupSettings.builder()
                        .allowMembersToInvite(true)
                        .allowMembersToChangeGroupInfo(false)
                        .allowMembersToSendMessages(true)
                        .allowMembersToSendMedia(true)
                        .build())
                .build();
        
        // Add admin as first member
        Group.GroupMember adminMember = Group.GroupMember.builder()
                .userId(adminId)
                .role(Group.GroupRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();
        
        group.setMembers(List.of(adminMember));
        
        group = groupRepository.save(group);
        
        log.info("Group created successfully with ID: {}", group.getId());
        
        return mapToGroupResponse(group);
    }
    
    @Override
    @Transactional
    public GroupResponse addMemberToGroup(String groupId, Long adminId, Long memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ChitChatException("Group not found", HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND"));
        
        // Check if user is admin or moderator
        if (!isUserAdminOrModerator(group, adminId)) {
            throw new ChitChatException("Only admin or moderator can add members", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        // Check if member already exists
        boolean memberExists = group.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(memberId));
        
        if (memberExists) {
            throw new ChitChatException("User is already a member", HttpStatus.CONFLICT, "MEMBER_EXISTS");
        }
        
        Group.GroupMember newMember = Group.GroupMember.builder()
                .userId(memberId)
                .role(Group.GroupRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
        
        group.getMembers().add(newMember);
        group.setLastActivity(LocalDateTime.now());
        
        group = groupRepository.save(group);
        
        return mapToGroupResponse(group);
    }
    
    @Override
    @Transactional
    public GroupResponse removeMemberFromGroup(String groupId, Long adminId, Long memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ChitChatException("Group not found", HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND"));
        
        // Check if user is admin or moderator
        if (!isUserAdminOrModerator(group, adminId)) {
            throw new ChitChatException("Only admin or moderator can remove members", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        // Remove member
        group.getMembers().removeIf(member -> member.getUserId().equals(memberId));
        group.setLastActivity(LocalDateTime.now());
        
        group = groupRepository.save(group);
        
        return mapToGroupResponse(group);
    }
    
    @Override
    @Transactional
    public GroupResponse updateGroupInfo(String groupId, Long adminId, String name, String description) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ChitChatException("Group not found", HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND"));
        
        // Check if user is admin
        if (!group.getAdminId().equals(adminId)) {
            throw new ChitChatException("Only admin can update group info", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        group.setName(name);
        group.setDescription(description);
        group.setLastActivity(LocalDateTime.now());
        
        group = groupRepository.save(group);
        
        return mapToGroupResponse(group);
    }
    
    @Override
    public List<GroupResponse> getUserGroups(Long userId) {
        List<Group> groups = groupRepository.findByUserId(userId);
        return groups.stream()
                .map(this::mapToGroupResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public GroupResponse getGroupById(String groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ChitChatException("Group not found", HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND"));
        
        return mapToGroupResponse(group);
    }
    
    @Override
    @Transactional
    public void leaveGroup(String groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ChitChatException("Group not found", HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND"));
        
        // Check if user is admin
        if (group.getAdminId().equals(userId)) {
            throw new ChitChatException("Admin cannot leave group. Transfer admin role first.", HttpStatus.BAD_REQUEST, "ADMIN_CANNOT_LEAVE");
        }
        
        // Remove user from group
        group.getMembers().removeIf(member -> member.getUserId().equals(userId));
        group.setLastActivity(LocalDateTime.now());
        
        groupRepository.save(group);
    }
    
    private void publishMessageEvent(Message message) {
        // Publish to Kafka for real-time delivery
        kafkaTemplate.send("message-events", message);
    }
    
    private void publishReadReceiptEvent(Message message) {
        // Publish read receipt to Kafka
        kafkaTemplate.send("read-receipt-events", message);
    }
    
    private void publishDeleteMessageEvent(Message message) {
        // Publish delete event to Kafka
        kafkaTemplate.send("delete-message-events", message);
    }
    
    private boolean isUserInGroup(Long userId, String groupId) {
        if (groupId == null) return false;
        
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) return false;
        
        return group.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(userId));
    }
    
    private boolean isUserAdminOrModerator(Group group, Long userId) {
        return group.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(userId) && 
                        (member.getRole() == Group.GroupRole.ADMIN || member.getRole() == Group.GroupRole.MODERATOR));
    }
    
    private MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .groupId(message.getGroupId())
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .mediaUrl(message.getMediaUrl())
                .thumbnailUrl(message.getThumbnailUrl())
                .replyToMessageId(message.getReplyToMessageId())
                .mentions(message.getMentions())
                .scheduledAt(message.getScheduledAt())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
    
    private GroupResponse mapToGroupResponse(Group group) {
        List<GroupResponse.GroupMemberResponse> memberResponses = group.getMembers().stream()
                .map(member -> GroupResponse.GroupMemberResponse.builder()
                        .userId(member.getUserId())
                        .role(member.getRole())
                        .joinedAt(member.getJoinedAt())
                        .lastSeen(member.getLastSeen())
                        .build())
                .collect(Collectors.toList());
        
        GroupResponse.GroupSettingsResponse settingsResponse = GroupResponse.GroupSettingsResponse.builder()
                .allowMembersToInvite(group.getSettings().isAllowMembersToInvite())
                .allowMembersToChangeGroupInfo(group.getSettings().isAllowMembersToChangeGroupInfo())
                .allowMembersToSendMessages(group.getSettings().isAllowMembersToSendMessages())
                .allowMembersToSendMedia(group.getSettings().isAllowMembersToSendMedia())
                .groupDescription(group.getSettings().getGroupDescription())
                .build();
        
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .adminId(group.getAdminId())
                .members(memberResponses)
                .settings(settingsResponse)
                .lastActivity(group.getLastActivity())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
