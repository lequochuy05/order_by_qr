package com.qros.modules.table.controller;

import com.qros.modules.table.dto.request.TableSessionHeartbeatRequest;
import com.qros.modules.table.dto.response.TableSessionStartResponse;
import com.qros.modules.table.dto.response.TableSessionStateResponse;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicTableSessionController {

    private final TableSessionService tableSessionService;

    @GetMapping("/tables/{tableCode}/session-state")
    public ApiResponse<TableSessionStateResponse> getSessionState(
            @PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableSessionService.getPublicState(tableCode));
    }

    @PostMapping("/tables/{tableCode}/start-session")
    public ApiResponse<TableSessionStartResponse> startSession(
            @PathVariable @NonNull String tableCode) {
        return ApiResponse.success(
                "Table session started",
                tableSessionService.startPublicSession(tableCode));
    }

    @PostMapping("/sessions/heartbeat")
    public ApiResponse<Void> heartbeat(
            @Valid @RequestBody @NonNull TableSessionHeartbeatRequest request) {
        tableSessionService.heartbeat(request);
        return ApiResponse.success(null);
    }
}
