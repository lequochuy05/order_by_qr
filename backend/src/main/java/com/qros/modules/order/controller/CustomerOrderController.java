package com.qros.modules.order.controller;

import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.PublicOrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.idempotency.IdempotencyService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.PUBLIC)
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/orders")
    public ApiResponse<OrderResponse> createCustomerOrder(
            @Valid @RequestBody @NonNull CustomerCreateOrderRequest request) {
        idempotencyService.requireNew(
                "public-order:" + request.tableCode() + ":" + request.sessionToken(), request.clientRequestId());

        return ApiResponse.success("Đặt món thành công", orderService.createCustomerOrder(request));
    }

    @PostMapping("/orders/preview")
    public ApiResponse<OrderPreviewResponse> previewCustomerOrder(
            @Valid @RequestBody @NonNull CustomerCreateOrderRequest request) {
        return ApiResponse.success(orderService.previewCustomerOrder(request));
    }

    @GetMapping("/tables/code/{tableCode}/current-order")
    public ApiResponse<PublicOrderResponse> getCurrentOrder(
            @PathVariable @NonNull String tableCode, @RequestParam @NonNull String sessionToken) {
        return orderService
                .getPublicCurrentOrderBySession(tableCode, sessionToken)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }
}
