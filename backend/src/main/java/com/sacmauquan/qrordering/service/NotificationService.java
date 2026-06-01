package com.sacmauquan.qrordering.service;

/**
 * NotificationService - Interface for broad system event notifications.
 * Routes internal system changes to the WebSocket messaging layer.
 */
public interface NotificationService {

    /**
     * Notifies all relevant clients (Tables and Kitchen) of changes in order status
     * or content.
     */
    void notifyOrderChange();

    /**
     * Notifies admin clients of changes in the menu structure.
     * 
     * @param type Change type (e.g., "created", "updated", "deleted")
     * @param id   The affected menu item identifier
     */
    void notifyMenuChange(String type, Object id);

    /**
     * Notifies admin and customer clients of table status or configuration changes.
     */
    void notifyTableChange();

    /**
     * Notifies clients of category lifecycle events.
     * 
     * @param event Event type
     * @param id    Category ID
     */
    void notifyCategoryChange(String event, Object id);

    /**
     * Notifies clients of combo lifecycle events.
     * 
     * @param event Event type
     * @param id    Combo ID
     */
    void notifyComboChange(String event, Object id);

    /**
     * Notifies the specific table/order of a successful payment transaction.
     * 
     * @param orderId       Target order
     * @param transactionId Successful transaction reference
     */
    void notifyPaymentSuccess(Long orderId, Long transactionId);

    /**
     * Notifies admin clients of changes in voucher configurations.
     */
    void notifyVoucherChange();

    /**
     * Notifies admin clients of changes in the user/staff list.
     */
    void notifyUserChange();

    /**
     * Notifies clients that restaurant/system settings changed.
     */
    void notifySettingsChange();
}
