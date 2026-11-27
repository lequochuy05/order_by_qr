package com.qros.modules.ai.controller;

import com.qros.modules.ai.dto.request.AiRecipeAssistantRequest;
import com.qros.modules.ai.dto.request.AiRestockSuggestionRequest;
import com.qros.modules.ai.dto.response.AiInvoiceOcrResponse;
import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse;
import com.qros.modules.ai.dto.response.AiRestockSuggestionResponse;
import com.qros.modules.ai.service.AiInventoryRestockService;
import com.qros.modules.ai.service.AiInvoiceOcrService;
import com.qros.modules.ai.service.AiQuotaService;
import com.qros.modules.ai.service.AiRecipeAssistantService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.subscription.PlanFeature;
import com.qros.shared.subscription.RequiresPlanFeature;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiRoutes.AI + "/inventory")
@RequiresPlanFeature(PlanFeature.AI_OPERATIONS_SUITE)
@RequiredArgsConstructor
public class AiInventoryController {

    private final AiInventoryRestockService aiInventoryRestockService;
    private final AiRecipeAssistantService aiRecipeAssistantService;
    private final AiInvoiceOcrService aiInvoiceOcrService;
    private final AiQuotaService aiQuotaService;

    @PostMapping("/restock-suggestions")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<AiRestockSuggestionResponse> suggestRestock(
            @Valid @RequestBody(required = false) AiRestockSuggestionRequest request, @NonNull Principal principal) {
        aiQuotaService.checkOperationsAi(principal.getName());
        return ApiResponse.success(aiInventoryRestockService.suggestRestock(request));
    }

    @PostMapping("/recipe-assistant")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<AiRecipeAssistantResponse> assistRecipe(
            @Valid @RequestBody AiRecipeAssistantRequest request, @NonNull Principal principal) {
        aiQuotaService.checkOperationsAi(principal.getName());
        return ApiResponse.success(aiRecipeAssistantService.assistRecipe(request));
    }

    @PostMapping(value = "/invoice-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<AiInvoiceOcrResponse> scanInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false, defaultValue = "vi") String language,
            @NonNull Principal principal) {
        return ApiResponse.success(aiInvoiceOcrService.parseInvoice(
                file, language, () -> aiQuotaService.checkOperationsAi(principal.getName())));
    }
}
