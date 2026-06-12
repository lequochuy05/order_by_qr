package com.qros.modules.menu.controller;

import com.qros.modules.menu.dto.request.ComboRequest;
import com.qros.modules.menu.dto.response.ComboResponse;
import com.qros.modules.menu.service.ComboService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    public ApiResponse<List<ComboResponse>> getAll() {
        return ApiResponse.success(comboService.getAll());
    }

    @GetMapping("/active")
    public ApiResponse<List<ComboResponse>> getAllActive() {
        return ApiResponse.success(comboService.getAllActive());
    }

    @GetMapping("/{id}")
    public ApiResponse<ComboResponse> getById(
            @PathVariable @NonNull Long id
    ) {
        return ApiResponse.success(comboService.getById(id));
    }

    @PostMapping
    public ApiResponse<ComboResponse> create(
            @Valid @RequestBody @NonNull ComboRequest req
    ) {
        return ApiResponse.success("Combo created successfully", comboService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ComboResponse> update(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull ComboRequest req
    ) {
        return ApiResponse.success("Combo updated successfully", comboService.update(id, req));
    }

    @PatchMapping("/{id}/toggle-active")
    public ApiResponse<ComboResponse> toggleActive(
            @PathVariable @NonNull Long id
    ) {
        return ApiResponse.success("Combo active status toggled successfully", comboService.toggleActive(id));
    }

    @PatchMapping("/{id}/toggle-available")
    public ApiResponse<ComboResponse> toggleAvailable(
            @PathVariable @NonNull Long id
    ) {
        return ApiResponse.success("Combo availability toggled successfully", comboService.toggleAvailable(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable @NonNull Long id
    ) {
        comboService.delete(id);
        return ApiResponse.success("Combo deleted successfully", null);
    }
}