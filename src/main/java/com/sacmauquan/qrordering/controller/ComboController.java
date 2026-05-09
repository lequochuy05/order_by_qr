package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.ComboRequest;
import com.sacmauquan.qrordering.model.Combo;
import com.sacmauquan.qrordering.service.ComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * ComboController - Quản lý các gói combo món ăn.
 */
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    /**
     * Lấy toàn bộ danh sách combo (Admin)
     */
    @GetMapping
    public ApiResponse<List<Combo>> getAll() {
        return ApiResponse.success(comboService.getAll());
    }

    /**
     * Lấy danh sách combo đang kinh doanh (Sử dụng Cache)
     */
    @GetMapping("/active")
    public ApiResponse<List<Combo>> getAllActiveCombos() {
        return ApiResponse.success(comboService.getAllActive());
    }

    /**
     * Lấy chi tiết combo theo ID
     */
    @GetMapping("/{id}")
    public ApiResponse<Combo> getById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(comboService.getById(id));
    }

    /**
     * Tạo mới combo kèm danh sách món
     */
    @PostMapping
    public ApiResponse<Combo> create(@Valid @RequestBody @NonNull ComboRequest req) {
        return ApiResponse.success("Tạo combo thành công", comboService.create(req));
    }

    /**
     * Cập nhật thông tin combo
     */
    @PutMapping("/{id}")
    public ApiResponse<Combo> update(@PathVariable @NonNull Long id, @Valid @RequestBody @NonNull ComboRequest req) {
        return ApiResponse.success("Cập nhật combo thành công", comboService.update(id, req));
    }

    /**
     * Xóa combo
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        comboService.delete(id);
        return ApiResponse.success("Xóa combo thành công", null);
    }

    /**
     * Bật/Tắt trạng thái kinh doanh của combo
     */
    @PatchMapping("/{id}/toggle-active")
    public ApiResponse<Combo> toggleActive(@PathVariable @NonNull Long id) {
        return ApiResponse.success("Cập nhật trạng thái thành công", comboService.toggleActive(id));
    }
}
