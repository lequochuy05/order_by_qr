package com.qros.modules.table.controller;

import com.qros.modules.table.dto.request.TableMergeRequest;
import com.qros.modules.table.dto.response.TableMergeResponse;
import com.qros.modules.table.service.TableMergeService;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.TABLE_SESSIONS)
@RequiredArgsConstructor
public class TableSessionController {

    private final TableMergeService tableMergeService;
    private final CurrentUserService currentUserService;

    @PostMapping("/merge")
    public ApiResponse<TableMergeResponse> merge(
            @Valid @RequestBody @NonNull TableMergeRequest request, Principal principal) {
        Long currentUserId = resolveUserId(principal);
        return ApiResponse.success("Tables merged successfully", tableMergeService.merge(request, currentUserId));
    }

    @PostMapping("/{sessionId}/cancel-merge")
    public ApiResponse<TableMergeResponse> cancelMerge(@PathVariable @NonNull Long sessionId) {
        return ApiResponse.success("Table merge cancelled", tableMergeService.cancelMerge(sessionId));
    }

    private Long resolveUserId(Principal principal) {
        if (principal == null
                || principal.getName() == null
                || principal.getName().isBlank()) {
            return null;
        }
        return currentUserService.getCurrentUserId(principal.getName());
    }
}
