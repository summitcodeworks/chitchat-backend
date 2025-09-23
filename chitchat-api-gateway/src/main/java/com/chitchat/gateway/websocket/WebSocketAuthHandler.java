package com.chitchat.gateway.websocket;

import com.chitchat.gateway.service.FirebaseService;
import com.chitchat.gateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket authentication handler
 * Validates tokens during WebSocket handshake
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthHandler extends DefaultHandshakeHandler {

    private final FirebaseService firebaseService;
    private final JwtService jwtService;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            log.warn("No token provided for WebSocket connection");
            return null;
        }

        // Validate Firebase token
        if (firebaseService.isFirebaseAvailable()) {
            Map<String, Object> firebaseResult = firebaseService.verifyIdToken(token);
            if ((Boolean) firebaseResult.get("valid")) {
                String uid = (String) firebaseResult.get("uid");
                String phoneNumber = (String) firebaseResult.get("phoneNumber");
                
                log.info("WebSocket authenticated with Firebase token for user: {}", uid);
                attributes.put("uid", uid);
                attributes.put("phoneNumber", phoneNumber);
                attributes.put("tokenType", "firebase");
                
                return new WebSocketPrincipal(uid, phoneNumber, "firebase");
            }
        }

        // Validate JWT token
        if (jwtService.validateToken(token)) {
            Long userId = jwtService.extractUserId(token);
            String username = jwtService.extractUsername(token);
            String phoneNumber = jwtService.extractPhoneNumber(token);
            
            log.info("WebSocket authenticated with JWT token for user: {}", userId);
            attributes.put("userId", userId);
            attributes.put("username", username);
            attributes.put("phoneNumber", phoneNumber);
            attributes.put("tokenType", "jwt");
            
            return new WebSocketPrincipal(String.valueOf(userId), phoneNumber, "jwt");
        }

        log.warn("Invalid token provided for WebSocket connection");
        return null;
    }

    private String extractTokenFromRequest(ServerHttpRequest request) {
        // Try to get token from query parameters
        String token = request.getURI().getQuery();
        if (token != null && token.startsWith("token=")) {
            return token.substring(6);
        }

        // Try to get token from headers
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    /**
     * Custom Principal for WebSocket connections
     */
    public static class WebSocketPrincipal implements Principal {
        private final String name;
        private final String phoneNumber;
        private final String tokenType;

        public WebSocketPrincipal(String name, String phoneNumber, String tokenType) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.tokenType = tokenType;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getTokenType() {
            return tokenType;
        }
    }
}
