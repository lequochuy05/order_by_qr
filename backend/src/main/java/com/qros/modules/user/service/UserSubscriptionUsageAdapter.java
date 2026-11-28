package com.qros.modules.user.service;

import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.subscription.SubscriptionResourceUsagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSubscriptionUsageAdapter implements SubscriptionResourceUsagePort {

    private final UserRepository userRepository;

    @Override
    public ResourceType getResourceType() {
        return ResourceType.ACTIVE_USERS;
    }

    @Override
    public long countActive() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }
}
