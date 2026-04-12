package com.sacmauquan.qrordering.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.OrderItem;
import com.sacmauquan.qrordering.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    // ===================== CREATE ORDER (thêm món / cộng dồn)
    // =====================
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        Order order = orderService.createOrder(Objects.requireNonNull(orderRequest));
        return ResponseEntity.ok(order);
    }

    // ===================== GET ALL ORDERS =====================
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    // ===================== GET ORDER HISTORY (PAGINATED) =====================
    @GetMapping("/history")
    public ResponseEntity<?> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search) {

        LocalDateTime start = null;
        LocalDateTime end = null;
        if (startDate != null && !startDate.isBlank()) {
            start = LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.isBlank()) {
            end = LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> result = orderService.getOrderHistory(status, start, end, search, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", result.getContent());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("currentPage", result.getNumber());
        response.put("size", result.getSize());

        return ResponseEntity.ok(response);
    }

    // ===================== GET ORDER STATS =====================
    @GetMapping("/stats")
    public ResponseEntity<?> getOrderStats(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDateTime start = null;
        LocalDateTime end = null;
        if (startDate != null && !startDate.isBlank()) {
            start = LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.isBlank()) {
            end = LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        return ResponseEntity.ok(orderService.getOrderStats(status, start, end));
    }

    // ===================== UPDATE STATUS =====================
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(orderService.updateStatus(
                Objects.requireNonNull(id),
                Objects.requireNonNull(body.get("status"))));
    }

    // ===================== MARK ITEM PREPARED =====================
    @PatchMapping("/items/{itemId}/prepared")
    public ResponseEntity<?> markItemPrepared(@PathVariable Long itemId) {
        orderService.updateItemStatus(Objects.requireNonNull(itemId), "FINISHED");
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái món"));
    }

    // ===================== UPDATE ITEM STATUS (KDS) =====================
    @PatchMapping("/items/{itemId}/status")
    public ResponseEntity<?> updateItemStatus(
            @PathVariable Long itemId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        orderService.updateItemStatus(Objects.requireNonNull(itemId), Objects.requireNonNull(status));
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái món: " + status));
    }

    // ===================== GET KITCHEN ORDERS = :p =====================
    @GetMapping("/kitchen")
    public List<Order> getKitchenOrders() {
        return orderService.getKitchenOrders();
    }

    // ===================== GET CURRENT ORDER BY TABLE =====================
    @GetMapping("/table/{tableId}/current")
    public ResponseEntity<?> getCurrentOrderByTable(@PathVariable Long tableId) {
        return orderService.getCurrentOrderByTable(Objects.requireNonNull(tableId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ===================== GET ALL ACTIVE ORDERS =====================
    @GetMapping("/active")
    public List<Order> getActiveOrders() {
        return orderService.getActiveOrders();
    }

    // ===================== UPDATE ORDER ITEM =====================
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<?> updateOrderItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body) {
        int qty = (int) body.getOrDefault("quantity", 1);
        String notes = (String) body.getOrDefault("notes", "");
        OrderItem updated = orderService.updateOrderItem(Objects.requireNonNull(itemId), qty, notes);
        return ResponseEntity.ok(updated);
    }

    // ===================== PAY ORDER =====================
    @PatchMapping("/{orderId}/pay")
    public ResponseEntity<?> payOrder(
            @PathVariable Long orderId,
            @RequestParam Long userId,
            @RequestParam(required = false) String voucherCode) {
        String result = orderService.payOrder(Objects.requireNonNull(orderId), Objects.requireNonNull(userId),
                voucherCode);
        return ResponseEntity.ok(Map.of("message", result));
    }

    // ===================== CANCEL ORDER ITEM =====================
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> cancelOrderItem(@PathVariable Long itemId) {
        orderService.cancelOrderItem(Objects.requireNonNull(itemId));
        return ResponseEntity.ok(Map.of("message", "Đã hủy món"));
    }

    // ===================== PREVIEW ORDER (tính thử tiền) =====================
    @PostMapping("/preview")
    public ResponseEntity<?> preview(@Valid @RequestBody OrderRequest req) {
        return ResponseEntity.ok(orderService.preview(Objects.requireNonNull(req)));
    }

}
