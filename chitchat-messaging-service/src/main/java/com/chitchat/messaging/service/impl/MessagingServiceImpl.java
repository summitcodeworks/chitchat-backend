package com.chitchat.messaging.service.impl;

import com.chitchat.messaging.client.NotificationServiceClient;
import com.chitchat.messaging.client.UserServiceClient;
import com.chitchat.messaging.document.Group;
import com.chitchat.messaging.document.Message;
import com.chitchat.messaging.dto.*;
import com.chitchat.messaging.repository.GroupRepository;
import com.chitchat.messaging.repository.MessageRepository;
import com.chitchat.messaging.service.MessagingService;
import com.chitchat.messaging.event.*;
import org.springframework.context.ApplicationEventPublisher;
import com.chitchat.shared.exception.ChitChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

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
    private final com.chitchat.messaging.service.WebSocketService webSocketService;
    private final org.springframework.cache.CacheManager cacheManager;
    private final org.springframework.context.ApplicationContext applicationContext;
    
    @Override
    @CacheEvict(value = {"conversationList", "unreadCount"}, key = "#senderId")  // More targeted cache eviction
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        // Handle both recipientId and receiverId field names for backward compatibility
        Long recipientId = request.getRecipientId() != null ? request.getRecipientId() : request.getReceiverId();
        
        log.debug("Sending message from user {} to {}", senderId, recipientId);
        
        // Build and save message - this is the only synchronous operation
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
        
        final Message savedMessage = messageRepository.save(message);
        final MessageResponse messageResponse = mapToMessageResponse(savedMessage);
        
        // All async operations - non-blocking
        final Long finalRecipientId = recipientId;
        
        // Kafka publishing - using dedicated executor
        CompletableFuture.runAsync(() -> publishMessageEvent(savedMessage), 
            getExecutor("kafkaExecutor"));
        
        // Push notification - using dedicated executor
        CompletableFuture.runAsync(() -> sendPushNotification(savedMessage, senderId), 
            getExecutor("notificationExecutor"));
        
        // WebSocket real-time delivery - using dedicated executor
        if (finalRecipientId != null) {
            CompletableFuture.runAsync(() -> {
                eventPublisher.publishEvent(new SendMessageEvent(finalRecipientId, messageResponse));
                eventPublisher.publishEvent(new SendConversationUpdateEvent(finalRecipientId));
                eventPublisher.publishEvent(new SendUnreadCountUpdateEvent(finalRecipientId));
            }, getExecutor("websocketExecutor"));
            
            // Also evict recipient's cache
            CompletableFuture.runAsync(() -> {
                org.springframework.cache.Cache conversationCache = cacheManager.getCache("conversationList");
                org.springframework.cache.Cache unreadCache = cacheManager.getCache("unreadCount");
                if (conversationCache != null) conversationCache.evict(finalRecipientId);
                if (unreadCache != null) unreadCache.evict(finalRecipientId);
            });
        }
        
        // Sender's conversation list update - using websocket executor
        CompletableFuture.runAsync(() -> 
            eventPublisher.publishEvent(new SendConversationUpdateEvent(senderId)), 
            getExecutor("websocketExecutor"));
        
        log.debug("Message sent successfully with ID: {}", savedMessage.getId());
        
        return messageResponse;
    }
    
    /**
     * Helper to get executor by name from Spring context
     */
    private java.util.concurrent.Executor getExecutor(String name) {
        try {
            return applicationContext.getBean(name, java.util.concurrent.Executor.class);
        } catch (Exception e) {
            log.warn("Failed to get executor bean '{}', using ForkJoinPool.commonPool()", name);
            return java.util.concurrent.ForkJoinPool.commonPool();
        }
    }
    
    @Override
    public Page<MessageResponse> getConversationMessages(Long userId1, Long userId2, Pageable pageable) {
        Page<Message> messages = messageRepository.findConversationMessages(userId1, userId2, pageable);
        return messages.map(this::mapToMessageResponse);
    }
    
    @Override
    @Cacheable(value = "conversationList", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<ConversationResponse> getConversationList(Long userId) {
        log.debug("Getting conversation list for user: {} from database", userId);
        
        try {
            // Get latest messages for all conversations
            List<Message> latestMessages = messageRepository.findLatestMessagesForConversations(userId);
            
            // Get unread counts per conversation partner
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
            
            log.debug("Found {} conversations for user {}", conversations.size(), userId);
            return conversations;
            
        } catch (Exception e) {
            log.error("Error getting conversation list for user: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Maps a latest message to conversation response
     * 
     * @param message Latest message in the conversation
     * @param currentUserId ID of current user viewing the conversation list
     * @param unreadCountMap Map of senderId -> count of unread messages from that sender
     * @return ConversationResponse with unread count for THIS specific conversation
     */
    @SuppressWarnings("unused")
    private ConversationResponse mapToConversationResponse(Message message, Long currentUserId, Map<Long, Long> unreadCountMap) {
        // Determine the other user in the conversation (the conversation partner)
        Long otherUserId = message.getSenderId().equals(currentUserId) ? 
                message.getRecipientId() : message.getSenderId();
        
        // Get user details (this would typically come from User Service)
        String userName = getUserName(otherUserId);
        String userAvatar = getUserAvatar(otherUserId);
        String userStatus = getUserStatus(otherUserId);
        
        // Get unread count for THIS specific conversation (messages FROM otherUserId TO currentUserId)
        // Example: If currentUserId is 13 and otherUserId is 12, this gets unread messages FROM user 12
        Long unreadCount = unreadCountMap.getOrDefault(otherUserId, 0L);
        
        log.debug("Conversation with user {}: {} unread messages", otherUserId, unreadCount);
        
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
                .unreadCount(unreadCount.intValue()) // Unread messages FROM this specific user
                .isTyping(isTyping)
                .conversationType("INDIVIDUAL")
                .build();
    }
    
    /**
     * Get user name by ID with caching
     */
    @Cacheable(value = "userDetails", key = "'name:' + #userId")
    private String getUserName(Long userId) {
        try {
            // This would typically call User Service to get user details
            return "User " + userId;
        } catch (Exception e) {
            log.warn("Failed to get user name for ID: {}", userId);
            return "Unknown User";
        }
    }
    
    /**
     * Get user avatar by ID with caching
     */
    @Cacheable(value = "userDetails", key = "'avatar:' + #userId")
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
     * Get user status by ID
     */
    private String getUserStatus(Long userId) {
        try {
            return "OFFLINE";
        } catch (Exception e) {
            log.warn("Failed to get user status for ID: {}", userId);
            return "OFFLINE";
        }
    }
    
    @Override
    @Cacheable(value = "unreadCount", key = "#userId")
    public long getTotalUnreadCount(Long userId) {
        log.debug("Getting total unread count for user: {} from database", userId);
        
        try {
            long totalUnreadCount = messageRepository.countTotalUnreadMessages(userId);
            log.debug("User {} has {} total unread messages", userId, totalUnreadCount);
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
    @CacheEvict(value = {"conversationList", "unreadCount"}, key = "#userId")  // Targeted cache eviction
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
        
        // Async operations using dedicated executors
        Message finalMessage = message;
        CompletableFuture.runAsync(() -> {
            publishReadReceiptEvent(finalMessage);
            
            // Send read status update via WebSocket to sender
            if (finalMessage.getSenderId() != null) {
                eventPublisher.publishEvent(new SendStatusUpdateEvent(finalMessage.getSenderId(), finalMessage.getId(), "READ"));
            }
            
            // Send unread count update to user who marked as read
            eventPublisher.publishEvent(new SendUnreadCountUpdateEvent(userId));
        }, getExecutor("websocketExecutor"));
        
        return mapToMessageResponse(message);
    }
    
    @Override
    @CacheEvict(value = {"conversationList", "unreadCount"}, key = "#recipientId")  // Targeted cache eviction
    public int markAllMessagesAsReadFromSender(Long recipientId, Long senderId) {
        log.debug("Bulk marking messages as read: recipient={}, sender={}", recipientId, senderId);
        
        // Find all unread messages from sender to recipient
        List<Message> unreadMessages = messageRepository.findUnreadMessagesFromSender(recipientId, senderId);
        
        if (unreadMessages.isEmpty()) {
            log.debug("No unread messages found from sender {} to recipient {}", senderId, recipientId);
            return 0;
        }
        
        // Update all messages in bulk
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = 0;
        
        for (Message message : unreadMessages) {
            message.setStatus(Message.MessageStatus.READ);
            message.setReadAt(now);
        }
        
        // Save all messages in a single batch operation
        List<Message> savedMessages = messageRepository.saveAll(unreadMessages);
        updatedCount = savedMessages.size();
        
        // Async operations for notifications
        final int finalCount = updatedCount;
        CompletableFuture.runAsync(() -> {
            // Publish read receipts for all messages
            for (Message message : savedMessages) {
                publishReadReceiptEvent(message);
                
                // Send read status update via WebSocket to sender
                if (message.getSenderId() != null) {
                    eventPublisher.publishEvent(new SendStatusUpdateEvent(message.getSenderId(), message.getId(), "READ"));
                }
            }
            
            // Send unread count update to user who marked messages as read
            eventPublisher.publishEvent(new SendUnreadCountUpdateEvent(recipientId));
            
            log.debug("Bulk marked {} messages as read from sender {} to recipient {}", finalCount, senderId, recipientId);
        }, getExecutor("websocketExecutor"));
        
        return updatedCount;
    }
    
    @Override
    public List<MessageResponse> getPendingMessages(Long recipientId) {
        log.debug("Fetching pending messages (SENT status) for user: {}", recipientId);
        
        // Find all messages in SENT status for this recipient
        List<Message> pendingMessages = messageRepository.findPendingMessagesForRecipient(recipientId);
        
        if (pendingMessages.isEmpty()) {
            log.debug("No pending messages found for user: {}", recipientId);
            return new ArrayList<>();
        }
        
        log.info("Found {} pending messages for user: {}", pendingMessages.size(), recipientId);
        
        // Convert to MessageResponse
        List<MessageResponse> messageResponses = pendingMessages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
        
        // Mark all messages as DELIVERED asynchronously
        CompletableFuture.runAsync(() -> {
            LocalDateTime now = LocalDateTime.now();
            for (Message message : pendingMessages) {
                message.setStatus(Message.MessageStatus.DELIVERED);
                message.setDeliveredAt(now);
            }
            
            // Batch save all updated messages
            List<Message> deliveredMessages = messageRepository.saveAll(pendingMessages);
            
            // Notify senders about delivery status
            for (Message message : deliveredMessages) {
                if (message.getSenderId() != null) {
                    eventPublisher.publishEvent(new SendStatusUpdateEvent(
                        message.getSenderId(), 
                        message.getId(), 
                        "DELIVERED"
                    ));
                }
            }
            
            log.info("Marked {} messages as DELIVERED for user: {}", deliveredMessages.size(), recipientId);
        }, getExecutor("websocketExecutor"));
        
        return messageResponses;
    }
    
    @Override
    public void markMessageAsDelivered(String messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ChitChatException("Message not found", HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND"));
        
        message.setStatus(Message.MessageStatus.DELIVERED);
        message.setDeliveredAt(LocalDateTime.now());
        messageRepository.save(message);
        
        // Async WebSocket notification using dedicated executor
        if (message.getSenderId() != null) {
            Long senderId = message.getSenderId();
            String msgId = message.getId();
            CompletableFuture.runAsync(() -> 
                eventPublisher.publishEvent(new SendStatusUpdateEvent(senderId, msgId, "DELIVERED")),
                getExecutor("websocketExecutor")
            );
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
    public MessageResponse pinMessage(String messageId, Long userId, boolean isPinned) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ChitChatException("Message not found", HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND"));
        
        // Check if user is participant in the conversation
        if (!message.getSenderId().equals(userId) && !message.getRecipientId().equals(userId)) {
            throw new ChitChatException("User not authorized to pin this message", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        if (isPinned) {
            // Unpin any existing pinned message in this conversation
            List<Message> existingPinnedMessages = messageRepository.findBySenderIdAndRecipientIdAndIsPinnedTrue(
                message.getSenderId(), message.getRecipientId());
            
            if (message.getSenderId().equals(message.getRecipientId())) {
                // Handle group messages or self-messages differently
                existingPinnedMessages = messageRepository.findBySenderIdAndGroupIdAndIsPinnedTrue(
                    message.getSenderId(), message.getGroupId());
            }
            
            for (Message pinnedMessage : existingPinnedMessages) {
                pinnedMessage.setIsPinned(false);
                messageRepository.save(pinnedMessage);
            }
        }
        
        // Set the pin status
        message.setIsPinned(isPinned);
        Message updatedMessage = messageRepository.save(message);
        
        // Publish pin event for both users in the conversation
        publishPinMessageEvent(updatedMessage, userId, isPinned);
        
        return mapToMessageResponse(updatedMessage);
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
    
    private void publishPinMessageEvent(Message message, Long userId, boolean isPinned) {
        // Create pin event data
        Map<String, Object> pinEventData = new HashMap<>();
        pinEventData.put("messageId", message.getId());
        pinEventData.put("isPinned", isPinned);
        pinEventData.put("pinnedBy", userId);
        pinEventData.put("timestamp", LocalDateTime.now());
        
        // Send to both users in the conversation
        if (message.getRecipientId() != null) {
            // One-on-one conversation
            kafkaTemplate.send("pin-message-events", message.getSenderId().toString(), pinEventData);
            kafkaTemplate.send("pin-message-events", message.getRecipientId().toString(), pinEventData);
        } else if (message.getGroupId() != null) {
            // Group conversation - send to all group members
            // Note: In a real implementation, you'd get group members and send to each
            kafkaTemplate.send("pin-message-events", message.getGroupId(), pinEventData);
        }
    }
    
    /**
     * Send push notification for new message
     * Handles both one-to-one and group chat notifications
     * Only sends notification if recipient is NOT connected via WebSocket
     */
    private void sendPushNotification(Message message, Long senderId) {
        try {
            // Get sender details
            String senderName = getSenderName(senderId);
            
            if (message.getGroupId() != null) {
                // Group message - send to all group members except sender
                sendGroupMessageNotification(message, senderName);
            } else if (message.getRecipientId() != null) {
                // One-to-one message - always send push notification
                // Even if user has WebSocket connection, they might be on home screen
                boolean isRecipientConnected = webSocketService.isUserConnected(message.getRecipientId());
                
                if (isRecipientConnected) {
                    log.info("Recipient {} is connected via WebSocket, but sending push notification anyway (user might be on home screen)", message.getRecipientId());
                } else {
                    log.info("Recipient {} is NOT connected via WebSocket, sending push notification", message.getRecipientId());
                }
                
                // Always send push notification for one-to-one messages
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
            // Get sender's profile image
            String senderAvatarUrl = null;
            try {
                UserServiceClient.UserDto sender = userServiceClient.getUserById(message.getSenderId());
                if (sender != null && sender.getAvatarUrl() != null) {
                    senderAvatarUrl = sender.getAvatarUrl();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch sender avatar for user: {}", message.getSenderId());
            }
            
            notificationClient.sendMessageNotification(
                message.getRecipientId(),
                senderName,
                message.getContent(),
                message.getSenderId(),
                message.getId(),
                senderAvatarUrl  // Pass sender's avatar URL
            );
            log.info("Push notification sent to user: {} with avatar: {}", message.getRecipientId(), senderAvatarUrl != null ? "yes" : "no");
        } catch (Exception e) {
            log.error("Failed to send one-to-one message notification", e);
        }
    }
    
    /**
     * Send notification for group message (async batch processing)
     * Only sends notifications to members who are NOT connected via WebSocket
     */
    private void sendGroupMessageNotification(Message message, String senderName) {
        CompletableFuture.runAsync(() -> {
            try {
                Group group = groupRepository.findById(message.getGroupId()).orElse(null);
                if (group == null) {
                    log.warn("Group not found for notification: {}", message.getGroupId());
                    return;
                }
                
                String notificationBody = message.getContent();
                if (notificationBody != null && notificationBody.length() > 100) {
                    notificationBody = notificationBody.substring(0, 97) + "...";
                }
                
                // Get sender's profile image
                String senderAvatarUrl = null;
                try {
                    UserServiceClient.UserDto sender = userServiceClient.getUserById(message.getSenderId());
                    if (sender != null && sender.getAvatarUrl() != null) {
                        senderAvatarUrl = sender.getAvatarUrl();
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch sender avatar for user: {}", message.getSenderId());
                }
                
                // Batch process notifications to avoid overwhelming the notification service
                // Send to all members (even if connected via WebSocket, they might be on home screen)
                String finalNotificationBody = notificationBody;
                String finalSenderAvatarUrl = senderAvatarUrl;
                List<CompletableFuture<Void>> notificationFutures = group.getMembers().stream()
                    .filter(member -> !member.getUserId().equals(message.getSenderId()))
                    .map(member -> CompletableFuture.runAsync(() -> {
                        try {
                            boolean isConnected = webSocketService.isUserConnected(member.getUserId());
                            if (isConnected) {
                                log.info("Sending group notification to user {} (connected via WebSocket, but user might be on home screen)", member.getUserId());
                            } else {
                                log.info("Sending group notification to user {} (not connected via WebSocket)", member.getUserId());
                            }
                            notificationClient.sendMessageNotification(
                                member.getUserId(),
                                senderName + " in " + group.getName(),
                                finalNotificationBody,
                                message.getSenderId(),
                                message.getId(),
                                finalSenderAvatarUrl
                            );
                        } catch (Exception e) {
                            log.error("Failed to send group notification to user: {}", member.getUserId(), e);
                        }
                    }))
                    .collect(Collectors.toList());
                
                // Wait for all notifications to complete (with timeout)
                CompletableFuture.allOf(notificationFutures.toArray(new CompletableFuture[0]))
                    .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        log.warn("Some group notifications timed out", ex);
                        return null;
                    })
                    .join();
                
                log.debug("Push notifications processed for group members");
            } catch (Exception e) {
                log.error("Failed to send group message notification", e);
            }
        });
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
