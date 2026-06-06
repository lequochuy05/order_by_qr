package com.qros.modules.menu.controller;

import com.qros.shared.response.ApiResponse;
import com.qros.modules.menu.dto.ComboRequest;
import com.qros.modules.menu.dto.ComboResponse;
import com.qros.modules.menu.service.ComboService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * ComboController - Manages food and beverage combo packages.
 */
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    /**
     * Retrieves all combos in the system (Admin only).
     * 
     * @return List of all ComboResponse objects
     */
    @GetMapping
    public ApiResponse<List<ComboResponse>> getAll() {
        return ApiResponse.success(comboService.getAll());
    }

    /**
     * Retrieves a list of all active combos currently being sold (Cached).
     * 
     * @return List of active ComboResponse objects
     */
    @GetMapping("/active")
    public ApiResponse<List<ComboResponse>> getAllActiveCombos() {
        return ApiResponse.success(comboService.getAllActive());
    }

    /**
     * Retrieves detailed information of a specific combo by its ID.
     * 
     * @param id Combo ID
     * @return Found ComboResponse object
     */
    @GetMapping("/{id}")
    public ApiResponse<ComboResponse> getById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(comboService.getById(id));
    }

    /**
     * Creates a new combo with a list of associated items.
     * 
     * @param req Combo data for creation
     * @return Created ComboResponse object
     */
    @PostMapping
    public ApiResponse<ComboResponse> create(@Valid @RequestBody @NonNull ComboRequest req) {
        return ApiResponse.success("Combo created successfully", comboService.create(req));
    }

    /**
     * Updates an existing combo's information.
     * 
     * @param id  Combo ID
     * @param req Updated combo data
     * @return Updated ComboResponse object
     */
    @PutMapping("/{id}")
    public ApiResponse<ComboResponse> update(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull ComboRequest req) {
        return ApiResponse.success("Combo updated successfully", comboService.update(id, req));
    }

    /**
     * Deletes a combo from the system.
     * 
     * @param id Combo ID to delete
     * @return Void success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        comboService.delete(id);
        return ApiResponse.success("Combo deleted successfully", null);
    }

    /**
     * Toggles the active status of a combo (Enable/Disable for sale).
     * 
     * @param id Combo ID to toggle
     * @return Updated ComboResponse object
     */
    @PatchMapping("/{id}/toggle-active")
    public ApiResponse<ComboResponse> toggleActive(@PathVariable @NonNull Long id) {
        return ApiResponse.success("Combo status toggled successfully", comboService.toggleActive(id));
    }
}
