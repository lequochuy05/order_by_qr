package com.qros.modules.notification.event;

/**
 * WebSocketEvent - Internal application event to trigger real-time notifications via WebSocket.
 * Uses an Object payload for automatic JSON serialization.
 * 
 * @param destination The WebSocket destination topic
 * @param payload The data object to be sent
 * @param logMessage Optional descriptive message for debugging logs
 */
public record WebSocketEvent(String destination, Object payload, String logMessage) {
}
