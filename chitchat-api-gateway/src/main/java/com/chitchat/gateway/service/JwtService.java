package com.chitchat.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service for token generation, validation, and claims extraction
 * 
 * This service handles all JWT (JSON Web Token) operations for the API Gateway:
 * - Token generation with user claims (userId, username, phoneNumber)
 * - Token validation and expiration checking
 * - Claims extraction (userId, username, phoneNumber)
 * - Signature verification using HMAC SHA-256
 * 
 * Security Features:
 * - Uses HMAC SHA-256 for token signing
 * - Configurable expiration time
 * - Secure key handling
 * - Token tampering detection through signature validation
 * 
 * Token Structure:
 * - Header: Algorithm (HS256) and token type (JWT)
 * - Payload: Claims (subject, userId, phoneNumber, issued at, expiration)
 * - Signature: HMAC SHA-256 hash of header + payload + secret
 */
@Slf4j
@Service
public class JwtService {

    /**
     * Secret key for JWT signing and verification
     * Should be a strong, random string stored securely (env variable/secrets manager)
     * Minimum 256 bits (32 characters) for HS256 algorithm
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Token expiration time in seconds
     * Default is typically 24 hours (86400 seconds)
     * Shorter expiration improves security, longer improves user experience
     */
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Creates the cryptographic signing key from the secret
     * 
     * Converts the secret string to a SecretKey object suitable for HMAC operations.
     * The key is used to sign tokens during generation and verify signatures during validation.
     * 
     * @return SecretKey for HMAC SHA-256 signing
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Extracts the username (subject) from the JWT token
     * 
     * The username is stored in the 'subject' claim of the JWT.
     * This is typically the user's unique identifier or username.
     * 
     * @param token JWT token string
     * @return Username extracted from token subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT token
     * 
     * Returns when the token will expire and become invalid.
     * Used to check if token is still valid.
     * 
     * @param token JWT token string
     * @return Expiration date of the token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract any claim from the JWT token
     * 
     * Uses a function resolver pattern to extract specific claims.
     * This allows flexible extraction of any claim type from the token.
     * 
     * @param token JWT token string
     * @param claimsResolver Function to resolve the desired claim from all claims
     * @param <T> Type of the claim being extracted
     * @return The extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // Parse all claims from the token
        final Claims claims = extractAllClaims(token);
        // Apply the resolver function to extract the specific claim
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from the JWT token
     * 
     * Parses and validates the JWT token, extracting all claims contained within.
     * This method performs the following validations:
     * 1. Verifies the token signature using the secret key
     * 2. Checks token structure (header, payload, signature)
     * 3. Validates the signature hasn't been tampered with
     * 
     * If any validation fails, an exception is thrown.
     * 
     * @param token JWT token string to parse
     * @return Claims object containing all token claims
     * @throws JwtException if token is invalid, expired, or tampered
     */
    private Claims extractAllClaims(String token) {
        log.info("API Gateway: Extracting claims from token: {}...", token.substring(0, Math.min(20, token.length())));
        try {
            // Build JWT parser with signing key for signature verification
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())  // Set the key to verify signature
                    .build()
                    .parseClaimsJws(token)           // Parse and validate the token
                    .getBody();                       // Extract the claims/payload
            log.info("API Gateway: Successfully extracted claims from token");
            return claims;
        } catch (Exception e) {
            // Log error but rethrow - let caller handle the exception
            log.error("API Gateway: Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Checks if the JWT token has expired
     * 
     * Compares the token's expiration time with the current time.
     * Expired tokens should not be accepted for authentication.
     * 
     * @param token JWT token to check
     * @return true if token is expired, false if still valid
     */
    private Boolean isTokenExpired(String token) {
        // Extract expiration date and compare with current date
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generates a new JWT token with user information
     * 
     * Creates a signed JWT token containing:
     * - Subject (username)
     * - User ID (custom claim)
     * - Phone number (custom claim)
     * - Issued at timestamp
     * - Expiration timestamp
     * 
     * The token is signed with HMAC SHA-256 to prevent tampering.
     * 
     * @param username The user's username (stored as subject)
     * @param userId The user's unique ID (custom claim)
     * @param phoneNumber The user's phone number (custom claim)
     * @return Signed JWT token string
     */
    public String generateToken(String username, Long userId, String phoneNumber) {
        // Prepare custom claims to include in the token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);               // User's database ID
        claims.put("phoneNumber", phoneNumber);     // User's phone number
        
        // Create and return the token with these claims
        return createToken(claims, username);
    }

    /**
     * Creates a JWT token with specified claims and subject
     * 
     * Builds the complete JWT token structure:
     * 1. Sets custom claims (userId, phoneNumber, etc.)
     * 2. Sets standard claims (subject, issued at, expiration)
     * 3. Signs the token with HMAC SHA-256
     * 4. Compacts to final JWT string format
     * 
     * @param claims Map of custom claims to include in token
     * @param subject The subject claim (typically username)
     * @return Signed and compact JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)           // Add custom claims (userId, phoneNumber)
                .setSubject(subject)         // Set the subject (username)
                .setIssuedAt(new Date(System.currentTimeMillis()))  // Set issue time to now
                // Calculate expiration: current time + expiration seconds * 1000 (convert to ms)
                .setExpiration(new Date(System.currentTimeMillis() + (expiration * 1000)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // Sign with HS256
                .compact();                  // Build and return compact JWT string
    }

    /**
     * Validates JWT token against a specific username
     * 
     * Performs two validations:
     * 1. Verifies the token belongs to the specified username
     * 2. Checks the token hasn't expired
     * 
     * Both conditions must be true for the token to be valid.
     * 
     * @param token JWT token to validate
     * @param username Expected username to match against token
     * @return true if token is valid and matches username, false otherwise
     */
    public Boolean validateToken(String token, String username) {
        // Extract username from token
        final String extractedUsername = extractUsername(token);
        
        // Token is valid only if username matches AND token is not expired
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * Validates JWT token without checking username
     * 
     * Performs basic token validation:
     * 1. Token can be parsed (valid structure)
     * 2. Signature is valid (not tampered)
     * 3. Token hasn't expired
     * 
     * Used by API Gateway where we don't have the username readily available.
     * 
     * @param token JWT token to validate
     * @return true if token is valid and not expired, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            log.info("Gateway JwtService: Validating token: {}...", token.substring(0, Math.min(20, token.length())));
            
            // Check if token is expired
            // extractAllClaims() will throw exception if token is invalid/tampered
            boolean expired = isTokenExpired(token);
            log.info("Gateway JwtService: Token expired: {}", expired);
            
            // Token is valid if NOT expired
            return !expired;
        } catch (Exception e) {
            // Any exception during validation means token is invalid
            log.error("Gateway JwtService: Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the user ID from the JWT token
     * 
     * The userId is a custom claim added during token generation.
     * It represents the user's unique database ID.
     * 
     * @param token JWT token string
     * @return User ID extracted from token claims
     */
    public Long extractUserId(String token) {
        // Parse all claims and extract the userId custom claim
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extracts the phone number from the JWT token
     * 
     * The phoneNumber is a custom claim added during token generation.
     * Used for SMS-based authentication in ChitChat.
     * 
     * @param token JWT token string
     * @return Phone number extracted from token claims
     */
    public String extractPhoneNumber(String token) {
        // Parse all claims and extract the phoneNumber custom claim
        Claims claims = extractAllClaims(token);
        return claims.get("phoneNumber", String.class);
    }
}
