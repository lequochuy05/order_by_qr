package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.event.WebSocketEvent;
import com.sacmauquan.qrordering.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void notifyOrderChange() {
        // Thông báo cho lễ tân / quản lý bàn
        eventPublisher.publishEvent(new WebSocketEvent("/topic/tables", "UPDATED",
                "[WS] Order change -> Sent UPDATED signal to /topic/tables"));

        // Thông báo cho nhà bếp
        eventPublisher.publishEvent(new WebSocketEvent(
                "/topic/kitchen",
                "UPDATED",
                "[WS] Order change -> Sent UPDATED signal to /topic/kitchen"));
    }

    @Override
    public void notifyMenuChange(String type, Object id) {
        eventPublisher.publishEvent(new WebSocketEvent(
                "/topic/menu",
                "UPDATED",
                "[WS] Menu thay đổi (" + type + " ID: " + id + ")"));
    }

    @Override
    public void notifyTableChange() {
        eventPublisher.publishEvent(new WebSocketEvent(
                "/topic/tables",
                "UPDATED",
                "[WS] DiningTable change -> Sent UPDATED signal"));
    }

    @Override
    public void notifyCategoryChange(String event, Object id) {
        eventPublisher.publishEvent(new WebSocketEvent(
                "/topic/categories",
                "UPDATED",
                "[WS] Category " + event + " -> Sent UPDATED signal"));
    }

    @Override
    public void notifyComboChange(String event, Object id) {
        eventPublisher.publishEvent(new WebSocketEvent(
                "/topic/combos",
                "UPDATED",
                "[WS] Combo " + event + " -> Sent UPDATED signal"
        ));
    }
}
