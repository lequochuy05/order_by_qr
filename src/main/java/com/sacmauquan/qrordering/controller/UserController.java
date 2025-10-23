// controller/UserController.java
package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // ===== Auth =====
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody UserUpsertRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }

    // ===== CRUD staff =====
    @GetMapping
    public ResponseEntity<List<User>> list() {
        return ResponseEntity.ok(userService.findAll()); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getOne(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody UserUpsertRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @RequestBody UserUpsertRequest req) {
        return ResponseEntity.ok(userService.update(id, req));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) AuthRequest body) {
        String newPwd = (body == null ? null : body.getPassword());
        userService.resetPassword(id, newPwd);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<UserDto> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            UserDto updated = userService.uploadAvatar(id, file);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

}
