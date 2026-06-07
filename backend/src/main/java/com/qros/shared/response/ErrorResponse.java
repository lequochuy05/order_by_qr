package com.qros.shared.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class ErrorResponse {
    @Builder.Default
    Instant timestamp = Instant.now();

    String code;
    String message;

    @Builder.Default
    Map<String, Object> details = Map.of();
}
