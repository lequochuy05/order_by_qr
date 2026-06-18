package com.qros.modules.user.controller;

import com.qros.modules.user.dto.request.*;
import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.modules.user.service.UserAvatarService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * CurrentUserController - Handles current user profile management.
 */
@RestController
@RequestMapping(ApiRoutes.USERS)
@RequiredArgsConstructor
public class CurrentUserController {

    private final CurrentUserService currentUserService;
    private final UserAvatarService userAvatarService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentProfile(@NonNull Principal principal) {
        return ApiResponse.success(currentUserService.getCurrentProfile(principal.getName()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateCurrentProfile(
            @NonNull Principal principal, @Valid @RequestBody @NonNull ProfileUpdateRequest req) {
        return ApiResponse.success(
                "Profile updated successfully", currentUserService.updateCurrentProfile(principal.getName(), req));
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> changeCurrentPassword(
            @NonNull Principal principal, @Valid @RequestBody @NonNull PasswordChangeRequest req) {
        currentUserService.changeCurrentPassword(principal.getName(), req);
        return ApiResponse.success("Password changed successfully", null);
    }

    @PostMapping("/me/avatar")
    public ApiResponse<UserResponse> uploadCurrentAvatar(
            @NonNull Principal principal, @RequestParam("file") @NonNull MultipartFile file) {
        UserResponse updated = userAvatarService.uploadCurrentAvatar(principal.getName(), file);
        return ApiResponse.success("Avatar updated successfully", updated);
    }
}
