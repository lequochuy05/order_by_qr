package com.qros.modules.order.listener;

import com.qros.modules.order.model.Order;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.service.TableSessionResolver;
import com.qros.shared.cache.CacheNames;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * OrderCacheInvalidationService - Dedicated service for clearing caches related
 * to Order mutations.
 * Centralizes cache eviction logic to avoid scattered @CacheEvict annotations
 * and to gracefully handle manual eviction needs for complex workflows.
 */
@Service
@RequiredArgsConstructor
public class OrderCacheInvalidationService {

    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final TableSessionResolver tableSessionResolver;

    /**
     * Evicts all caches affected by an order mutation (creation, update, status
     * change, deletion).
     *
     * @param order The mutated order
     */
    public void evictAfterOrderMutation(Order order) {
        evictAfterOrderMutation(order, null);
    }

    public void evictAfterOrderMutation(Order order, List<String> preCapturedTableCodes) {
        if (order == null) return;
        Long orderId = order.getId();
        List<String> tableCodes = (preCapturedTableCodes != null && !preCapturedTableCodes.isEmpty())
                ? List.copyOf(preCapturedTableCodes)
                : resolveTableCodes(order);
        eventPublisher.publishEvent(new OrderCacheInvalidationRequest(orderId, tableCodes));
    }

    public List<String> resolveTableCodes(Order order) {
        return tableCodes(order);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderCacheInvalidation(OrderCacheInvalidationRequest request) {
        evict(CacheNames.ORDER_ID_CACHE, request.orderId());

        for (String tableCode : request.tableCodes()) {
            evict(CacheNames.TABLES, "code_" + tableCode);
            evict(CacheNames.TABLES, "public_code_" + tableCode);
        }
        evict(CacheNames.TABLES, "all_sorted");

        clear(CacheNames.ORDER_STATS);
        clear(CacheNames.ANALYTICS);
        clear(CacheNames.STATS_DASHBOARD);
    }

    public record OrderCacheInvalidationRequest(Long orderId, List<String> tableCodes) {}

    private List<String> tableCodes(Order order) {
        if (order == null) {
            return List.of();
        }
        if (order.getTableSession() != null && order.getTableSession().getId() != null) {
            Long sessionId = order.getTableSession().getId();
            List<String> codes = tableSessionResolver.getActiveMemberTables(sessionId).stream()
                    .map(DiningTable::getTableCode)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            if (!codes.isEmpty()) {
                return codes;
            }
            List<String> allSessionCodes = tableSessionResolver.getAllMemberTables(sessionId).stream()
                    .map(DiningTable::getTableCode)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            if (!allSessionCodes.isEmpty()) {
                return allSessionCodes;
            }
        }

        String tableCode = order.getTable() == null ? null : order.getTable().getTableCode();
        return tableCode == null ? List.of() : List.of(tableCode);
    }

    private void evict(String cacheName, Object key) {
        com.qros.shared.cache.CacheHelper.evict(cacheManager, cacheName, key);
    }

    private void clear(String cacheName) {
        com.qros.shared.cache.CacheHelper.clear(cacheManager, cacheName);
    }
}
