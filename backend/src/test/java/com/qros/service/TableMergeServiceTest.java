package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.modules.table.dto.request.TableMergeRequest;
import com.qros.modules.table.dto.response.TableMergeResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionTable;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.repository.TableSessionTableRepository;
import com.qros.modules.table.service.TableActiveOrderChecker;
import com.qros.modules.table.service.TableMergeService;
import com.qros.modules.table.service.TableSessionResolver;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

class TableMergeServiceTest {

    private final DiningTableRepository tableRepository = mock(DiningTableRepository.class);
    private final TableSessionRepository sessionRepository = mock(TableSessionRepository.class);
    private final TableSessionTableRepository membershipRepository = mock(TableSessionTableRepository.class);
    private final TableSessionResolver sessionResolver = mock(TableSessionResolver.class);
    private final TableSessionService tableSessionService = mock(TableSessionService.class);
    private final TableActiveOrderChecker activeOrderChecker = mock(TableActiveOrderChecker.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final TableMergeService mergeService = new TableMergeService(
            tableRepository,
            sessionRepository,
            membershipRepository,
            sessionResolver,
            tableSessionService,
            activeOrderChecker,
            eventPublisher);

    @Test
    void existingSessionIsLockedBeforeItsTablesAndMemberships() {
        DiningTable first = table(1L, "1");
        DiningTable second = table(2L, "2");
        TableSession session = session(10L, first);
        List<TableSessionTable> memberships =
                List.of(membership(101L, session, first, true), membership(102L, session, second, false));
        TableMergeRequest request = new TableMergeRequest(1L, List.of(1L, 2L), 4);

        when(membershipRepository.findActiveOpenSessionIdsByTableIds(request.tableIds(), TableSessionStatus.OPEN))
                .thenReturn(List.of(10L));
        when(sessionRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(session));
        when(membershipRepository.findActiveTableIdsBySessionId(10L)).thenReturn(List.of(1L, 2L));
        when(tableRepository.findAllByIdInForUpdate(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(membershipRepository.findActiveBySessionIdForUpdate(10L)).thenReturn(memberships);
        when(membershipRepository.findActiveBySessionId(10L)).thenReturn(memberships);
        when(sessionResolver.requirePrimaryTable(session)).thenReturn(first);
        when(sessionResolver.totalCapacity(any())).thenReturn(8);
        when(sessionResolver.displayName(any())).thenReturn("Table 1 + 2");

        TableMergeResponse response = mergeService.merge(request, 7L);

        assertThat(response.sessionId()).isEqualTo(10L);
        InOrder lockOrder = inOrder(sessionRepository, membershipRepository, tableRepository);
        lockOrder.verify(sessionRepository).findByIdForUpdate(10L);
        lockOrder.verify(membershipRepository).findActiveTableIdsBySessionId(10L);
        lockOrder.verify(tableRepository).findAllByIdInForUpdate(List.of(1L, 2L));
        lockOrder.verify(membershipRepository).findActiveBySessionIdForUpdate(10L);
    }

    @Test
    void concurrentDoubleMergeCreatesOneSessionAndRejectsTheStaleRequest() throws Exception {
        DiningTable first = table(1L, "1");
        DiningTable second = table(2L, "2");
        List<DiningTable> tables = List.of(first, second);
        TableMergeRequest request = new TableMergeRequest(1L, List.of(1L, 2L), 4);
        AtomicReference<TableSession> activeSession = new AtomicReference<>();
        CopyOnWriteArrayList<TableSessionTable> memberships = new CopyOnWriteArrayList<>();
        AtomicInteger initialSessionLookups = new AtomicInteger();
        AtomicInteger tableLockCalls = new AtomicInteger();
        AtomicInteger createdSessions = new AtomicInteger();
        CountDownLatch bothRequestsObservedNoSession = new CountDownLatch(2);
        CountDownLatch firstMergeCompleted = new CountDownLatch(1);

        when(membershipRepository.findActiveOpenSessionIdsByTableIds(request.tableIds(), TableSessionStatus.OPEN))
                .thenAnswer(invocation -> {
                    if (activeSession.get() == null && initialSessionLookups.incrementAndGet() <= 2) {
                        bothRequestsObservedNoSession.countDown();
                        assertThat(bothRequestsObservedNoSession.await(5, TimeUnit.SECONDS))
                                .isTrue();
                        return List.of();
                    }
                    TableSession current = activeSession.get();
                    return current == null ? List.of() : List.of(current.getId());
                });
        when(tableRepository.findAllByIdInForUpdate(List.of(1L, 2L))).thenAnswer(invocation -> {
            if (tableLockCalls.incrementAndGet() == 2) {
                assertThat(firstMergeCompleted.await(5, TimeUnit.SECONDS)).isTrue();
            }
            return tables;
        });
        when(sessionRepository.save(any(TableSession.class))).thenAnswer(invocation -> {
            TableSession saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(10L);
                activeSession.set(saved);
                createdSessions.incrementAndGet();
            }
            return saved;
        });
        when(membershipRepository.save(any(TableSessionTable.class))).thenAnswer(invocation -> {
            TableSessionTable saved = invocation.getArgument(0);
            saved.setId(100L + memberships.size());
            memberships.add(saved);
            return saved;
        });
        when(membershipRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<TableSessionTable> saved = invocation.getArgument(0);
            saved.forEach(memberships::add);
            return saved;
        });
        when(membershipRepository.findActiveBySessionIdForUpdate(10L))
                .thenAnswer(invocation -> List.copyOf(memberships));
        when(membershipRepository.findActiveBySessionId(10L)).thenAnswer(invocation -> {
            firstMergeCompleted.countDown();
            return List.copyOf(memberships);
        });
        when(sessionResolver.requirePrimaryTable(any(TableSession.class)))
                .thenAnswer(
                        invocation -> invocation.<TableSession>getArgument(0).getTable());
        when(sessionResolver.totalCapacity(any())).thenReturn(8);
        when(sessionResolver.displayName(any())).thenReturn("Table 1 + 2");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MergeAttempt> firstAttempt = executor.submit(() -> attemptMerge(request, ready, start));
            Future<MergeAttempt> secondAttempt = executor.submit(() -> attemptMerge(request, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<MergeAttempt> attempts =
                    List.of(firstAttempt.get(5, TimeUnit.SECONDS), secondAttempt.get(5, TimeUnit.SECONDS));

            assertThat(attempts)
                    .filteredOn(attempt -> attempt.response() != null)
                    .hasSize(1);
            assertThat(attempts)
                    .filteredOn(attempt -> attempt.error() != null)
                    .singleElement()
                    .satisfies(attempt ->
                            assertThat(attempt.error().getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION));
            assertThat(createdSessions).hasValue(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private MergeAttempt attemptMerge(TableMergeRequest request, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            return new MergeAttempt(mergeService.merge(request, 7L), null);
        } catch (BusinessException error) {
            return new MergeAttempt(null, error);
        }
    }

    private DiningTable table(Long id, String number) {
        return DiningTable.builder()
                .id(id)
                .tableNumber(number)
                .tableCode("BAN_" + number)
                .qrCodeUrl("https://example.test/" + number)
                .qrCodePublicId("table-" + number)
                .status(TableStatus.AVAILABLE)
                .capacity(4)
                .build();
    }

    private TableSession session(Long id, DiningTable primaryTable) {
        return TableSession.builder()
                .id(id)
                .table(primaryTable)
                .status(TableSessionStatus.OPEN)
                .guestCount(4)
                .build();
    }

    private TableSessionTable membership(Long id, TableSession session, DiningTable table, boolean primaryTable) {
        return TableSessionTable.builder()
                .id(id)
                .session(session)
                .table(table)
                .primaryTable(primaryTable)
                .joinedAt(com.qros.shared.time.AppTime.now())
                .build();
    }

    private record MergeAttempt(TableMergeResponse response, BusinessException error) {}
}
