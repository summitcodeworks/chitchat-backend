package com.chitchat.shared.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Standard error response format for ChitChat API
 */
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String errorCode;
    private String details;
    private String path;
}
