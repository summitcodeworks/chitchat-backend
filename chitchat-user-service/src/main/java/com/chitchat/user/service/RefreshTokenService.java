package com.chitchat.user.service;

import com.chitchat.shared.exception.ChitChatException;
import com.chitchat.shared.service.ConfigurationService;
import com.chitchat.user.entity.RefreshToken;
import com.chitchat.user.entity.User;
import com.chitchat.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing refresh tokens
 * 
 * Provides functionality to:
 * - Generate new refresh tokens
 * - Validate existing refresh tokens
 * - Revoke tokens (single, user, or all)
 * - Clean up expired tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final ConfigurationService configurationService;
    
    /**
     * Generate a new refresh token for a user
     * 
     * @param user The user to generate token for
     * @param deviceInfo Optional device information
     * @param ipAddress Optional IP address
     * @return The generated refresh token
     */
    @Transactional
    public RefreshToken generateRefreshToken(User user, String deviceInfo, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        Long expirationSeconds = getRefreshTokenExpiration();
        LocalDateTime expiresAt = now.plusSeconds(expirationSeconds);
        
        // Generate a secure random token
        String token = UUID.randomUUID().toString();
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .isRevoked(false)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();
        
        refreshToken = refreshTokenRepository.save(refreshToken);
        
        log.info("Generated refresh token for user ID: {} (expires in {} days)", 
                user.getId(), expirationSeconds / 86400);
        
        return refreshToken;
    }
    
    /**
     * Validate a refresh token
     * 
     * @param token The refresh token string
     * @return The valid RefreshToken entity
     * @throws ChitChatException if token is invalid, revoked, or expired
     */
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findValidToken(token, LocalDateTime.now())
                .orElseThrow(() -> new ChitChatException(
                        "Invalid or expired refresh token", 
                        HttpStatus.UNAUTHORIZED, 
                        "INVALID_REFRESH_TOKEN"
                ));
        
        log.debug("Validated refresh token for user ID: {}", refreshToken.getUserId());
        return refreshToken;
    }
    
    /**
     * Update last used timestamp for a refresh token
     * 
     * @param refreshToken The refresh token to update
     */
    @Transactional
    public void updateLastUsed(RefreshToken refreshToken) {
        refreshToken.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        log.debug("Updated last used time for refresh token ID: {}", refreshToken.getId());
    }
    
    /**
     * Revoke a specific refresh token
     * 
     * @param token The refresh token string to revoke
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.revokeToken(token, LocalDateTime.now());
        log.info("Revoked refresh token: {}", token.substring(0, Math.min(8, token.length())));
    }
    
    /**
     * Revoke all refresh tokens for a user
     * 
     * @param userId The user ID
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllTokensByUserId(userId, LocalDateTime.now());
        log.info("Revoked all refresh tokens for user ID: {}", userId);
    }
    
    /**
     * Revoke all refresh tokens for a phone number
     * 
     * @param phoneNumber The phone number
     */
    @Transactional
    public void revokeAllPhoneNumberTokens(String phoneNumber) {
        refreshTokenRepository.revokeAllTokensByPhoneNumber(phoneNumber, LocalDateTime.now());
        log.info("Revoked all refresh tokens for phone number: {}", phoneNumber);
    }
    
    /**
     * Get all valid refresh tokens for a user
     * 
     * @param userId The user ID
     * @return List of valid refresh tokens
     */
    public List<RefreshToken> getUserRefreshTokens(Long userId) {
        return refreshTokenRepository.findValidTokensByUserId(userId, LocalDateTime.now());
    }
    
    /**
     * Clean up expired refresh tokens
     * This should be called periodically (e.g., daily scheduled job)
     * 
     * @return Number of tokens deleted
     */
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteExpiredTokens(now);
        log.info("Cleaned up expired refresh tokens");
    }
    
    /**
     * Get refresh token expiration time from configuration
     * 
     * @return Expiration time in seconds
     */
    private Long getRefreshTokenExpiration() {
        try {
            return configurationService.getConfigValueAsLong("jwt.refresh.expiration", 2592000L); // Default 30 days
        } catch (Exception e) {
            log.warn("Failed to get refresh token expiration from config, using default: 30 days");
            return 2592000L; // 30 days default
        }
    }
}

