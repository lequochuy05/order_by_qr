package com.qros.modules.user.service;

import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Module-boundary lookup facade for user entities consumed by other modules.
 */
@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserRepository userRepository;

    public Optional<User> findByEmailIgnoreCase(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    public Optional<User> findById(@NonNull Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    public User save(@NonNull User user) {
        return userRepository.save(user);
    }
}
