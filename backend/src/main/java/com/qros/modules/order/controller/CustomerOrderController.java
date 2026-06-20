package com.qros.modules.order.controller;

import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.PublicOrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/orders")
    public ApiResponse<OrderResponse> createCustomerOrder(
            @Valid @RequestBody @NonNull CustomerCreateOrderRequest request) {
        return ApiResponse.success("Order placed successfully", orderService.createCustomerOrder(request));
    }

    @PostMapping("/orders/preview")
    public ApiResponse<OrderPreviewResponse> previewCustomerOrder(
            @Valid @RequestBody @NonNull CustomerCreateOrderRequest request) {
        return ApiResponse.success(orderService.previewCustomerOrder(request));
    }

    @GetMapping("/tables/code/{tableCode}/current-order")
    public ResponseEntity<ApiResponse<PublicOrderResponse>> getCurrentOrder(
            @PathVariable @NonNull String tableCode, @RequestParam @NonNull String sessionToken) {
        ApiResponse<PublicOrderResponse> response = orderService
                .getPublicCurrentOrderBySession(tableCode, sessionToken)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
