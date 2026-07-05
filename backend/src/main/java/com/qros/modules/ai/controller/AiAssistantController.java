package com.qros.modules.ai.controller;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.response.AiChatResponse;
import com.qros.modules.ai.service.AiAssistantService;
import com.qros.modules.ai.service.AiQuotaService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.PUBLIC + "/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final AiQuotaService aiQuotaService;

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        aiQuotaService.checkPublicChat(httpRequest);
        return ApiResponse.success(aiAssistantService.chat(request));
    }
}
