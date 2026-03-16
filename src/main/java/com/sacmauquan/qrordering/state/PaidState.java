package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class PaidState implements OrderState {

    @Override
    public void handleRequest(Order order) {
        if (!"PENDING".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus())) {
             throw new IllegalStateException("Only PENDING or COMPLETED orders can be moved to PAID state.");
        }
        order.setStatus(getStatusString());
    }

    @Override
    public String getStatusString() {
        return "PAID";
    }
}
