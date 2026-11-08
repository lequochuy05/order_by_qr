package com.qros.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record Resend2faRequest(@NotBlank(message = "Pre-auth token cannot be blank") String preAuthToken) {}
