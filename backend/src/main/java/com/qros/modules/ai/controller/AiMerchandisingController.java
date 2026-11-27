package com.qros.modules.ai.controller;

import com.qros.modules.ai.dto.request.AiMerchandisingRequest;
import com.qros.modules.ai.dto.response.AiComboSuggestionsResponse;
import com.qros.modules.ai.dto.response.AiPromotionSuggestionsResponse;
import com.qros.modules.ai.service.AiMerchandisingService;
import com.qros.modules.ai.service.AiQuotaService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.subscription.PlanFeature;
import com.qros.shared.subscription.RequiresPlanFeature;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.AI + "/merchandising")
@RequiresPlanFeature(PlanFeature.AI_OPERATIONS_SUITE)
@RequiredArgsConstructor
public class AiMerchandisingController {

    private final AiMerchandisingService aiMerchandisingService;
    private final AiQuotaService aiQuotaService;

    @PostMapping("/combo-suggestions")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<AiComboSuggestionsResponse> suggestCombos(
            @Valid @RequestBody(required = false) AiMerchandisingRequest request, @NonNull Principal principal) {
        aiQuotaService.checkOperationsAi(principal.getName());
        return ApiResponse.success(aiMerchandisingService.suggestCombos(request));
    }

    @PostMapping("/promotion-suggestions")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<AiPromotionSuggestionsResponse> suggestPromotions(
            @Valid @RequestBody(required = false) AiMerchandisingRequest request, @NonNull Principal principal) {
        aiQuotaService.checkOperationsAi(principal.getName());
        return ApiResponse.success(aiMerchandisingService.suggestPromotions(request));
    }
}
