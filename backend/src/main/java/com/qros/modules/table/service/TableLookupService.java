package com.qros.modules.table.service;

import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableServiceRequestRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Module-boundary lookup facade for dining tables and table service requests.
 */
@Service
@RequiredArgsConstructor
public class TableLookupService {

    private final DiningTableRepository diningTableRepository;
    private final TableServiceRequestRepository tableServiceRequestRepository;

    public List<DiningTable> findAllTablesOrdered() {
        return diningTableRepository.findAllByOrderByTableNumberAsc();
    }

    public List<DiningTable> saveAllTables(@NonNull Iterable<DiningTable> tables) {
        return diningTableRepository.saveAll(tables);
    }

    public List<TableServiceRequest> findServiceRequestsByStatuses(
            Collection<ServiceRequestStatus> statuses, Long tableId, Long claimedByUserId) {
        return tableServiceRequestRepository.findByStatusesOrderByCreatedAtAsc(statuses, tableId, claimedByUserId);
    }

    public List<TableServiceRequest> findServiceRequestsByCreatedAtRange(
            LocalDateTime from, LocalDateTime toExclusive) {
        return tableServiceRequestRepository.findByCreatedAtRange(from, toExclusive);
    }
}
