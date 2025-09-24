package com.chitchat.gateway.filter;

import com.chitchat.gateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global authentication filter for API Gateway
 * Handles JWT-based authentication for SMS-based login
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/users/send-otp",
            "/api/users/verify-otp",
            "/api/users/register",
            "/api/users/login",
            "/api/users/refresh-token",
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics"
    );

    // WebSocket endpoints that require special handling
    private static final List<String> WEBSOCKET_ENDPOINTS = Arrays.asList(
            "/ws/messages",
            "/ws/notifications",
            "/ws/calls"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        log.debug("Processing request: {} {}", method, path);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        // Extract token from Authorization header
        String token = extractToken(request);
        if (token == null) {
            log.warn("No token found in request to: {}", path);
            return handleUnauthorized(exchange, "No authentication token provided");
        }

        // Determine token type and validate
        return validateToken(token, path)
                .flatMap(validationResult -> {
                    if (!(Boolean) validationResult.get("valid")) {
                        log.warn("Token validation failed for path {}: {}", path, validationResult.get("error"));
                        return handleUnauthorized(exchange, (String) validationResult.get("error"));
                    }

                    // Add user information to request headers
                    ServerHttpRequest modifiedRequest = addUserInfoToRequest(request, validationResult);
                    ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();

                    log.debug("Token validated successfully for path: {}", path);
                    return chain.filter(modifiedExchange);
                })
                .onErrorResume(throwable -> {
                    log.error("Error during token validation for path {}: {}", path, throwable.getMessage());
                    return handleUnauthorized(exchange, "Token validation error");
                });
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Map<String, Object>> validateToken(String token, String path) {
        return Mono.fromCallable(() -> {
            Map<String, Object> result = new HashMap<>();

            // Validate as internal JWT token
            if (jwtService.validateToken(token)) {
                log.debug("Valid JWT token");
                result.put("valid", true);
                result.put("tokenType", "jwt");
                result.put("userId", jwtService.extractUserId(token));
                result.put("username", jwtService.extractUsername(token));
                result.put("phoneNumber", jwtService.extractPhoneNumber(token));
                return result;
            }

            result.put("valid", false);
            result.put("error", "Invalid JWT token");
            return result;
        });
    }

    private ServerHttpRequest addUserInfoToRequest(ServerHttpRequest request, Map<String, Object> userInfo) {
        ServerHttpRequest.Builder builder = request.mutate();

        // Add user information as headers for downstream services
        if ("jwt".equals(userInfo.get("tokenType"))) {
            builder.header("X-User-ID", String.valueOf(userInfo.get("userId")));
            builder.header("X-User-Username", (String) userInfo.get("username"));
            builder.header("X-User-Phone", (String) userInfo.get("phoneNumber"));
            builder.header("X-Token-Type", "jwt");
        }

        return builder.build();
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}", 
                message, java.time.Instant.now().toString());

        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // High priority to run before other filters
    }
    
}
