package com.qros.modules.user.controller;

import com.qros.modules.user.dto.request.*;
import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.service.UserAvatarService;
import com.qros.modules.user.service.UserQueryService;
import com.qros.modules.user.service.UserService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * UserAdminController - Handles user account management.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;
    private final UserQueryService userQueryService;
    private final UserAvatarService userAvatarService;

    @GetMapping
    public ApiResponse<Page<UserResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable) {
        return ApiResponse.success(userQueryService.search(q, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getOne(@PathVariable @NonNull Long id) {
        return ApiResponse.success(userQueryService.getOne(id));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @NonNull @RequestBody CreateUserRequest req) {
        return ApiResponse.success("Staff account created successfully", userService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable @NonNull Long id, @Valid @NonNull @RequestBody UpdateUserRequest req) {
        return ApiResponse.success("Profile updated successfully", userService.update(id, req));
    }

    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable @NonNull Long id, @Valid @NonNull @RequestBody StaffResetPasswordRequest req) {
        userService.resetPassword(id, req.newPassword());
        return ApiResponse.success("Password reset successfully", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        userService.delete(id);
        return ApiResponse.success("Staff member removed from system", null);
    }

    @PostMapping("/{id}/avatar")
    public ApiResponse<UserResponse> uploadAvatar(
            @PathVariable @NonNull Long id, @RequestParam("file") @NonNull MultipartFile file) {
        UserResponse updated = userAvatarService.uploadAvatar(id, file);
        return ApiResponse.success("Avatar updated successfully", updated);
    }
}
