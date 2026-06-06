package com.qros.modules.kitchen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KitchenItemStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;
}
