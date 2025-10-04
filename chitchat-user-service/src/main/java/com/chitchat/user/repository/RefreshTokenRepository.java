package com.chitchat.user.repository;

import com.chitchat.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken entity operations
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    /**
     * Find a valid refresh token by token string
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.isRevoked = false AND rt.expiresAt > :currentTime")
    Optional<RefreshToken> findValidToken(@Param("token") String token, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find refresh token by token string (including revoked/expired)
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Find all valid refresh tokens for a user
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.isRevoked = false AND rt.expiresAt > :currentTime")
    List<RefreshToken> findValidTokensByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find all valid refresh tokens for a phone number
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.phoneNumber = :phoneNumber AND rt.isRevoked = false AND rt.expiresAt > :currentTime")
    List<RefreshToken> findValidTokensByPhoneNumber(@Param("phoneNumber") String phoneNumber, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Revoke all refresh tokens for a user
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.updatedAt = :currentTime WHERE rt.userId = :userId")
    void revokeAllTokensByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Revoke all refresh tokens for a phone number
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.updatedAt = :currentTime WHERE rt.phoneNumber = :phoneNumber")
    void revokeAllTokensByPhoneNumber(@Param("phoneNumber") String phoneNumber, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Revoke a specific refresh token
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.updatedAt = :currentTime WHERE rt.token = :token")
    void revokeToken(@Param("token") String token, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Delete expired refresh tokens
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :currentTime")
    void deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Check if refresh token exists and is valid
     */
    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.token = :token AND rt.isRevoked = false AND rt.expiresAt > :currentTime")
    boolean existsValidToken(@Param("token") String token, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Count valid refresh tokens for a user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.isRevoked = false AND rt.expiresAt > :currentTime")
    long countValidTokensByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);
}

