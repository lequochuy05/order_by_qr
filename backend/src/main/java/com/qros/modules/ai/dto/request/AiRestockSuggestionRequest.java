package com.qros.modules.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record AiRestockSuggestionRequest(
        @Min(value = 1, message = "Forecast days must be at least 1") @Max(value = 30, message = "Forecast days must not exceed 30") Integer forecastDays,

        @Pattern(regexp = "^(vi|en)$", message = "Language must be vi or en")
        String language) {}
