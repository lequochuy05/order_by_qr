package com.qros.modules.order.controller;

import com.qros.modules.order.dto.request.OrderPayRequest;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderPaymentController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @PostMapping("/{orderId}/pay")
    public ApiResponse<String> payOrder(
            @PathVariable @NonNull Long orderId,
            @Valid @RequestBody @NonNull OrderPayRequest request,
            @NonNull Principal principal) {
        Long currentUserId = currentUserService.getCurrentUserId(principal.getName());
        return ApiResponse.success(
                "Pay order successfully",
                orderService.payOrder(orderId, currentUserId, request));
    }

    @PostMapping("/{orderId}/confirm-paid")
    public ApiResponse<OrderResponse> confirmPaid(
            @PathVariable @NonNull Long orderId) {
        return ApiResponse.success(
                "Confirm paid successfully",
                orderService.confirmPaid(orderId));
    }

    @PostMapping("/{orderId}/reconcile")
    public ApiResponse<OrderResponse> reconcileOrder(
            @PathVariable @NonNull Long orderId) {
        return ApiResponse.success(
                "Reconcile successfully",
                orderService.reconcileOrder(orderId));
    }
}
