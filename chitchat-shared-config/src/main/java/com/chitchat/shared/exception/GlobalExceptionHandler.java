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
 * 
 * This class provides centralized exception handling across all microservices using
 * Spring's @RestControllerAdvice annotation.
 * 
 * Key Features:
 * - Converts all exceptions to consistent ErrorResponse format
 * - Generates unique trace IDs for error tracking
 * - Logs comprehensive error details for debugging
 * - Maps exceptions to appropriate HTTP status codes
 * - Handles both application exceptions and framework exceptions
 * 
 * Exception Types Handled:
 * - ChitChatException: Custom application exceptions
 * - MethodArgumentNotValidException: Bean validation failures
 * - DataIntegrityViolationException: Database constraint violations
 * - EmptyResultDataAccessException: Resource not found errors
 * - MissingServletRequestParameterException: Missing required parameters
 * - MethodArgumentTypeMismatchException: Type conversion failures
 * - Exception: All other unexpected exceptions (catch-all)
 * 
 * All errors are logged with full context including:
 * - Trace ID (for correlation across services)
 * - Service name
 * - HTTP endpoint and method
 * - User information (if authenticated)
 * - Client IP and user agent
 * - Complete stack trace
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Service name from configuration
     * Used in error logging to identify which microservice generated the error
     */
    @Value("${spring.application.name:unknown}")
    private String serviceName;

    /**
     * Handles ChitChatException - our custom application exception
     * 
     * This is the primary exception type thrown by our application code.
     * It already contains HTTP status, error code, and message.
     * 
     * Process:
     * 1. Generate unique trace ID for this error
     * 2. Log comprehensive error details
     * 3. Build structured error response
     * 4. Return response with appropriate HTTP status
     * 
     * @param ex The ChitChatException that was thrown
     * @param request Web request context
     * @return ResponseEntity with ErrorResponse body and HTTP status from exception
     */
    @ExceptionHandler(ChitChatException.class)
    public ResponseEntity<ErrorResponse> handleChitChatException(ChitChatException ex, WebRequest request) {
        // Generate unique 8-character trace ID for error tracking across services
        String traceId = generateTraceId();

        // Log full error details for debugging and monitoring
        logErrorDetails(traceId, "CHITCHAT_EXCEPTION", ex.getMessage(), ex, request, ex.getHttpStatus().value());

        // Build structured error response using builder pattern
        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromChitChatException(ex, request)  // Extract info from exception
                .traceId(traceId)                    // Add trace ID for tracking
                .build();

        // Return response with HTTP status from the exception
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handles validation errors from @Valid annotation
     * 
     * Triggered when request body validation fails (e.g., @NotNull, @Size, @Email).
     * Collects all validation errors and returns them in a structured format.
     * 
     * Process:
     * 1. Extract all validation errors from binding result
     * 2. Map field names to their error messages
     * 3. Log the validation failures
     * 4. Build error response with all validation errors
     * 
     * Example: If a request has invalid email and missing required field,
     * both errors are returned together.
     * 
     * @param ex MethodArgumentNotValidException containing validation errors
     * @param request Web request context
     * @return ResponseEntity with 400 Bad Request and validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = generateTraceId();

        // Collect all validation errors into a map
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            // Extract field name (e.g., "email", "phoneNumber")
            String fieldName = ((FieldError) error).getField();
            // Extract validation message (e.g., "must not be null", "invalid format")
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Log all validation failures for debugging
        logErrorDetails(traceId, "VALIDATION_ERROR", "Validation failed: " + errors.toString(), ex, request, HttpStatus.BAD_REQUEST.value());

        // Build error response with all validation errors
        ErrorResponse errorResponse = ErrorResponseBuilder
                .validationError(errors.toString(), request)
                .traceId(traceId)
                .build();

        // Return 400 Bad Request for validation failures
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

    /**
     * Handles all other unhandled exceptions (catch-all handler)
     * 
     * This is the fallback handler for any exception not caught by specific handlers.
     * Used for unexpected errors like NullPointerException, RuntimeException, etc.
     * 
     * Critical Behavior:
     * - Never expose internal error details to clients (security concern)
     * - Log full stack trace for developers
     * - Provide trace ID so users can report the error
     * - Return generic error message to avoid information leakage
     * 
     * Process:
     * 1. Generate trace ID for error tracking
     * 2. Log complete error details with stack trace
     * 3. Return generic error message with trace ID
     * 4. Always return 500 Internal Server Error
     * 
     * @param ex Any unhandled exception
     * @param request Web request context
     * @return ResponseEntity with 500 Internal Server Error and trace ID
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String traceId = generateTraceId();

        // Log full error details internally (never exposed to client)
        logErrorDetails(traceId, "INTERNAL_SERVER_ERROR", "Unexpected error: " + ex.getMessage(), ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());

        // Build generic error response without exposing internals
        // Only provide trace ID for support/debugging purposes
        ErrorResponse errorResponse = ErrorResponseBuilder
                .internalError("Please contact support with trace ID: " + traceId, request)
                .traceId(traceId)
                .build();

        // Always return 500 for unexpected errors
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Logs comprehensive error details for monitoring and debugging
     * 
     * Creates a structured log entry with all available context about the error.
     * This is critical for:
     * - Debugging production issues
     * - Monitoring error rates
     * - Security incident analysis
     * - Performance troubleshooting
     * 
     * Logged Information:
     * - Trace ID (unique identifier for this error instance)
     * - Service name (which microservice encountered the error)
     * - Error code (application-specific error identifier)
     * - HTTP status code
     * - Endpoint and HTTP method
     * - User ID (if authenticated)
     * - Client IP address (for security analysis)
     * - User agent (browser/app information)
     * - Error message
     * - Full stack trace
     * 
     * Note: This method never throws exceptions to avoid masking the original error.
     * 
     * @param traceId Unique identifier for this error instance
     * @param errorCode Application error code
     * @param errorMessage Human-readable error message
     * @param throwable The exception that occurred (can be null)
     * @param request Web request context
     * @param httpStatus HTTP status code to be returned
     */
    private void logErrorDetails(String traceId, String errorCode, String errorMessage,
                                Throwable throwable, WebRequest request, int httpStatus) {
        try {
            // Initialize variables for request context
            String endpoint = null;
            String httpMethod = null;
            String userAgent = null;
            String clientIp = null;
            String userId = null;

            // Extract HTTP request details if available
            if (request instanceof ServletWebRequest) {
                HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
                endpoint = httpRequest.getRequestURI();        // e.g., "/api/users/123"
                httpMethod = httpRequest.getMethod();          // e.g., "GET", "POST"
                userAgent = httpRequest.getHeader("User-Agent"); // Browser/app info
                clientIp = getClientIpAddress(httpRequest);    // Real client IP (handles proxies)

                // Try to extract authenticated user information
                try {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    // Check if user is actually authenticated (not anonymous)
                    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                        userId = auth.getName();  // Username or user ID from JWT
                    }
                } catch (Exception e) {
                    // Silently ignore auth extraction errors - don't want to mask original error
                }
            }

            // Create structured log entry with all context
            // This format allows easy parsing by log aggregation tools (e.g., ELK, Splunk)
            log.error("ERROR_DETAILS: TraceId={}, Service={}, ErrorCode={}, HttpStatus={}, " +
                     "Endpoint={}, Method={}, UserId={}, ClientIP={}, UserAgent={}, " +
                     "Message={}, Exception={}",
                     traceId, serviceName, errorCode, httpStatus,
                     endpoint, httpMethod, userId, clientIp, userAgent,
                     errorMessage, throwable != null ? throwable.getClass().getSimpleName() : "N/A");

            // Log full stack trace for debugging (separate log entry for clarity)
            if (throwable != null) {
                log.error("STACK_TRACE [TraceId: {}]: ", traceId, throwable);
            }

        } catch (Exception e) {
            // Fallback logging if detailed logging fails
            // Never let logging errors hide the original error
            log.error("Failed to log error details for trace ID {}: {}", traceId, e.getMessage());
            log.error("Original error [TraceId: {}]: {}", traceId, errorMessage, throwable);
        }
    }

    /**
     * Extracts the real client IP address from the HTTP request
     * 
     * When the application is behind a proxy or load balancer, the direct
     * remote address is the proxy, not the actual client.
     * This method checks various proxy headers to find the real client IP.
     * 
     * Checked Headers (in order):
     * 1. X-Forwarded-For (standard proxy header)
     * 2. X-Real-IP (nginx proxy header)
     * 3. Proxy-Client-IP
     * 4. WL-Proxy-Client-IP (WebLogic)
     * 5. HTTP_X_FORWARDED_FOR
     * 6. HTTP_CLIENT_IP
     * 
     * If no proxy headers exist, returns the direct remote address.
     * 
     * @param request HTTP servlet request
     * @return Real client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // List of common proxy/load balancer headers that contain client IP
        String[] ipHeaders = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_CLIENT_IP"
        };

        // Check each header in order
        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            // Valid IP found if not null, not empty, and not the placeholder "unknown"
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
                // The first one is the original client
                return ip.split(",")[0].trim();
            }
        }

        // No proxy headers found, return direct remote address
        return request.getRemoteAddr();
    }

    /**
     * Generates a unique trace ID for error tracking
     * 
     * Creates a short (8-character) unique identifier using UUID.
     * This ID is:
     * - Included in error responses (users can provide it to support)
     * - Logged with all error details
     * - Used to correlate errors across microservices
     * - Easy to communicate (short and memorable)
     * 
     * Example: "a3f2b1c9"
     * 
     * @return 8-character hexadecimal trace ID
     */
    private String generateTraceId() {
        // Generate full UUID and take first 8 characters
        // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        // We take: xxxxxxxx (first 8 hex characters)
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
