package com.chitchat.messaging.service.impl;

import com.chitchat.messaging.client.NotificationServiceClient;
import com.chitchat.messaging.client.UserServiceClient;
import com.chitchat.messaging.document.Group;
import com.chitchat.messaging.document.Message;
import com.chitchat.messaging.dto.*;
import com.chitchat.messaging.repository.GroupRepository;
import com.chitchat.messaging.repository.MessageRepository;
import com.chitchat.messaging.service.MessagingService;
import com.chitchat.messaging.service.WebSocketService;
import com.chitchat.messaging.event.*;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Map;
import java.util.ArrayList;

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
    private final NotificationServiceClient notificationClient;
    private final UserServiceClient userServiceClient;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        // Handle both recipientId and receiverId field names for backward compatibility
        Long recipientId = request.getRecipientId() != null ? request.getRecipientId() : request.getReceiverId();
        
        log.info("Sending message from user {} to {}", senderId, recipientId);
        log.info("Request details - recipientId: {}, receiverId: {}, groupId: {}, content: {}", 
                request.getRecipientId(), request.getReceiverId(), request.getGroupId(), request.getContent());
        
        Message message = Message.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .groupId(request.getGroupId())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : Message.MessageType.TEXT)
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
        
        // Send push notification to recipient(s)
        sendPushNotification(message, senderId);
        
        // Send message via WebSocket for real-time delivery
        MessageResponse messageResponse = mapToMessageResponse(message);
        if (recipientId != null) {
            eventPublisher.publishEvent(new SendMessageEvent(recipientId, messageResponse));
            log.debug("Message sent to user {} via WebSocket", recipientId);
            
            // Send conversation list update to recipient
            eventPublisher.publishEvent(new SendConversationUpdateEvent(recipientId));
            
            // Send unread count update to recipient
            eventPublisher.publishEvent(new SendUnreadCountUpdateEvent(recipientId));
        }
        
        // Send conversation list update to sender
        eventPublisher.publishEvent(new SendConversationUpdateEvent(senderId));
        
        log.info("Message sent successfully with ID: {}", message.getId());
        
        return messageResponse;
    }
    
    @Override
    public Page<MessageResponse> getConversationMessages(Long userId1, Long userId2, Pageable pageable) {
        Page<Message> messages = messageRepository.findConversationMessages(userId1, userId2, pageable);
        return messages.map(this::mapToMessageResponse);
    }
    
    @Override
    public List<ConversationResponse> getConversationList(Long userId) {
        log.info("Getting conversation list for user: {}", userId);
        
        try {
            // Get latest messages for all conversations
            List<Message> latestMessages = messageRepository.findLatestMessagesForConversations(userId);
            
            // Get unread counts for each conversation partner
            List<MessageRepository.UnreadCountResult> unreadCounts = messageRepository.findUnreadCountsBySender(userId);
            Map<Long, Long> unreadCountMap = unreadCounts.stream()
                    .collect(Collectors.toMap(
                            MessageRepository.UnreadCountResult::getSenderId,
                            MessageRepository.UnreadCountResult::getUnreadCount
                    ));
            
            // Convert to conversation responses
            List<ConversationResponse> conversations = latestMessages.stream()
                    .map(message -> mapToConversationResponse(message, userId, unreadCountMap))
                    .collect(Collectors.toList());
            
            log.info("Found {} conversations for user {}", conversations.size(), userId);
            return conversations;
            
        } catch (Exception e) {
            log.error("Error getting conversation list for user: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Maps a latest message to conversation response
     */
    private ConversationResponse mapToConversationResponse(Message message, Long currentUserId, Map<Long, Long> unreadCountMap) {
        // Determine the other user in the conversation
        Long otherUserId = message.getSenderId().equals(currentUserId) ? 
                message.getRecipientId() : message.getSenderId();
        
        // Get user details (this would typically come from User Service)
        String userName = getUserName(otherUserId);
        String userAvatar = getUserAvatar(otherUserId);
        String userStatus = getUserStatus(otherUserId);
        
        // Get unread count for this conversation
        Long unreadCount = unreadCountMap.getOrDefault(otherUserId, 0L);
        
        // Check if user is currently typing (this would come from WebSocket state)
        Boolean isTyping = false; // Placeholder - would be managed by WebSocket state
        
        return ConversationResponse.builder()
                .userId(otherUserId)
                .userName(userName)
                .userAvatar(userAvatar)
                .userStatus(userStatus)
                .latestMessageId(message.getId())
                .latestMessageContent(message.getContent())
                .latestMessageType(message.getType().name())
                .latestMessageSenderId(message.getSenderId())
                .latestMessageSenderName(getUserName(message.getSenderId()))
                .latestMessageTime(message.getCreatedAt())
                .latestMessageStatus(message.getStatus().name())
                .unreadCount(unreadCount.intValue())
                .isTyping(isTyping)
                .conversationType("INDIVIDUAL")
                .build();
    }
    
    /**
     * Get user name by ID (placeholder - would call User Service)
     */
    private String getUserName(Long userId) {
        try {
            // This would typically call User Service to get user details
            // For now, return a placeholder
            return "User " + userId;
        } catch (Exception e) {
            log.warn("Failed to get user name for ID: {}", userId);
            return "Unknown User";
        }
    }
    
    /**
     * Get user avatar by ID (placeholder - would call User Service)
     */
    private String getUserAvatar(Long userId) {
        try {
            // This would typically call User Service to get user avatar
            return "/default-avatar.png";
        } catch (Exception e) {
            log.warn("Failed to get user avatar for ID: {}", userId);
            return "/default-avatar.png";
        }
    }
    
    /**
     * Get user status by ID (placeholder - would call User Service or WebSocket state)
     */
    private String getUserStatus(Long userId) {
        try {
            // For now, return OFFLINE - WebSocket status would be managed separately
            return "OFFLINE";
        } catch (Exception e) {
            log.warn("Failed to get user status for ID: {}", userId);
            return "OFFLINE";
        }
    }
    
    @Override
    public long getTotalUnreadCount(Long userId) {
        log.info("Getting total unread count for user: {}", userId);
        
        try {
            long totalUnreadCount = messageRepository.countTotalUnreadMessages(userId);
            log.info("User {} has {} total unread messages", userId, totalUnreadCount);
            return totalUnreadCount;
        } catch (Exception e) {
            log.error("Error getting total unread count for user: {}", userId, e);
            return 0;
        }
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
        
        // Send read status update via WebSocket to sender
        if (message.getSenderId() != null) {
            eventPublisher.publishEvent(new SendStatusUpdateEvent(message.getSenderId(), message.getId(), "READ"));
            log.debug("Read status sent to sender {} via WebSocket", message.getSenderId());
        }
        
        // Send unread count update to user who marked as read
        eventPublisher.publishEvent(new SendUnreadCountUpdateEvent(userId));
        
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
        
        // Send delivery status update via WebSocket to sender
        if (message.getSenderId() != null) {
            eventPublisher.publishEvent(new SendStatusUpdateEvent(message.getSenderId(), message.getId(), "DELIVERED"));
            log.debug("Delivery status sent to sender {} via WebSocket", message.getSenderId());
        }
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
    
    /**
     * Send push notification for new message
     * Handles both one-to-one and group chat notifications
     */
    private void sendPushNotification(Message message, Long senderId) {
        try {
            // Get sender details
            String senderName = getSenderName(senderId);
            
            if (message.getGroupId() != null) {
                // Group message - send to all group members except sender
                sendGroupMessageNotification(message, senderName);
            } else if (message.getRecipientId() != null) {
                // One-to-one message - send to recipient
                sendOneToOneMessageNotification(message, senderName);
            }
        } catch (Exception e) {
            log.error("Failed to send push notification for message: {}", message.getId(), e);
            // Don't throw - notification failure shouldn't block message sending
        }
    }
    
    /**
     * Send notification for one-to-one message
     */
    private void sendOneToOneMessageNotification(Message message, String senderName) {
        try {
            notificationClient.sendMessageNotification(
                message.getRecipientId(),
                senderName,
                message.getContent(),
                message.getSenderId(),
                message.getId()
            );
            log.info("Push notification sent to user: {}", message.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to send one-to-one message notification", e);
        }
    }
    
    /**
     * Send notification for group message
     */
    private void sendGroupMessageNotification(Message message, String senderName) {
        try {
            Group group = groupRepository.findById(message.getGroupId()).orElse(null);
            if (group == null) {
                log.warn("Group not found for notification: {}", message.getGroupId());
                return;
            }
            
            // Send to all group members except the sender
            group.getMembers().stream()
                .filter(member -> !member.getUserId().equals(message.getSenderId()))
                .forEach(member -> {
                    try {
                        String notificationBody = message.getContent();
                        if (notificationBody.length() > 100) {
                            notificationBody = notificationBody.substring(0, 97) + "...";
                        }
                        
                        notificationClient.sendMessageNotification(
                            member.getUserId(),
                            senderName + " in " + group.getName(),
                            notificationBody,
                            message.getSenderId(),
                            message.getId()
                        );
                    } catch (Exception e) {
                        log.error("Failed to send group notification to user: {}", member.getUserId(), e);
                    }
                });
            
            log.info("Push notifications sent to {} group members", group.getMembers().size() - 1);
        } catch (Exception e) {
            log.error("Failed to send group message notification", e);
        }
    }
    
    /**
     * Get sender name from User Service
     */
    private String getSenderName(Long senderId) {
        try {
            UserServiceClient.UserDto user = userServiceClient.getUserById(senderId);
            return (user != null && user.getName() != null) ? user.getName() : "User";
        } catch (Exception e) {
            log.error("Failed to get sender name for user: {}", senderId, e);
            return "User"; // Fallback
        }
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
                .receiverId(message.getRecipientId()) // Set receiverId to same value as recipientId for backward compatibility
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
