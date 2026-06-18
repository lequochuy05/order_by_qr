package com.qros.modules.order.controller;

import com.qros.modules.order.dto.request.OrderStatusUpdateRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * StaffOrderController - Staff/Admin order-level APIs.
 */
@RestController
@RequestMapping(ApiRoutes.ORDERS)
@RequiredArgsConstructor
public class StaffOrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> createStaffOrder(@Valid @RequestBody @NonNull StaffCreateOrderRequest request) {
        return ApiResponse.success("Đặt món thành công", orderService.createStaffOrder(request));
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
    public ApiResponse<OrderResponse> getOrderById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(orderService.getOrderById(id));
    }

    @GetMapping("/active")
    public ApiResponse<List<OrderResponse>> getActiveOrders() {
        return ApiResponse.success(orderService.getActiveOrders());
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable @NonNull Long orderId, @Valid @RequestBody @NonNull OrderStatusUpdateRequest request) {
        return ApiResponse.success("Update order status successfully", orderService.updateStatus(orderId, request));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable @NonNull Long orderId) {
        return ApiResponse.success("Cancel order successfully", orderService.cancelOrder(orderId));
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(@PathVariable @NonNull Long orderId) {
        orderService.deleteOrder(orderId);
        return ApiResponse.success("Delete order successfully", null);
    }
}
