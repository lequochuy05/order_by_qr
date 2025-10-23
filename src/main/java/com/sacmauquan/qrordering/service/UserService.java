// service/UserService.java
package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.repository.UserRepository;
import com.sacmauquan.qrordering.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageManagerService imageManagerService;
    private final JwtService jwtService;

    // ===== Upload Avatar =====
    public UserDto uploadAvatar(Long id, MultipartFile file) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File không hợp lệ");
        }

        try {
            // Upload lên Cloudinary → trả về secure_url dạng String
            String newUrl = imageManagerService.upload(file, "order_by_qr/avatars");

            // Xóa ảnh cũ nếu có
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                imageManagerService.delete(user.getAvatarUrl());
            }

            // Lưu URL mới
            user.setAvatarUrl(newUrl);
            userRepository.save(user);
            return toDto(user);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải ảnh lên Cloudinary");
        }
    }


    // ===== Auth =====
    public AuthResponse register(UserUpsertRequest req) {
        if (isBlank(req.getEmail()) || isBlank(req.getPassword()) || isBlank(req.getFullName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu email/password/fullName");
        }
        userRepository.findByEmail(req.getEmail()).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        });

        User u = new User();
        applyUpsert(u, req, true);
        u = userRepository.save(u);

        return buildAuthResponse(u);
    }

    public AuthResponse login(AuthRequest req) {
        if (isBlank(req.getEmail()) || isBlank(req.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu email/password");
        }
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email không tồn tại"));

        if (!"ACTIVE".equalsIgnoreCase(nvl(u.getStatus(), "ACTIVE"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đang bị khóa");
        }
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai mật khẩu");
        }
        return buildAuthResponse(u);
    }

    private AuthResponse buildAuthResponse(User u) {
        var roleName = (u.getRole() != null ? u.getRole().name() : "STAFF"); // fallback
        String token = jwtService.generateToken(
            u.getEmail(),
            Map.of("uid", u.getId(), "role", roleName)
        );
        return new AuthResponse(u.getId(), u.getFullName(), u.getRole(), token); 
    }

    // ===== CRUD nhân viên =====
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public UserDto getOne(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        return toDto(u);
    }

    public UserDto create(UserUpsertRequest req) {
        if (isBlank(req.getEmail()) || isBlank(req.getPassword()) || isBlank(req.getFullName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu email/password/fullName");
        }
        userRepository.findByEmail(req.getEmail()).ifPresent(x -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        });

        User u = new User();
        applyUpsert(u, req, true);
        u = userRepository.save(u);
        return toDto(u);
    }

    public UserDto update(Long id, UserUpsertRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        // Không cho đổi email nếu bạn muốn cố định:
        req.setEmail(null);

        applyUpsert(u, req, false);
        u = userRepository.save(u);
        return toDto(u);
    }

    public void resetPassword(Long id, String newPassword) {
        if (isBlank(newPassword)) newPassword = "123456";
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }
        userRepository.deleteById(id);
    }

    // ===== Helpers =====
    private void applyUpsert(User u, UserUpsertRequest req, boolean creating) {
        if (creating) {
            u.setEmail(req.getEmail());
            u.setCreatedAt(u.getCreatedAt() == null ? Instant.now() : u.getCreatedAt()); // nếu có cột này
        }
        if (req.getFullName() != null) u.setFullName(req.getFullName());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getRole() != null) u.setRole(req.getRole());
        else if (creating && u.getRole() == null) u.setRole(User.Role.STAFF);

        String status = nvl(req.getStatus(), creating ? "ACTIVE" : u.getStatus());
        u.setStatus(status == null ? "ACTIVE" : status);

        if (creating || notBlank(req.getPassword())) {
            if (isBlank(req.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không hợp lệ");
            }
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
    }

    private static String nvl(String s, String dft){ 
        return s != null ? s : dft; 
    }
    private static boolean isBlank(String s){ 
        return s == null || s.trim().isEmpty(); 
    }
    private static boolean notBlank(String s){ 
        return !isBlank(s); 
    }

    private UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .status(nvl(u.getStatus(), "ACTIVE"))
                .createdAt(u.getCreatedAt())
                .avatarUrl(u.getAvatarUrl())
                .build();
    }

    

}
