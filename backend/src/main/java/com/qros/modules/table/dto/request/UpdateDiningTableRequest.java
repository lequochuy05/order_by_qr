package com.qros.modules.table.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDiningTableRequest(
        @NotBlank(message = "Table number cannot be empty") String tableNumber,

        @NotNull(message = "Capacity is required") @Min(value = 1, message = "Capacity must be at least 1 person") Integer capacity) {
}
