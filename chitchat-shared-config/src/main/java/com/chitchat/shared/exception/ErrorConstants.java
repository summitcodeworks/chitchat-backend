package com.chitchat.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Centralized error constants for ChitChat application
 * Provides consistent error codes, messages, and HTTP status mappings
 */
public class ErrorConstants {
    
    // Authentication & Authorization Errors
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String FIREBASE_AUTH_FAILED = "FIREBASE_AUTH_FAILED";
    public static final String ACCOUNT_DEACTIVATED = "ACCOUNT_DEACTIVATED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    
    // User Management Errors
    public static final String USER_EXISTS = "USER_EXISTS";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ADMIN_NOT_FOUND = "ADMIN_NOT_FOUND";
    public static final String USER_ALREADY_BLOCKED = "USER_ALREADY_BLOCKED";
    public static final String USERNAME_EXISTS = "USERNAME_EXISTS";
    public static final String EMAIL_EXISTS = "EMAIL_EXISTS";
    
    // Validation Errors
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MISSING_PARAMETER = "MISSING_PARAMETER";
    public static final String INVALID_PARAMETER_TYPE = "INVALID_PARAMETER_TYPE";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";
    
    // Resource Errors
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String RESOURCE_ALREADY_EXISTS = "RESOURCE_ALREADY_EXISTS";
    public static final String RESOURCE_CONFLICT = "RESOURCE_CONFLICT";
    
    // Database Errors
    public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String CONNECTION_ERROR = "CONNECTION_ERROR";
    
    // Business Logic Errors
    public static final String ACTIVE_CALL_EXISTS = "ACTIVE_CALL_EXISTS";
    public static final String INVALID_CALL_STATUS = "INVALID_CALL_STATUS";
    public static final String STATUS_EXPIRED = "STATUS_EXPIRED";
    public static final String MEMBER_EXISTS = "MEMBER_EXISTS";
    public static final String ADMIN_CANNOT_LEAVE = "ADMIN_CANNOT_LEAVE";
    public static final String OPERATION_NOT_ALLOWED = "OPERATION_NOT_ALLOWED";
    
    // System Errors
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String TIMEOUT_ERROR = "TIMEOUT_ERROR";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    
    // File & Media Errors
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
    public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
    
    /**
     * Get HTTP status for error code
     */
    public static HttpStatus getHttpStatus(String errorCode) {
        switch (errorCode) {
            case INVALID_CREDENTIALS:
            case FIREBASE_AUTH_FAILED:
            case TOKEN_EXPIRED:
            case INVALID_TOKEN:
                return HttpStatus.UNAUTHORIZED;
                
            case ACCOUNT_DEACTIVATED:
            case UNAUTHORIZED:
            case ACCESS_DENIED:
                return HttpStatus.FORBIDDEN;
                
            case VALIDATION_ERROR:
            case MISSING_PARAMETER:
            case INVALID_PARAMETER_TYPE:
            case INVALID_FORMAT:
                return HttpStatus.BAD_REQUEST;
                
            case USER_EXISTS:
            case USERNAME_EXISTS:
            case EMAIL_EXISTS:
            case RESOURCE_ALREADY_EXISTS:
            case ACTIVE_CALL_EXISTS:
            case MEMBER_EXISTS:
            case DATA_INTEGRITY_VIOLATION:
                return HttpStatus.CONFLICT;
                
            case USER_NOT_FOUND:
            case ADMIN_NOT_FOUND:
            case RESOURCE_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
                
            case STATUS_EXPIRED:
                return HttpStatus.GONE;
                
            case SERVICE_UNAVAILABLE:
                return HttpStatus.SERVICE_UNAVAILABLE;
                
            case RATE_LIMIT_EXCEEDED:
                return HttpStatus.TOO_MANY_REQUESTS;
                
            case INTERNAL_ERROR:
            case DATABASE_ERROR:
            case CONNECTION_ERROR:
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
    
    /**
     * Get default error message for error code
     */
    public static String getDefaultMessage(String errorCode) {
        switch (errorCode) {
            case INVALID_CREDENTIALS:
                return "Invalid credentials provided";
            case FIREBASE_AUTH_FAILED:
                return "Firebase authentication failed";
            case ACCOUNT_DEACTIVATED:
                return "Account has been deactivated";
            case UNAUTHORIZED:
                return "Unauthorized access";
            case ACCESS_DENIED:
                return "Access denied";
            case TOKEN_EXPIRED:
                return "Token has expired";
            case INVALID_TOKEN:
                return "Invalid token provided";
            case USER_EXISTS:
                return "User already exists";
            case USER_NOT_FOUND:
                return "User not found";
            case ADMIN_NOT_FOUND:
                return "Admin not found";
            case USER_ALREADY_BLOCKED:
                return "User is already blocked";
            case USERNAME_EXISTS:
                return "Username already exists";
            case EMAIL_EXISTS:
                return "Email already exists";
            case VALIDATION_ERROR:
                return "Validation failed";
            case MISSING_PARAMETER:
                return "Missing required parameter";
            case INVALID_PARAMETER_TYPE:
                return "Invalid parameter type";
            case INVALID_FORMAT:
                return "Invalid format";
            case RESOURCE_NOT_FOUND:
                return "Resource not found";
            case RESOURCE_ALREADY_EXISTS:
                return "Resource already exists";
            case DATA_INTEGRITY_VIOLATION:
                return "Data integrity constraint violation";
            case ACTIVE_CALL_EXISTS:
                return "User already has an active call";
            case INVALID_CALL_STATUS:
                return "Invalid call status";
            case STATUS_EXPIRED:
                return "Status has expired";
            case MEMBER_EXISTS:
                return "User is already a member";
            case ADMIN_CANNOT_LEAVE:
                return "Admin cannot leave group. Transfer admin role first";
            case OPERATION_NOT_ALLOWED:
                return "Operation not allowed";
            case INTERNAL_ERROR:
                return "An unexpected error occurred";
            case SERVICE_UNAVAILABLE:
                return "Service is currently unavailable";
            case TIMEOUT_ERROR:
                return "Request timeout";
            case RATE_LIMIT_EXCEEDED:
                return "Rate limit exceeded";
            case FILE_TOO_LARGE:
                return "File size exceeds maximum limit";
            case INVALID_FILE_TYPE:
                return "Invalid file type";
            case UPLOAD_FAILED:
                return "File upload failed";
            default:
                return "An error occurred";
        }
    }
}
