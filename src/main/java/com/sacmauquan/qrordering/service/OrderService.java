package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OrderRepository orderRepository;

     @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private DiningTableRepository tableRepository;

    public Order createOrder(OrderRequest orderRequest) {
        if (orderRequest == null || orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        }

        DiningTable table = tableRepository.findById(orderRequest.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + orderRequest.getTableId()));

        Order order = orderRepository.findFirstByTableIdAndStatus(table.getId(), "PENDING");

        boolean isNewOrder = false;

        if (order == null) {
            order = new Order();
            order.setTable(table);
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            order.setOrderItems(new ArrayList<>());
            order.setTotalAmount(0);
            isNewOrder = true;

            table.setStatus("Đang phục vụ");
            tableRepository.save(table);
        }

        double totalAmount = order.getTotalAmount();

        for (OrderRequest.ItemRequest itemReq : orderRequest.getItems()) {
            if (itemReq.getMenuItemId() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Món ăn không hợp lệ");
            }

            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + itemReq.getMenuItemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(menuItem.getPrice());
            orderItem.setNotes(itemReq.getNotes());

            totalAmount += menuItem.getPrice() * itemReq.getQuantity();
            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public void markItemPrepared(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));
        item.setPrepared(true);
        orderItemRepository.save(item);
    }

    public Optional<Order> getCurrentOrderByTable(Long tableId) {
    return Optional.ofNullable(orderRepository.findFirstByTableIdAndStatus(tableId, "PENDING"));
}

    public String payOrder(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        if (!order.getStatus().equals("PENDING")) {
            throw new RuntimeException("Đơn đã được thanh toán hoặc đã hủy.");
        }
        
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        order.setStatus("PAID");
        order.setPaidBy(currentUser);
        order.setPaymentTime(LocalDateTime.now());  
        orderRepository.save(order);

        DiningTable table = order.getTable();
        table.setStatus("Trống");
        tableRepository.save(table);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");

        return "Thanh toán thành công";
    }
}
