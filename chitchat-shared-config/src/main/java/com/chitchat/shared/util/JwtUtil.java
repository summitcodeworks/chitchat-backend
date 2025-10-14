package com.chitchat.shared.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.function.Function;

/**
 * Shared JWT utility for extracting information from JWT tokens
 * Used across microservices for consistent token handling
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:chitchat-super-secret-key-for-jwt-token-generation-min-256-bits}")
    private String jwtSecret;
    
    /**
     * Get the signing key for JWT verification
     */
    private SecretKey getSigningKey() {
        if (jwtSecret == null) {
            throw new RuntimeException("JWT secret not configured");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    /**
     * Extract user ID from JWT token
     * 
     * @param token JWT token string (without "Bearer " prefix)
     * @return User ID from the token's userId claim
     */
    public Long extractUserId(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userId", Long.class));
        } catch (Exception e) {
            log.error("Failed to extract user ID from token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired token", e);
        }
    }
    
    /**
     * Extract user ID from Authorization header
     * 
     * @param authHeader Authorization header value (e.g., "Bearer eyJhbGc...")
     * @return User ID from the token
     */
    public Long extractUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization header");
        }
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return extractUserId(token);
    }
    
    /**
     * Extract phone number (username) from JWT token
     * 
     * @param token JWT token string
     * @return Phone number from the token's subject
     */
    public String extractPhoneNumber(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.error("Failed to extract phone number from token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired token", e);
        }
    }
    
    /**
     * Extract any claim from JWT token
     * 
     * @param token JWT token string
     * @param claimsResolver Function to extract specific claim
     * @return The extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extract all claims from JWT token
     * 
     * @param token JWT token string
     * @return All claims from the token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw new RuntimeException("Failed to parse JWT token", e);
        }
    }
    
    /**
     * Validate if a token is from the system (messaging service calling notification service)
     * 
     * @param authHeader Authorization header value
     * @return true if this is a system token
     */
    public boolean isSystemToken(String authHeader) {
        return authHeader != null && authHeader.equals("Bearer SYSTEM_TOKEN");
    }
}

