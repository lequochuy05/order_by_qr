package com.qros.modules.analytics.service;

import com.qros.modules.analytics.repository.DailySummaryRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailySummaryRefreshService {

    private final DailySummaryRepository dailySummaryRepository;
    private final CacheManager cacheManager;
    private final TransactionSideEffectService sideEffects;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshDate(@NonNull LocalDate businessDate) {
        dailySummaryRepository.ensureRevenueSummaryRow(businessDate);
        dailySummaryRepository.lockRevenueSummaryRow(businessDate);
        dailySummaryRepository.refreshRevenueSummary(businessDate);
        dailySummaryRepository.deleteStaffSalesSummary(businessDate);
        dailySummaryRepository.refreshStaffSalesSummary(businessDate);
        dailySummaryRepository.deleteItemSalesSummary(businessDate);
        dailySummaryRepository.refreshItemSalesSummary(businessDate);

        sideEffects.afterCommit(this::clearAnalyticsCaches, "clear analytics caches after summary refresh");
    }

    private void clearAnalyticsCaches() {
        clear(CacheNames.ANALYTICS);
        clear(CacheNames.STATS_DASHBOARD);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
