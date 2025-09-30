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
 * JWT Authentication Filter to process Bearer tokens
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String phoneNumber;

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token
        jwt = authHeader.substring(7);

        try {
            log.info("Starting JWT validation for token: {}...", jwt.substring(0, Math.min(20, jwt.length())));
            
            // Extract phone number from token
            phoneNumber = jwtService.extractUsername(jwt);
            log.info("Extracted phone number from token: {}", phoneNumber);

            // If token is valid and user is not already authenticated
            if (phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("Phone number is valid and user not authenticated, proceeding with validation");

                // Validate token
                log.info("Calling validateToken for phone: {}", phoneNumber);
                if (jwtService.validateToken(jwt, phoneNumber)) {
                    log.info("JWT token validated successfully for phone: {}", phoneNumber);

                    // Extract user ID from token
                    Long userId = jwtService.extractUserId(jwt);

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        phoneNumber, // principal (phone number)
                        userId,      // credentials (user ID)
                        new ArrayList<>() // authorities (empty for now)
                    );

                    // Set authentication details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Authentication set for phone: {} with userId: {}", phoneNumber, userId);
                } else {
                    log.warn("Invalid JWT token for phone: {}", phoneNumber);
                }
            }
        } catch (Exception e) {
            log.error("JWT token validation error: {}", e.getMessage());
            // Clear security context on error
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}