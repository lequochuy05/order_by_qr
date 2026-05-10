package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.event.WebSocketEvent;
import com.sacmauquan.qrordering.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * NotificationServiceImpl - Notification Management via WebSocket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String TOPIC_TABLES = "/topic/tables";
    private static final String TOPIC_KITCHEN = "/topic/kitchen";
    private static final String TOPIC_MENU = "/topic/menu";
    private static final String TOPIC_CATEGORIES = "/topic/categories";
    private static final String TOPIC_COMBOS = "/topic/combos";
    private static final String TOPIC_VOUCHERS = "/topic/vouchers";
    private static final String TOPIC_USERS = "/topic/users";

    private static final String EVENT_UPDATED = "UPDATED";

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Notify order change
     */
    @Override
    public void notifyOrderChange() {
        publishInternalEvent(TOPIC_TABLES, EVENT_UPDATED, "Order change for tables");
        publishInternalEvent(TOPIC_KITCHEN, EVENT_UPDATED, "Order change for kitchen");
    }

    /**
     * Notify menu change
     */
    @Override
    public void notifyMenuChange(String type, Object id) {
        publishInternalEvent(TOPIC_MENU, Map.of("type", type, "id", id), "Menu changed");
    }

    /**
     * Notify table change
     */
    @Override
    public void notifyTableChange() {
        publishInternalEvent(TOPIC_TABLES, EVENT_UPDATED, "Tables status updated");
    }

    /**
     * Notify category change
     */
    @Override
    public void notifyCategoryChange(String event, Object id) {
        publishInternalEvent(TOPIC_CATEGORIES, Map.of("event", event, "id", id), "Categories changed");
    }

    /**
     * Notify combo change
     */
    @Override
    public void notifyComboChange(String event, Object id) {
        publishInternalEvent(TOPIC_COMBOS, Map.of("event", event, "id", id), "Combos changed");
    }

    /**
     * Notify payment success
     */
    @Override
    public void notifyPaymentSuccess(Long orderId, Long transactionId) {
        Map<String, Object> payload = Map.of(
                "event", "PAYMENT_SUCCESS",
                "orderId", orderId,
                "transactionId", transactionId);
        publishInternalEvent(TOPIC_TABLES, payload, "Payment success");
    }

    /**
     * Notify voucher change
     */
    @Override
    public void notifyVoucherChange() {
        publishInternalEvent(TOPIC_VOUCHERS, EVENT_UPDATED, "Vouchers updated");
    }

    /**
     * Notify user change
     */
    @Override
    public void notifyUserChange() {
        publishInternalEvent(TOPIC_USERS, EVENT_UPDATED, "User list updated");
    }

    /**
     * Publish internal event
     */
    private void publishInternalEvent(String topic, Object payload, String logMsg) {
        log.debug("[Internal Event] Preparing WS notify for {}: {}", topic, logMsg);
        eventPublisher.publishEvent(new WebSocketEvent(topic, payload, logMsg));
    }
}
