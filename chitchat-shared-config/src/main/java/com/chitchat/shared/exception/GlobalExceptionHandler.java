package com.chitchat.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
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

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @ExceptionHandler(ChitChatException.class)
    public ResponseEntity<ErrorResponse> handleChitChatException(ChitChatException ex, WebRequest request) {
        String traceId = generateTraceId();

        // Enhanced logging with context
        logErrorDetails(traceId, "CHITCHAT_EXCEPTION", ex.getMessage(), ex, request, ex.getHttpStatus().value());

        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromChitChatException(ex, request)
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = generateTraceId();

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Enhanced logging with validation details
        logErrorDetails(traceId, "VALIDATION_ERROR", "Validation failed: " + errors.toString(), ex, request, HttpStatus.BAD_REQUEST.value());

        ErrorResponse errorResponse = ErrorResponseBuilder
                .validationError(errors.toString(), request)
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String traceId = generateTraceId();

        // Enhanced logging with database context
        logErrorDetails(traceId, "DATA_INTEGRITY_VIOLATION", "Database constraint violation: " + ex.getMessage(), ex, request, HttpStatus.CONFLICT.value());

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

        // Enhanced logging for resource not found
        logErrorDetails(traceId, "RESOURCE_NOT_FOUND", "Resource not found: " + ex.getMessage(), ex, request, HttpStatus.NOT_FOUND.value());

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

        // Enhanced logging with parameter details
        logErrorDetails(traceId, "MISSING_PARAMETER", "Missing required parameter: " + ex.getParameterName(), ex, request, HttpStatus.BAD_REQUEST.value());

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

        // Enhanced logging with type details
        logErrorDetails(traceId, "INVALID_PARAMETER_TYPE", "Type mismatch for parameter '" + ex.getName() + "': expected " +
                      (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"), ex, request, HttpStatus.BAD_REQUEST.value());

        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromErrorCode(ErrorConstants.INVALID_PARAMETER_TYPE, request)
                .message("Invalid parameter type for: " + ex.getName())
                .details("Parameter '" + ex.getName() + "' must be of type " +
                        (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"))
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String traceId = generateTraceId();

        // Enhanced logging for unexpected errors
        logErrorDetails(traceId, "INTERNAL_SERVER_ERROR", "Unexpected error: " + ex.getMessage(), ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());

        ErrorResponse errorResponse = ErrorResponseBuilder
                .internalError("Please contact support with trace ID: " + traceId, request)
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logErrorDetails(String traceId, String errorCode, String errorMessage,
                                Throwable throwable, WebRequest request, int httpStatus) {
        try {
            // Log to standard output with enhanced details
            String endpoint = null;
            String httpMethod = null;
            String userAgent = null;
            String clientIp = null;
            String userId = null;

            // Extract request details if available
            if (request instanceof ServletWebRequest) {
                HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
                endpoint = httpRequest.getRequestURI();
                httpMethod = httpRequest.getMethod();
                userAgent = httpRequest.getHeader("User-Agent");
                clientIp = getClientIpAddress(httpRequest);

                // Try to extract user ID from security context
                try {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                        userId = auth.getName();
                    }
                } catch (Exception e) {
                    // Ignore auth extraction errors
                }
            }

            // Enhanced structured logging
            log.error("ERROR_DETAILS: TraceId={}, Service={}, ErrorCode={}, HttpStatus={}, " +
                     "Endpoint={}, Method={}, UserId={}, ClientIP={}, UserAgent={}, " +
                     "Message={}, Exception={}",
                     traceId, serviceName, errorCode, httpStatus,
                     endpoint, httpMethod, userId, clientIp, userAgent,
                     errorMessage, throwable != null ? throwable.getClass().getSimpleName() : "N/A");

            // Log stack trace if available
            if (throwable != null) {
                log.error("STACK_TRACE [TraceId: {}]: ", traceId, throwable);
            }

        } catch (Exception e) {
            // Fallback logging if error logging fails
            log.error("Failed to log error details for trace ID {}: {}", traceId, e.getMessage());
            log.error("Original error [TraceId: {}]: {}", traceId, errorMessage, throwable);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_CLIENT_IP"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
