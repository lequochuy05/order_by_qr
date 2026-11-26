package com.qros.modules.auth.service;

import com.qros.modules.user.service.UserAuthSessionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuthSessionPortImpl implements UserAuthSessionPort {

    private final RefreshTokenService refreshTokenService;
    private final AccountLockoutService accountLockoutService;

    @Override
    public void revokeUserTokens(@NonNull Long userId) {
        refreshTokenService.revokeUserTokens(userId);
    }

    @Override
    public void unlockAccount(String email) {
        accountLockoutService.unlock(email);
    }
}
