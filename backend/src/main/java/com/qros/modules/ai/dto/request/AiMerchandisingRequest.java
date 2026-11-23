package com.qros.modules.ai.dto.request;

import jakarta.validation.constraints.Pattern;

public record AiMerchandisingRequest(
        @Pattern(regexp = "^(vi|en)$", message = "Language must be vi or en")
        String language) {}
