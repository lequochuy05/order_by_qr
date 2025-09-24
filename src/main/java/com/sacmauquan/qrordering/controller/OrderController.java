package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import com.sacmauquan.qrordering.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ===================== CREATE ORDER (thêm món) =====================
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest orderRequest) {
        try {
            return ResponseEntity.ok(orderService.createOrder(orderRequest));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================== OTHER ORDER OPERATIONS =====================
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(orderService.updateStatus(id, body.get("status")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/items/{itemId}/prepared")
    public ResponseEntity<?> markItemPrepared(@PathVariable Long itemId) {
        try {
            orderService.markItemPrepared(itemId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/table/{tableId}/current")
    public ResponseEntity<?> getCurrentOrderByTable(@PathVariable Long tableId) {
        return orderService.getCurrentOrderByTable(tableId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateOrderItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body) {
        try {
            int qty = (int) body.getOrDefault("quantity", 1);
            String notes = (String) body.getOrDefault("notes", "");
            return ResponseEntity.ok(orderService.updateOrderItem(itemId, qty, notes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    @PutMapping("/{orderId}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long orderId,
                                    @RequestParam Long userId) {
        try {
            String result = orderService.payOrder(orderId, userId);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (IllegalStateException e) {
            // còn món chưa làm → báo lỗi rõ ràng
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> cancelOrderItem(@PathVariable Long itemId) {
        try {
            orderService.cancelOrderItem(itemId);
            return ResponseEntity.ok("Đã hủy món");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
