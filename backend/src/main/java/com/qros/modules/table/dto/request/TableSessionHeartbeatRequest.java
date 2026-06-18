package com.qros.modules.table.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TableSessionHeartbeatRequest(@NotBlank(message = "Session token is required") String sessionToken) {}
