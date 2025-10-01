package com.chitchat.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception class for ChitChat application
 * 
 * Provides structured and consistent error handling across all microservices.
 * This runtime exception extends RuntimeException to allow unchecked exception handling.
 * 
 * Key Features:
 * - HTTP status code for appropriate REST API responses
 * - Error code for programmatic error identification
 * - Human-readable message for users/developers
 * - Optional details for additional context
 * - Support for exception chaining (cause)
 * 
 * Usage Examples:
 * 
 * Simple error:
 * throw new ChitChatException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
 * 
 * With additional details:
 * throw new ChitChatException("Validation failed", HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", 
 *                             "Phone number must be 10 digits");
 * 
 * With cause (wrapping another exception):
 * throw new ChitChatException("Database error", e, HttpStatus.INTERNAL_SERVER_ERROR, "DB_ERROR");
 * 
 * All fields are accessible via getters (Lombok @Getter) for error response building.
 */
@Getter
public class ChitChatException extends RuntimeException {
    
    /**
     * HTTP status code for the error response
     * Determines the HTTP response status (404, 400, 500, etc.)
     */
    private final HttpStatus httpStatus;
    
    /**
     * Application-specific error code
     * Used for programmatic error handling and i18n error messages
     * Examples: "USER_NOT_FOUND", "INVALID_OTP", "UNAUTHORIZED_ACCESS"
     */
    private final String errorCode;
    
    /**
     * Additional detailed information about the error
     * Provides context beyond the main message
     * Can be null if no additional details are needed
     */
    private final String details;
    
    /**
     * Constructor for simple error without details
     * 
     * @param message Human-readable error message
     * @param httpStatus HTTP status code for the response
     * @param errorCode Application error code
     */
    public ChitChatException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = null;
    }
    
    /**
     * Constructor for error with additional details
     * 
     * @param message Human-readable error message
     * @param httpStatus HTTP status code for the response
     * @param errorCode Application error code
     * @param details Additional context or information about the error
     */
    public ChitChatException(String message, HttpStatus httpStatus, String errorCode, String details) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }
    
    /**
     * Constructor for error with cause (exception chaining)
     * 
     * Used when wrapping another exception to preserve the stack trace.
     * 
     * @param message Human-readable error message
     * @param cause The underlying exception that caused this error
     * @param httpStatus HTTP status code for the response
     * @param errorCode Application error code
     */
    public ChitChatException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = null;
    }
}
