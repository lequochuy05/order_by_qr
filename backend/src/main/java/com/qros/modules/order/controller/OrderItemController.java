package com.qros.modules.order.controller;

import com.qros.modules.order.dto.request.OrderItemStatusUpdateRequest;
import com.qros.modules.order.dto.request.OrderItemUpdateRequest;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/orders/items")
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @PatchMapping("/{itemId}")
    public ApiResponse<OrderResponse> updateOrderItem(
            @PathVariable @NonNull Long itemId,
            @Valid @RequestBody @NonNull OrderItemUpdateRequest request) {
        return ApiResponse.success(
                "Update order item successfully",
                orderService.updateOrderItem(itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> cancelOrderItem(
            @PathVariable @NonNull Long itemId) {
        orderService.cancelOrderItem(itemId);
        return ApiResponse.success("Cancel order item successfully", null);
    }

    @PatchMapping("/{itemId}/status")
    public ApiResponse<Void> updateItemStatus(
            @PathVariable @NonNull Long itemId,
            @Valid @RequestBody @NonNull OrderItemStatusUpdateRequest request,
            @NonNull Principal principal) {
        Long currentUserId = currentUserService.getCurrentUserId(principal.getName());
        orderService.updateItemStatus(itemId, request.status(), currentUserId);

        return ApiResponse.success("Update item status successfully", null);
    }

    @PatchMapping("/{itemId}/prepared")
    public ApiResponse<Void> markItemPrepared(
            @PathVariable @NonNull Long itemId,
            @NonNull Principal principal) {
        Long currentUserId = currentUserService.getCurrentUserId(principal.getName());
        orderService.markItemPrepared(itemId, currentUserId);
        return ApiResponse.success("Mark item prepared successfully", null);
    }
}
