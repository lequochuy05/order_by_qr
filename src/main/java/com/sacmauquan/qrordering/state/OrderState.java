package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;

public interface OrderState {
    void handleRequest(Order order);
    String getStatusString();
}
