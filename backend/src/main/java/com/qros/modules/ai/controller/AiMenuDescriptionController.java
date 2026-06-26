package com.qros.modules.ai.controller;

import com.qros.modules.ai.dto.request.AiMenuDescriptionRequest;
import com.qros.modules.ai.dto.response.AiMenuDescriptionResponse;
import com.qros.modules.ai.service.AiMenuDescriptionService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.AI + "/menu-item")
@RequiredArgsConstructor
public class AiMenuDescriptionController {

    private final AiMenuDescriptionService aiMenuDescriptionService;

    @PostMapping("/description")
    public ApiResponse<AiMenuDescriptionResponse> generateDescription(
            @Valid @RequestBody AiMenuDescriptionRequest request) {
        return ApiResponse.success(aiMenuDescriptionService.generate(request));
    }
}
