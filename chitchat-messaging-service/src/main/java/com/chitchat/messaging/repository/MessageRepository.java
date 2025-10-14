package com.chitchat.messaging.repository;

import com.chitchat.messaging.document.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
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
     * Finds unread messages for a recipient (SENT or DELIVERED, not READ)
     * 
     * Used for:
     * - Unread message count
     * - Marking messages as read in batch
     * - Notification badge count
     * 
     * @param recipientId User ID of message recipient
     * @return List of unread messages (SENT or DELIVERED status)
     */
    @Query("{ recipientId: ?0, status: { $in: ['SENT', 'DELIVERED'] } }")
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
    
    /**
     * Finds all unique conversation partners for a user
     * 
     * Returns distinct user IDs that the given user has had conversations with.
     * Used for building conversation list.
     * 
     * MongoDB Aggregation:
     * 1. Match messages where user is sender or recipient
     * 2. Project the other user's ID (if user is sender, get recipientId, vice versa)
     * 3. Group by other user's ID to get unique conversation partners
     * 
     * @param userId User ID to find conversation partners for
     * @return List of user IDs that have conversations with the given user
     */
    @Query(value = "{ $or: [ { senderId: ?0 }, { recipientId: ?0 } ] }", fields = "{ senderId: 1, recipientId: 1 }")
    List<Message> findConversationPartners(Long userId);
    
    /**
     * Finds the latest message in each conversation for a user
     * 
     * Used for conversation list to show most recent message.
     * Returns one message per conversation (the latest one).
     * 
     * MongoDB Aggregation Pipeline:
     * 1. Match messages where user is involved
     * 2. Add computed field for conversation partner ID
     * 3. Sort by createdAt descending
     * 4. Group by conversation partner to get latest message per conversation
     * 5. Sort results by latest message time descending
     * 
     * @param userId User ID to get conversations for
     * @return List of latest messages for each conversation
     */
    @Aggregation(pipeline = {
        "{ $match: { $or: [ { senderId: ?0 }, { recipientId: ?0 } ] } }",
        "{ $addFields: { conversationPartner: { $cond: [ { $eq: ['$senderId', ?0] }, '$recipientId', '$senderId' ] } } }",
        "{ $sort: { createdAt: -1 } }",
        "{ $group: { _id: '$conversationPartner', latestMessage: { $first: '$$ROOT' } } }",
        "{ $replaceRoot: { newRoot: '$latestMessage' } }",
        "{ $sort: { createdAt: -1 } }"
    })
    List<Message> findLatestMessagesForConversations(Long userId);
    
    /**
     * Counts unread messages for each conversation partner
     * 
     * Returns unread message count for each user the given user has conversations with.
     * Used for showing unread badges in conversation list.
     * 
     * MongoDB Aggregation Pipeline:
     * 1. Match messages where user is recipient and status is SENT or DELIVERED (unread)
     * 2. Group by sender ID to count unread messages per sender
     * 
     * Note: Both SENT and DELIVERED count as unread (not READ yet)
     * 
     * @param userId User ID to count unread messages for
     * @return List of unread counts per conversation partner
     */
    @Aggregation(pipeline = {
        "{ $match: { recipientId: ?0, status: { $in: ['SENT', 'DELIVERED'] } } }",
        "{ $group: { _id: '$senderId', unreadCount: { $sum: 1 } } }"
    })
    List<UnreadCountResult> findUnreadCountsBySender(Long userId);
    
    /**
     * Counts total unread messages for a user (messages sent TO the user)
     * 
     * Returns total count of unread messages where the user is the recipient.
     * Used for showing total unread count badge in the app header.
     * 
     * MongoDB Query:
     * - recipientId: User ID (messages sent TO this user)
     * - status: SENT or DELIVERED (messages that are not read yet)
     * 
     * Note: Both SENT and DELIVERED count as unread (not READ yet)
     * 
     * @param userId User ID to count total unread messages for
     * @return Total count of unread messages
     */
    @Query(value = "{ recipientId: ?0, status: { $in: ['SENT', 'DELIVERED'] } }", count = true)
    long countTotalUnreadMessages(Long userId);
    
    /**
     * Finds pinned messages in a one-on-one conversation
     * 
     * Used for:
     * - Finding existing pinned messages to unpin before pinning a new one
     * - Ensuring only one message per conversation can be pinned
     * 
     * @param senderId First user ID in the conversation
     * @param recipientId Second user ID in the conversation
     * @return List of pinned messages in the conversation
     */
    @Query("{ $or: [ { senderId: ?0, recipientId: ?1 }, { senderId: ?1, recipientId: ?0 } ], isPinned: true }")
    List<Message> findBySenderIdAndRecipientIdAndIsPinnedTrue(Long senderId, Long recipientId);
    
    /**
     * Finds pinned messages in a group conversation
     * 
     * Used for:
     * - Finding existing pinned messages to unpin before pinning a new one
     * - Ensuring only one message per group can be pinned
     * 
     * @param senderId User ID who sent the message
     * @param groupId Group ID for the conversation
     * @return List of pinned messages in the group
     */
    @Query("{ senderId: ?0, groupId: ?1, isPinned: true }")
    List<Message> findBySenderIdAndGroupIdAndIsPinnedTrue(Long senderId, String groupId);
    
    /**
     * Finds messages by IDs in bulk
     * 
     * Used for batch operations when updating multiple messages at once.
     * More efficient than calling findById multiple times.
     * 
     * @param messageIds List of message IDs to retrieve
     * @return List of messages matching the IDs
     */
    @Query("{ _id: { $in: ?0 } }")
    List<Message> findByIdIn(List<String> messageIds);
    
    /**
     * Bulk update message status
     * 
     * Updates status for multiple messages to READ in a single operation.
     * Used when marking all messages in a conversation as read.
     * 
     * @param recipientId User ID marking messages as read
     * @param senderId User ID who sent the messages
     * @return Number of messages updated
     */
    @Query(value = "{ recipientId: ?0, senderId: ?1, status: { $in: ['SENT', 'DELIVERED'] } }", delete = false, count = false)
    List<Message> findUnreadMessagesFromSender(Long recipientId, Long senderId);
    
    /**
     * Finds all messages in SENT status for a specific recipient
     * 
     * Used when user comes online to deliver pending messages.
     * Returns only messages in SENT status (not DELIVERED or READ).
     * These are messages that were sent while the user was offline.
     * 
     * @param recipientId User ID to find pending messages for
     * @return List of messages in SENT status
     */
    @Query("{ recipientId: ?0, status: 'SENT' }")
    List<Message> findPendingMessagesForRecipient(Long recipientId);
    
    /**
     * Result class for unread count aggregation
     */
    class UnreadCountResult {
        private Long senderId;
        private Long unreadCount;
        
        public UnreadCountResult() {}
        
        public UnreadCountResult(Long senderId, Long unreadCount) {
            this.senderId = senderId;
            this.unreadCount = unreadCount;
        }
        
        public Long getSenderId() { return senderId; }
        public void setSenderId(Long senderId) { this.senderId = senderId; }
        
        public Long getUnreadCount() { return unreadCount; }
        public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
    }
}
