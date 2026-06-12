package com.qros.modules.user.service;

import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.mapper.UserMapper;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Cacheable(value = CacheNames.USERS, key = "'all'")
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserResponse getOne(@NonNull Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
