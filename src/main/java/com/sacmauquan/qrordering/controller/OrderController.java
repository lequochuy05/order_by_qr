package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest orderRequest) {
        if (orderRequest == null || orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        DiningTable table = tableRepository.findById(orderRequest.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + orderRequest.getTableId()));

        Order order = new Order();
        order.setTable(table);
        order.setStatus(orderRequest.getStatus() != null ? orderRequest.getStatus() : "PENDING");
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0;

        for (OrderRequest.ItemRequest itemReq : orderRequest.getItems()) {
            if (itemReq.getMenuItemId() == null || itemReq.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body(null);
            }

            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + itemReq.getMenuItemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(menuItem.getPrice());

            totalAmount += menuItem.getPrice() * itemReq.getQuantity();
            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order); // cascade sẽ lưu luôn OrderItem
        return ResponseEntity.ok(savedOrder);
    }

    
}