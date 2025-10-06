package com.chitchat.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for WebSocket events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class WebSocketEvent {
    private Long userId;
    private String eventType;
}
