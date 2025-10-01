package com.chitchat.messaging.repository;

import com.chitchat.messaging.document.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Message document MongoDB operations
 * 
 * Provides data access methods for message storage and retrieval.
 * Extends MongoRepository for CRUD operations plus custom MongoDB queries.
 * 
 * Standard Methods (from MongoRepository):
 * - save(), findById(), findAll(), delete(), etc.
 * 
 * Custom Query Methods:
 * - Conversation message retrieval (bi-directional)
 * - Group message queries
 * - Message search (full-text)
 * - Delivery status tracking
 * - Unread message counts
 * 
 * Database: MongoDB
 * Collection: messages
 * 
 * Query Syntax: MongoDB JSON query format
 * - Uses MongoDB operators: $or, $in, $regex, $lt, $gte, $ne
 * - Efficient with proper indexes on senderId, recipientId, groupId
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    /**
     * Finds conversation messages between two users (bi-directional)
     * 
     * Returns all messages where:
     * - user1 sent to user2 OR
     * - user2 sent to user1
     * 
     * This gives complete conversation history regardless of who sent what.
     * 
     * MongoDB Query Explained:
     * { $or: [
     *   { senderId: userId1, recipientId: userId2 },
     *   { senderId: userId2, recipientId: userId1 }
     * ]}
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param pageable Pagination with sorting (typically by createdAt DESC)
     * @return Page of messages in the conversation
     */
    @Query("{ $or: [ { senderId: ?0, recipientId: ?1 }, { senderId: ?1, recipientId: ?0 } ] }")
    Page<Message> findConversationMessages(Long userId1, Long userId2, Pageable pageable);
    
    /**
     * Finds all messages in a specific group
     * 
     * Simple query filtering by groupId.
     * Used for group chat message history.
     * 
     * @param groupId MongoDB ObjectId of the group
     * @param pageable Pagination parameters
     * @return Page of group messages
     */
    @Query("{ groupId: ?0 }")
    Page<Message> findGroupMessages(String groupId, Pageable pageable);
    
    /**
     * Finds all messages for a user (sent, received, and group messages)
     * 
     * Returns messages where user is:
     * - Sender (senderId matches)
     * - Recipient (recipientId matches)
     * - Group member (groupId in user's groups)
     * 
     * Used for complete user message history.
     * 
     * @param userId User's ID
     * @param groupIds List of group IDs user is member of
     * @param pageable Pagination parameters
     * @return Page of all user's messages
     */
    @Query("{ $or: [ { senderId: ?0 }, { recipientId: ?0 }, { groupId: { $in: ?1 } } ] }")
    Page<Message> findUserMessages(Long userId, List<String> groupIds, Pageable pageable);
    
    /**
     * Searches messages by content (full-text search)
     * 
     * MongoDB Query Features:
     * - $regex: Regular expression pattern matching
     * - $options: 'i' for case-insensitive search
     * - Only searches user's own messages (privacy)
     * 
     * Example: query="hello" matches "Hello", "HELLO world", "say hello"
     * 
     * Note: For production, use MongoDB text indexes for better performance.
     * 
     * @param query Search term
     * @param userId User performing the search (privacy filter)
     * @return List of matching messages
     */
    @Query("{ content: { $regex: ?0, $options: 'i' }, $or: [ { senderId: ?1 }, { recipientId: ?1 } ] }")
    List<Message> searchMessages(String query, Long userId);
    
    /**
     * Finds messages that were sent but not delivered
     * 
     * Used for retry mechanism:
     * - Find messages stuck in SENT status
     * - Retry notification delivery
     * - Handle offline recipients
     * 
     * Typically run as scheduled task.
     * 
     * @param before Cutoff time (e.g., 5 minutes ago)
     * @return List of undelivered messages
     */
    @Query("{ status: 'SENT', createdAt: { $lt: ?0 } }")
    List<Message> findUndeliveredMessages(LocalDateTime before);
    
    /**
     * Finds unread messages sent by a specific user
     * 
     * Used for:
     * - Showing unread count to sender
     * - Identifying which messages need read receipts
     * 
     * $ne: 'READ' means status is not READ
     * Includes SENT and DELIVERED messages.
     * 
     * @param senderId User ID of message sender
     * @return List of messages not yet read by recipients
     */
    @Query("{ senderId: ?0, status: { $ne: 'READ' } }")
    List<Message> findUnreadMessagesForSender(Long senderId);
    
    /**
     * Finds delivered but not yet read messages for a recipient
     * 
     * Used for:
     * - Unread message count
     * - Marking messages as read in batch
     * - Notification badge count
     * 
     * @param recipientId User ID of message recipient
     * @return List of delivered messages waiting to be read
     */
    @Query("{ recipientId: ?0, status: 'DELIVERED' }")
    List<Message> findDeliveredMessagesForRecipient(Long recipientId);
    
    /**
     * Finds recent messages in a group since a specific time
     * 
     * Used for:
     * - Loading recent activity when user opens group
     * - Catching up on missed messages
     * - Real-time message updates
     * 
     * $gte: Greater than or equal to (includes boundary)
     * 
     * @param groupId Group identifier
     * @param since Timestamp to get messages after
     * @return List of messages since the specified time
     */
    @Query("{ groupId: ?0, createdAt: { $gte: ?1 } }")
    List<Message> findRecentGroupMessages(String groupId, LocalDateTime since);
}
