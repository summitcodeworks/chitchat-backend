package com.chitchat.notification.repository;

import com.chitchat.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Notification entity
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserId(Long userId);
    
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdAndStatus(Long userId, Notification.NotificationStatus status);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status = 'PENDING' ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotificationsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.scheduledAt <= :now ORDER BY n.scheduledAt ASC")
    List<Notification> findScheduledNotificationsToSend(@Param("now") LocalDateTime now);
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < 3 ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotificationsToRetry();
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status = 'SENT' AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT n FROM Notification n WHERE n.createdAt < :before AND n.status IN ('SENT', 'DELIVERED', 'READ')")
    List<Notification> findOldNotificationsToCleanup(@Param("before") LocalDateTime before);
}
