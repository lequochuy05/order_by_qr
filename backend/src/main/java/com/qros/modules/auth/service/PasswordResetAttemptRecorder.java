package com.qros.modules.auth.service;

import com.qros.modules.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetAttemptRecorder {

    public static final int MAX_OTP_ATTEMPTS = 5;

    private final PasswordResetTokenRepository tokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedOtpAttempt(Long tokenId) {
        if (tokenId == null) {
            return;
        }
        tokenRepository.findById(tokenId).ifPresent(token -> {
            int nextAttempts = token.getAttempts() + 1;
            token.setAttempts(nextAttempts);
            if (nextAttempts >= MAX_OTP_ATTEMPTS) {
                token.setUsed(true);
            }
            tokenRepository.save(token);
        });
    }
}
