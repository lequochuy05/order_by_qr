package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.table.config.TableSessionProperties;
import com.qros.modules.table.dto.request.TableSessionHeartbeatRequest;
import com.qros.modules.table.dto.response.TableSessionStartResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionToken;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.repository.TableSessionTokenRepository;
import com.qros.modules.table.service.TableActiveOrderChecker;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TableSessionServiceTest {

    @Mock
    DiningTableRepository tableRepository;

    @Mock
    TableSessionRepository sessionRepository;

    @Mock
    TableSessionTokenRepository tokenRepository;

    @Mock
    CacheManager cacheManager;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    TableActiveOrderChecker activeOrderChecker;

    private TableSessionService tableSessionService;

    @BeforeEach
    void setUp() {
        tableSessionService = new TableSessionService(
                tableRepository,
                sessionRepository,
                tokenRepository,
                cacheManager,
                eventPublisher,
                activeOrderChecker,
                new TableSessionProperties());
    }

    @Test
    void startPublicSessionCreatesOpenSessionAndClientToken() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableNumber("05")
                .tableCode("BAN_05")
                .status(TableStatus.AVAILABLE)
                .capacity(4)
                .build();

        when(tableRepository.findByTableCodeForUpdate("BAN_05")).thenReturn(Optional.of(table));
        when(sessionRepository.findByTableIdAndStatusForUpdate(5L, TableSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(TableSession.class))).thenAnswer(invocation -> {
            TableSession session = invocation.getArgument(0);
            session.setId(100L);
            return session;
        });
        when(tokenRepository.save(any(TableSessionToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TableSessionStartResponse response = tableSessionService.startPublicSession("BAN_05");

        assertThat(response.sessionId()).isEqualTo(100L);
        assertThat(response.sessionToken()).isNotBlank();
        assertThat(response.sessionStatus()).isEqualTo("OPEN");
        assertThat(response.canOrder()).isTrue();
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);

        verify(tokenRepository).save(any(TableSessionToken.class));
        verify(tableRepository).save(table);
        verify(activeOrderChecker).attachActiveOrderToSession(eq(5L), any(TableSession.class));
    }

    @Test
    void startPublicSessionDelegatesStaffOrderAttachment() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableNumber("05")
                .tableCode("BAN_05")
                .status(TableStatus.OCCUPIED)
                .capacity(4)
                .build();

        when(tableRepository.findByTableCodeForUpdate("BAN_05")).thenReturn(Optional.of(table));
        when(sessionRepository.findByTableIdAndStatusForUpdate(5L, TableSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(TableSession.class))).thenAnswer(invocation -> {
            TableSession session = invocation.getArgument(0);
            session.setId(100L);
            return session;
        });
        when(tokenRepository.save(any(TableSessionToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tableSessionService.startPublicSession("BAN_05");

        verify(activeOrderChecker).attachActiveOrderToSession(eq(5L), any(TableSession.class));
    }

    @Test
    void requireOpenSessionForReadValidatesWithoutWriteLockOrTouch() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableNumber("05")
                .tableCode("BAN_05")
                .status(TableStatus.OCCUPIED)
                .capacity(4)
                .build();
        TableSession session = TableSession.builder()
                .id(100L)
                .table(table)
                .status(TableSessionStatus.OPEN)
                .build();
        TableSessionToken token = TableSessionToken.builder()
                .id(200L)
                .session(session)
                .tokenHash("HASHED")
                .issuedAt(AppTime.now())
                .lastSeenAt(AppTime.now())
                .build();

        when(tokenRepository.findFirstByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));

        TableSession result = tableSessionService.requireOpenSessionForRead("BAN_05", "SESSION");

        assertThat(result).isSameAs(session);
        verify(sessionRepository, never()).findByIdForUpdate(any());
        verify(sessionRepository, never()).save(any(TableSession.class));
        verify(tokenRepository, never()).save(any(TableSessionToken.class));
    }

    @Test
    void requireSessionForReadAllowsClosedSessionWithoutWriteLockOrTouch() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableNumber("05")
                .tableCode("BAN_05")
                .status(TableStatus.AVAILABLE)
                .capacity(4)
                .build();
        TableSession session = TableSession.builder()
                .id(100L)
                .table(table)
                .status(TableSessionStatus.CLOSED)
                .build();
        TableSessionToken token = TableSessionToken.builder()
                .id(200L)
                .session(session)
                .tokenHash("HASHED")
                .issuedAt(AppTime.now())
                .lastSeenAt(AppTime.now())
                .build();

        when(tokenRepository.findFirstByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));

        TableSession result = tableSessionService.requireSessionForRead("BAN_05", "SESSION");

        assertThat(result).isSameAs(session);
        verify(sessionRepository, never()).findByIdForUpdate(any());
        verify(sessionRepository, never()).save(any(TableSession.class));
        verify(tokenRepository, never()).save(any(TableSessionToken.class));
    }

    @Test
    void heartbeatTouchesOpenSessionWithBulkUpdate() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableNumber("05")
                .tableCode("BAN_05")
                .status(TableStatus.OCCUPIED)
                .capacity(4)
                .build();
        TableSession session = TableSession.builder()
                .id(100L)
                .table(table)
                .status(TableSessionStatus.OPEN)
                .build();
        TableSessionToken token = TableSessionToken.builder()
                .id(200L)
                .session(session)
                .tokenHash("HASHED")
                .issuedAt(AppTime.now())
                .build();

        when(tokenRepository.findFirstByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));

        tableSessionService.heartbeat(new TableSessionHeartbeatRequest("SESSION"));

        verify(tokenRepository).save(token);
        verify(sessionRepository).touchOpenSessionActivity(eq(100L), any());
        verify(sessionRepository, never()).save(any(TableSession.class));
    }

    @Test
    void heartbeatRejectsClosedSessionInsteadOfSilentlySucceeding() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableCode("BAN_05")
                .status(TableStatus.AVAILABLE)
                .build();
        TableSession session = TableSession.builder()
                .id(100L)
                .table(table)
                .status(TableSessionStatus.CLOSED)
                .build();
        TableSessionToken token = TableSessionToken.builder()
                .session(session)
                .tokenHash("HASHED")
                .issuedAt(AppTime.now())
                .lastSeenAt(AppTime.now())
                .build();
        when(tokenRepository.findFirstByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tableSessionService.heartbeat(new TableSessionHeartbeatRequest("SESSION")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.TABLE_SESSION_EXPIRED));
    }

    @Test
    void heartbeatSkipsWriteWhenTokenWasSeenRecently() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableCode("BAN_05")
                .status(TableStatus.OCCUPIED)
                .build();
        TableSession session = TableSession.builder()
                .id(100L)
                .table(table)
                .status(TableSessionStatus.OPEN)
                .build();
        TableSessionToken token = TableSessionToken.builder()
                .session(session)
                .tokenHash("HASHED")
                .issuedAt(AppTime.now())
                .lastSeenAt(AppTime.now())
                .build();
        when(tokenRepository.findFirstByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));

        tableSessionService.heartbeat(new TableSessionHeartbeatRequest("SESSION"));

        verify(tokenRepository, never()).save(token);
        verify(sessionRepository, never()).touchOpenSessionActivity(any(), any());
    }

    @Test
    void expiredTokenIsRejected() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableCode("BAN_05")
                .status(TableStatus.OCCUPIED)
                .build();
        TableSession session = TableSession.builder()
                .id(100L)
                .table(table)
                .status(TableSessionStatus.OPEN)
                .build();
        TableSessionToken token = TableSessionToken.builder()
                .session(session)
                .tokenHash("HASHED")
                .issuedAt(AppTime.now().minusHours(3))
                .lastSeenAt(AppTime.now().minusHours(3))
                .build();
        when(tokenRepository.findFirstByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tableSessionService.requireOpenSessionForRead("BAN_05", "SESSION"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.TABLE_SESSION_EXPIRED));
    }

    @Test
    void closeSessionRevokesTokensAndReleasesUnusedTable() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableCode("BAN_05")
                .status(TableStatus.OCCUPIED)
                .build();
        TableSession session = TableSession.builder()
                .id(100L)
                .table(table)
                .status(TableSessionStatus.OPEN)
                .build();
        when(sessionRepository.findTableIdById(100L)).thenReturn(Optional.of(5L));
        when(tableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(table));
        when(sessionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(session));
        when(activeOrderChecker.hasActiveOrders(5L)).thenReturn(false);

        tableSessionService.closeSession(100L, TableSessionStatus.CLOSED, "Order settled");

        assertThat(session.getStatus()).isEqualTo(TableSessionStatus.CLOSED);
        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        verify(tokenRepository).revokeActiveTokensBySessionId(eq(100L), any());
        verify(tableRepository).save(table);
    }

    @Test
    void issuingNinthTokenRevokesOldestActiveToken() {
        DiningTable table = DiningTable.builder()
                .id(5L)
                .tableNumber("05")
                .tableCode("BAN_05")
                .status(TableStatus.OCCUPIED)
                .capacity(4)
                .build();
        TableSession session = TableSession.builder()
                .id(100L)
                .table(table)
                .status(TableSessionStatus.OPEN)
                .build();
        List<TableSessionToken> activeTokens = new ArrayList<>();
        for (long id = 1; id <= 8; id++) {
            activeTokens.add(TableSessionToken.builder()
                    .id(id)
                    .session(session)
                    .tokenHash("HASHED_" + id)
                    .issuedAt(AppTime.now().minusMinutes(9 - id))
                    .lastSeenAt(AppTime.now())
                    .build());
        }
        when(tableRepository.findByTableCodeForUpdate("BAN_05")).thenReturn(Optional.of(table));
        when(sessionRepository.findByTableIdAndStatusForUpdate(5L, TableSessionStatus.OPEN))
                .thenReturn(Optional.of(session));
        when(tokenRepository.findActiveBySessionIdForUpdate(100L)).thenReturn(activeTokens);

        tableSessionService.startPublicSession("BAN_05");

        assertThat(activeTokens.getFirst().getRevokedAt()).isNotNull();
        verify(tokenRepository).saveAll(any());
        verify(tokenRepository).save(any(TableSessionToken.class));
    }
}
