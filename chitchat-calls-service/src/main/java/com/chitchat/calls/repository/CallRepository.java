package com.chitchat.calls.repository;

import com.chitchat.calls.entity.Call;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Call entity
 */
@Repository
public interface CallRepository extends JpaRepository<Call, Long> {
    
    Optional<Call> findBySessionId(String sessionId);
    
    List<Call> findByCallerId(Long callerId);
    
    List<Call> findByCalleeId(Long calleeId);
    
    @Query("SELECT c FROM Call c WHERE (c.callerId = :userId OR c.calleeId = :userId) ORDER BY c.createdAt DESC")
    Page<Call> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT c FROM Call c WHERE (c.callerId = :userId OR c.calleeId = :userId) AND c.status = :status ORDER BY c.createdAt DESC")
    List<Call> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Call.CallStatus status);
    
    @Query("SELECT c FROM Call c WHERE c.calleeId = :userId AND c.status = 'MISSED' ORDER BY c.createdAt DESC")
    List<Call> findMissedCallsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Call c WHERE (c.callerId = :userId OR c.calleeId = :userId) AND c.createdAt >= :since ORDER BY c.createdAt DESC")
    List<Call> findRecentCallsByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT c FROM Call c WHERE c.status IN ('INITIATED', 'RINGING') AND c.createdAt < :before")
    List<Call> findStuckCalls(@Param("before") LocalDateTime before);
    
    @Query("SELECT c FROM Call c WHERE c.callerId = :callerId AND c.calleeId = :calleeId AND c.status = 'ANSWERED' ORDER BY c.createdAt DESC")
    List<Call> findAnsweredCallsBetweenUsers(@Param("callerId") Long callerId, @Param("calleeId") Long calleeId);
}
