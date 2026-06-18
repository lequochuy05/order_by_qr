package com.qros.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record StockInRequest(
        @NotNull(message = "Quantity cannot be empty") @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
                BigDecimal quantity,
        @Size(max = 500, message = "Note cannot exceed 500 characters") String note) {}
