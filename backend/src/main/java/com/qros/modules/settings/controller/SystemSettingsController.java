package com.qros.modules.settings.controller;

import com.qros.modules.settings.dto.request.SystemSettingsUpdateRequest;
import com.qros.modules.settings.dto.response.SystemSettingsResponse;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsService settingsService;

    @GetMapping
    public ApiResponse<SystemSettingsResponse> getSettings() {
        return ApiResponse.success(settingsService.getSettings());
    }

    @PutMapping
    public ApiResponse<SystemSettingsResponse> updateSettings(
            @Valid @RequestBody SystemSettingsUpdateRequest request) {
        return ApiResponse.success(
                "System settings updated successfully",
                settingsService.updateSettings(request));
    }
}