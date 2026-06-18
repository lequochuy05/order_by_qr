package com.qros.modules.order.controller;

import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.TableBoardResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.ORDERS)
@RequiredArgsConstructor
public class TableOrderController {

    private final OrderService orderService;

    @GetMapping("/table-board")
    public ApiResponse<TableBoardResponse> getTableBoard() {
        return ApiResponse.success(orderService.getTableBoard());
    }

    @GetMapping("/table/{tableId}/current")
    public ApiResponse<OrderResponse> getCurrentOrderByTable(@PathVariable @NonNull Long tableId) {
        return orderService
                .getCurrentOrderByTable(tableId)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }

    @GetMapping("/table/{tableId}/preview")
    public ApiResponse<OrderPreviewResponse> getOrderPreviewByTableId(@PathVariable @NonNull Long tableId) {
        return ApiResponse.success(orderService.getOrderPreviewByTableId(tableId));
    }
}
