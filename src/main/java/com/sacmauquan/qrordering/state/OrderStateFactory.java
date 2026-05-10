package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.EnumMap;
import java.util.List;

/**
 * OrderStateFactory - Quản lý việc khởi tạo và truy xuất các trạng thái đơn hàng
 */
@Component
public class OrderStateFactory {
    
    private final Map<Order.OrderStatus, OrderState> states = new EnumMap<>(Order.OrderStatus.class);

    public OrderStateFactory(List<OrderState> orderStates) {
        for (OrderState state : orderStates) {
            states.put(state.getStatus(), state);
        }
    }

    /**
     * Lấy State object dựa trên Enum status.
     */
    public OrderState getState(Order.OrderStatus status) {
        OrderState state = states.get(status);
        if (state == null) {
            throw new IllegalArgumentException("State handler not found for status: " + status);
        }
        return state;
    }

    /**
     * Lấy State object dựa trên String status (dùng cho API input).
     */
    public OrderState getState(String statusName) {
        try {
            Order.OrderStatus status = Order.OrderStatus.valueOf(statusName.toUpperCase());
            return getState(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + statusName);
        }
    }
}
