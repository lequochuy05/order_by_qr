package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.AiChatRequest;
import com.sacmauquan.qrordering.dto.AiChatResponse;
import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AiAssistantController - Exposes the AI chat endpoint for customer-facing
 * food recommendation conversations.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    /**
     * Handles customer chat messages for AI-powered food recommendations.
     *
     * @param request The chat message and conversation history
     * @return AI-generated recommendation response
     */
    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.success(aiAssistantService.chat(request));
    }
}
