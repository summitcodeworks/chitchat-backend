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
 * Repository interface for Message document
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    @Query("{ $or: [ { senderId: ?0, recipientId: ?1 }, { senderId: ?1, recipientId: ?0 } ] }")
    Page<Message> findConversationMessages(Long userId1, Long userId2, Pageable pageable);
    
    @Query("{ groupId: ?0 }")
    Page<Message> findGroupMessages(String groupId, Pageable pageable);
    
    @Query("{ $or: [ { senderId: ?0 }, { recipientId: ?0 }, { groupId: { $in: ?1 } } ] }")
    Page<Message> findUserMessages(Long userId, List<String> groupIds, Pageable pageable);
    
    @Query("{ content: { $regex: ?0, $options: 'i' }, $or: [ { senderId: ?1 }, { recipientId: ?1 } ] }")
    List<Message> searchMessages(String query, Long userId);
    
    @Query("{ status: 'SENT', createdAt: { $lt: ?0 } }")
    List<Message> findUndeliveredMessages(LocalDateTime before);
    
    @Query("{ senderId: ?0, status: { $ne: 'READ' } }")
    List<Message> findUnreadMessagesForSender(Long senderId);
    
    @Query("{ recipientId: ?0, status: 'DELIVERED' }")
    List<Message> findDeliveredMessagesForRecipient(Long recipientId);
    
    @Query("{ groupId: ?0, createdAt: { $gte: ?1 } }")
    List<Message> findRecentGroupMessages(String groupId, LocalDateTime since);
}
