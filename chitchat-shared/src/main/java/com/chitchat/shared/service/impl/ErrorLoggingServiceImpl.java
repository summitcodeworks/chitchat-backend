package com.chitchat.shared.service.impl;

import com.chitchat.shared.entity.ErrorLog;
import com.chitchat.shared.repository.ErrorLogRepository;
import com.chitchat.shared.service.ErrorLoggingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ErrorLoggingService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorLoggingServiceImpl implements ErrorLoggingService {

    private final ErrorLogRepository errorLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String environment;

    @Override
    @Transactional
    public String logError(String serviceName,
                          String endpoint,
                          String httpMethod,
                          Integer httpStatus,
                          String errorCode,
                          String errorMessage,
                          Throwable throwable,
                          HttpServletRequest request,
                          Long userId,
                          String userPhone,
                          Map<String, Object> additionalContext) {

        String traceId = generateTraceId();

        try {
            ErrorLog.ErrorLogBuilder builder = ErrorLog.builder()
                    .traceId(traceId)
                    .serviceName(serviceName != null ? serviceName : applicationName)
                    .endpoint(endpoint)
                    .httpMethod(httpMethod)
                    .httpStatus(httpStatus)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .userId(userId)
                    .userPhone(userPhone)
                    .environment(environment)
                    .threadName(Thread.currentThread().getName());

            // Add stack trace if throwable is provided
            if (throwable != null) {
                builder.stackTrace(getStackTrace(throwable));
                builder.className(throwable.getStackTrace().length > 0 ? throwable.getStackTrace()[0].getClassName() : null);
                builder.methodName(throwable.getStackTrace().length > 0 ? throwable.getStackTrace()[0].getMethodName() : null);
                builder.lineNumber(throwable.getStackTrace().length > 0 ? throwable.getStackTrace()[0].getLineNumber() : null);
            }

            // Extract information from HTTP request
            if (request != null) {
                builder.clientIp(getClientIpAddress(request))
                       .userAgent(request.getHeader("User-Agent"))
                       .sessionId(request.getSession(false) != null ? request.getSession().getId() : null)
                       .requestHeaders(extractHeaders(request))
                       .requestBody(extractRequestBody(request))
                       .requestParams(extractRequestParams(request));
            }

            // Add server information
            try {
                builder.serverName(InetAddress.getLocalHost().getHostName());
            } catch (Exception e) {
                builder.serverName("unknown");
            }

            // Add additional context
            if (additionalContext != null && !additionalContext.isEmpty()) {
                try {
                    builder.additionalContext(objectMapper.writeValueAsString(additionalContext));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize additional context for trace ID {}: {}", traceId, e.getMessage());
                }
            }

            ErrorLog errorLog = builder.build();
            errorLogRepository.save(errorLog);

            log.info("Error logged with trace ID: {} for service: {}", traceId, serviceName);
            return traceId;

        } catch (Exception e) {
            log.error("Failed to log error to database for trace ID {}: {}", traceId, e.getMessage(), e);
            return traceId; // Still return trace ID even if DB logging fails
        }
    }

    @Override
    public String logError(String serviceName, String errorCode, String errorMessage, Throwable throwable) {
        return logError(serviceName, null, null, 500, errorCode, errorMessage, throwable, null, null, null, null);
    }

    @Override
    public String logError(String serviceName, HttpServletRequest request, Throwable throwable) {
        String endpoint = request != null ? request.getRequestURI() : null;
        String httpMethod = request != null ? request.getMethod() : null;
        String errorCode = throwable != null ? throwable.getClass().getSimpleName() : "UNKNOWN_ERROR";
        String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error occurred";

        return logError(serviceName, endpoint, httpMethod, 500, errorCode, errorMessage, throwable, request, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ErrorLog> getErrorByTraceId(String traceId) {
        return errorLogRepository.findByTraceId(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ErrorLog> getErrorsByService(String serviceName) {
        return errorLogRepository.findByServiceNameOrderByCreatedAtDesc(serviceName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ErrorLog> getUnresolvedErrors() {
        return errorLogRepository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ErrorLog> getErrors(Pageable pageable) {
        return errorLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ErrorLog> searchErrors(String serviceName, String errorCode, Integer httpStatus, Boolean resolved, Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return errorLogRepository.findByCriteria(serviceName, errorCode, httpStatus, resolved, userId, startDate, endDate, pageable);
    }

    @Override
    @Transactional
    public void resolveError(String traceId, String resolvedBy, String resolutionNotes) {
        Optional<ErrorLog> errorLogOpt = errorLogRepository.findByTraceId(traceId);
        if (errorLogOpt.isPresent()) {
            ErrorLog errorLog = errorLogOpt.get();
            errorLog.setResolved(true);
            errorLog.setResolvedBy(resolvedBy);
            errorLog.setResolutionNotes(resolutionNotes);
            errorLog.setResolvedAt(LocalDateTime.now());
            errorLogRepository.save(errorLog);
            log.info("Error {} marked as resolved by {}", traceId, resolvedBy);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getErrorStatistics(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = errorLogRepository.getErrorStatsByTimeRange(start, end);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getServiceErrorStatistics(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = errorLogRepository.getServiceErrorStatsByTimeRange(start, end);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getErrorCount(String serviceName, LocalDateTime start, LocalDateTime end) {
        return errorLogRepository.countByServiceAndTimeRange(serviceName, start, end);
    }

    @Override
    @Transactional
    public void cleanupOldLogs(LocalDateTime cutoffDate) {
        try {
            errorLogRepository.deleteByCreatedAtBefore(cutoffDate);
            log.info("Cleaned up error logs older than {}", cutoffDate);
        } catch (Exception e) {
            log.error("Failed to cleanup old error logs: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ErrorLog> getUserErrors(Long userId, int limit) {
        List<ErrorLog> userErrors = errorLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return userErrors.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ErrorLog> getErrorsByEndpoint(String endpointPattern) {
        return errorLogRepository.findByEndpointContaining(endpointPattern);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean errorExists(String traceId) {
        return errorLogRepository.findByTraceId(traceId).isPresent();
    }

    @Override
    @Transactional
    public void updateErrorLog(String traceId, Map<String, Object> updates) {
        Optional<ErrorLog> errorLogOpt = errorLogRepository.findByTraceId(traceId);
        if (errorLogOpt.isPresent()) {
            ErrorLog errorLog = errorLogOpt.get();

            // Update fields based on the updates map
            updates.forEach((key, value) -> {
                switch (key.toLowerCase()) {
                    case "resolved":
                        errorLog.setResolved((Boolean) value);
                        break;
                    case "resolutionnotes":
                        errorLog.setResolutionNotes((String) value);
                        break;
                    case "resolvedby":
                        errorLog.setResolvedBy((String) value);
                        break;
                    case "additionalcontext":
                        try {
                            errorLog.setAdditionalContext(objectMapper.writeValueAsString(value));
                        } catch (JsonProcessingException e) {
                            log.warn("Failed to serialize additional context update for trace ID {}: {}", traceId, e.getMessage());
                        }
                        break;
                }
            });

            errorLogRepository.save(errorLog);
            log.info("Updated error log for trace ID: {}", traceId);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String extractHeaders(HttpServletRequest request) {
        try {
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);

                // Mask sensitive headers
                if (headerName.toLowerCase().contains("authorization") ||
                    headerName.toLowerCase().contains("cookie") ||
                    headerName.toLowerCase().contains("password")) {
                    headerValue = "***MASKED***";
                }

                headers.put(headerName, headerValue);
            }
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            log.warn("Failed to extract request headers: {}", e.getMessage());
            return null;
        }
    }

    private String extractRequestBody(HttpServletRequest request) {
        // Note: Request body extraction is complex and may require custom wrapper
        // For now, return null. Can be implemented with HttpServletRequestWrapper
        return null;
    }

    private String extractRequestParams(HttpServletRequest request) {
        try {
            Map<String, String[]> paramMap = request.getParameterMap();
            if (paramMap.isEmpty()) {
                return null;
            }

            Map<String, Object> params = new HashMap<>();
            paramMap.forEach((key, values) -> {
                if (values.length == 1) {
                    // Mask sensitive parameters
                    if (key.toLowerCase().contains("password") ||
                        key.toLowerCase().contains("token") ||
                        key.toLowerCase().contains("secret")) {
                        params.put(key, "***MASKED***");
                    } else {
                        params.put(key, values[0]);
                    }
                } else {
                    params.put(key, Arrays.asList(values));
                }
            });
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to extract request parameters: {}", e.getMessage());
            return null;
        }
    }
}