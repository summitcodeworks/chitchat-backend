package com.chitchat.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception class for ChitChat application
 * Provides structured error handling across all microservices
 */
@Getter
public class ChitChatException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String details;
    
    public ChitChatException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public ChitChatException(String message, HttpStatus httpStatus, String errorCode, String details) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public ChitChatException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = null;
    }
}
