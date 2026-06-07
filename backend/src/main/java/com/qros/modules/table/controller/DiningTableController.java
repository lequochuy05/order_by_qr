package com.qros.modules.table.controller;

import com.qros.shared.response.ApiResponse;
import com.qros.modules.table.dto.DiningTableRequest;
import com.qros.modules.table.dto.DiningTableResponse;
import com.qros.modules.table.service.DiningTableService;
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

    /**
     * Retrieves a list of all dining tables, sorted accordingly.
     * 
     * @return List of DiningTableResponse objects
     */
    @GetMapping
    public ApiResponse<List<DiningTableResponse>> getAllTables() {
        return ApiResponse.success(tableService.getAllTablesSorted());
    }

    /**
     * Retrieves detailed information of a table by its ID.
     * 
     * @param id Table ID
     * @return Found DiningTableResponse object
     */
    @GetMapping("/{id}")
    public ApiResponse<DiningTableResponse> getTableById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(tableService.getByIdResponse(id));
    }

    /**
     * Handles customer QR scan: Retrieves table information based on the table
     * code.
     * 
     * @param tableCode Unique identifier encoded in the QR code
     * @return DiningTableResponse object
     */
    @GetMapping("/code/{tableCode}")
    public ApiResponse<DiningTableResponse> getTableByCode(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableService.getByTableCode(tableCode));
    }

    /**
     * Creates a new dining table. QR code generation and Cloudinary storage are
     * handled automatically.
     * 
     * @param request Data for the new table
     * @return Created DiningTableResponse object
     */
    @PostMapping
    public ApiResponse<DiningTableResponse> createTable(@Valid @RequestBody @NonNull DiningTableRequest request) {
        return ApiResponse.success("Table created successfully", tableService.create(request));
    }

    /**
     * Updates the status or information of an existing table.
     * 
     * @param id      Table ID to update
     * @param request Updated table data
     * @return Updated DiningTableResponse object
     */
    @PutMapping("/{id}")
    public ApiResponse<DiningTableResponse> updateTable(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull DiningTableRequest request) {
        return ApiResponse.success("Table updated successfully", tableService.update(id, request));
    }

    /**
     * Deletes a dining table and cleans up its associated QR code image.
     * 
     * @param id Table ID to delete
     * @return Void success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTable(@PathVariable @NonNull Long id) {
        tableService.delete(id);
        return ApiResponse.success("Table deleted successfully", null);
    }

    /**
     * Regenerates the QR code for an existing table.
     * 
     * @param id Table ID
     * @return Updated DiningTableResponse object
     */
    @PostMapping("/{id}/regenerate-qr")
    public ApiResponse<DiningTableResponse> regenerateQrCode(@PathVariable @NonNull Long id) {
        return ApiResponse.success("QR Code regenerated successfully", tableService.regenerateQrCode(id));
    }
}
