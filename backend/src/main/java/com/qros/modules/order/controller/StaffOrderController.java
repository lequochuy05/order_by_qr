package com.qros.modules.order.controller;

import com.qros.modules.order.dto.request.OrderStatusUpdateRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.TableBoardResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * StaffOrderController - Staff/Admin order-level APIs.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class StaffOrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> createStaffOrder(
            @Valid @RequestBody @NonNull StaffCreateOrderRequest request) {
        return ApiResponse.success(
                "Đặt món thành công",
                orderService.createStaffOrder(request));
    }

    @PostMapping("/preview")
    public ApiResponse<OrderPreviewResponse> previewStaffOrder(
            @Valid @RequestBody @NonNull StaffCreateOrderRequest request) {
        return ApiResponse.success(orderService.previewStaffOrder(request));
    }

    @GetMapping
    public ApiResponse<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 15, sort = "createdAt", direction = Direction.DESC) @NonNull Pageable pageable) {
        return ApiResponse.success(orderService.getAllOrders(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(
            @PathVariable @NonNull Long id) {
        return ApiResponse.success(orderService.getOrderById(id));
    }

    @GetMapping("/history")
    public ApiResponse<Page<OrderResponse>> getOrderHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String tableNumber,
            @PageableDefault(size = 15, sort = "createdAt", direction = Direction.DESC) @NonNull Pageable pageable) {
        return ApiResponse.success(
                orderService.getOrderHistory(status, from, to, orderId, tableNumber, pageable));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> getOrderAnalytics(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String tableNumber) {
        return ApiResponse.success(
                orderService.getOrderAnalytics(status, from, to, orderId, tableNumber));
    }

    @GetMapping("/active")
    public ApiResponse<List<OrderResponse>> getActiveOrders() {
        return ApiResponse.success(orderService.getActiveOrders());
    }

    @GetMapping("/table-board")
    public ApiResponse<TableBoardResponse> getTableBoard() {
        return ApiResponse.success(orderService.getTableBoard());
    }

    @GetMapping("/table/{tableId}/current")
    public ApiResponse<OrderResponse> getCurrentOrderByTable(
            @PathVariable @NonNull Long tableId) {
        return orderService.getCurrentOrderByTable(tableId)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }

    @GetMapping("/table/{tableId}/preview")
    public ApiResponse<OrderPreviewResponse> getOrderPreviewByTableId(
            @PathVariable @NonNull Long tableId) {
        return ApiResponse.success(orderService.getOrderPreviewByTableId(tableId));
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable @NonNull Long orderId,
            @Valid @RequestBody @NonNull OrderStatusUpdateRequest request) {
        return ApiResponse.success(
                "Update order status successfully",
                orderService.updateStatus(orderId, request));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable @NonNull Long orderId) {
        return ApiResponse.success(
                "Cancel order successfully",
                orderService.cancelOrder(orderId));
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(
            @PathVariable @NonNull Long orderId) {
        orderService.deleteOrder(orderId);
        return ApiResponse.success("Delete order successfully", null);
    }
}
