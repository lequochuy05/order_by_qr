package com.qros.modules.table.service;

import com.qros.modules.table.dto.internal.TableQrMedia;
import com.qros.modules.table.dto.request.CreateDiningTableRequest;
import com.qros.modules.table.dto.request.UpdateDiningTableRequest;
import com.qros.modules.table.dto.request.UpdateTableStatusRequest;
import com.qros.modules.table.dto.response.DiningTableResponse;
import com.qros.modules.table.dto.response.PublicTableResponse;
import com.qros.modules.table.mapper.DiningTableMapper;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.repository.TableSessionTableRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.subscription.SubscriptionEntitlementPort;
import com.qros.shared.transaction.TransactionExecutor;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class DiningTableService {

    private final DiningTableRepository tableRepo;
    private final TableCodeGenerator tableCodeGenerator;
    private final TableQrService tableQrService;
    private final DiningTableMapper diningTableMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionSideEffectService sideEffects;
    private final TableSessionRepository sessionRepository;
    private final TableSessionTableRepository membershipRepository;
    private final TableActiveOrderChecker activeOrderChecker;
    private final TransactionExecutor transactionExecutor;
    private final ObjectProvider<SubscriptionEntitlementPort> entitlementPortProvider;

    public DiningTableService(
            DiningTableRepository tableRepo,
            TableCodeGenerator tableCodeGenerator,
            TableQrService tableQrService,
            DiningTableMapper diningTableMapper,
            ApplicationEventPublisher eventPublisher,
            TransactionSideEffectService sideEffects,
            TableSessionRepository sessionRepository,
            TableSessionTableRepository membershipRepository,
            TableActiveOrderChecker activeOrderChecker) {
        this(
                tableRepo,
                tableCodeGenerator,
                tableQrService,
                diningTableMapper,
                eventPublisher,
                sideEffects,
                sessionRepository,
                membershipRepository,
                activeOrderChecker,
                TransactionExecutor.direct(),
                null);
    }

    public DiningTableService(
            DiningTableRepository tableRepo,
            TableCodeGenerator tableCodeGenerator,
            TableQrService tableQrService,
            DiningTableMapper diningTableMapper,
            ApplicationEventPublisher eventPublisher,
            TransactionSideEffectService sideEffects,
            TableSessionRepository sessionRepository,
            TableSessionTableRepository membershipRepository,
            TableActiveOrderChecker activeOrderChecker,
            TransactionExecutor transactionExecutor) {
        this(
                tableRepo,
                tableCodeGenerator,
                tableQrService,
                diningTableMapper,
                eventPublisher,
                sideEffects,
                sessionRepository,
                membershipRepository,
                activeOrderChecker,
                transactionExecutor,
                null);
    }

    @Autowired
    public DiningTableService(
            DiningTableRepository tableRepo,
            TableCodeGenerator tableCodeGenerator,
            TableQrService tableQrService,
            DiningTableMapper diningTableMapper,
            ApplicationEventPublisher eventPublisher,
            TransactionSideEffectService sideEffects,
            TableSessionRepository sessionRepository,
            TableSessionTableRepository membershipRepository,
            TableActiveOrderChecker activeOrderChecker,
            TransactionExecutor transactionExecutor,
            ObjectProvider<SubscriptionEntitlementPort> entitlementPortProvider) {
        this.tableRepo = tableRepo;
        this.tableCodeGenerator = tableCodeGenerator;
        this.tableQrService = tableQrService;
        this.diningTableMapper = diningTableMapper;
        this.eventPublisher = eventPublisher;
        this.sideEffects = sideEffects;
        this.sessionRepository = sessionRepository;
        this.membershipRepository = membershipRepository;
        this.activeOrderChecker = activeOrderChecker;
        this.transactionExecutor = transactionExecutor != null ? transactionExecutor : TransactionExecutor.direct();
        this.entitlementPortProvider = entitlementPortProvider;
    }

    @Cacheable(value = CacheNames.TABLES, key = "'all_sorted'")
    public List<DiningTableResponse> getAllSorted() {
        return tableRepo.findAllByOrderByTableNumberAsc().stream()
                .sorted(Comparator.comparing(DiningTable::getTableNumber, TableNumberComparator::compare))
                .map(diningTableMapper::toResponse)
                .toList();
    }

    public DiningTable getEntityById(@NonNull Long id) {
        return tableRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));
    }

    private DiningTable getEntityByIdForUpdate(Long id) {
        return tableRepo.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));
    }

    public DiningTableResponse getById(@NonNull Long id) {
        return diningTableMapper.toResponse(getEntityById(id));
    }

    @Cacheable(value = CacheNames.TABLES, key = "'code_' + #tableCode")
    public DiningTableResponse getByCode(@NonNull String tableCode) {
        return tableRepo
                .findByTableCode(tableCode)
                .map(diningTableMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));
    }

    @Cacheable(value = CacheNames.TABLES, key = "'public_code_' + #tableCode")
    public PublicTableResponse getPublicByCode(@NonNull String tableCode) {
        return tableRepo
                .findByTableCode(tableCode)
                .map(diningTableMapper::toPublicTable)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CacheEvict(
            value = {CacheNames.TABLES, CacheNames.STATS_DASHBOARD},
            allEntries = true)
    public DiningTableResponse create(@NonNull CreateDiningTableRequest req) {
        transactionExecutor.readOnly(() -> {
            assertTablePlanLimit();
            if (tableRepo.existsByTableNumberIgnoreCase(req.tableNumber())) {
                throw new BusinessException(ErrorCode.TABLE_NUMBER_EXISTS);
            }
            return null;
        });

        String tableCode = tableCodeGenerator.generate();
        TableQrMedia qrMedia = tableQrService.generate(tableCode);

        try {
            return transactionExecutor.write(() -> {
                assertTablePlanLimit();
                if (tableRepo.existsByTableNumberIgnoreCase(req.tableNumber())) {
                    throw new BusinessException(ErrorCode.TABLE_NUMBER_EXISTS);
                }
                sideEffects.afterRollback(
                        () -> tableQrService.delete(qrMedia.publicId()), "delete rolled back table QR media");

                DiningTable table = DiningTable.builder()
                        .tableNumber(req.tableNumber())
                        .capacity(req.capacity())
                        .status(TableStatus.AVAILABLE)
                        .tableCode(tableCode)
                        .qrCodeUrl(qrMedia.url())
                        .qrCodePublicId(qrMedia.publicId())
                        .build();

                DiningTable saved = tableRepo.save(table);
                eventPublisher.publishEvent(new TableChangeEvent());
                return diningTableMapper.toResponse(saved);
            });
        } catch (RuntimeException exception) {
            deleteUploadedQrSilently(qrMedia.publicId());
            throw exception;
        }
    }

    @Transactional
    @CacheEvict(
            value = {CacheNames.TABLES, CacheNames.STATS_DASHBOARD},
            allEntries = true)
    public DiningTableResponse update(@NonNull Long id, @NonNull UpdateDiningTableRequest req) {
        DiningTable table = getEntityById(id);
        if (tableRepo.existsByTableNumberIgnoreCaseAndIdNot(req.tableNumber(), id)) {
            throw new BusinessException(ErrorCode.TABLE_NUMBER_EXISTS);
        }

        table.setTableNumber(req.tableNumber());
        table.setCapacity(req.capacity());

        DiningTable saved = tableRepo.save(table);
        eventPublisher.publishEvent(new TableChangeEvent());

        return diningTableMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(
            value = {CacheNames.TABLES, CacheNames.STATS_DASHBOARD},
            allEntries = true)
    public DiningTableResponse updateStatus(@NonNull Long id, @NonNull UpdateTableStatusRequest req) {
        DiningTable table = getEntityByIdForUpdate(id);
        assertVersionMatches(table.getVersion(), req.version());
        validateStatusTransition(table, req.status());

        table.setStatus(req.status());

        DiningTable saved = tableRepo.save(table);
        eventPublisher.publishEvent(new TableChangeEvent());

        return diningTableMapper.toResponse(saved);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CacheEvict(
            value = {CacheNames.TABLES, CacheNames.STATS_DASHBOARD},
            allEntries = true)
    public DiningTableResponse regenerateQrCode(@NonNull Long id) {
        transactionExecutor.readOnly(() -> {
            DiningTable existing = getEntityById(id);
            requireTableNotInUse(existing.getId(), "Cannot regenerate QR code while the table is in use");
            return null;
        });

        String newTableCode = tableCodeGenerator.generate();
        TableQrMedia qrMedia = tableQrService.generate(newTableCode);

        try {
            return transactionExecutor.write(() -> {
                DiningTable table = getEntityByIdForUpdate(id);
                requireTableNotInUse(table.getId(), "Cannot regenerate QR code while the table is in use");

                String oldPublicId = table.getQrCodePublicId();
                sideEffects.afterRollback(
                        () -> tableQrService.delete(qrMedia.publicId()),
                        "delete rolled back regenerated table QR media");

                table.setTableCode(newTableCode);
                table.setQrCodeUrl(qrMedia.url());
                table.setQrCodePublicId(qrMedia.publicId());

                DiningTable saved = tableRepo.save(table);

                sideEffects.afterCommit(() -> tableQrService.delete(oldPublicId), "delete old table QR media " + id);

                eventPublisher.publishEvent(new TableChangeEvent());
                return diningTableMapper.toResponse(saved);
            });
        } catch (RuntimeException exception) {
            deleteUploadedQrSilently(qrMedia.publicId());
            throw exception;
        }
    }

    private void deleteUploadedQrSilently(String publicId) {
        try {
            tableQrService.delete(publicId);
        } catch (Exception cleanupEx) {
            log.warn("Failed to clean up uploaded table QR {}: {}", publicId, cleanupEx.getMessage());
        }
    }

    @Transactional
    @CacheEvict(
            value = {CacheNames.TABLES, CacheNames.STATS_DASHBOARD},
            allEntries = true)
    public void delete(@NonNull Long id) {
        DiningTable table = getEntityByIdForUpdate(id);
        requireTableNotInUse(table.getId(), "Cannot delete a table with an open session or active order");
        String publicId = table.getQrCodePublicId();

        tableRepo.delete(table);

        sideEffects.afterCommit(
                () -> tableQrService.delete(publicId), "delete table QR media after table delete " + id);

        eventPublisher.publishEvent(new TableChangeEvent());
    }

    private void assertVersionMatches(Long currentVersion, Long expectedVersion) {
        com.qros.shared.validation.OptimisticVersionGuard.matchIfPresent(currentVersion, expectedVersion, "table");
    }

    private void validateStatusTransition(DiningTable table, TableStatus newStatus) {
        if (table.getStatus() == newStatus) {
            return;
        }

        TableUsage usage = inspectUsage(table.getId());
        if ((newStatus == TableStatus.AVAILABLE || newStatus == TableStatus.INACTIVE) && usage.inUse()) {
            throw new BusinessException(
                    ErrorCode.TABLE_IN_USE,
                    "Cannot set table to " + newStatus + " while it has an open session or active order");
        }

        if (newStatus == TableStatus.WAITING_FOR_PAYMENT) {
            if (!usage.hasActiveOrders()) {
                throw new BusinessException(
                        ErrorCode.TABLE_STATUS_TRANSITION_INVALID,
                        "A table can wait for payment only when it has an active order");
            }
            if (activeOrderChecker.hasUnfinishedItems(table.getId())) {
                throw new BusinessException(
                        ErrorCode.ORDER_HAS_UNFINISHED_ITEMS,
                        "Cannot set table to WAITING_FOR_PAYMENT while kitchen still has unfinished items");
            }
        }
    }

    private void requireTableNotInUse(Long tableId, String message) {
        if (inspectUsage(tableId).inUse()) {
            throw new BusinessException(ErrorCode.TABLE_IN_USE, message);
        }
    }

    private TableUsage inspectUsage(Long tableId) {
        boolean hasOpenSession = sessionRepository.existsByTableIdAndStatus(tableId, TableSessionStatus.OPEN)
                || membershipRepository.existsActiveOpenMembershipByTableId(tableId, TableSessionStatus.OPEN);
        boolean hasActiveOrders = activeOrderChecker.hasActiveOrders(tableId);
        return new TableUsage(hasOpenSession, hasActiveOrders);
    }

    private void assertTablePlanLimit() {
        if (entitlementPortProvider == null) {
            return;
        }
        SubscriptionEntitlementPort port = entitlementPortProvider.getIfAvailable();
        if (port != null) {
            port.assertTableLimitNotExceeded(tableRepo.countByStatusNot(TableStatus.INACTIVE));
        }
    }

    private record TableUsage(boolean hasOpenSession, boolean hasActiveOrders) {
        private boolean inUse() {
            return hasOpenSession || hasActiveOrders;
        }
    }
}
