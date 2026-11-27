package com.qros.modules.ai.controller;

import com.qros.modules.ai.dto.request.AiExecutiveSummaryRequest;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse;
import com.qros.modules.ai.service.AiExecutiveInsightsService;
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
@RequestMapping(ApiRoutes.AI + "/insights")
@RequiresPlanFeature(PlanFeature.AI_OPERATIONS_SUITE)
@RequiredArgsConstructor
public class AiInsightsController {

    private final AiExecutiveInsightsService aiExecutiveInsightsService;
    private final AiQuotaService aiQuotaService;

    @PostMapping("/summary")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<AiExecutiveSummaryResponse> summarize(
            @Valid @RequestBody(required = false) AiExecutiveSummaryRequest request, @NonNull Principal principal) {
        aiQuotaService.checkExecutiveInsights(principal.getName());
        return ApiResponse.success(aiExecutiveInsightsService.generateSummary(request));
    }
}
