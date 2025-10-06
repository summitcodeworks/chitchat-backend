package com.chitchat.notification.repository;

import com.chitchat.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DeviceToken entity
 */
@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    
    List<DeviceToken> findByUserId(Long userId);
    
    List<DeviceToken> findByUserIdAndIsActive(Long userId, Boolean isActive);
    
    Optional<DeviceToken> findByToken(String token);
    
    Optional<DeviceToken> findByUserIdAndDeviceId(Long userId, String deviceId);
    
    Optional<DeviceToken> findByDeviceId(String deviceId);
    
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.userId = :userId AND dt.isActive = true")
    List<DeviceToken> findActiveTokensByUserId(@Param("userId") Long userId);
    
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.deviceType = :deviceType AND dt.isActive = true")
    List<DeviceToken> findActiveTokensByDeviceType(@Param("deviceType") DeviceToken.DeviceType deviceType);
}
