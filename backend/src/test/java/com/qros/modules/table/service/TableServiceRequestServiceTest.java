package com.qros.modules.table.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.table.dto.request.CancelServiceRequestDto;
import com.qros.modules.table.dto.request.CreateServiceRequestDto;
import com.qros.modules.table.dto.response.TableServiceRequestResponse;
import com.qros.modules.table.mapper.TableServiceRequestMapper;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.ServiceRequestReason;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableServiceRequestRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.event.DomainEvents.ServiceRequestChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TableServiceRequestServiceTest {

    @Mock
    private TableServiceRequestRepository serviceRequestRepository;

    @Mock
    private DiningTableRepository tableRepository;

    @Mock
    private TableSessionService tableSessionService;

    @Mock
    private TableSessionResolver tableSessionResolver;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TableServiceRequestService service;

    @BeforeEach
    void setUp() {
        service = new TableServiceRequestService(
                serviceRequestRepository,
                tableRepository,
                tableSessionService,
                tableSessionResolver,
                userRepository,
                new TableServiceRequestMapper(),
                eventPublisher);
    }

    @Test
    void createCustomerRequestSavesPendingRequestWithoutChangingTableStatusOnRequestBill() {
        DiningTable table = table(5L, "BAN_05", "05");
        TableSession session = session(100L, table);

        when(tableSessionService.requireOpenSessionForOrdering("BAN_05", "tok-1"))
                .thenReturn(session);
        when(tableRepository.findByTableCode("BAN_05")).thenReturn(Optional.of(table));
        when(serviceRequestRepository.findFirstByTableSessionIdAndClientRequestId(100L, "cid-1"))
                .thenReturn(Optional.empty());
        when(serviceRequestRepository.findTopByTableIdAndTableSessionIdOrderByCreatedAtDesc(5L, 100L))
                .thenReturn(Optional.empty());
        when(serviceRequestRepository.existsByTableIdAndReasonAndStatusIn(
                        eq(5L), eq(ServiceRequestReason.REQUEST_BILL), any()))
                .thenReturn(false);
        when(serviceRequestRepository.countByTableSessionIdAndStatusIn(eq(100L), any()))
                .thenReturn(0L);
        when(serviceRequestRepository.save(any(TableServiceRequest.class))).thenAnswer(invocation -> {
            TableServiceRequest saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        when(tableSessionResolver.getActiveMemberTables(100L)).thenReturn(List.of(table));
        when(tableSessionResolver.displayName(List.of(table))).thenReturn("Table 05");

        TableServiceRequestResponse response = service.createCustomerRequest(
                "BAN_05",
                new CreateServiceRequestDto(
                        "tok-1", ServiceRequestReason.REQUEST_BILL, "  Xin bill giúp ạ  ", "cid-1"));

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.status()).isEqualTo(ServiceRequestStatus.PENDING);
        assertThat(response.reason()).isEqualTo(ServiceRequestReason.REQUEST_BILL);
        assertThat(response.note()).isEqualTo("Xin bill giúp ạ");
        assertThat(response.tableDisplayName()).isEqualTo("Table 05");
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);

        ArgumentCaptor<ServiceRequestChangeEvent> eventCaptor =
                ArgumentCaptor.forClass(ServiceRequestChangeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().action()).isEqualTo("CREATED");
        assertThat(eventCaptor.getValue().requestId()).isEqualTo(11L);
        assertThat(eventCaptor.getValue().tableId()).isEqualTo(5L);
    }

    @Test
    void createCustomerRequestRejectsOtherWithoutMinimumTwoCharNote() {
        DiningTable table = table(5L, "BAN_05", "05");
        TableSession session = session(100L, table);

        when(tableSessionService.requireOpenSessionForOrdering("BAN_05", "tok-1"))
                .thenReturn(session);
        when(tableRepository.findByTableCode("BAN_05")).thenReturn(Optional.of(table));
        when(serviceRequestRepository.findFirstByTableSessionIdAndClientRequestId(100L, "cid-other"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCustomerRequest(
                        "BAN_05", new CreateServiceRequestDto("tok-1", ServiceRequestReason.OTHER, " a ", "cid-other")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(ex.getDetails()).containsKey("note");
                });

        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void createCustomerRequestEnforcesSixtySecondCooldownWithRetryAfterDetails() {
        DiningTable table = table(5L, "BAN_05", "05");
        TableSession session = session(100L, table);
        TableServiceRequest recent = TableServiceRequest.builder()
                .id(1L)
                .table(table)
                .tableSession(session)
                .reason(ServiceRequestReason.WATER_ICE)
                .status(ServiceRequestStatus.PENDING)
                .clientRequestId("cid-prev")
                .build();
        recent.setCreatedAt(AppTime.now().minusSeconds(15));

        when(tableSessionService.requireOpenSessionForOrdering("BAN_05", "tok-1"))
                .thenReturn(session);
        when(tableRepository.findByTableCode("BAN_05")).thenReturn(Optional.of(table));
        when(serviceRequestRepository.findFirstByTableSessionIdAndClientRequestId(100L, "cid-2"))
                .thenReturn(Optional.empty());
        when(serviceRequestRepository.findTopByTableIdAndTableSessionIdOrderByCreatedAtDesc(5L, 100L))
                .thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> service.createCustomerRequest(
                        "BAN_05",
                        new CreateServiceRequestDto("tok-1", ServiceRequestReason.CLEAN_TABLE, null, "cid-2")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_REQUEST_COOLDOWN);
                    assertThat(ex.getDetails().get("retryAfterSeconds")).isInstanceOf(Integer.class);
                    assertThat((Integer) ex.getDetails().get("retryAfterSeconds"))
                            .isBetween(1, 60);
                });
    }

    @Test
    void createCustomerRequestReturnsExistingRequestIdempotentlyEvenDuringCooldown() {
        DiningTable table = table(5L, "BAN_05", "05");
        TableSession session = session(100L, table);
        TableServiceRequest existing = TableServiceRequest.builder()
                .id(77L)
                .table(table)
                .tableSession(session)
                .reason(ServiceRequestReason.WATER_ICE)
                .status(ServiceRequestStatus.PENDING)
                .clientRequestId("cid-dup")
                .build();
        existing.setCreatedAt(AppTime.now().minusSeconds(5));

        when(tableSessionService.requireOpenSessionForOrdering("BAN_05", "tok-1"))
                .thenReturn(session);
        when(tableRepository.findByTableCode("BAN_05")).thenReturn(Optional.of(table));
        when(serviceRequestRepository.findFirstByTableSessionIdAndClientRequestId(100L, "cid-dup"))
                .thenReturn(Optional.of(existing));
        when(tableSessionResolver.getActiveMemberTables(100L)).thenReturn(List.of(table));
        when(tableSessionResolver.displayName(List.of(table))).thenReturn("Table 05");

        TableServiceRequestResponse response = service.createCustomerRequest(
                "BAN_05", new CreateServiceRequestDto("tok-1", ServiceRequestReason.WATER_ICE, null, "cid-dup"));

        assertThat(response.id()).isEqualTo(77L);
        verify(serviceRequestRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createCustomerRequestRejectsDuplicateActiveReasonAndOpenLimit() {
        DiningTable table = table(5L, "BAN_05", "05");
        TableSession session = session(100L, table);

        when(tableSessionService.requireOpenSessionForOrdering("BAN_05", "tok-1"))
                .thenReturn(session);
        when(tableRepository.findByTableCode("BAN_05")).thenReturn(Optional.of(table));
        when(serviceRequestRepository.findFirstByTableSessionIdAndClientRequestId(100L, "cid-x"))
                .thenReturn(Optional.empty());
        when(serviceRequestRepository.findTopByTableIdAndTableSessionIdOrderByCreatedAtDesc(5L, 100L))
                .thenReturn(Optional.empty());
        when(serviceRequestRepository.existsByTableIdAndReasonAndStatusIn(
                        eq(5L), eq(ServiceRequestReason.WATER_ICE), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createCustomerRequest(
                        "BAN_05", new CreateServiceRequestDto("tok-1", ServiceRequestReason.WATER_ICE, null, "cid-x")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_REQUEST_DUPLICATE_ACTIVE));

        when(serviceRequestRepository.existsByTableIdAndReasonAndStatusIn(
                        eq(5L), eq(ServiceRequestReason.UTENSILS_NAPKINS), any()))
                .thenReturn(false);
        when(serviceRequestRepository.countByTableSessionIdAndStatusIn(eq(100L), any()))
                .thenReturn(3L);

        assertThatThrownBy(() -> service.createCustomerRequest(
                        "BAN_05",
                        new CreateServiceRequestDto("tok-1", ServiceRequestReason.UTENSILS_NAPKINS, null, "cid-x")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_REQUEST_OPEN_LIMIT_REACHED));
    }

    @Test
    void customerCancelAllowsPendingAndRejectsClaimed() {
        DiningTable table = table(5L, "BAN_05", "05");
        TableSession session = session(100L, table);
        TableServiceRequest pending = TableServiceRequest.builder()
                .id(12L)
                .table(table)
                .tableSession(session)
                .reason(ServiceRequestReason.WATER_ICE)
                .status(ServiceRequestStatus.PENDING)
                .clientRequestId("cid-12")
                .build();

        when(tableSessionService.requireOpenSessionForOrdering("BAN_05", "tok-1"))
                .thenReturn(session);
        when(serviceRequestRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(pending));
        when(serviceRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tableSessionResolver.getActiveMemberTables(100L)).thenReturn(List.of(table));
        when(tableSessionResolver.displayName(List.of(table))).thenReturn("Table 05");

        TableServiceRequestResponse cancelled =
                service.cancelByCustomer("BAN_05", 12L, new CancelServiceRequestDto("tok-1", "No longer needed"));

        assertThat(cancelled.status()).isEqualTo(ServiceRequestStatus.CANCELLED);
        assertThat(cancelled.cancelledByType()).isEqualTo("CUSTOMER");

        TableServiceRequest claimed = TableServiceRequest.builder()
                .id(13L)
                .table(table)
                .tableSession(session)
                .reason(ServiceRequestReason.CLEAN_TABLE)
                .status(ServiceRequestStatus.CLAIMED)
                .clientRequestId("cid-13")
                .build();
        when(serviceRequestRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(claimed));

        assertThatThrownBy(() -> service.cancelByCustomer("BAN_05", 13L, new CancelServiceRequestDto("tok-1", null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_REQUEST_INVALID_STATE));
    }

    @Test
    void claimRequestEnforcesSingleClaimAndReturnsClaimedByNameOnConflict() {
        DiningTable table = table(5L, "BAN_05", "05");
        TableSession session = session(100L, table);
        User staffA = user(1L, "staffa@qros.test", "Nguyen Van A", UserRole.STAFF);
        User staffB = user(2L, "staffb@qros.test", "Tran Thi B", UserRole.STAFF);

        TableServiceRequest request = TableServiceRequest.builder()
                .id(20L)
                .table(table)
                .tableSession(session)
                .reason(ServiceRequestReason.WATER_ICE)
                .status(ServiceRequestStatus.PENDING)
                .clientRequestId("cid-20")
                .build();

        when(userRepository.findByEmailIgnoreCase("staffa@qros.test")).thenReturn(Optional.of(staffA));
        when(userRepository.findByEmailIgnoreCase("staffb@qros.test")).thenReturn(Optional.of(staffB));
        when(serviceRequestRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(request));
        when(serviceRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tableSessionResolver.getActiveMemberTables(100L)).thenReturn(List.of(table));
        when(tableSessionResolver.displayName(List.of(table))).thenReturn("Table 05");

        TableServiceRequestResponse firstClaim = service.claimRequest(20L, "staffa@qros.test");
        assertThat(firstClaim.status()).isEqualTo(ServiceRequestStatus.CLAIMED);
        assertThat(firstClaim.claimedByName()).isEqualTo("Nguyen Van A");

        assertThatThrownBy(() -> service.claimRequest(20L, "staffb@qros.test"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_REQUEST_ALREADY_CLAIMED);
                    assertThat(ex.getDetails().get("claimedByName")).isEqualTo("Nguyen Van A");
                });
    }

    @Test
    void resolveAndCancelEnforceAssigneeOrManagerPermission() {
        DiningTable table = table(5L, "BAN_05", "05");
        TableSession session = session(100L, table);
        User staffA = user(1L, "staffa@qros.test", "Nguyen Van A", UserRole.STAFF);
        User staffB = user(2L, "staffb@qros.test", "Tran Thi B", UserRole.STAFF);
        User manager = user(3L, "manager@qros.test", "Le Quan Ly", UserRole.MANAGER);

        TableServiceRequest claimedByA = TableServiceRequest.builder()
                .id(30L)
                .table(table)
                .tableSession(session)
                .reason(ServiceRequestReason.ORDER_ASSISTANCE)
                .status(ServiceRequestStatus.CLAIMED)
                .claimedByUser(staffA)
                .clientRequestId("cid-30")
                .build();

        when(userRepository.findByEmailIgnoreCase("staffb@qros.test")).thenReturn(Optional.of(staffB));
        when(userRepository.findByEmailIgnoreCase("manager@qros.test")).thenReturn(Optional.of(manager));
        when(serviceRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(claimedByA));
        when(serviceRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tableSessionResolver.getActiveMemberTables(100L)).thenReturn(List.of(table));
        when(tableSessionResolver.displayName(List.of(table))).thenReturn("Table 05");

        assertThatThrownBy(() -> service.resolveRequest(30L, "staffb@qros.test"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_REQUEST_NOT_ASSIGNED_TO_USER));

        assertThatThrownBy(
                        () -> service.cancelByStaff(30L, new CancelServiceRequestDto(null, "test"), "staffb@qros.test"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_REQUEST_NOT_ASSIGNED_TO_USER));

        TableServiceRequestResponse resolved = service.resolveRequest(30L, "manager@qros.test");
        assertThat(resolved.status()).isEqualTo(ServiceRequestStatus.RESOLVED);
        assertThat(resolved.resolvedByName()).isEqualTo("Le Quan Ly");
    }

    private static DiningTable table(Long id, String tableCode, String tableNumber) {
        return DiningTable.builder()
                .id(id)
                .tableCode(tableCode)
                .tableNumber(tableNumber)
                .status(TableStatus.OCCUPIED)
                .capacity(4)
                .build();
    }

    private static TableSession session(Long id, DiningTable table) {
        return TableSession.builder()
                .id(id)
                .table(table)
                .status(TableSessionStatus.OPEN)
                .build();
    }

    private static User user(Long id, String email, String fullName, UserRole role) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName(fullName)
                .password("encoded")
                .role(role)
                .build();
    }
}
