package com.qros.modules.ai.controller;

import com.qros.modules.ai.dto.request.AiStaffRequest;
import com.qros.modules.ai.dto.response.AiStaffResponse;
import com.qros.modules.ai.service.AiQuotaService;
import com.qros.modules.ai.service.AiStaffService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
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
@RequestMapping(ApiRoutes.AI + "/staff")
@RequiredArgsConstructor
public class AiStaffController {

    private final AiStaffService aiStaffService;
    private final AiQuotaService aiQuotaService;

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF','CHEF')")
    public ApiResponse<AiStaffResponse> query(
            @Valid @RequestBody AiStaffRequest request, @NonNull Principal principal) {
        aiQuotaService.checkStaffAssistant(principal.getName());
        return ApiResponse.success(aiStaffService.query(request));
    }
}
