package com.qros.modules.order.controller;

import com.qros.modules.order.dto.request.OrderItemUpdateRequest;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.constants.ApiRoutes;
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

@RestController
@RequestMapping(ApiRoutes.ORDER_ITEMS)
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderService orderService;

    @PatchMapping("/{itemId}")
    public ApiResponse<OrderResponse> updateOrderItem(
            @PathVariable @NonNull Long itemId, @Valid @RequestBody @NonNull OrderItemUpdateRequest request) {
        return ApiResponse.success("Update order item successfully", orderService.updateOrderItem(itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> cancelOrderItem(@PathVariable @NonNull Long itemId) {
        orderService.cancelOrderItem(itemId);
        return ApiResponse.success("Cancel order item successfully", null);
    }
}
