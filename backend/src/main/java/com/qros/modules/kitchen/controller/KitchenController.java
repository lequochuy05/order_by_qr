package com.qros.modules.kitchen.controller;

import com.qros.modules.kitchen.dto.KitchenOrderDto;
import com.qros.modules.kitchen.dto.KitchenMarkPreparedRequest;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.qros.modules.kitchen.dto.KitchenItemStatusRequest;
import jakarta.validation.Valid;
import java.util.Objects;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenService kitchenService;

    @GetMapping("/orders")
    public ApiResponse<List<KitchenOrderDto>> getKitchenOrders() {
        return ApiResponse.success(kitchenService.getKitchenOrders());
    }

    @PatchMapping("/items/{itemId}/prepared")
    public ApiResponse<Void> markItemPrepared(
            @PathVariable @NonNull Long itemId,
            @RequestBody(required = false) KitchenMarkPreparedRequest body) {
        kitchenService.markItemPrepared(itemId, body != null ? body.getUserId() : null);
        return ApiResponse.success("Mark item as prepared successfully", null);
    }

    @PatchMapping("/items/{itemId}/status")
    public ApiResponse<Void> updateItemStatus(
            @PathVariable @NonNull Long itemId,
            @Valid @RequestBody @NonNull KitchenItemStatusRequest body) {
        String status = body.getStatus();
        kitchenService.updateItemStatus(itemId, Objects.requireNonNull(status), body.getUserId());
        return ApiResponse.success("Update item status successfully", null);
    }
}
