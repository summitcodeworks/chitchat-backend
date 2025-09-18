package com.chitchat.status.repository;

import com.chitchat.status.document.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Status document
 */
@Repository
public interface StatusRepository extends MongoRepository<Status, String> {
    
    List<Status> findByUserId(Long userId);
    
    Page<Status> findByUserId(Long userId, Pageable pageable);
    
    @Query("{ userId: ?0, expiresAt: { $gt: ?1 } }")
    List<Status> findActiveStatusesByUserId(Long userId, LocalDateTime now);
    
    @Query("{ userId: { $in: ?0 }, expiresAt: { $gt: ?1 } }")
    List<Status> findActiveStatusesByUserIds(List<Long> userIds, LocalDateTime now);
    
    @Query("{ expiresAt: { $lt: ?0 } }")
    List<Status> findExpiredStatuses(LocalDateTime now);
    
    @Query("{ userId: ?0, 'views.userId': { $ne: ?1 } }")
    List<Status> findUnviewedStatusesByUserId(Long userId, Long viewerId);
    
    @Query("{ userId: ?0, 'reactions.userId': ?1 }")
    List<Status> findStatusesReactedByUser(Long userId, Long reactorId);
    
    @Query("{ userId: ?0, type: ?1, expiresAt: { $gt: ?2 } }")
    List<Status> findActiveStatusesByUserIdAndType(Long userId, Status.StatusType type, LocalDateTime now);
    
    @Query("{ userId: ?0, privacy: ?1, expiresAt: { $gt: ?2 } }")
    List<Status> findActiveStatusesByUserIdAndPrivacy(Long userId, Status.StatusPrivacy privacy, LocalDateTime now);
}
