package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;

@Component
public class PendingState implements OrderState {

    @Override
    public void handleRequest(Order order) {
        order.setStatus(getStatusString());
    }

    @Override
    public String getStatusString() {
        return "PENDING";
    }
}
