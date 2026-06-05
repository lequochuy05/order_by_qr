package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

/**
 * UserController - Handles user authentication, profile management, and staff
 * administration.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Retrieves the authenticated user's own profile.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentProfile(@NonNull Principal principal) {
        return ApiResponse.success(userService.getCurrentProfile(principal.getName()));
    }

    /**
     * Updates the authenticated user's editable profile fields.
     */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateCurrentProfile(
            @NonNull Principal principal,
            @Valid @RequestBody @NonNull ProfileUpdateRequest req) {
        return ApiResponse.success("Profile updated successfully",
                userService.updateCurrentProfile(principal.getName(), Objects.requireNonNull(req)));
    }

    /**
     * Changes the authenticated user's own password.
     */
    @PatchMapping("/me/password")
    public ApiResponse<Void> changeCurrentPassword(
            @NonNull Principal principal,
            @Valid @RequestBody @NonNull PasswordChangeRequest req) {
        userService.changeCurrentPassword(principal.getName(), Objects.requireNonNull(req));
        return ApiResponse.success("Password changed successfully", null);
    }

    /**
     * Updates the authenticated user's own avatar.
     */
    @PostMapping("/me/avatar")
    public ApiResponse<UserResponse> uploadCurrentAvatar(
            @NonNull Principal principal,
            @RequestParam("file") @NonNull MultipartFile file) {
        UserResponse updated = userService.uploadCurrentAvatar(principal.getName(), Objects.requireNonNull(file));
        return ApiResponse.success("Avatar updated successfully", updated);
    }

    /**
     * Retrieves a list of all staff members (Managers, Staff, Chefs).
     * 
     * @return List of UserResponse objects
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.success(userService.findAll());
    }

    /**
     * Retrieves detailed information of a specific user by ID.
     * 
     * @param id User ID
     * @return UserResponse object
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getOne(@PathVariable @NonNull Long id) {
        return ApiResponse.success(userService.getOne(id));
    }

    /**
     * Creates a new staff account.
     * 
     * @param req User data for creation
     * @return Created UserResponse object
     */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserUpsertRequest req) {
        return ApiResponse.success("Staff account created successfully",
                userService.create(Objects.requireNonNull(req)));
    }

    /**
     * Updates personal information or role of a staff member.
     * 
     * @param id  User ID to update
     * @param req Updated user data
     * @return Updated UserResponse object
     */
    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody UserUpsertRequest req) {
        return ApiResponse.success("Profile updated successfully",
                userService.update(id, Objects.requireNonNull(req)));
    }

    /**
     * Resets the password for a staff member.
     * Password should be provided in the request body for security.
     * 
     * @param id   User ID to reset password
     * @param body Request body containing the new password
     * @return Void success response
     */
    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull AuthRequest body) {
        userService.resetPassword(id, Objects.requireNonNull(body.getPassword()));
        return ApiResponse.success("Password reset successfully", null);
    }

    /**
     * Deletes a staff account from the system using soft delete.
     * 
     * @param id User ID to delete
     * @return Void success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        userService.delete(id);
        return ApiResponse.success("Staff member removed from system", null);
    }

    /**
     * Updates the avatar image of a staff member.
     * 
     * @param id   User ID
     * @param file Image file to upload
     * @return Updated UserResponse object
     */
    @PostMapping("/{id}/avatar")
    public ApiResponse<UserResponse> uploadAvatar(
            @PathVariable @NonNull Long id,
            @RequestParam("file") @NonNull MultipartFile file) {
        UserResponse updated = userService.uploadAvatar(id, Objects.requireNonNull(file));
        return ApiResponse.success("Avatar updated successfully", updated);
    }

}
