package com.qros.modules.menu.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ItemOptionValueRequest(
        Long id,
        @NotBlank(message = "Option value name cannot be empty") String name,
        @NotNull(message = "Extra price cannot be empty") @DecimalMin(value = "0.0", message = "Extra price cannot be negative")
                BigDecimal extraPrice) {}
