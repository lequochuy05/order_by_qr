package com.qros.modules.payment.controller;

import com.qros.modules.order.service.OrderService;
import com.qros.modules.payment.dto.response.PaymentOrderResponse;
import com.qros.modules.payment.mapper.PaymentMapper;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.ORDER_BY_ID)
@RequiredArgsConstructor
public class PaymentOrderController {

    private final OrderService orderService;
    private final PaymentMapper paymentMapper;

    @PostMapping("/reconcile")
    public ApiResponse<PaymentOrderResponse> reconcileOrder(@PathVariable @NonNull Long orderId) {
        return ApiResponse.success(
                "Reconcile successfully", paymentMapper.toOrderResponse(orderService.reconcileOrder(orderId)));
    }
}
