package com.qros.modules.settings.controller;

import com.qros.shared.response.ApiResponse;
import com.qros.modules.settings.dto.SystemSettingsDto;
import com.qros.modules.settings.service.SystemSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingsController {
    private final SystemSettingsService systemSettingsService;

    @GetMapping
    public ApiResponse<SystemSettingsDto> getSettings(Authentication authentication) {
        boolean includeSensitive = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()));
        return ApiResponse.success(systemSettingsService.getCurrent(includeSensitive));
    }

    @PutMapping
    public ApiResponse<SystemSettingsDto> updateSettings(@Valid @RequestBody @NonNull SystemSettingsDto request) {
        return ApiResponse.success("Settings updated successfully", systemSettingsService.update(request));
    }
}
