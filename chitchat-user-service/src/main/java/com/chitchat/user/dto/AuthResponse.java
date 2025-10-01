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
     * Type of token (always "Bearer" for JWT)
     * 
     * Indicates the token scheme.
     * Client should use: "Bearer " + accessToken
     */
    private String tokenType;
    
    /**
     * Token expiration time in seconds
     * 
     * Example: 86400 = 24 hours
     * 
     * Client should:
     * - Track when token expires
     * - Request new token before expiration
     * - Handle token refresh or re-login
     */
    private Long expiresIn;
    
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