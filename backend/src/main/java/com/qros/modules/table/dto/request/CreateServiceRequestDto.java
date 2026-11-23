package com.qros.modules.table.dto.request;

import com.qros.modules.table.model.enums.ServiceRequestReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestDto(
        @NotBlank(message = "Session token is required") String sessionToken,

        @NotNull(message = "Service request reason is required") ServiceRequestReason reason,

        @Size(max = 200, message = "Note cannot exceed 200 characters") String note,

        @NotBlank(message = "Client request ID is required") @Size(max = 64, message = "Client request ID cannot exceed 64 characters") String clientRequestId) {}
