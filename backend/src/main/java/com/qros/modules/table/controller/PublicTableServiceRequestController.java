package com.qros.modules.table.controller;

import com.qros.modules.table.dto.request.CancelServiceRequestDto;
import com.qros.modules.table.dto.request.CreateServiceRequestDto;
import com.qros.modules.table.dto.response.CustomerServiceRequestsStateResponse;
import com.qros.modules.table.dto.response.TableServiceRequestResponse;
import com.qros.modules.table.service.TableServiceRequestService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.PUBLIC)
@RequiredArgsConstructor
public class PublicTableServiceRequestController {

    private final TableServiceRequestService tableServiceRequestService;

    @GetMapping("/tables/{tableCode}/service-requests")
    public ApiResponse<CustomerServiceRequestsStateResponse> getCustomerState(
            @PathVariable @NonNull String tableCode,
            @RequestParam(value = "sessionToken", required = false) String sessionToken,
            @RequestHeader(value = "X-Session-Token", required = false) String headerSessionToken) {
        String resolvedToken = StringUtils.hasText(sessionToken) ? sessionToken : headerSessionToken;
        if (!StringUtils.hasText(resolvedToken)) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID, "Session token is required");
        }
        return ApiResponse.success(tableServiceRequestService.getCustomerState(tableCode, resolvedToken));
    }

    @PostMapping("/tables/{tableCode}/service-requests")
    public ApiResponse<TableServiceRequestResponse> createServiceRequest(
            @PathVariable @NonNull String tableCode, @Valid @RequestBody @NonNull CreateServiceRequestDto request) {
        return ApiResponse.success(
                "Service request submitted", tableServiceRequestService.createCustomerRequest(tableCode, request));
    }

    @PostMapping("/tables/{tableCode}/service-requests/{id}/cancel")
    public ApiResponse<TableServiceRequestResponse> cancelServiceRequest(
            @PathVariable @NonNull String tableCode,
            @PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull CancelServiceRequestDto request) {
        return ApiResponse.success(
                "Service request cancelled", tableServiceRequestService.cancelByCustomer(tableCode, id, request));
    }
}
