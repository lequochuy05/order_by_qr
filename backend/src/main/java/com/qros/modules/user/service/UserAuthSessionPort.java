package com.qros.modules.user.service;

import org.springframework.lang.NonNull;

/**
 * Port invoked by the user module when account security changes require revoking auth sessions or clearing lockouts.
 */
public interface UserAuthSessionPort {

    void revokeUserTokens(@NonNull Long userId);

    void unlockAccount(String email);
}
