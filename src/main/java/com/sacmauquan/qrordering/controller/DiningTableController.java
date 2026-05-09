package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.DiningTableRequest;
import com.sacmauquan.qrordering.dto.DiningTableResponse;
import com.sacmauquan.qrordering.service.DiningTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DiningTableController - Quản lý bàn ăn và QR Code.
 */
@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class DiningTableController {

    private final DiningTableService tableService;

    /**
     * Lấy danh sách bàn 
     */
    @GetMapping
    public ApiResponse<List<DiningTableResponse>> getAllTables() {
        return ApiResponse.success(tableService.getAllTablesSorted());
    }

    /**
     * Lấy thông tin chi tiết bàn theo ID
     */
    @GetMapping("/{id}")
    public ApiResponse<DiningTableResponse> getTableById(@PathVariable @NonNull Long id) {
        // Sử dụng service để lấy dữ liệu đã map sẵn sang Response
        return ApiResponse.success(tableService.getByIdResponse(id));
    }

    /**
     * Khách quét mã QR: Truy vấn thông tin bàn từ tableCode
     */
    @GetMapping("/code/{tableCode}")
    public ApiResponse<DiningTableResponse> getTableByCode(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableService.getByTableCode(tableCode));
    }

    /**
     * Tạo bàn mới: Tự động tạo mã QR và lưu trữ Cloudinary trong Service
     */
    @PostMapping
    public ApiResponse<DiningTableResponse> createTable(@Valid @RequestBody @NonNull DiningTableRequest request) {
        return ApiResponse.success("Tạo bàn mới thành công", tableService.create(request));
    }

    /**
     * Cập nhật trạng thái hoặc thông tin bàn
     */
    @PatchMapping("/{id}")
    public ApiResponse<DiningTableResponse> updateTable(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull DiningTableRequest request) {
        return ApiResponse.success("Cập nhật bàn thành công", tableService.update(id, request));
    }

    /**
     * Xóa bàn và dọn dẹp ảnh QR liên quan
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTable(@PathVariable @NonNull Long id) {
        tableService.delete(id);
        return ApiResponse.success("Xóa bàn thành công", null);
    }
}
