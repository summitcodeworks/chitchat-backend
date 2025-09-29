package com.chitchat.user.service;

import com.chitchat.user.entity.JwtToken;
import com.chitchat.user.entity.User;
import com.chitchat.user.repository.JwtTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JwtTokenService {
    
    @Autowired
    private JwtTokenRepository jwtTokenRepository;
    
    /**
     * Save a JWT token to the database
     */
    public JwtToken saveToken(String token, User user, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        // First, revoke all existing tokens for this user
        revokeAllTokensByUserId(user.getId());
        
        // Create and save new token
        JwtToken jwtToken = new JwtToken(token, user.getId(), user.getPhoneNumber(), issuedAt, expiresAt);
        return jwtTokenRepository.save(jwtToken);
    }
    
    /**
     * Validate a JWT token from the database
     */
    public boolean validateToken(String token) {
        LocalDateTime currentTime = LocalDateTime.now();
        return jwtTokenRepository.existsValidToken(token, currentTime);
    }
    
    /**
     * Find a valid JWT token
     */
    public Optional<JwtToken> findValidToken(String token) {
        LocalDateTime currentTime = LocalDateTime.now();
        return jwtTokenRepository.findValidToken(token, currentTime);
    }
    
    /**
     * Find user ID from token
     */
    public Optional<Long> getUserIdFromToken(String token) {
        Optional<JwtToken> jwtToken = findValidToken(token);
        return jwtToken.map(JwtToken::getUserId);
    }
    
    /**
     * Find phone number from token
     */
    public Optional<String> getPhoneNumberFromToken(String token) {
        Optional<JwtToken> jwtToken = findValidToken(token);
        return jwtToken.map(JwtToken::getPhoneNumber);
    }
    
    /**
     * Revoke a specific token
     */
    public void revokeToken(String token) {
        LocalDateTime currentTime = LocalDateTime.now();
        jwtTokenRepository.revokeToken(token, currentTime);
    }
    
    /**
     * Revoke all tokens for a user
     */
    public void revokeAllTokensByUserId(Long userId) {
        LocalDateTime currentTime = LocalDateTime.now();
        jwtTokenRepository.revokeAllTokensByUserId(userId, currentTime);
    }
    
    /**
     * Revoke all tokens for a phone number
     */
    public void revokeAllTokensByPhoneNumber(String phoneNumber) {
        LocalDateTime currentTime = LocalDateTime.now();
        jwtTokenRepository.revokeAllTokensByPhoneNumber(phoneNumber, currentTime);
    }
    
    /**
     * Get all valid tokens for a user
     */
    public List<JwtToken> getValidTokensByUserId(Long userId) {
        LocalDateTime currentTime = LocalDateTime.now();
        return jwtTokenRepository.findValidTokensByUserId(userId, currentTime);
    }
    
    /**
     * Get all valid tokens for a phone number
     */
    public List<JwtToken> getValidTokensByPhoneNumber(String phoneNumber) {
        LocalDateTime currentTime = LocalDateTime.now();
        return jwtTokenRepository.findValidTokensByPhoneNumber(phoneNumber, currentTime);
    }
    
    /**
     * Clean up expired tokens
     */
    public void cleanupExpiredTokens() {
        LocalDateTime currentTime = LocalDateTime.now();
        jwtTokenRepository.deleteExpiredTokens(currentTime);
    }
    
    /**
     * Check if token exists (including revoked/expired)
     */
    public boolean tokenExists(String token) {
        return jwtTokenRepository.findByToken(token).isPresent();
    }
}
