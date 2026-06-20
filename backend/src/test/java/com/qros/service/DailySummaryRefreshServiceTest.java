package com.qros.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.analytics.repository.DailySummaryRepository;
import com.qros.modules.analytics.service.DailySummaryRefreshService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class DailySummaryRefreshServiceTest {

    @Mock
    DailySummaryRepository dailySummaryRepository;

    @Mock
    CacheManager cacheManager;

    @Mock
    TransactionSideEffectService sideEffects;

    @Mock
    Cache analyticsCache;

    @Mock
    Cache dashboardCache;

    private DailySummaryRefreshService refreshService;

    @BeforeEach
    void setUp() {
        refreshService = new DailySummaryRefreshService(dailySummaryRepository, cacheManager, sideEffects);
    }

    @Test
    void refreshDateRebuildsRevenueAndItemSummariesInOrder() {
        LocalDate businessDate = LocalDate.of(2026, 6, 19);

        refreshService.refreshDate(businessDate);

        InOrder inOrder = inOrder(dailySummaryRepository);
        inOrder.verify(dailySummaryRepository).ensureRevenueSummaryRow(businessDate);
        inOrder.verify(dailySummaryRepository).lockRevenueSummaryRow(businessDate);
        inOrder.verify(dailySummaryRepository).refreshRevenueSummary(businessDate);
        inOrder.verify(dailySummaryRepository).deleteStaffSalesSummary(businessDate);
        inOrder.verify(dailySummaryRepository).refreshStaffSalesSummary(businessDate);
        inOrder.verify(dailySummaryRepository).deleteItemSalesSummary(businessDate);
        inOrder.verify(dailySummaryRepository).refreshItemSalesSummary(businessDate);
        verify(sideEffects).afterCommit(org.mockito.ArgumentMatchers.any(Runnable.class), anyString());
    }

    @Test
    void clearsAnalyticsCachesOnlyThroughAfterCommitCallback() {
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        when(cacheManager.getCache(CacheNames.ANALYTICS)).thenReturn(analyticsCache);
        when(cacheManager.getCache(CacheNames.STATS_DASHBOARD)).thenReturn(dashboardCache);

        refreshService.refreshDate(LocalDate.of(2026, 6, 19));

        verify(sideEffects).afterCommit(callback.capture(), anyString());
        callback.getValue().run();

        verify(analyticsCache).clear();
        verify(dashboardCache).clear();
    }
}
