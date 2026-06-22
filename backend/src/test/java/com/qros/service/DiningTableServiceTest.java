package com.qros.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.table.dto.request.UpdateTableStatusRequest;
import com.qros.modules.table.mapper.DiningTableMapper;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.service.DiningTableService;
import com.qros.modules.table.service.TableActiveOrderChecker;
import com.qros.modules.table.service.TableCodeGenerator;
import com.qros.modules.table.service.TableQrService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class DiningTableServiceTest {

    private DiningTableRepository tableRepository;
    private TableSessionRepository sessionRepository;
    private TableActiveOrderChecker activeOrderChecker;
    private TableQrService tableQrService;
    private DiningTableService tableService;

    @BeforeEach
    void setUp() {
        tableRepository = mock(DiningTableRepository.class);
        sessionRepository = mock(TableSessionRepository.class);
        activeOrderChecker = mock(TableActiveOrderChecker.class);
        tableQrService = mock(TableQrService.class);
        tableService = new DiningTableService(
                tableRepository,
                mock(TableCodeGenerator.class),
                tableQrService,
                mock(DiningTableMapper.class),
                mock(ApplicationEventPublisher.class),
                mock(TransactionSideEffectService.class),
                sessionRepository,
                activeOrderChecker);
    }

    @Test
    void deleteRejectsTableWithOpenSession() {
        DiningTable table = table(TableStatus.OCCUPIED);
        when(tableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(table));
        when(sessionRepository.existsByTableIdAndStatus(5L, TableSessionStatus.OPEN))
                .thenReturn(true);

        assertThatThrownBy(() -> tableService.delete(5L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TABLE_IN_USE));

        verify(tableRepository, never()).delete(table);
    }

    @Test
    void statusCannotBecomeAvailableWhileActiveOrderExists() {
        DiningTable table = table(TableStatus.WAITING_FOR_PAYMENT);
        when(tableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(table));
        when(activeOrderChecker.hasActiveOrders(5L)).thenReturn(true);

        assertThatThrownBy(() -> tableService.updateStatus(5L, new UpdateTableStatusRequest(TableStatus.AVAILABLE)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TABLE_IN_USE));

        verify(tableRepository, never()).save(table);
    }

    @Test
    void waitingForPaymentRequiresActiveOrder() {
        DiningTable table = table(TableStatus.OCCUPIED);
        when(tableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(table));

        assertThatThrownBy(() ->
                        tableService.updateStatus(5L, new UpdateTableStatusRequest(TableStatus.WAITING_FOR_PAYMENT)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TABLE_STATUS_TRANSITION_INVALID));
    }

    @Test
    void regenerateQrRejectsTableWithOpenSession() {
        DiningTable table = table(TableStatus.OCCUPIED);
        when(tableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(table));
        when(sessionRepository.existsByTableIdAndStatus(5L, TableSessionStatus.OPEN))
                .thenReturn(true);

        assertThatThrownBy(() -> tableService.regenerateQrCode(5L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TABLE_IN_USE));

        verify(tableQrService, never()).generate(org.mockito.ArgumentMatchers.anyString());
    }

    private DiningTable table(TableStatus status) {
        return DiningTable.builder()
                .id(5L)
                .tableNumber("05")
                .tableCode("BAN_05")
                .qrCodeUrl("https://example.com/qr.png")
                .qrCodePublicId("qr_public_id")
                .status(status)
                .capacity(4)
                .build();
    }
}
