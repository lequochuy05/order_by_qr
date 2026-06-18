package com.qros.modules.table.dto.request;

import com.qros.modules.table.model.enums.TableStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTableStatusRequest(@NotNull(message = "Table status is required") TableStatus status) {}
