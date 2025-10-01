package com.chitchat.user.config;

import com.chitchat.user.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for User Service
 * 
 * Configures Spring Security for the user service with:
 * - JWT-based authentication
 * - Stateless session management (no server-side sessions)
 * - CORS configuration for cross-origin requests
 * - Public endpoints for authentication flows
 * - Protected endpoints requiring JWT tokens
 * 
 * Security Architecture:
 * - No traditional username/password (uses SMS OTP)
 * - JWT tokens for stateless authentication
 * - Custom JWT filter intercepts all requests
 * - Token validation before accessing protected resources
 * - CSRF disabled (not needed for stateless JWT auth)
 * 
 * Authentication Flow:
 * 1. User requests OTP (public endpoint)
 * 2. User verifies OTP (public endpoint)
 * 3. JWT token issued on successful verification
 * 4. JWT token included in Authorization header for all subsequent requests
 * 5. JwtAuthenticationFilter validates token
 * 6. User identity extracted and added to SecurityContext
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Custom JWT authentication filter
     * Validates JWT tokens and populates SecurityContext
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Constructor for dependency injection
     * 
     * @param jwtAuthenticationFilter Custom JWT filter
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the security filter chain
     * 
     * Sets up:
     * 1. CSRF protection (disabled for REST API)
     * 2. CORS configuration
     * 3. Session management (stateless)
     * 4. Public vs protected endpoints
     * 5. JWT authentication filter
     * 
     * @param http HttpSecurity builder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST API (not needed with JWT)
            // CSRF protection is for session-based auth, not JWT
            .csrf(csrf -> csrf.disable())

            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless session management
            // No server-side sessions - all state in JWT token
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configure URL-based authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                // These allow users to register and login
                .requestMatchers(
                    "/api/users/authenticate",       // Firebase token authentication
                    "/api/users/send-otp",           // SMS OTP request
                    "/api/users/verify-otp",         // SMS OTP verification
                    "/api/users/register",           // Legacy registration
                    "/api/users/login",              // Legacy login
                    "/api/users/phone/**",           // Phone number checks
                    "/api/users/admin/password/**",  // Password utility endpoints
                    "/actuator/**",                   // Health checks and metrics
                    "/error"                         // Error page
                ).permitAll()

                // All other endpoints require valid JWT token
                .anyRequest().authenticated()
            )

            // Add custom JWT filter BEFORE Spring's UsernamePasswordAuthenticationFilter
            // This ensures JWT validation happens first
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Disable X-Frame-Options for development (allows iframe embedding)
            // TODO: Enable in production for clickjacking protection
            .headers(headers -> headers.frameOptions().disable());

        return http.build();
    }

    /**
     * Password encoder bean for BCrypt hashing
     * 
     * Although ChitChat doesn't use passwords for user authentication,
     * this is provided for:
     * - Admin panel password management
     * - Legacy features
     * - Testing utilities
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings
     * 
     * Allows the frontend application to make API calls from different origins.
     * 
     * Current Configuration (Development):
     * - Allows all origins (*)
     * - Allows all standard HTTP methods
     * - Allows common headers including Authorization
     * - Credentials disabled (no cookies with JWT auth)
     * - Preflight cache: 1 hour
     * 
     * Production Recommendations:
     * - Restrict allowedOriginPatterns to specific domains
     * - Example: ["https://chitchat.com", "https://app.chitchat.com"]
     * - Enable credentials if using cookies
     * - Use environment-specific configuration
     * 
     * @return CorsConfigurationSource with configured CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins in development
        // TODO: In production, replace with specific domains:
        // configuration.setAllowedOrigins(Arrays.asList("https://chitchat.com"));
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // Allow all standard HTTP methods
        // Covers typical REST API operations
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));

        // Allow common request headers
        // Authorization: For JWT tokens
        // Content-Type: For JSON payloads
        // Other headers: For cross-origin compatibility
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept",
            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));

        // Credentials disabled since we're not using cookies
        // JWT tokens go in Authorization header, not cookies
        configuration.setAllowCredentials(false);

        // Cache preflight OPTIONS requests for 1 hour
        // Reduces preflight overhead for repeated requests
        configuration.setMaxAge(3600L);

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}