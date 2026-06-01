package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.SystemSettingsDto;
import com.sacmauquan.qrordering.service.SystemSettingsService;
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
