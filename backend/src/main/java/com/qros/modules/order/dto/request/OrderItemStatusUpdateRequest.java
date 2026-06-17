package com.qros.modules.order.dto.request;

import com.qros.modules.order.model.enums.OrderItemStatus;
import jakarta.validation.constraints.NotNull;

public record OrderItemStatusUpdateRequest(@NotNull OrderItemStatus status) {}
