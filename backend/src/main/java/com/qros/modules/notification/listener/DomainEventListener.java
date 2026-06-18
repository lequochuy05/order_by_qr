package com.qros.modules.notification.listener;

import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.event.DomainEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChange(OrderChangeEvent event) {
        notificationService.notifyOrderChange();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMenuChange(MenuChangeEvent event) {
        notificationService.notifyMenuChange(event.type(), event.id());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTableChange(TableChangeEvent event) {
        notificationService.notifyTableChange();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryChange(CategoryChangeEvent event) {
        notificationService.notifyCategoryChange(event.type(), event.id());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComboChange(ComboChangeEvent event) {
        notificationService.notifyComboChange(event.type(), event.id());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        notificationService.notifyPaymentSuccess(event.orderId(), event.transactionId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVoucherChange(VoucherChangeEvent event) {
        notificationService.notifyVoucherChange();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromotionChange(PromotionChangeEvent event) {
        notificationService.notifyPromotionChange();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserChange(UserChangeEvent event) {
        notificationService.notifyUserChange();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSettingsChange(SettingsChangeEvent event) {
        if (event.publicSettings() != null) {
            notificationService.notifySettingsChange(event.publicSettings());
        } else {
            notificationService.notifySettingsChange();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInventoryChange(InventoryChangeEvent event) {
        notificationService.notifyInventoryChange(event.type(), event.id());
    }
}
