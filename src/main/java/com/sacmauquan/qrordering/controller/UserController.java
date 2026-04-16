// controller/UserController.java
package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // ===== Auth =====
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserUpsertRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(Objects.requireNonNull(req)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        return ResponseEntity.ok(userService.login(Objects.requireNonNull(req)));
    }

    // ===== CRUD staff =====
    @GetMapping
    public ResponseEntity<List<UserDto>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getOne(Objects.requireNonNull(id)));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserUpsertRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(Objects.requireNonNull(req)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @Valid @RequestBody UserUpsertRequest req) {
        return ResponseEntity
                .ok(userService.update(Objects.requireNonNull(id), Objects.requireNonNull(req)));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) AuthRequest body) {
        String newPwd = (body == null ? "" : Objects.requireNonNullElse(body.getPassword(), ""));
        userService.resetPassword(Objects.requireNonNull(id), newPwd);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<UserDto> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        UserDto updated = userService.uploadAvatar(Objects.requireNonNull(id),
                Objects.requireNonNull(file));
        return ResponseEntity.ok(updated);
    }

}
