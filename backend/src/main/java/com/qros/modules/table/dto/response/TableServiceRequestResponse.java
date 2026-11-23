package com.qros.modules.table.dto.response;

import com.qros.modules.table.model.enums.ServiceRequestReason;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import java.time.LocalDateTime;

public record TableServiceRequestResponse(
        Long id,
        Long tableId,
        String tableCode,
        String tableNumber,
        String tableDisplayName,
        Long tableSessionId,
        ServiceRequestReason reason,
        String note,
        ServiceRequestStatus status,
        Long claimedByUserId,
        String claimedByName,
        LocalDateTime claimedAt,
        Long resolvedByUserId,
        String resolvedByName,
        LocalDateTime resolvedAt,
        String cancelledByType,
        String cancelledReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version) {}
