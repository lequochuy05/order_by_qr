package com.qros.modules.auth.dto.internal;

import com.qros.modules.auth.dto.response.TokenResponse;

public record RefreshResult(
        TokenResponse response,
        String refreshToken
) {
}