package com.qros.modules.promotion.controller;

import com.qros.modules.promotion.dto.request.PromotionRequest;
import com.qros.modules.promotion.dto.response.PromotionResponse;
import com.qros.modules.promotion.service.PromotionService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ApiResponse<Page<PromotionResponse>> search(@RequestParam(required = false) String q, Pageable pageable) {
        return ApiResponse.success(promotionService.searchForManagement(q, pageable));
    }

    @GetMapping("/management-summary")
    public ApiResponse<Page<PromotionResponse>> managementSummary(
            @RequestParam(required = false) String q, Pageable pageable) {
        return ApiResponse.success(promotionService.searchForManagement(q, pageable));
    }

    @GetMapping("/active")
    public ApiResponse<List<PromotionResponse>> active() {
        return ApiResponse.success(promotionService.findActivePromotionResponses());
    }

    @GetMapping("/{id}")
    public ApiResponse<PromotionResponse> get(@PathVariable Long id) {
        return ApiResponse.success(promotionService.findById(id));
    }

    @PostMapping
    public ApiResponse<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        return ApiResponse.success("Promotion created successfully", promotionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromotionResponse> update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.success("Promotion updated successfully", promotionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ApiResponse.success("Promotion deleted successfully", null);
    }
}
