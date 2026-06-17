package com.qros.modules.user.service;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAvatarService {
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final TransactionSideEffectService sideEffects;
    private final UserMapper userMapper;

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse uploadAvatar(@NonNull Long id, @NonNull MultipartFile file) {
        User u = userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        try {
            String newUrl = storageService.upload(file, "order_by_qr/avatars");
            if (StringUtils.hasText(u.getAvatarUrl())) {
                String oldUrl = u.getAvatarUrl();
                sideEffects.afterCommit(
                        () -> storageService.delete(oldUrl), "delete replaced avatar image for user " + id);
            }
            sideEffects.afterRollback(
                    () -> storageService.delete(newUrl), "delete rolled back avatar image for user " + id);
            u.setAvatarUrl(newUrl);
            userRepository.save(u);
            log.info("Avatar updated for user: {}", u.getEmail());
            return userMapper.toDto(u);
        } catch (IOException e) {
            log.error("Failed to upload avatar: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload avatar", e);
        }
    }

    @Transactional
    @CacheEvict(value = CacheNames.USERS, allEntries = true)
    public UserResponse uploadCurrentAvatar(@NonNull String email, @NonNull MultipartFile file) {
        User u = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return uploadAvatar(u.getId(), file);
    }
}
