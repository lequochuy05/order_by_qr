package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.event.WebSocketEvent;
import com.sacmauquan.qrordering.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * NotificationServiceImpl - Implementation of NotificationService using Spring
 * Application Events.
 * Dispatches internal events which are subsequently picked up by
 * WebSocketEventListener for real-time delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    // STOMP Destination Topics
    private static final String TOPIC_TABLES = "/topic/tables";
    private static final String TOPIC_KITCHEN = "/topic/kitchen";
    private static final String TOPIC_MENU = "/topic/menu";
    private static final String TOPIC_CATEGORIES = "/topic/categories";
    private static final String TOPIC_COMBOS = "/topic/combos";
    private static final String TOPIC_VOUCHERS = "/topic/vouchers";
    private static final String TOPIC_USERS = "/topic/users";
    private static final String TOPIC_SETTINGS = "/topic/settings";

    private static final String EVENT_UPDATED = "UPDATED";

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Dispatches order change notifications to both customer tables and the kitchen
     * management view.
     */
    @Override
    public void notifyOrderChange() {
        publishInternalEvent(TOPIC_TABLES, EVENT_UPDATED, "Order state changed for customer views");
        publishInternalEvent(TOPIC_KITCHEN, EVENT_UPDATED, "Order state changed for kitchen queue");
    }

    /**
     * Notifies admin clients of changes in menu items.
     */
    @Override
    public void notifyMenuChange(String type, Object id) {
        publishInternalEvent(TOPIC_MENU, Map.of("type", type, "id", id), "Menu catalog modified");
    }

    /**
     * Notifies relevant clients of table status or configuration updates.
     */
    @Override
    public void notifyTableChange() {
        publishInternalEvent(TOPIC_TABLES, EVENT_UPDATED, "Dining table statuses refreshed");
    }

    /**
     * Notifies clients of category modifications.
     */
    @Override
    public void notifyCategoryChange(String event, Object id) {
        publishInternalEvent(TOPIC_CATEGORIES, Map.of("event", event, "id", id), "Category metadata updated");
    }

    /**
     * Notifies clients of combo modifications.
     */
    @Override
    public void notifyComboChange(String event, Object id) {
        publishInternalEvent(TOPIC_COMBOS, Map.of("event", event, "id", id), "Menu combos updated");
    }

    /**
     * Sends a specialized real-time notification for successful payments.
     */
    @Override
    public void notifyPaymentSuccess(Long orderId, Long transactionId) {
        Map<String, Object> payload = Map.of(
                "event", "PAYMENT_SUCCESS",
                "orderId", orderId,
                "transactionId", transactionId);
        publishInternalEvent(TOPIC_TABLES, payload, "Successful payment received for Order #" + orderId);
    }

    /**
     * Notifies clients of voucher catalog changes.
     */
    @Override
    public void notifyVoucherChange() {
        publishInternalEvent(TOPIC_VOUCHERS, EVENT_UPDATED, "Promotional vouchers catalog modified");
    }

    /**
     * Notifies administrative clients of user/staff account changes.
     */
    @Override
    public void notifyUserChange() {
        publishInternalEvent(TOPIC_USERS, EVENT_UPDATED, "Internal user management list modified");
    }

    /**
     * Notifies clients of restaurant/system settings changes.
     */
    @Override
    public void notifySettingsChange() {
        publishInternalEvent(TOPIC_SETTINGS, EVENT_UPDATED, "System settings updated");
    }

    /**
     * Internal helper to wrap and publish events to the Spring Application Context.
     * These events are asynchronously broadcast via WebSockets.
     */
    private void publishInternalEvent(String topic, Object payload, String logMsg) {
        log.debug("[WebSocket Notification] Topic: {} | Action: {}", topic, logMsg);
        eventPublisher.publishEvent(new WebSocketEvent(topic, payload, logMsg));
    }
}
