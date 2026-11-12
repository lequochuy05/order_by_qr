package com.qros.modules.settings.controller;

import com.qros.modules.settings.dto.request.SystemSettingsUpdateRequest;
import com.qros.modules.settings.dto.response.SystemSettingsResponse;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiRoutes.SETTINGS)
public class SystemSettingsController {

    private final SystemSettingsService settingsService;

    public SystemSettingsController(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER','STAFF','CHEF')")
    public ApiResponse<SystemSettingsResponse> getSettings() {
        return ApiResponse.success(settingsService.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SystemSettingsResponse> updateSettings(
            @Valid @RequestBody SystemSettingsUpdateRequest request, @NonNull Principal principal) {
        return ApiResponse.success(
                "System settings updated successfully", settingsService.updateSettings(request, principal.getName()));
    }

    @PostMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SystemSettingsResponse> uploadLogo(
            @RequestParam("file") @NonNull MultipartFile file, @NonNull Principal principal) {
        return ApiResponse.success(
                "Restaurant logo uploaded successfully", settingsService.uploadLogo(file, principal.getName()));
    }
}
