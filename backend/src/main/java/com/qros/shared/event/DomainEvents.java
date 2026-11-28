package com.qros.shared.event;

import java.time.LocalDate;

public interface DomainEvents {
    record OrderChangeEvent() {}

    record OrderSettledEvent(Long orderId, LocalDate businessDate) {}

    record MenuChangeEvent(String type, Object id) {}

    record MenuItemDeletedEvent(Long menuItemId) {}

    record TableChangeEvent() {}

    record CategoryChangeEvent(String type, Object id) {}

    record ComboChangeEvent(String type, Object id) {}

    record PaymentSuccessEvent(Long orderId, Long transactionId) {}

    record VoucherChangeEvent() {}

    record PromotionChangeEvent() {}

    record UserChangeEvent() {}

    record UserAuthChangeEvent(String email, String previousEmail) {}

    record SettingsChangeEvent(Object publicSettings) {}

    record SubscriptionChangeEvent(String planTier, String status) {}

    record InventoryChangeEvent(String type, Object id) {}

    record ServiceRequestChangeEvent(String action, Long requestId, Long tableId) {}
}
