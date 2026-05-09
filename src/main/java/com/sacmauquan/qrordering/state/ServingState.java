package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class ServingState implements OrderState {

    @Override
    public void handleRequest(Order order) {
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ đơn hàng PENDING mới có thể chuyển sang trạng thái đang phục vụ.");
        }
        order.setStatus(getStatus());
    }

    @Override
    public Order.OrderStatus getStatus() {
        return Order.OrderStatus.SERVING;
    }
}
