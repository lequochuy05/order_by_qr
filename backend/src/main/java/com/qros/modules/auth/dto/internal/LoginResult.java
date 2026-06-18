package com.qros.modules.auth.dto.internal;

import com.qros.modules.auth.dto.response.LoginResponse;

public record LoginResult(LoginResponse response, AuthenticatedUser authUser) {}
