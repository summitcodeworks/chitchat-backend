package com.chitchat.gateway.exception;

import com.chitchat.shared.exception.ChitChatException;
import com.chitchat.shared.exception.ErrorResponse;
import com.chitchat.shared.exception.ErrorResponseBuilder;
import com.chitchat.shared.exception.ErrorConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

/**
 * WebFlux-compatible exception handler for the API Gateway
 * 
 * This handler works with Spring WebFlux reactive stack instead of traditional
 * Spring MVC servlet stack. It handles exceptions from the reactive gateway.
 */
@Slf4j
@RestControllerAdvice
public class WebFluxExceptionHandler {

    @Value("${spring.application.name:chitchat-api-gateway}")
    private String serviceName;

    @ExceptionHandler(ChitChatException.class)
    public ResponseEntity<ErrorResponse> handleChitChatException(ChitChatException ex, ServerWebExchange exchange) {
        String traceId = generateTraceId();
        
        logErrorDetails(traceId, "CHITCHAT_EXCEPTION", ex.getMessage(), ex, exchange, ex.getHttpStatus().value());

        ErrorResponse errorResponse = ErrorResponseBuilder
                .fromChitChatException(ex, null)  // No WebRequest in WebFlux
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, ServerWebExchange exchange) {
        String traceId = generateTraceId();

        logErrorDetails(traceId, "INTERNAL_SERVER_ERROR", "Unexpected error: " + ex.getMessage(), ex, exchange, HttpStatus.INTERNAL_SERVER_ERROR.value());

        ErrorResponse errorResponse = ErrorResponseBuilder
                .internalError("Please contact support with trace ID: " + traceId, null)  // No WebRequest in WebFlux
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logErrorDetails(String traceId, String errorCode, String errorMessage,
                                Throwable throwable, ServerWebExchange exchange, int httpStatus) {
        try {
            String endpoint = exchange.getRequest().getPath().value();
            String httpMethod = exchange.getRequest().getMethod().name();
            String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
            String clientIp = exchange.getRequest().getRemoteAddress() != null ? 
                             exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";

            log.error("ERROR_DETAILS: TraceId={}, Service={}, ErrorCode={}, HttpStatus={}, " +
                     "Endpoint={}, Method={}, ClientIP={}, UserAgent={}, " +
                     "Message={}, Exception={}",
                     traceId, serviceName, errorCode, httpStatus,
                     endpoint, httpMethod, clientIp, userAgent,
                     errorMessage, throwable != null ? throwable.getClass().getSimpleName() : "N/A");

            if (throwable != null) {
                log.error("STACK_TRACE [TraceId: {}]: ", traceId, throwable);
            }

        } catch (Exception e) {
            log.error("Failed to log error details for trace ID {}: {}", traceId, e.getMessage());
            log.error("Original error [TraceId: {}]: {}", traceId, errorMessage, throwable);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
