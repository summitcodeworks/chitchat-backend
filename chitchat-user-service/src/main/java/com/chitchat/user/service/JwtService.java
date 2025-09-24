package com.chitchat.user.service;

import com.chitchat.shared.service.ConfigurationService;
import com.chitchat.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
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
        return createToken(claims, user.getPhoneNumber());
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
        final String username = extractUsername(token);
        return (username.equals(phoneNumber) && !isTokenExpired(token));
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
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }
}
