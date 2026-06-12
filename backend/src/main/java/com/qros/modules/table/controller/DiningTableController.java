package com.qros.modules.table.controller;

import com.qros.modules.table.dto.request.CreateDiningTableRequest;
import com.qros.modules.table.dto.request.UpdateDiningTableRequest;
import com.qros.modules.table.dto.request.UpdateTableStatusRequest;
import com.qros.modules.table.dto.response.DiningTableResponse;
import com.qros.modules.table.service.DiningTableService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DiningTableController - Manages dining tables and their associated QR codes.
 */
@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class DiningTableController {

    private final DiningTableService tableService;

    @GetMapping
    public ApiResponse<List<DiningTableResponse>> getAll() {
        return ApiResponse.success(tableService.getAllSorted());
    }

    @GetMapping("/{id}")
    public ApiResponse<DiningTableResponse> getById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(tableService.getById(id));
    }

    @GetMapping("/code/{tableCode}")
    public ApiResponse<DiningTableResponse> getByCode(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableService.getByCode(tableCode));
    }

    @PostMapping
    public ApiResponse<DiningTableResponse> create(@Valid @RequestBody @NonNull CreateDiningTableRequest req) {
        return ApiResponse.success("Table created successfully", tableService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<DiningTableResponse> update(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull UpdateDiningTableRequest req) {
        return ApiResponse.success("Table updated successfully", tableService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<DiningTableResponse> updateStatus(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull UpdateTableStatusRequest req) {
        return ApiResponse.success("Table status updated successfully", tableService.updateStatus(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        tableService.delete(id);
        return ApiResponse.success("Table deleted successfully", null);
    }

    @PostMapping("/{id}/regenerate-qr")
    public ApiResponse<DiningTableResponse> regenerateQrCode(@PathVariable @NonNull Long id) {
        return ApiResponse.success("QR Code regenerated successfully", tableService.regenerateQrCode(id));
    }
}
