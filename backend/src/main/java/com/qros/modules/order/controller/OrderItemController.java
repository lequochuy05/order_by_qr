package com.qros.modules.order.controller;

import com.qros.modules.order.dto.request.OrderItemUpdateRequest;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.ORDER_ITEMS)
public class OrderItemController {

    private final OrderService orderService;

    public OrderItemController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PatchMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ApiResponse<OrderResponse> updateOrderItem(
            @PathVariable @NonNull Long itemId, @Valid @RequestBody @NonNull OrderItemUpdateRequest request) {
        return ApiResponse.success("Update order item successfully", orderService.updateOrderItem(itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'CHEF')")
    public ApiResponse<Void> cancelOrderItem(
            @PathVariable @NonNull Long itemId,
            @RequestParam(value = "reason", required = false) String reason,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : null;
        orderService.cancelOrderItem(itemId, actor, reason);
        return ApiResponse.success("Cancel order item successfully", null);
    }
}
