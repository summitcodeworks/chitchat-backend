package com.chitchat.user.repository;

import com.chitchat.user.entity.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {
    
    /**
     * Find a valid JWT token by token string
     */
    @Query("SELECT jt FROM JwtToken jt WHERE jt.token = :token AND jt.isRevoked = false AND jt.expiresAt > :currentTime")
    Optional<JwtToken> findValidToken(@Param("token") String token, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find all valid tokens for a user
     */
    @Query("SELECT jt FROM JwtToken jt WHERE jt.userId = :userId AND jt.isRevoked = false AND jt.expiresAt > :currentTime")
    List<JwtToken> findValidTokensByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find all valid tokens for a phone number
     */
    @Query("SELECT jt FROM JwtToken jt WHERE jt.phoneNumber = :phoneNumber AND jt.isRevoked = false AND jt.expiresAt > :currentTime")
    List<JwtToken> findValidTokensByPhoneNumber(@Param("phoneNumber") String phoneNumber, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Revoke all tokens for a user
     */
    @Modifying
    @Transactional
    @Query("UPDATE JwtToken jt SET jt.isRevoked = true, jt.updatedAt = :currentTime WHERE jt.userId = :userId")
    void revokeAllTokensByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Revoke all tokens for a phone number
     */
    @Modifying
    @Transactional
    @Query("UPDATE JwtToken jt SET jt.isRevoked = true, jt.updatedAt = :currentTime WHERE jt.phoneNumber = :phoneNumber")
    void revokeAllTokensByPhoneNumber(@Param("phoneNumber") String phoneNumber, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Revoke a specific token
     */
    @Modifying
    @Transactional
    @Query("UPDATE JwtToken jt SET jt.isRevoked = true, jt.updatedAt = :currentTime WHERE jt.token = :token")
    void revokeToken(@Param("token") String token, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Delete expired tokens
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JwtToken jt WHERE jt.expiresAt < :currentTime")
    void deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find token by token string (including revoked/expired)
     */
    Optional<JwtToken> findByToken(String token);
    
    /**
     * Check if token exists and is valid
     */
    @Query("SELECT COUNT(jt) > 0 FROM JwtToken jt WHERE jt.token = :token AND jt.isRevoked = false AND jt.expiresAt > :currentTime")
    boolean existsValidToken(@Param("token") String token, @Param("currentTime") LocalDateTime currentTime);
}
