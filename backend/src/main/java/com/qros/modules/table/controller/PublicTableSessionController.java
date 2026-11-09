package com.qros.modules.table.controller;

import com.qros.modules.table.dto.request.TableSessionHeartbeatRequest;
import com.qros.modules.table.dto.response.TableSessionStartResponse;
import com.qros.modules.table.dto.response.TableSessionStateResponse;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.PUBLIC)
public class PublicTableSessionController {

    private final TableSessionService tableSessionService;

    public PublicTableSessionController(TableSessionService tableSessionService) {
        this.tableSessionService = tableSessionService;
    }

    @GetMapping("/tables/{tableCode}/session-state")
    public ApiResponse<TableSessionStateResponse> getSessionState(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableSessionService.getPublicState(tableCode));
    }

    @PostMapping("/tables/{tableCode}/start-session")
    public ApiResponse<TableSessionStartResponse> startSession(
            @PathVariable @NonNull String tableCode, @RequestParam(value = "sig", required = false) String sig) {
        return ApiResponse.success("Table session started", tableSessionService.startPublicSession(tableCode, sig));
    }

    @PostMapping("/sessions/heartbeat")
    public ApiResponse<Void> heartbeat(@Valid @RequestBody @NonNull TableSessionHeartbeatRequest request) {
        tableSessionService.heartbeat(request);
        return ApiResponse.success(null);
    }
}
