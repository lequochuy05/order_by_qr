package com.sacmauquan.qrordering.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.dto.OrderResponse;
import com.sacmauquan.qrordering.dto.OrderPreviewResponse;
import com.sacmauquan.qrordering.service.OrderService;

import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * OrderController -
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create new order
     */
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody @NonNull OrderRequest orderRequest) {
        return ApiResponse.success("Đặt món thành công", orderService.createOrder(orderRequest));
    }

    /**
     * Get all orders — DEPRECATED: Use /history (paginated) instead.
     * This endpoint is unsafe at scale. Limited to 100 records as a safety net.
     */
    @Deprecated
    @GetMapping
    public ApiResponse<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        if (orders.size() > 100) {
            orders = orders.subList(0, 100);
        }
        return ApiResponse.success(orders);
    }

    /**
     * Get order history with advanced filtering and pagination
     */
    @GetMapping("/history")
    public ApiResponse<Page<OrderResponse>> getOrderHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String tableNumber,
            @PageableDefault(size = 15, sort = "createdAt", direction = Direction.DESC) @NonNull Pageable pageable) {

        return ApiResponse
                .success(orderService.getOrderHistory(status, startDate, endDate, orderId, tableNumber, pageable));
    }

    /**
     * Get order statistics with time filter
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getOrderStats(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String tableNumber) {

        return ApiResponse.success(orderService.getOrderStats(status, startDate, endDate, orderId, tableNumber));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable @NonNull Long id,
            @RequestBody @NonNull Map<String, String> body) {
        String status = body.get("status");
        return ApiResponse.success("Update status successfully",
                orderService.updateStatus(id, Objects.requireNonNull(status)));
    }

    /**
     * Reconcile payment status with external providers
     */
    @PostMapping("/{id}/reconcile")
    public ApiResponse<OrderResponse> reconcileOrder(@PathVariable @NonNull Long id) {
        return ApiResponse.success("Reconcile successfully", orderService.reconcileOrder(id));
    }

    /**
     * Mark item as prepared (KDS)
     */
    @PatchMapping("/items/{itemId}/prepared")
    public ApiResponse<Void> markItemPrepared(@PathVariable @NonNull Long itemId) {
        orderService.updateItemStatus(itemId, "FINISHED");
        return ApiResponse.success("Mark item as prepared successfully", null);
    }

    /**
     * Update item status
     */
    @PatchMapping("/items/{itemId}/status")
    public ApiResponse<Void> updateItemStatus(
            @PathVariable @NonNull Long itemId,
            @RequestBody @NonNull Map<String, String> body) {
        String status = body.get("status");
        orderService.updateItemStatus(itemId, Objects.requireNonNull(status));
        return ApiResponse.success("Update item status successfully", null);
    }

    /**
     * Get kitchen orders
     */
    @GetMapping("/kitchen")
    public ApiResponse<List<OrderResponse>> getKitchenOrders() {
        return ApiResponse.success(orderService.getKitchenOrders());
    }

    /**
     * Get current order of a table
     */
    @GetMapping("/table/{tableId}/current")
    public ApiResponse<OrderResponse> getCurrentOrderByTable(@PathVariable @NonNull Long tableId) {
        return orderService.getCurrentOrderByTable(tableId)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }

    /**
     * Get active orders
     */
    @GetMapping("/active")
    public ApiResponse<List<OrderResponse>> getActiveOrders() {
        return ApiResponse.success(orderService.getActiveOrders());
    }

    /**
     * Update order item
     */
    @PatchMapping("/items/{itemId}")
    public ApiResponse<OrderResponse> updateOrderItem(
            @PathVariable @NonNull Long itemId,
            @RequestBody @NonNull Map<String, Object> body) {
        int qty = (int) body.getOrDefault("quantity", 1);
        String notes = (String) body.getOrDefault("notes", "");
        return ApiResponse.success("Update order item successfully",
                orderService.updateOrderItem(itemId, qty, notes));
    }

    /**
     * Pay order and apply voucher
     */
    @PatchMapping("/{orderId}/pay")
    public ApiResponse<String> payOrder(
            @PathVariable @NonNull Long orderId,
            @RequestParam @NonNull Long userId,
            @RequestParam(required = false) String voucherCode) {
        String result = orderService.payOrder(orderId, userId, voucherCode);
        return ApiResponse.success("Pay order successfully", result);
    }

    /**
     * Cancel order item
     */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> cancelOrderItem(@PathVariable @NonNull Long itemId) {
        orderService.cancelOrderItem(itemId);
        return ApiResponse.success("Cancel order item successfully", null);
    }

    /**
     * Preview order
     */
    @PostMapping("/preview")
    public ApiResponse<OrderPreviewResponse> preview(@Valid @RequestBody @NonNull OrderRequest req) {
        return ApiResponse.success(orderService.preview(req));
    }
}
