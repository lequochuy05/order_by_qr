package com.qros.modules.order.service;

import com.qros.modules.order.model.OrderItem;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * Port for reserving, adjusting, restoring, and consuming inventory for order line items.
 */
public interface OrderInventoryReservationPort {

    void reserveForOrderItems(@NonNull Map<OrderItem, Integer> quantitiesToReserve);

    void reserveForOrderItemQuantity(@NonNull OrderItem orderItem, @NonNull Integer quantity);

    void adjustReservationForQuantity(@NonNull OrderItem orderItem, int oldQuantity, int newQuantity);

    void restoreOrderItem(@NonNull OrderItem orderItem);

    void consumeForOrderItem(@NonNull OrderItem orderItem);
}
