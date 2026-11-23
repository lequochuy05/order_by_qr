package com.qros.modules.table.service;

import com.qros.modules.table.dto.request.CancelServiceRequestDto;
import com.qros.modules.table.dto.request.CreateServiceRequestDto;
import com.qros.modules.table.dto.response.CustomerServiceRequestsStateResponse;
import com.qros.modules.table.dto.response.TableServiceRequestResponse;
import com.qros.modules.table.mapper.TableServiceRequestMapper;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.ServiceRequestReason;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableServiceRequestRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.event.DomainEvents.ServiceRequestChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TableServiceRequestService {

    public static final int COOLDOWN_SECONDS = 60;
    public static final int MAX_OPEN_REQUESTS_PER_SESSION = 3;
    private static final int MIN_OTHER_NOTE_LENGTH = 2;
    private static final int MAX_NOTE_LENGTH = 200;

    private static final List<ServiceRequestStatus> OPEN_STATUSES =
            List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.CLAIMED);

    private final TableServiceRequestRepository serviceRequestRepository;
    private final DiningTableRepository tableRepository;
    private final TableSessionService tableSessionService;
    private final TableSessionResolver tableSessionResolver;
    private final UserRepository userRepository;
    private final TableServiceRequestMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public CustomerServiceRequestsStateResponse getCustomerState(
            @NonNull String tableCode, @NonNull String sessionToken) {
        TableSession session = tableSessionService.requireOpenSessionForRead(tableCode, sessionToken);
        DiningTable table = requireTableByCode(tableCode);

        List<TableServiceRequest> sessionRequests =
                serviceRequestRepository.findBySessionIdOrderByCreatedAtDesc(session.getId());
        String displayName = resolveSessionDisplayName(session.getId(), table);

        List<TableServiceRequestResponse> responses = sessionRequests.stream()
                .map(request -> mapper.toResponse(request, displayName))
                .toList();

        int openCount = (int) sessionRequests.stream()
                .filter(request ->
                        request.getStatus() != null && request.getStatus().isOpen())
                .count();

        List<ServiceRequestReason> activeReasons = sessionRequests.stream()
                .filter(request ->
                        request.getStatus() != null && request.getStatus().isOpen())
                .filter(request -> request.getTable() != null
                        && table.getId().equals(request.getTable().getId()))
                .map(TableServiceRequest::getReason)
                .distinct()
                .toList();

        int cooldownRemaining = computeCooldownRemaining(table.getId(), session.getId(), AppTime.now());

        return new CustomerServiceRequestsStateResponse(
                responses, cooldownRemaining, openCount, MAX_OPEN_REQUESTS_PER_SESSION, activeReasons);
    }

    @Transactional
    public TableServiceRequestResponse createCustomerRequest(
            @NonNull String tableCode, @NonNull CreateServiceRequestDto dto) {
        TableSession session = tableSessionService.requireOpenSessionForOrdering(tableCode, dto.sessionToken());
        DiningTable table = requireTableByCode(tableCode);

        String clientRequestId = dto.clientRequestId().trim();
        Optional<TableServiceRequest> existing =
                serviceRequestRepository.findFirstByTableSessionIdAndClientRequestId(session.getId(), clientRequestId);
        if (existing.isPresent()) {
            return mapper.toResponse(existing.get(), resolveSessionDisplayName(session.getId(), table));
        }

        String normalizedNote = normalizeNote(dto.note());
        if (dto.reason() == ServiceRequestReason.OTHER
                && (normalizedNote == null || normalizedNote.length() < MIN_OTHER_NOTE_LENGTH)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "A note of at least 2 characters is required when reason is OTHER.",
                    Map.of("note", "Note must be at least 2 characters for OTHER reason"));
        }

        LocalDateTime now = AppTime.now();
        int cooldownRemaining = computeCooldownRemaining(table.getId(), session.getId(), now);
        if (cooldownRemaining > 0) {
            throw new BusinessException(
                    ErrorCode.SERVICE_REQUEST_COOLDOWN,
                    "Please wait before sending another service request.",
                    Map.of("retryAfterSeconds", cooldownRemaining));
        }

        if (serviceRequestRepository.existsByTableIdAndReasonAndStatusIn(table.getId(), dto.reason(), OPEN_STATUSES)) {
            throw new BusinessException(ErrorCode.SERVICE_REQUEST_DUPLICATE_ACTIVE);
        }

        long openCount = serviceRequestRepository.countByTableSessionIdAndStatusIn(session.getId(), OPEN_STATUSES);
        if (openCount >= MAX_OPEN_REQUESTS_PER_SESSION) {
            throw new BusinessException(ErrorCode.SERVICE_REQUEST_OPEN_LIMIT_REACHED);
        }

        TableServiceRequest request = TableServiceRequest.builder()
                .table(table)
                .tableSession(session)
                .reason(dto.reason())
                .note(normalizedNote)
                .status(ServiceRequestStatus.PENDING)
                .clientRequestId(clientRequestId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        TableServiceRequest saved = serviceRequestRepository.save(request);
        eventPublisher.publishEvent(new ServiceRequestChangeEvent("CREATED", saved.getId(), table.getId()));
        return mapper.toResponse(saved, resolveSessionDisplayName(session.getId(), table));
    }

    @Transactional
    public TableServiceRequestResponse cancelByCustomer(
            @NonNull String tableCode, @NonNull Long requestId, CancelServiceRequestDto dto) {
        if (dto == null || !StringUtils.hasText(dto.sessionToken())) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID);
        }

        TableSession session = tableSessionService.requireOpenSessionForOrdering(tableCode, dto.sessionToken());
        TableServiceRequest request = serviceRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND));

        if (request.getTableSession() == null
                || !session.getId().equals(request.getTableSession().getId())) {
            throw new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND);
        }

        if (request.getStatus() != ServiceRequestStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only pending requests can be cancelled by the customer.");
        }

        LocalDateTime now = AppTime.now();
        request.setStatus(ServiceRequestStatus.CANCELLED);
        request.setCancelledByType("CUSTOMER");
        request.setCancelledReason(normalizeNote(dto.reason()));
        request.setUpdatedAt(now);

        TableServiceRequest saved = serviceRequestRepository.save(request);
        Long tableId = saved.getTable() != null ? saved.getTable().getId() : null;
        eventPublisher.publishEvent(new ServiceRequestChangeEvent("CANCELLED", saved.getId(), tableId));
        return mapper.toResponse(saved, resolveSessionDisplayName(session.getId(), saved.getTable()));
    }

    @Transactional(readOnly = true)
    public List<TableServiceRequestResponse> getStaffQueue(
            String statusFilter, boolean mineOnly, Long tableId, @NonNull String userEmail) {
        User currentUser = requireUserByEmail(userEmail);
        Long claimedByUserId = mineOnly ? currentUser.getId() : null;

        String normalizedStatus =
                StringUtils.hasText(statusFilter) ? statusFilter.trim().toUpperCase(Locale.ROOT) : "OPEN";

        List<TableServiceRequest> requests = switch (normalizedStatus) {
            case "OPEN" ->
                serviceRequestRepository.findByStatusesOrderByCreatedAtAsc(OPEN_STATUSES, tableId, claimedByUserId);
            case "PENDING" ->
                serviceRequestRepository.findByStatusesOrderByCreatedAtAsc(
                        List.of(ServiceRequestStatus.PENDING), tableId, claimedByUserId);
            case "CLAIMED" ->
                serviceRequestRepository.findByStatusesOrderByCreatedAtAsc(
                        List.of(ServiceRequestStatus.CLAIMED), tableId, claimedByUserId);
            case "RESOLVED" ->
                serviceRequestRepository.findByStatusesOrderByCreatedAtDesc(
                        List.of(ServiceRequestStatus.RESOLVED), tableId, claimedByUserId);
            case "CANCELLED" ->
                serviceRequestRepository.findByStatusesOrderByCreatedAtDesc(
                        List.of(ServiceRequestStatus.CANCELLED), tableId, claimedByUserId);
            case "ALL" ->
                serviceRequestRepository.findByStatusesOrderByCreatedAtDesc(
                        List.of(ServiceRequestStatus.values()), tableId, claimedByUserId);
            default ->
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST, "Unsupported service request status filter: " + normalizedStatus);
        };

        Map<Long, String> displayNames = new HashMap<>();
        return requests.stream()
                .map(request -> {
                    Long sessionId = request.getTableSession() != null
                            ? request.getTableSession().getId()
                            : null;
                    String displayName = sessionId != null
                            ? displayNames.computeIfAbsent(
                                    sessionId, id -> resolveSessionDisplayName(id, request.getTable()))
                            : resolveSessionDisplayName(null, request.getTable());
                    return mapper.toResponse(request, displayName);
                })
                .toList();
    }

    @Transactional
    public TableServiceRequestResponse claimRequest(@NonNull Long requestId, @NonNull String userEmail) {
        User currentUser = requireUserByEmail(userEmail);
        TableServiceRequest request = serviceRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND));

        if (request.getStatus() == ServiceRequestStatus.CLAIMED) {
            User assignee = request.getClaimedByUser();
            String claimedByName = assignee != null && StringUtils.hasText(assignee.getFullName())
                    ? assignee.getFullName()
                    : "another staff member";
            Map<String, Object> details = new HashMap<>();
            details.put("claimedByName", claimedByName);
            if (assignee != null && assignee.getId() != null) {
                details.put("claimedByUserId", assignee.getId());
            }
            throw new BusinessException(
                    ErrorCode.SERVICE_REQUEST_ALREADY_CLAIMED,
                    "This service request has already been claimed by " + claimedByName + ".",
                    details);
        }

        if (request.getStatus() != ServiceRequestStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only pending requests can be claimed.");
        }

        LocalDateTime now = AppTime.now();
        request.setStatus(ServiceRequestStatus.CLAIMED);
        request.setClaimedByUser(currentUser);
        request.setClaimedAt(now);
        request.setUpdatedAt(now);

        TableServiceRequest saved = serviceRequestRepository.save(request);
        Long tableId = saved.getTable() != null ? saved.getTable().getId() : null;
        Long sessionId =
                saved.getTableSession() != null ? saved.getTableSession().getId() : null;
        eventPublisher.publishEvent(new ServiceRequestChangeEvent("CLAIMED", saved.getId(), tableId));
        return mapper.toResponse(saved, resolveSessionDisplayName(sessionId, saved.getTable()));
    }

    @Transactional
    public TableServiceRequestResponse resolveRequest(@NonNull Long requestId, @NonNull String userEmail) {
        User currentUser = requireUserByEmail(userEmail);
        TableServiceRequest request = serviceRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND));

        if (request.getStatus() != ServiceRequestStatus.CLAIMED) {
            throw new BusinessException(
                    ErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only claimed requests can be resolved.");
        }

        ensureCanManageClaimedRequest(request, currentUser);

        LocalDateTime now = AppTime.now();
        request.setStatus(ServiceRequestStatus.RESOLVED);
        request.setResolvedByUser(currentUser);
        request.setResolvedAt(now);
        request.setUpdatedAt(now);

        TableServiceRequest saved = serviceRequestRepository.save(request);
        Long tableId = saved.getTable() != null ? saved.getTable().getId() : null;
        Long sessionId =
                saved.getTableSession() != null ? saved.getTableSession().getId() : null;
        eventPublisher.publishEvent(new ServiceRequestChangeEvent("RESOLVED", saved.getId(), tableId));
        return mapper.toResponse(saved, resolveSessionDisplayName(sessionId, saved.getTable()));
    }

    @Transactional
    public TableServiceRequestResponse cancelByStaff(
            @NonNull Long requestId, CancelServiceRequestDto dto, @NonNull String userEmail) {
        User currentUser = requireUserByEmail(userEmail);
        TableServiceRequest request = serviceRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND));

        if (request.getStatus() == null || !request.getStatus().isOpen()) {
            throw new BusinessException(
                    ErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only pending or claimed requests can be cancelled.");
        }

        if (request.getStatus() == ServiceRequestStatus.CLAIMED) {
            ensureCanManageClaimedRequest(request, currentUser);
        }

        LocalDateTime now = AppTime.now();
        request.setStatus(ServiceRequestStatus.CANCELLED);
        request.setCancelledByType(currentUser.getRole().name());
        request.setCancelledReason(dto != null ? normalizeNote(dto.reason()) : null);
        request.setUpdatedAt(now);

        TableServiceRequest saved = serviceRequestRepository.save(request);
        Long tableId = saved.getTable() != null ? saved.getTable().getId() : null;
        Long sessionId =
                saved.getTableSession() != null ? saved.getTableSession().getId() : null;
        eventPublisher.publishEvent(new ServiceRequestChangeEvent("CANCELLED", saved.getId(), tableId));
        return mapper.toResponse(saved, resolveSessionDisplayName(sessionId, saved.getTable()));
    }

    private DiningTable requireTableByCode(String tableCode) {
        return tableRepository
                .findByTableCode(tableCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));
    }

    private User requireUserByEmail(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private int computeCooldownRemaining(Long tableId, Long sessionId, LocalDateTime now) {
        Optional<TableServiceRequest> latest =
                serviceRequestRepository.findTopByTableIdAndTableSessionIdOrderByCreatedAtDesc(tableId, sessionId);
        if (latest.isEmpty() || latest.get().getCreatedAt() == null) {
            return 0;
        }
        long elapsedSeconds =
                Math.max(0L, Duration.between(latest.get().getCreatedAt(), now).getSeconds());
        if (elapsedSeconds >= COOLDOWN_SECONDS) {
            return 0;
        }
        return (int) (COOLDOWN_SECONDS - elapsedSeconds);
    }

    private String resolveSessionDisplayName(Long sessionId, DiningTable fallbackTable) {
        if (sessionId != null) {
            List<DiningTable> memberTables = tableSessionResolver.getActiveMemberTables(sessionId);
            if (!memberTables.isEmpty()) {
                return tableSessionResolver.displayName(memberTables);
            }
        }
        if (fallbackTable != null && StringUtils.hasText(fallbackTable.getTableNumber())) {
            return "Table " + fallbackTable.getTableNumber();
        }
        return "Table";
    }

    private static void ensureCanManageClaimedRequest(TableServiceRequest request, User currentUser) {
        boolean isManagerOrAdmin = currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.MANAGER;
        if (isManagerOrAdmin) {
            return;
        }
        User assignee = request.getClaimedByUser();
        if (assignee == null || !assignee.getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_ASSIGNED_TO_USER);
        }
    }

    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > MAX_NOTE_LENGTH ? trimmed.substring(0, MAX_NOTE_LENGTH) : trimmed;
    }
}
