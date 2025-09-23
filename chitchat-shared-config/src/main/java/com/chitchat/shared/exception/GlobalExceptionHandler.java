package com.chitchat.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for all ChitChat microservices
 * Provides consistent error response format across the application
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChitChatException.class)
    public ResponseEntity<ErrorResponse> handleChitChatException(ChitChatException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("ChitChat Exception [TraceId: {}]: {}", traceId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromChitChatException(ex, request)
                .traceId(traceId)
                .build();
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Validation Exception [TraceId: {}]: {}", traceId, ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponseBuilder
                .validationError(errors.toString(), request)
                .traceId(traceId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Data integrity violation [TraceId: {}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromErrorCode(ErrorConstants.DATA_INTEGRITY_VIOLATION, request)
                .details("The operation violates database constraints")
                .traceId(traceId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleEmptyResultDataAccess(EmptyResultDataAccessException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Empty result data access [TraceId: {}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromErrorCode(ErrorConstants.RESOURCE_NOT_FOUND, request)
                .details("The requested resource does not exist")
                .traceId(traceId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Missing request parameter [TraceId: {}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromErrorCode(ErrorConstants.MISSING_PARAMETER, request)
                .message("Missing required parameter: " + ex.getParameterName())
                .details("Required parameter '" + ex.getParameterName() + "' is not present")
                .traceId(traceId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Method argument type mismatch [TraceId: {}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromErrorCode(ErrorConstants.INVALID_PARAMETER_TYPE, request)
                .message("Invalid parameter type for: " + ex.getName())
                .details("Parameter '" + ex.getName() + "' must be of type " + ex.getRequiredType().getSimpleName())
                .traceId(traceId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Unexpected error occurred [TraceId: {}]: {}", traceId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponseBuilder
                .internalError("Please contact support with trace ID: " + traceId, request)
                .traceId(traceId)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
