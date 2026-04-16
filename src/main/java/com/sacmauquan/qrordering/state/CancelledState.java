package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class CancelledState implements OrderState {

    @Override
    public void handleRequest(Order order) {
        if ("COMPLETED".equals(order.getStatus())) {
            throw new IllegalStateException("COMPLETED orders cannot be CANCELLED.");
        }
        order.setStatus(getStatusString());
    }

    @Override
    public String getStatusString() {
        return "CANCELLED";
    }
}
