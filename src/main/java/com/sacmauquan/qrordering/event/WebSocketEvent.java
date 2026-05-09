package com.sacmauquan.qrordering.event;

/**
 * WebSocketEvent - Sự kiện nội bộ để kích hoạt thông báo thời gian thực.
 * Sử dụng Object payload để Jackson tự động serialize sang JSON chuẩn.
 */
public record WebSocketEvent(String destination, Object payload, String logMessage) {
}
