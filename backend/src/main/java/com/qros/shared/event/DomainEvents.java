package com.qros.shared.event;

import java.time.LocalDate;

public interface DomainEvents {
    record OrderChangeEvent() {}

    record OrderSettledEvent(Long orderId, LocalDate businessDate) {}

    record MenuChangeEvent(String type, Object id) {}

    record TableChangeEvent() {}

    record CategoryChangeEvent(String type, Object id) {}

    record ComboChangeEvent(String type, Object id) {}

    record PaymentSuccessEvent(Long orderId, Long transactionId) {}

    record VoucherChangeEvent() {}

    record PromotionChangeEvent() {}

    record UserChangeEvent() {}

    record SettingsChangeEvent(Object publicSettings) {}

    record InventoryChangeEvent(String type, Object id) {}
}
