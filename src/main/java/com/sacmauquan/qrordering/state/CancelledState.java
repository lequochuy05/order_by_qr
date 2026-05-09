package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class CancelledState implements OrderState {

    @Override
    public void handleRequest(Order order) {
        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new IllegalStateException("Đơn hàng đã hoàn thành không thể hủy.");
        }
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.CANCELLED;
    }
}
