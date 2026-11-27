package com.qros.modules.table.dto.request;

import jakarta.validation.constraints.Size;

public record CancelTableServiceRequest(
        String sessionToken,

        @Size(max = 200, message = "Reason cannot exceed 200 characters") String reason) {}
