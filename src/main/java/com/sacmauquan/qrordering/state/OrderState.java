package com.sacmauquan.qrordering.state;

import com.sacmauquan.qrordering.model.Order;

/**
 * OrderState - Giao diện chung cho các trạng thái đơn hàng.
 */
public interface OrderState {
    /**
     * Xử lý logic chuyển đổi trạng thái cho đơn hàng.
     */
    void handleRequest(Order order);

    /**
     * Trả về Enum OrderStatus tương ứng.
     */
    Order.OrderStatus getStatus();
}
