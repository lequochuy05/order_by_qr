package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.mapper.UserMapper;
import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.repository.UserRepository;
import com.sacmauquan.qrordering.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageManagerService imageManagerService;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    // Authentication
    public AuthResponse register(UserUpsertRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        User u = handleEntityMapping(new User(), req);

        u.setRole(User.Role.STAFF);
        u.setStatus(User.Status.ACTIVE);
        return buildAuthResponse(userRepository.save(u));
    }

    public AuthResponse login(AuthRequest req) {

        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email không tồn tại"));

        if (u.getStatus() == User.Status.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }
        if (u.getStatus() == User.Status.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa được kích hoạt");
        }

        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai mật khẩu");
        }
        return buildAuthResponse(u);
    }

    // CRUD staff
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public UserDto getOne(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    public UserDto create(UserUpsertRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng");
        }
        // if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone())) {
        // throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã
        // được sử dụng");
        // }

        User u = handleEntityMapping(new User(), req);
        userRepository.save(u);
        notifyChange();
        return userMapper.toDto(u);
    }

    public UserDto update(Long id, UserUpsertRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        req.setEmail(null);

        handleEntityMapping(u, req);
        userRepository.save(u);
        notifyChange();
        return userMapper.toDto(u);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }
        userRepository.deleteById(id);
        notifyChange();
    }

    public void resetPassword(Long id, String newPassword) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (!StringUtils.hasText(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được để trống");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    // Upload Avatar
    public UserDto uploadAvatar(Long id, MultipartFile file) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (file == null || file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File ảnh trống");

        try {
            String newUrl = imageManagerService.upload(file, "order_by_qr/avatars");
            if (StringUtils.hasText(u.getAvatarUrl())) {
                imageManagerService.delete(u.getAvatarUrl());
            }
            u.setAvatarUrl(newUrl);
            userRepository.save(u);
            notifyChange();
            return userMapper.toDto(u);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi upload ảnh");
        }
    }

    //  PRIVATE METHODS 
    private User handleEntityMapping(User u, UserUpsertRequest req) {
        boolean isNew = (u.getId() == null);

        if (isNew) {
            u = userMapper.toEntity(req);
            u.setStatus(User.Status.ACTIVE);
            u.setRole(User.Role.STAFF);
            if (!StringUtils.hasText(req.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không được để trống");
            }
            if (StringUtils.hasText(req.getEmail())) {
                u.setEmail(req.getEmail());
            }
        } else {
            userMapper.updateEntity(u, req);
        }

        if (StringUtils.hasText(req.getStatus())) {
            try {
                u.setStatus(User.Status.valueOf(req.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                u.setStatus(User.Status.ACTIVE);
            }
        }

        if (req.getRole() != null) {
            u.setRole(req.getRole());
        }

        if (StringUtils.hasText(req.getPassword())) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return u;
    }

    private AuthResponse buildAuthResponse(User u) {
        var roleName = (u.getRole() != null ? u.getRole().name() : "STAFF");
        String token = jwtService.generateToken(
                u.getEmail(),
                Map.of("uid", u.getId(), "role", roleName));
        return new AuthResponse(u.getId(), u.getFullName(), u.getRole(), token, u.getAvatarUrl());
    }

    private void notifyChange() {
        eventPublisher.publishEvent(new com.sacmauquan.qrordering.event.WebSocketEvent(
                "/topic/users",
                "UPDATED",
                "[WS] User changed -> Sent UPDATED signal"));
    }
}