package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;

/**
 * UserController - Quản lý nhân sự, xác thực và hồ sơ người dùng.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Đăng nhập hệ thống và trả về JWT Token cùng thông tin cơ bản của người dùng.
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        return ApiResponse.success("Đăng nhập thành công", userService.login(req));
    }

    /**
     * Lấy danh sách toàn bộ nhân viên (Manager, Staff, Chef).
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.success(userService.findAll());
    }

    /**
     * Lấy thông tin chi tiết của một người dùng theo ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getOne(@PathVariable @NonNull Long id) {
        return ApiResponse.success(userService.getOne(id));
    }

    /**
     * Tạo mới tài khoản nhân viên.
     */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserUpsertRequest req) {
        return ApiResponse.success("Tạo tài khoản nhân viên thành công", userService.create(req));
    }

    /**
     * Cập nhật thông tin cá nhân hoặc vai trò của nhân viên.
     */
    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody UserUpsertRequest req) {
        return ApiResponse.success("Cập nhật thông tin thành công", userService.update(id, req));
    }

    /**
     * Đặt lại mật khẩu cho nhân viên. 
     * Đảm bảo mật khẩu không được truyền qua URL QueryParams để bảo mật.
     */
    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable @NonNull Long id,
            @RequestBody(required = false) AuthRequest body) {
        String newPwd = (body == null || body.getPassword() == null) ? "" : body.getPassword();
        userService.resetPassword(id, newPwd);
        return ApiResponse.success("Đã đặt lại mật khẩu thành công", null);
    }

    /**
     * Xóa tài khoản nhân viên khỏi hệ thống (Sử dụng cơ chế Soft Delete).
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        userService.delete(id);
        return ApiResponse.success("Đã xóa nhân viên khỏi hệ thống", null);
    }

    /**
     * Cập nhật ảnh đại diện của nhân viên.
     */
    @PostMapping("/{id}/avatar")
    public ApiResponse<UserResponse> uploadAvatar(
            @PathVariable @NonNull Long id,
            @RequestParam("file") MultipartFile file) {
        UserResponse updated = userService.uploadAvatar(id, file);
        return ApiResponse.success("Cập nhật ảnh đại diện thành công", updated);
    }
}
