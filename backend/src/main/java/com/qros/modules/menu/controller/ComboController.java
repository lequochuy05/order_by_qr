package com.qros.modules.menu.controller;

import com.qros.modules.menu.dto.request.ComboRequest;
import com.qros.modules.menu.dto.response.ComboResponse;
import com.qros.modules.menu.service.ComboService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.COMBOS)
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    public ApiResponse<Page<ComboResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(
                            size = 24,
                            sort = {"displayOrder", "name"},
                            direction = Direction.ASC)
                    Pageable pageable) {
        return ApiResponse.success(comboService.searchSummaries(q, active, pageable));
    }

    @GetMapping("/active")
    public ApiResponse<List<ComboResponse>> getAllActive() {
        return ApiResponse.success(comboService.getAllActive());
    }

    @GetMapping("/{id}")
    public ApiResponse<ComboResponse> getById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(comboService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ComboResponse>> create(@Valid @RequestBody @NonNull ComboRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(), "Combo created successfully", comboService.create(req)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ComboResponse> update(
            @PathVariable @NonNull Long id, @Valid @RequestBody @NonNull ComboRequest req) {
        return ApiResponse.success("Combo updated successfully", comboService.update(id, req));
    }

    @PatchMapping("/{id}/toggle-active")
    public ApiResponse<ComboResponse> toggleActive(
            @PathVariable @NonNull Long id, @RequestParam @NonNull Long version) {
        return ApiResponse.success("Combo active status toggled successfully", comboService.toggleActive(id, version));
    }

    @PatchMapping("/{id}/toggle-available")
    public ApiResponse<ComboResponse> toggleAvailable(
            @PathVariable @NonNull Long id, @RequestParam @NonNull Long version) {
        return ApiResponse.success(
                "Combo availability toggled successfully", comboService.toggleAvailable(id, version));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        comboService.delete(id);
        return ApiResponse.success("Combo deleted successfully", null);
    }
}
