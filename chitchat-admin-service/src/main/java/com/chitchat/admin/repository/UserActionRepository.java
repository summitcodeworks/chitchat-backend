package com.chitchat.admin.repository;

import com.chitchat.admin.entity.UserAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for UserAction entity
 */
@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    
    List<UserAction> findByUserId(Long userId);
    
    Page<UserAction> findByUserId(Long userId, Pageable pageable);
    
    List<UserAction> findByAction(String action);
    
    List<UserAction> findByResource(String resource);
    
    @Query("SELECT ua FROM UserAction ua WHERE ua.createdAt >= :since ORDER BY ua.createdAt DESC")
    List<UserAction> findRecentActions(@Param("since") LocalDateTime since);
    
    @Query("SELECT ua FROM UserAction ua WHERE ua.userId = :userId AND ua.createdAt >= :since ORDER BY ua.createdAt DESC")
    List<UserAction> findRecentActionsByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT ua FROM UserAction ua WHERE ua.status = :status ORDER BY ua.createdAt DESC")
    List<UserAction> findActionsByStatus(@Param("status") UserAction.ActionStatus status);
    
    @Query("SELECT ua FROM UserAction ua WHERE ua.action = :action AND ua.resource = :resource ORDER BY ua.createdAt DESC")
    List<UserAction> findActionsByActionAndResource(@Param("action") String action, @Param("resource") String resource);
}
