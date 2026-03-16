package com.sacmauquan.qrordering.state;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Component
public class OrderStateFactory {
    
    private final Map<String, OrderState> states = new HashMap<>();

    public OrderStateFactory(List<OrderState> orderStates) {
        for (OrderState state : orderStates) {
            states.put(state.getStatusString(), state);
        }
    }

    public OrderState getState(String status) {
        OrderState state = states.get(status);
        if (state == null) {
            throw new IllegalArgumentException("Invalid state: " + status);
        }
        return state;
    }
}
