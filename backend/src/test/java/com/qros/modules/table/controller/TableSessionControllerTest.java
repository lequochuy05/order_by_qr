package com.qros.modules.table.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.table.dto.request.TableMergeRequest;
import com.qros.modules.table.dto.response.TableMergeResponse;
import com.qros.modules.table.service.TableMergeService;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.shared.response.ApiResponse;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableSessionControllerTest {

    private TableMergeService tableMergeService;
    private CurrentUserService currentUserService;
    private TableSessionController controller;

    @BeforeEach
    void setUp() {
        tableMergeService = mock(TableMergeService.class);
        currentUserService = mock(CurrentUserService.class);
        controller = new TableSessionController(tableMergeService, currentUserService);
    }

    @Test
    void merge_withAuthenticatedPrincipal_resolvesUserIdAndPassesToService() {
        TableMergeRequest request = new TableMergeRequest(1L, List.of(1L, 2L), 4);
        Principal principal = () -> "staff@qros.vn";
        Long expectedUserId = 42L;

        when(currentUserService.getCurrentUserId("staff@qros.vn")).thenReturn(expectedUserId);
        TableMergeResponse mockResponse = mock(TableMergeResponse.class);
        when(tableMergeService.merge(eq(request), eq(expectedUserId))).thenReturn(mockResponse);

        ApiResponse<TableMergeResponse> response = controller.merge(request, principal);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(mockResponse);
        verify(currentUserService).getCurrentUserId("staff@qros.vn");
        verify(tableMergeService).merge(request, expectedUserId);
    }

    @Test
    void merge_withNullPrincipal_passesNullUserIdToService() {
        TableMergeRequest request = new TableMergeRequest(1L, List.of(1L, 2L), 4);
        TableMergeResponse mockResponse = mock(TableMergeResponse.class);
        when(tableMergeService.merge(eq(request), eq(null))).thenReturn(mockResponse);

        ApiResponse<TableMergeResponse> response = controller.merge(request, null);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(mockResponse);
        verify(currentUserService, never()).getCurrentUserId(org.mockito.ArgumentMatchers.anyString());
        verify(tableMergeService).merge(request, null);
    }

    @Test
    void cancelMerge_delegatesToTableMergeService() {
        Long sessionId = 100L;
        TableMergeResponse mockResponse = mock(TableMergeResponse.class);
        when(tableMergeService.cancelMerge(sessionId)).thenReturn(mockResponse);

        ApiResponse<TableMergeResponse> response = controller.cancelMerge(sessionId);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(mockResponse);
        verify(tableMergeService).cancelMerge(sessionId);
    }
}
