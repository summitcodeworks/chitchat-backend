package com.chitchat.shared.service;

import com.chitchat.shared.entity.ErrorLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for error logging and tracking
 */
public interface ErrorLoggingService {

    /**
     * Log an error with full context
     */
    String logError(String serviceName,
                   String endpoint,
                   String httpMethod,
                   Integer httpStatus,
                   String errorCode,
                   String errorMessage,
                   Throwable throwable,
                   HttpServletRequest request,
                   Long userId,
                   String userPhone,
                   Map<String, Object> additionalContext);

    /**
     * Log an error with minimal context
     */
    String logError(String serviceName,
                   String errorCode,
                   String errorMessage,
                   Throwable throwable);

    /**
     * Log an error from exception with auto-detection
     */
    String logError(String serviceName,
                   HttpServletRequest request,
                   Throwable throwable);

    /**
     * Get error log by trace ID
     */
    Optional<ErrorLog> getErrorByTraceId(String traceId);

    /**
     * Get error logs by service
     */
    List<ErrorLog> getErrorsByService(String serviceName);

    /**
     * Get unresolved errors
     */
    List<ErrorLog> getUnresolvedErrors();

    /**
     * Get error logs with pagination
     */
    Page<ErrorLog> getErrors(Pageable pageable);

    /**
     * Search errors by criteria
     */
    Page<ErrorLog> searchErrors(String serviceName,
                               String errorCode,
                               Integer httpStatus,
                               Boolean resolved,
                               Long userId,
                               LocalDateTime startDate,
                               LocalDateTime endDate,
                               Pageable pageable);

    /**
     * Mark error as resolved
     */
    void resolveError(String traceId, String resolvedBy, String resolutionNotes);

    /**
     * Get error statistics
     */
    Map<String, Long> getErrorStatistics(LocalDateTime start, LocalDateTime end);

    /**
     * Get service error statistics
     */
    Map<String, Long> getServiceErrorStatistics(LocalDateTime start, LocalDateTime end);

    /**
     * Get error count for a service in time range
     */
    Long getErrorCount(String serviceName, LocalDateTime start, LocalDateTime end);

    /**
     * Clean up old error logs
     */
    void cleanupOldLogs(LocalDateTime cutoffDate);

    /**
     * Get recent errors for a user
     */
    List<ErrorLog> getUserErrors(Long userId, int limit);

    /**
     * Get errors by endpoint pattern
     */
    List<ErrorLog> getErrorsByEndpoint(String endpointPattern);

    /**
     * Check if error exists by trace ID
     */
    boolean errorExists(String traceId);

    /**
     * Update error log with additional information
     */
    void updateErrorLog(String traceId, Map<String, Object> updates);
}