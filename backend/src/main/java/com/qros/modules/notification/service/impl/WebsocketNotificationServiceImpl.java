package com.qros.modules.notification.service.impl;

import com.qros.modules.notification.dto.internal.NotificationPayload;
import com.qros.modules.notification.event.WebSocketEvent;
import com.qros.modules.notification.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebsocketNotificationServiceImpl implements NotificationService {

    private static final String TOPIC_TABLES = "/topic/tables";
    private static final String TOPIC_ORDERS = "/topic/orders";
    private static final String TOPIC_KITCHEN = "/topic/kitchen";
    private static final String TOPIC_MENU = "/topic/menu";
    private static final String TOPIC_CATEGORIES = "/topic/categories";
    private static final String TOPIC_COMBOS = "/topic/combos";
    private static final String TOPIC_VOUCHERS = "/topic/vouchers";
    private static final String TOPIC_PROMOTIONS = "/topic/promotions";
    private static final String TOPIC_USERS = "/topic/users";
    private static final String TOPIC_SETTINGS = "/topic/settings";
    private static final String TOPIC_INVENTORY = "/topic/inventory";

    private static final String EVENT_UPDATED = "UPDATED";
    private static final String EVENT_PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    private static final String EVENT_SETTINGS_UPDATED = "SETTINGS_UPDATED";

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void notifyOrderChange() {
        publishInternalEvent(
                TOPIC_TABLES, NotificationPayload.event(EVENT_UPDATED), "Order state changed for customer views");

        publishInternalEvent(
                TOPIC_ORDERS, NotificationPayload.event(EVENT_UPDATED), "Order state changed for management views");

        publishInternalEvent(
                TOPIC_KITCHEN, NotificationPayload.event(EVENT_UPDATED), "Order state changed for kitchen queue");
    }

    @Override
    public void notifyMenuChange(String type, Object id) {
        publishInternalEvent(TOPIC_MENU, NotificationPayload.eventWithId(type, id), "Menu catalog modified");
    }

    @Override
    public void notifyTableChange() {
        publishInternalEvent(TOPIC_TABLES, NotificationPayload.event(EVENT_UPDATED), "Dining table statuses refreshed");
    }

    @Override
    public void notifyCategoryChange(String event, Object id) {
        publishInternalEvent(TOPIC_CATEGORIES, NotificationPayload.eventWithId(event, id), "Category metadata updated");
    }

    @Override
    public void notifyComboChange(String event, Object id) {
        publishInternalEvent(TOPIC_COMBOS, NotificationPayload.eventWithId(event, id), "Menu combos updated");
    }

    @Override
    public void notifyPaymentSuccess(Long orderId, Long transactionId) {
        Map<String, Object> payload = Map.of(
                "event", EVENT_PAYMENT_SUCCESS,
                "orderId", orderId,
                "transactionId", transactionId);

        publishInternalEvent(TOPIC_TABLES, payload, "Successful payment received for Order #" + orderId);
    }

    @Override
    public void notifyVoucherChange() {
        publishInternalEvent(
                TOPIC_VOUCHERS, NotificationPayload.event(EVENT_UPDATED), "Promotional vouchers catalog modified");
    }

    @Override
    public void notifyPromotionChange() {
        publishInternalEvent(TOPIC_PROMOTIONS, NotificationPayload.event(EVENT_UPDATED), "Promotions catalog modified");
    }

    @Override
    public void notifyUserChange() {
        publishInternalEvent(
                TOPIC_USERS, NotificationPayload.event(EVENT_UPDATED), "Internal user management list modified");
    }

    @Override
    public void notifySettingsChange() {
        publishInternalEvent(TOPIC_SETTINGS, NotificationPayload.event(EVENT_UPDATED), "System settings updated");
    }

    @Override
    public void notifySettingsChange(Object publicSettings) {
        publishInternalEvent(
                TOPIC_SETTINGS,
                NotificationPayload.eventWithData(EVENT_SETTINGS_UPDATED, publicSettings),
                "System settings updated with public settings snapshot");
    }

    @Override
    public void notifyInventoryChange(String type, Object id) {
        publishInternalEvent(TOPIC_INVENTORY, NotificationPayload.eventWithId(type, id), "Inventory state modified");
    }

    private void publishInternalEvent(String topic, Object payload, String logMessage) {
        log.debug("[WebSocket Notification] Topic: {} | Action: {}", topic, logMessage);
        eventPublisher.publishEvent(new WebSocketEvent(topic, payload, logMessage));
    }
}
