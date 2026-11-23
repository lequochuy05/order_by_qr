package com.qros.modules.table.controller;

import com.qros.modules.table.dto.request.CancelServiceRequestDto;
import com.qros.modules.table.dto.response.TableServiceRequestResponse;
import com.qros.modules.table.service.TableServiceRequestService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.TABLE_SERVICE_REQUESTS)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
public class TableServiceRequestController {

    private final TableServiceRequestService tableServiceRequestService;

    @GetMapping
    public ApiResponse<List<TableServiceRequestResponse>> getQueue(
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "false") boolean mineOnly,
            @RequestParam(required = false) Long tableId,
            Principal principal) {
        return ApiResponse.success(
                tableServiceRequestService.getStaffQueue(status, mineOnly, tableId, requirePrincipalName(principal)));
    }

    @PatchMapping("/{id}/claim")
    public ApiResponse<TableServiceRequestResponse> claim(@PathVariable @NonNull Long id, Principal principal) {
        return ApiResponse.success(
                "Service request claimed",
                tableServiceRequestService.claimRequest(id, requirePrincipalName(principal)));
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<TableServiceRequestResponse> resolve(@PathVariable @NonNull Long id, Principal principal) {
        return ApiResponse.success(
                "Service request resolved",
                tableServiceRequestService.resolveRequest(id, requirePrincipalName(principal)));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<TableServiceRequestResponse> cancel(
            @PathVariable @NonNull Long id,
            @RequestBody(required = false) @Valid CancelServiceRequestDto request,
            Principal principal) {
        return ApiResponse.success(
                "Service request cancelled",
                tableServiceRequestService.cancelByStaff(id, request, requirePrincipalName(principal)));
    }

    private static String requirePrincipalName(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getName();
    }
}
