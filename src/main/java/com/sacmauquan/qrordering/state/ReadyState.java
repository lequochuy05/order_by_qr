package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class ReadyState implements OrderState {

    @Override
    public void handleRequest(Order order) {
        if (!"PREPARING".equals(order.getStatus())) {
            throw new IllegalStateException("Only PREPARING orders can be moved to READY state.");
        }
        order.setStatus(getStatusString());
    }

    @Override
    public String getStatusString() {
        return "READY";
    }
}
