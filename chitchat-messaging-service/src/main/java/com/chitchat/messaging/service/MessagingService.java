package com.chitchat.messaging.service;

import com.chitchat.messaging.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for messaging operations
 * 
 * Handles all messaging functionality including:
 * - One-on-one messaging
 * - Group messaging
 * - Message delivery tracking
 * - Message search
 * - Message deletion
 * - Group management
 * 
 * This service interacts with:
 * - MongoDB for message storage
 * - Kafka for real-time event publishing
 * - User service for sender/recipient validation
 * - Media service for media messages
 * - Notification service for push notifications
 */
public interface MessagingService {
    
    /**
     * Sends a message (one-on-one or group)
     * 
     * Process:
     * 1. Validate sender exists
     * 2. Validate recipient/group exists
     * 3. Save message to MongoDB
     * 4. Publish event to Kafka
     * 5. Trigger push notification
     * 6. Return message details
     * 
     * @param senderId ID of user sending the message
     * @param request Message content and metadata
     * @return MessageResponse with saved message details
     * @throws ChitChatException if sender, recipient, or group not found
     */
    MessageResponse sendMessage(Long senderId, SendMessageRequest request);
    
    /**
     * Gets paginated conversation messages between two users
     * 
     * Returns messages in chronological order (newest first for pagination).
     * Includes both sent and received messages in the conversation.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of MessageResponse objects
     */
    Page<MessageResponse> getConversationMessages(Long userId1, Long userId2, Pageable pageable);
    
    /**
     * Gets paginated messages for a specific group
     * 
     * Returns messages in chronological order (newest first).
     * Only accessible to group members.
     * 
     * @param groupId Group identifier
     * @param pageable Pagination parameters
     * @return Page of MessageResponse objects
     * @throws ChitChatException if group not found
     */
    Page<MessageResponse> getGroupMessages(String groupId, Pageable pageable);
    
    /**
     * Gets all messages for a user (sent and received)
     * 
     * Used for:
     * - User's inbox/message list
     * - Message history
     * - Data export
     * 
     * @param userId ID of the user
     * @param pageable Pagination parameters
     * @return Page of MessageResponse objects
     */
    Page<MessageResponse> getUserMessages(Long userId, Pageable pageable);
    
    /**
     * Searches messages by content
     * 
     * Full-text search across:
     * - Message content
     * - Only user's accessible messages
     * 
     * Uses MongoDB text indexes for efficient searching.
     * 
     * @param query Search query string
     * @param userId ID of user performing search
     * @return List of matching MessageResponse objects
     */
    List<MessageResponse> searchMessages(String query, Long userId);
    
    /**
     * Marks a message as read by the recipient
     * 
     * Updates:
     * - Message status to READ
     * - Sets readAt timestamp
     * - Publishes read receipt event (blue ticks)
     * 
     * @param messageId Message identifier
     * @param userId ID of user marking as read (must be recipient)
     * @return Updated MessageResponse
     * @throws ChitChatException if message not found or user not recipient
     */
    MessageResponse markMessageAsRead(String messageId, Long userId);
    
    /**
     * Marks a message as delivered to recipient's device
     * 
     * Updates:
     * - Message status to DELIVERED
     * - Sets deliveredAt timestamp
     * - Publishes delivery receipt event (double gray ticks)
     * 
     * Called automatically when message reaches recipient's device.
     * 
     * @param messageId Message identifier
     */
    void markMessageAsDelivered(String messageId);
    
    /**
     * Deletes a message
     * 
     * Two deletion modes:
     * 1. Delete for me: Message hidden only for this user
     * 2. Delete for everyone: Message removed for all participants
     * 
     * "Delete for everyone" is time-limited (e.g., within 1 hour of sending).
     * 
     * @param messageId Message identifier
     * @param userId ID of user deleting the message
     * @param deleteForEveryone true to delete for all, false to delete for self only
     * @throws ChitChatException if message not found or time limit exceeded
     */
    void deleteMessage(String messageId, Long userId, boolean deleteForEveryone);
    
    /**
     * Creates a new group chat
     * 
     * Creator becomes group admin by default.
     * 
     * @param adminId ID of user creating the group (becomes admin)
     * @param request Group details (name, description, members)
     * @return GroupResponse with created group details
     */
    GroupResponse createGroup(Long adminId, CreateGroupRequest request);
    
    /**
     * Adds a member to an existing group
     * 
     * Only group admins can add members.
     * 
     * @param groupId Group identifier
     * @param adminId ID of admin adding the member
     * @param memberId ID of user being added
     * @return Updated GroupResponse
     * @throws ChitChatException if not admin or user already in group
     */
    GroupResponse addMemberToGroup(String groupId, Long adminId, Long memberId);
    
    /**
     * Removes a member from a group
     * 
     * Only group admins can remove members.
     * Cannot remove other admins.
     * 
     * @param groupId Group identifier
     * @param adminId ID of admin removing the member
     * @param memberId ID of user being removed
     * @return Updated GroupResponse
     * @throws ChitChatException if not admin or trying to remove admin
     */
    GroupResponse removeMemberFromGroup(String groupId, Long adminId, Long memberId);
    
    /**
     * Updates group information
     * 
     * Only group admins can update group info.
     * 
     * @param groupId Group identifier
     * @param adminId ID of admin updating the group
     * @param name New group name (null to keep current)
     * @param description New description (null to keep current)
     * @return Updated GroupResponse
     * @throws ChitChatException if not admin
     */
    GroupResponse updateGroupInfo(String groupId, Long adminId, String name, String description);
    
    /**
     * Gets all groups a user is member of
     * 
     * @param userId ID of the user
     * @return List of GroupResponse objects
     */
    List<GroupResponse> getUserGroups(Long userId);
    
    /**
     * Gets group details by ID
     * 
     * @param groupId Group identifier
     * @return GroupResponse with group details
     * @throws ChitChatException if group not found
     */
    GroupResponse getGroupById(String groupId);
    
    /**
     * User leaves a group voluntarily
     * 
     * If last admin leaves, group is disbanded.
     * 
     * @param groupId Group identifier
     * @param userId ID of user leaving
     * @throws ChitChatException if user not in group
     */
    void leaveGroup(String groupId, Long userId);
}
