package com.qros.modules.table.dto.response;

import com.qros.modules.table.model.enums.ServiceRequestReason;
import java.util.List;

public record CustomerServiceRequestsStateResponse(
        List<TableServiceRequestResponse> requests,
        int cooldownRemainingSeconds,
        int openCount,
        int maxOpenRequests,
        List<ServiceRequestReason> activeReasons) {}
