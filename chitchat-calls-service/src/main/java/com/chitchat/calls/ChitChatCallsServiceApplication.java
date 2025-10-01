package com.chitchat.calls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChitChat Calls Service Application - Voice/Video Call Microservice
 * 
 * This microservice handles all voice and video calling functionality:
 * 
 * Core Functionalities:
 * - WebRTC signaling for peer-to-peer connections
 * - Call initiation and acceptance
 * - Call state management (ringing, active, ended)
 * - Call history and logs
 * - STUN/TURN server coordination
 * - Call quality metrics
 * - Group voice/video calls
 * - Screen sharing support
 * - Call recording metadata
 * 
 * Call Types:
 * - One-on-one voice calls
 * - One-on-one video calls
 * - Group voice calls (conference)
 * - Group video calls (conference)
 * - Screen sharing sessions
 * 
 * Technology Stack:
 * - Spring Boot microservice framework
 * - WebRTC for real-time communication
 * - WebSocket for signaling
 * - PostgreSQL for call history/metadata
 * - STUN/TURN servers for NAT traversal
 * 
 * WebRTC Architecture:
 * - Signaling: This service handles offer/answer exchange
 * - Media: Peer-to-peer between clients (not through server)
 * - STUN: For finding public IP addresses
 * - TURN: Relay server when P2P fails (firewall/NAT issues)
 * 
 * Call Flow:
 * 1. Caller initiates call via REST API
 * 2. Call record created with status RINGING
 * 3. Push notification sent to callee
 * 4. WebSocket signaling for WebRTC negotiation
 * 5. Offer/Answer SDP exchange
 * 6. ICE candidate exchange
 * 7. Direct P2P connection established
 * 8. Call status updated (ACTIVE, ENDED, MISSED)
 * 9. Call metadata saved to database
 * 
 * Security:
 * - End-to-end encryption via WebRTC (DTLS/SRTP)
 * - Token-based call authorization
 * - Call participant verification
 * - Encrypted signaling messages
 * 
 * Note: This service only handles signaling and metadata.
 * Actual audio/video streams flow peer-to-peer via WebRTC.
 */
@SpringBootApplication
public class ChitChatCallsServiceApplication {

    /**
     * Application entry point
     * 
     * Starts the Calls Service microservice on configured port (default: 8083)
     * Registers with Eureka service discovery
     * Initializes WebSocket endpoints for signaling
     * Configures STUN/TURN server connections
     * Sets up call state management
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ChitChatCallsServiceApplication.class, args);
    }
}
