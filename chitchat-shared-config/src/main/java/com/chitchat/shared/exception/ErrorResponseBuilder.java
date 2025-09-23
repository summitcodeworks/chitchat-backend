package com.chitchat.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builder utility for creating standardized error responses
 * Provides consistent error response creation across the application
 */
@Slf4j
public class ErrorResponseBuilder {
    
    private String traceId;
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String errorCode;
    private String details;
    private String path;
    
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }
    
    public ErrorResponseBuilder traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
    
    public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public ErrorResponseBuilder status(HttpStatus httpStatus) {
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        return this;
    }
    
    public ErrorResponseBuilder status(int status, String error) {
        this.status = status;
        this.error = error;
        return this;
    }
    
    public ErrorResponseBuilder message(String message) {
        this.message = message;
        return this;
    }
    
    public ErrorResponseBuilder errorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }
    
    public ErrorResponseBuilder details(String details) {
        this.details = details;
        return this;
    }
    
    public ErrorResponseBuilder path(String path) {
        this.path = path;
        return this;
    }
    
    public ErrorResponseBuilder fromWebRequest(WebRequest request) {
        this.path = request.getDescription(false).replace("uri=", "");
        return this;
    }
    
    public ErrorResponse build() {
        // Set defaults if not provided
        if (this.traceId == null) {
            this.traceId = generateTraceId();
        }
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
        
        return ErrorResponse.builder()
                .traceId(this.traceId)
                .timestamp(this.timestamp)
                .status(this.status)
                .error(this.error)
                .message(this.message)
                .errorCode(this.errorCode)
                .details(this.details)
                .path(this.path)
                .build();
    }
    
    /**
     * Quick builder for common error scenarios
     */
    public static ErrorResponseBuilder fromErrorCode(String errorCode, WebRequest request) {
        HttpStatus httpStatus = ErrorConstants.getHttpStatus(errorCode);
        String defaultMessage = ErrorConstants.getDefaultMessage(errorCode);
        
        return builder()
                .errorCode(errorCode)
                .status(httpStatus)
                .message(defaultMessage)
                .fromWebRequest(request);
    }
    
    /**
     * Quick builder for ChitChatException
     */
    public static ErrorResponseBuilder fromChitChatException(ChitChatException ex, WebRequest request) {
        return builder()
                .errorCode(ex.getErrorCode())
                .status(ex.getHttpStatus())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .fromWebRequest(request);
    }
    
    /**
     * Quick builder for validation errors
     */
    public static ErrorResponseBuilder validationError(String details, WebRequest request) {
        return builder()
                .errorCode(ErrorConstants.VALIDATION_ERROR)
                .status(HttpStatus.BAD_REQUEST)
                .message("Validation failed")
                .details(details)
                .fromWebRequest(request);
    }
    
    /**
     * Quick builder for unauthorized errors
     */
    public static ErrorResponseBuilder unauthorized(String message, WebRequest request) {
        return builder()
                .errorCode(ErrorConstants.UNAUTHORIZED)
                .status(HttpStatus.UNAUTHORIZED)
                .message(message)
                .fromWebRequest(request);
    }
    
    /**
     * Quick builder for forbidden errors
     */
    public static ErrorResponseBuilder forbidden(String message, WebRequest request) {
        return builder()
                .errorCode(ErrorConstants.ACCESS_DENIED)
                .status(HttpStatus.FORBIDDEN)
                .message(message)
                .fromWebRequest(request);
    }
    
    /**
     * Quick builder for not found errors
     */
    public static ErrorResponseBuilder notFound(String resource, WebRequest request) {
        return builder()
                .errorCode(ErrorConstants.RESOURCE_NOT_FOUND)
                .status(HttpStatus.NOT_FOUND)
                .message(resource + " not found")
                .fromWebRequest(request);
    }
    
    /**
     * Quick builder for conflict errors
     */
    public static ErrorResponseBuilder conflict(String message, WebRequest request) {
        return builder()
                .errorCode(ErrorConstants.RESOURCE_CONFLICT)
                .status(HttpStatus.CONFLICT)
                .message(message)
                .fromWebRequest(request);
    }
    
    /**
     * Quick builder for internal server errors
     */
    public static ErrorResponseBuilder internalError(String details, WebRequest request) {
        return builder()
                .errorCode(ErrorConstants.INTERNAL_ERROR)
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("An unexpected error occurred")
                .details(details)
                .fromWebRequest(request);
    }
    
    private static String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
