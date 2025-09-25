package com.sacmauquan.qrordering.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.OrderItem;
import com.sacmauquan.qrordering.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ===================== CREATE ORDER (thêm món / cộng dồn) =====================
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest orderRequest) {
        try {
            Order order = orderService.createOrder(orderRequest);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            Map<String,Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // ===================== GET ALL ORDERS =====================
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    // ===================== UPDATE STATUS =====================
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(orderService.updateStatus(id, body.get("status")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===================== MARK ITEM PREPARED =====================
    @PutMapping("/items/{itemId}/prepared")
    public ResponseEntity<?> markItemPrepared(@PathVariable Long itemId) {
        try {
            orderService.markItemPrepared(itemId);
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái món"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===================== GET CURRENT ORDER BY TABLE =====================
    @GetMapping("/table/{tableId}/current")
    public ResponseEntity<?> getCurrentOrderByTable(@PathVariable Long tableId) {
        return orderService.getCurrentOrderByTable(tableId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ===================== UPDATE ORDER ITEM =====================
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateOrderItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body) {
        try {
            int qty = (int) body.getOrDefault("quantity", 1);
            String notes = (String) body.getOrDefault("notes", "");
            OrderItem updated = orderService.updateOrderItem(itemId, qty, notes);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===================== PAY ORDER =====================
    @PutMapping("/{orderId}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long orderId,
                                      @RequestParam Long userId) {
        try {
            String result = orderService.payOrder(orderId, userId);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===================== CANCEL ORDER ITEM =====================
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> cancelOrderItem(@PathVariable Long itemId) {
        try {
            orderService.cancelOrderItem(itemId);
            return ResponseEntity.ok(Map.of("message", "Đã hủy món"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===================== PREVIEW ORDER (tính thử tiền) =====================
    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody OrderRequest req) {
        try {
            return ResponseEntity.ok(orderService.preview(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"
            ));
        }
    }

}
