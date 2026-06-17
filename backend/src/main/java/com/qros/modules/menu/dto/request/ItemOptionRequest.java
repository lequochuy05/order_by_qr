package com.qros.modules.menu.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ItemOptionRequest(
        Long id,
        @NotBlank(message = "Option name cannot be empty") String name,
        Boolean required,
        @Min(value = 1, message = "Max selection must be at least 1") Integer maxSelection,
        @Valid @NotEmpty(message = "Option must contain at least one value")
                List<ItemOptionValueRequest> optionValues) {}
