package com.sacmauquan.qrordering.event;

public record WebSocketEvent(String destination, String payload, String logMessage) {
}
