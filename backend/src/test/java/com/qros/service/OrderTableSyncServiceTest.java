package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.service.OrderTableSyncService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.service.TableSessionResolver;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.event.DomainEvents.TableChangeEvent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderTableSyncServiceTest {

    @Mock
    private DiningTableRepository tableRepository;

    @Mock
    private TableSessionResolver tableSessionResolver;

    @Mock
    private TableSessionService tableSessionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<List<DiningTable>> tableListCaptor;

    private OrderTableSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService =
                new OrderTableSyncService(tableRepository, tableSessionResolver, tableSessionService, eventPublisher);
    }

    @Test
    void recalcTableStatus_whenStatusChanges_evictsCacheAndPublishesEvent() {
        DiningTable table = DiningTable.builder()
                .id(1L)
                .tableNumber("101")
                .tableCode("TBL-101")
                .status(TableStatus.OCCUPIED)
                .build();

        OrderItem item =
                OrderItem.builder().id(10L).status(OrderItemStatus.FINISHED).build();

        Order order = Order.builder()
                .id(100L)
                .status(OrderStatus.SERVING)
                .table(table)
                .orderItems(Set.of(item))
                .build();

        syncService.recalcTableStatus(order);

        assertThat(table.getStatus()).isEqualTo(TableStatus.WAITING_FOR_PAYMENT);
        verify(tableRepository).saveAll(tableListCaptor.capture());
        assertThat(tableListCaptor.getValue()).containsExactly(table);

        verify(tableSessionService).evictTableSessionCachesForTables(tableListCaptor.capture());
        assertThat(tableListCaptor.getValue()).containsExactly(table);

        verify(eventPublisher).publishEvent(any(TableChangeEvent.class));
    }

    @Test
    void recalcTableStatus_whenStatusUnchanged_doesNotEvictOrPublish() {
        DiningTable table = DiningTable.builder()
                .id(1L)
                .tableNumber("101")
                .tableCode("TBL-101")
                .status(TableStatus.OCCUPIED)
                .build();

        OrderItem item =
                OrderItem.builder().id(10L).status(OrderItemStatus.COOKING).build();

        Order order = Order.builder()
                .id(100L)
                .status(OrderStatus.SERVING)
                .table(table)
                .orderItems(Set.of(item))
                .build();

        syncService.recalcTableStatus(order);

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        verify(tableRepository, never()).saveAll(any());
        verify(tableSessionService, never()).evictTableSessionCachesForTables(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void recalcTableStatus_withTableSession_syncsAllMemberTables() {
        TableSession session = TableSession.builder().id(50L).build();
        DiningTable table1 = DiningTable.builder()
                .id(1L)
                .tableNumber("101")
                .tableCode("TBL-101")
                .status(TableStatus.OCCUPIED)
                .build();
        DiningTable table2 = DiningTable.builder()
                .id(2L)
                .tableNumber("102")
                .tableCode("TBL-102")
                .status(TableStatus.OCCUPIED)
                .build();

        when(tableSessionResolver.getActiveMemberTables(50L)).thenReturn(List.of(table1, table2));

        Order order = Order.builder()
                .id(100L)
                .status(OrderStatus.COMPLETED)
                .tableSession(session)
                .build();

        syncService.recalcTableStatus(order);

        assertThat(table1.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(table2.getStatus()).isEqualTo(TableStatus.AVAILABLE);

        verify(tableRepository).saveAll(tableListCaptor.capture());
        assertThat(tableListCaptor.getValue()).containsExactly(table1, table2);

        verify(tableSessionService).evictTableSessionCachesForTables(tableListCaptor.capture());
        assertThat(tableListCaptor.getValue()).containsExactly(table1, table2);

        verify(eventPublisher).publishEvent(any(TableChangeEvent.class));
    }

    @Test
    void releaseTable_updatesStatusToAvailableAndEvictsCacheAndPublishesEvent() {
        DiningTable table = DiningTable.builder()
                .id(1L)
                .tableNumber("101")
                .tableCode("TBL-101")
                .status(TableStatus.WAITING_FOR_PAYMENT)
                .build();

        Order order = Order.builder().id(100L).table(table).build();

        syncService.releaseTable(order);

        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        verify(tableRepository).saveAll(tableListCaptor.capture());
        assertThat(tableListCaptor.getValue()).containsExactly(table);

        verify(tableSessionService).evictTableSessionCachesForTables(tableListCaptor.capture());
        assertThat(tableListCaptor.getValue()).containsExactly(table);

        verify(eventPublisher).publishEvent(any(TableChangeEvent.class));
    }

    @Test
    void recalcTableStatus_whenOrderNull_doesNothing() {
        syncService.recalcTableStatus(null);
        verify(tableRepository, never()).saveAll(any());
        verify(tableSessionService, never()).evictTableSessionCachesForTables(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void releaseTable_whenOrderNull_doesNothing() {
        syncService.releaseTable(null);
        verify(tableRepository, never()).saveAll(any());
        verify(tableSessionService, never()).evictTableSessionCachesForTables(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
