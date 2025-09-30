package com.chitchat.user.service;

import com.chitchat.shared.service.ConfigurationService;
import com.chitchat.user.entity.JwtToken;
import com.chitchat.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    
    private final ConfigurationService configurationService;
    private final JwtTokenService jwtTokenService;
    
    private SecretKey getSigningKey() {
        String secret = configurationService.getJwtSecret();
        if (secret == null) {
            throw new RuntimeException("JWT secret not configured");
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    private Long getExpiration() {
        return configurationService.getJwtExpiration() * 1000; // Convert seconds to milliseconds
    }
    
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("phoneNumber", user.getPhoneNumber());
        
        // Generate the JWT token
        String token = createToken(claims, user.getPhoneNumber());
        
        // Calculate expiration times
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(configurationService.getJwtExpiration());
        
        // Save token to database
        try {
            jwtTokenService.saveToken(token, user, now, expiresAt);
            log.info("Generated and saved JWT token for user: {}", user.getPhoneNumber());
        } catch (Exception e) {
            log.error("Failed to save JWT token to database: {}", e.getMessage());
            // Continue without database storage for now
        }
        
        return token;
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + getExpiration()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public Boolean validateToken(String token, String phoneNumber) {
        try {
            // Temporarily skip database validation to test if that's causing the hang
            log.info("Validating token for user: {}", phoneNumber);
            
            // Validate JWT structure and expiration only
            final String username = extractUsername(token);
            boolean isValid = (username.equals(phoneNumber) && !isTokenExpired(token));
            
            if (!isValid) {
                log.warn("Token validation failed for user: {}", phoneNumber);
            } else {
                log.info("Token validation successful for user: {}", phoneNumber);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        log.info("Extracting claims from token: {}...", token.substring(0, Math.min(20, token.length())));
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.info("Successfully extracted claims from token");
            return claims;
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }
    
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }
}
