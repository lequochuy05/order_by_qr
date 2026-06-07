package com.qros.modules.order.controller;

import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.order.dto.OrderRequest;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody @NonNull OrderRequest orderRequest) {
        return ApiResponse.success("Đặt món thành công", orderService.createOrder(orderRequest));
    }

    @GetMapping("/tables/{tableId}/current-order")
    public ApiResponse<PublicMenuResponse.Order> getCurrentOrder(@PathVariable @NonNull Long tableId) {
        return orderService.getPublicCurrentOrderByTable(tableId)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }
}
