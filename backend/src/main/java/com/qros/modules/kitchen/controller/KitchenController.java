package com.qros.modules.kitchen.controller;

import com.qros.modules.kitchen.dto.request.KitchenItemStatusRequest;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiRoutes.KITCHEN)
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenService kitchenService;
    private final CurrentUserService currentUserService;

    @GetMapping("/orders")
    public ApiResponse<List<KitchenOrderResponse>> getKitchenOrders() {
        return ApiResponse.success(kitchenService.getKitchenOrders());
    }

    @PatchMapping("/items/{itemId}/prepared")
    public ApiResponse<Void> markItemPrepared(
            @PathVariable @Positive(message = "Item ID must be positive") Long itemId, Principal principal) {
        Long currentUserId = currentUserService.getCurrentUserId(principal.getName());
        kitchenService.markItemPrepared(itemId, currentUserId);
        return ApiResponse.success("Mark item as prepared successfully", null);
    }

    @PatchMapping("/items/{itemId}/status")
    public ApiResponse<Void> updateItemStatus(
            @PathVariable @Positive(message = "Item ID must be positive") Long itemId,
            @Valid @RequestBody KitchenItemStatusRequest body,
            Principal principal) {
        Long currentUserId = currentUserService.getCurrentUserId(principal.getName());
        kitchenService.updateItemStatus(itemId, body.status(), currentUserId);
        return ApiResponse.success("Update item status successfully", null);
    }
}
