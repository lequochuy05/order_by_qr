package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OrderItemRepository orderItemRepository;

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

        // ✅ Kiểm tra nếu đã có order PENDING
        Order order = orderRepository.findFirstByTableIdAndStatus(table.getId(), "PENDING");

        boolean isNewOrder = false;

        if (order == null) {
            order = new Order();
            order.setTable(table);
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now());
            order.setOrderItems(new ArrayList<>());
            order.setTotalAmount(0);
            isNewOrder = true;

            // ✅ Cập nhật trạng thái bàn
            table.setStatus("Đang phục vụ");
            tableRepository.save(table);
        }

        double totalAmount = order.getTotalAmount();

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
            orderItem.setNotes(itemReq.getNotes()); 

            totalAmount += menuItem.getPrice() * itemReq.getQuantity();
            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order); // cascade lưu luôn orderItems

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");

        return ResponseEntity.ok(savedOrder);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        order.setStatus(body.get("status"));
        return ResponseEntity.ok(orderRepository.save(order));
    }

    @PutMapping("/items/{itemId}/prepared")
    public ResponseEntity<?> markItemPrepared(@PathVariable Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        item.setPrepared(true);
        orderItemRepository.save(item);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/table/{tableId}/current")
    public ResponseEntity<?> getCurrentOrderByTable(@PathVariable Long tableId) {
        Order order = orderRepository.findFirstByTableIdAndStatus(tableId, "PENDING");

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        if (!order.getStatus().equals("PENDING")) {
            return ResponseEntity.badRequest().body("Đơn đã được thanh toán hoặc đã hủy.");
        }

        order.setStatus("PAID");
        orderRepository.save(order);

        // Cập nhật trạng thái bàn về trống
        DiningTable table = order.getTable();
        table.setStatus("Trống");
        tableRepository.save(table);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");

        return ResponseEntity.ok("Thanh toán thành công");
    }
}
