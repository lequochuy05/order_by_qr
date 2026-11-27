package com.qros.modules.user.service;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.event.DomainEvents.UserChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionExecutor;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.validation.ImageFileValidator;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAvatarService {
    private static final String AVATAR_FOLDER = "order_by_qr/avatars";

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final TransactionSideEffectService sideEffects;
    private final UserMapper userMapper;
    private final ImageFileValidator imageFileValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionExecutor transactionExecutor;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserResponse uploadAvatar(@NonNull Long id, @NonNull MultipartFile file) {
        return uploadAvatar(id, file, null);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserResponse uploadAvatar(@NonNull Long id, @NonNull MultipartFile file, String actorEmail) {
        imageFileValidator.validate(file);
        transactionExecutor.readOnly(() -> {
            User target =
                    userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            if (StringUtils.hasText(actorEmail)) {
                User actor =
                        userRepository.findByEmailIgnoreCase(actorEmail.trim()).orElse(null);
                if (actor != null
                        && actor.getRole() == UserRole.MANAGER
                        && !Objects.equals(actor.getId(), target.getId())
                        && (target.getRole() == UserRole.ADMIN || target.getRole() == UserRole.MANAGER)) {
                    throw new BusinessException(
                            ErrorCode.FORBIDDEN,
                            "Manager is not allowed to update avatar for Admin or Manager accounts");
                }
            }
            return null;
        });

        return uploadAndPersistAvatar(id, file);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserResponse uploadCurrentAvatar(@NonNull String email, @NonNull MultipartFile file) {
        imageFileValidator.validate(file);
        Long userId = transactionExecutor.readOnly(() -> userRepository
                .findByEmailIgnoreCase(email)
                .map(User::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)));

        return uploadAndPersistAvatar(userId, file);
    }

    private UserResponse uploadAndPersistAvatar(Long id, MultipartFile file) {
        String newUrl = uploadAvatarOutsideTransaction(file);

        try {
            return transactionExecutor.write(() -> {
                User u = userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                String oldUrl = u.getAvatarUrl();

                u.setAvatarUrl(newUrl);
                userRepository.saveAndFlush(u);

                if (StringUtils.hasText(oldUrl)) {
                    sideEffects.afterCommit(
                            () -> storageService.delete(oldUrl), "delete replaced avatar image for user " + id);
                }
                eventPublisher.publishEvent(new UserChangeEvent());
                log.info("Avatar updated for user: {}", u.getEmail());
                return userMapper.toDto(u);
            });
        } catch (RuntimeException e) {
            deleteUploadedAvatarSilently(newUrl, id);
            throw e;
        }
    }

    private String uploadAvatarOutsideTransaction(MultipartFile file) {
        try {
            return storageService.upload(file, AVATAR_FOLDER);
        } catch (IOException e) {
            log.error("Failed to upload avatar: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload avatar", e);
        }
    }

    private void deleteUploadedAvatarSilently(String uploadedUrl, Long id) {
        try {
            storageService.delete(uploadedUrl);
        } catch (Exception cleanupException) {
            log.warn("Failed to clean up rolled back avatar image for user {}: {}", id, cleanupException.getMessage());
        }
    }
}
