package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class CompletedState implements OrderState {

    @Override
    public void handleRequest(Order order) {
        if (!"READY".equals(order.getStatus()) && !"PENDING".equals(order.getStatus())) {
             // Depending on business rules, we might allow PENDING -> COMPLETED direct for some cases, but generally it's READY -> COMPLETED.
             // We'll allow READY -> COMPLETED.
             throw new IllegalStateException("Only READY orders can be moved to COMPLETED state.");
        }
        order.setStatus(getStatusString());
    }

    @Override
    public String getStatusString() {
        return "COMPLETED";
    }
}
