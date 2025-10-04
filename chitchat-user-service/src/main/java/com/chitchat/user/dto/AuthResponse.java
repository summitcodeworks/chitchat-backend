package com.chitchat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication response
 * 
 * Returned after successful authentication (OTP verification or Firebase login).
 * Contains all information needed for client to establish authenticated session.
 * 
 * Response Structure:
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 86400,
 *   "user": { ... user details ... },
 *   "message": "Login successful"
 * }
 * 
 * Client Usage:
 * 1. Store accessToken securely (encrypted storage, not localStorage)
 * 2. Include in all API requests: Authorization: Bearer {accessToken}
 * 3. Refresh token before expiresIn seconds
 * 4. Clear token on logout
 * 
 * Security Notes:
 * - Token should be stored securely on client
 * - expiresIn allows proactive refresh
 * - tokenType is always "Bearer" for JWT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    /**
     * JWT access token for authentication
     * 
     * This token must be included in Authorization header for all protected API calls.
     * Format: Long base64-encoded string
     * 
     * Usage in requests:
     * Authorization: Bearer {accessToken}
     */
    private String accessToken;
    
    /**
     * Refresh token for obtaining new access tokens
     * 
     * This token is used to get a new access token when the current one expires.
     * It has a longer expiration time (typically 30 days).
     * 
     * Usage:
     * - Store securely on client
     * - Use to refresh access token via /api/users/refresh-token endpoint
     * - Rotate after each use for better security
     */
    private String refreshToken;
    
    /**
     * Type of token (always "Bearer" for JWT)
     * 
     * Indicates the token scheme.
     * Client should use: "Bearer " + accessToken
     */
    private String tokenType;
    
    /**
     * Access token expiration time in seconds
     * 
     * Example: 3600 = 1 hour
     * 
     * Client should:
     * - Track when token expires
     * - Request new token before expiration using refresh token
     * - Handle token refresh or re-login
     */
    private Long expiresIn;
    
    /**
     * Refresh token expiration time in seconds
     * 
     * Example: 2592000 = 30 days
     * 
     * Client should track when refresh token expires.
     * When it expires, user must re-authenticate.
     */
    private Long refreshExpiresIn;
    
    /**
     * User profile information
     * 
     * Contains authenticated user's details:
     * - User ID
     * - Phone number
     * - Name
     * - Avatar
     * - etc.
     * 
     * Allows client to display user info without additional API call.
     */
    private UserResponse user;
    
    /**
     * Human-readable status message
     * 
     * Examples:
     * - "Login successful"
     * - "User registered successfully"
     * - "Welcome back!"
     * 
     * Can be displayed to user in UI.
     */
    private String message;
}