package com.chitchat.user.filter;

import com.chitchat.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT Authentication Filter for validating Bearer tokens on every request
 * 
 * This filter intercepts all HTTP requests and validates JWT tokens.
 * Extends OncePerRequestFilter to guarantee single execution per request.
 * 
 * Filter Responsibilities:
 * 1. Extract JWT token from Authorization header
 * 2. Validate token signature and expiration
 * 3. Extract user information from token claims
 * 4. Populate Spring Security context with user details
 * 5. Allow request to proceed if valid, or let it fail at authorization check
 * 
 * Filter Order:
 * - Runs BEFORE UsernamePasswordAuthenticationFilter
 * - Configured in SecurityConfig.filterChain()
 * 
 * Authentication Flow:
 * Request -> JwtAuthenticationFilter -> Validate Token -> Set SecurityContext -> Continue
 * 
 * If No Token or Invalid:
 * - Request continues without authentication
 * - Authorization check in SecurityConfig will reject it
 * - Public endpoints are still accessible
 * 
 * Token Format:
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 
 * Security Context Population:
 * - Principal: Phone number (username)
 * - Credentials: User ID
 * - Authorities: Empty (role-based auth not implemented)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT service for token validation and claims extraction
     */
    private final JwtService jwtService;

    /**
     * Main filter method called for every HTTP request
     * 
     * Process:
     * 1. Check for Authorization header
     * 2. Extract Bearer token
     * 3. Validate token
     * 4. Extract user claims
     * 5. Set authentication in SecurityContext
     * 6. Continue filter chain
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Remaining filters to execute
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String phoneNumber;

        // Step 1: Check if Authorization header exists and has Bearer scheme
        // No header or wrong scheme -> skip JWT authentication (public endpoint or error)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract JWT token (remove "Bearer " prefix, which is 7 characters)
        jwt = authHeader.substring(7);

        try {
            log.info("Starting JWT validation for token: {}...", jwt.substring(0, Math.min(20, jwt.length())));
            
            // Step 3: Extract phone number (username) from token
            // This is the 'subject' claim in the JWT
            phoneNumber = jwtService.extractUsername(jwt);
            log.info("Extracted phone number from token: {}", phoneNumber);

            // Step 4: Check if we should proceed with authentication
            // - phoneNumber must be extracted successfully
            // - User must not already be authenticated (avoid re-authentication)
            if (phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("Phone number is valid and user not authenticated, proceeding with validation");

                // Step 5: Validate the JWT token
                // Checks:
                // - Token signature is valid (not tampered)
                // - Token hasn't expired
                // - Phone number in token matches expected
                log.info("Calling validateToken for phone: {}", phoneNumber);
                if (jwtService.validateToken(jwt, phoneNumber)) {
                    log.info("JWT token validated successfully for phone: {}", phoneNumber);

                    // Step 6: Extract user ID from custom claim
                    Long userId = jwtService.extractUserId(jwt);

                    // Step 7: Create Spring Security authentication token
                    // - Principal: Phone number (acts as username)
                    // - Credentials: User ID (for easy access in controllers)
                    // - Authorities: Empty list (role-based auth not implemented)
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        phoneNumber,       // principal (who the user is)
                        userId,            // credentials (user's ID)
                        new ArrayList<>()  // authorities (roles/permissions - none for now)
                    );

                    // Step 8: Add request details (IP address, session ID, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Step 9: Set authentication in SecurityContext
                    // This makes the user "authenticated" for this request
                    // Other filters and controllers can now access user info
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Authentication set for phone: {} with userId: {}", phoneNumber, userId);
                } else {
                    log.warn("Invalid JWT token for phone: {}", phoneNumber);
                    // Token is invalid - user remains unauthenticated
                    // Request will likely be rejected by authorization rules
                }
            }
        } catch (Exception e) {
            // Exception during token processing
            // Could be: Expired token, invalid signature, malformed token, etc.
            log.error("JWT token validation error: {}", e.getMessage());
            
            // Clear security context to ensure no partial authentication
            SecurityContextHolder.clearContext();
        }

        // Step 10: Continue with the rest of the filter chain
        // Whether authentication succeeded or failed, let request proceed
        // Authorization checks in SecurityConfig will handle rejection
        filterChain.doFilter(request, response);
    }
}