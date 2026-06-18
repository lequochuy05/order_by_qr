package com.qros.modules.settings.controller;

import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.PUBLIC_SETTINGS)
@RequiredArgsConstructor
public class PublicSettingsController {

    private final SystemSettingsService settingsService;

    @GetMapping
    public ApiResponse<PublicSettingsResponse> getPublicSettings() {
        return ApiResponse.success(settingsService.getPublicSettings());
    }
}
