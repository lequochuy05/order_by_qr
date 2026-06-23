import { useEffect, useRef } from 'react';
import { Loader2 } from 'lucide-react';

import { ErrorBoundary } from '@shared/ui';
import { showBrowserNotification } from '@shared/lib/browserNotification.js';
import { playLoudSound, playNotificationSound } from '@shared/lib/notificationSound.js';
import useSettingsStore from '@shared/model/settingsStore.js';
import KitchenColumn from './KitchenColumn.jsx';
import { KITCHEN_COLUMNS } from './lib/kitchenItems.js';
import useKitchenFilters from './model/useKitchenFilters.js';
import useKitchenNotifications from './model/useKitchenNotifications.js';
import useKitchenOrders from './model/useKitchenOrders.js';
import KitchenFilters from './ui/KitchenFilters.jsx';
import KitchenSummaryCards from './ui/KitchenSummaryCards.jsx';
import KitchenToolbar from './ui/KitchenToolbar.jsx';

const KitchenBoardContent = () => {
  const settings = useSettingsStore((state) => state.settings);
  const overdueMinutes = Number(settings.kitchenOverdueThresholdMinutes || 20);
  const kitchen = useKitchenOrders();
  const filters = useKitchenFilters(kitchen.orders, kitchen.now, overdueMinutes);
  const notifications = useKitchenNotifications(
    () => kitchen.fetchKitchenOrders({ silent: true }),
    settings.newOrderNotificationEnabled !== false,
  );
  const previousOverdueCountRef = useRef(null);

  useEffect(() => {
    const previousCount = previousOverdueCountRef.current;
    previousOverdueCountRef.current = filters.summary.overdue;

    if (
      previousCount !== null &&
      filters.summary.overdue > previousCount &&
      settings.kitchenOverdueNotificationEnabled !== false
    ) {
      playNotificationSound();
      playLoudSound();
      showBrowserNotification('Cảnh báo bếp', {
        body: `${filters.summary.overdue} món đã chờ quá ${overdueMinutes} phút.`,
        tag: 'kitchen-overdue',
      });
    }
  }, [filters.summary.overdue, overdueMinutes, settings.kitchenOverdueNotificationEnabled]);

  return (
    <div className="min-h-screen w-full min-w-0 bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <section className="overflow-hidden rounded-3xl bg-white px-5 py-6 shadow-xl shadow-slate-200/70 dark:bg-slate-900 dark:text-slate-100 dark:shadow-none sm:px-7">
        <div className="flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-end">
          <KitchenToolbar
            wakeLock={notifications.wakeLock}
            soundEnabled={notifications.preferences.notificationSound}
            onToggleSound={notifications.toggleSound}
            refreshing={kitchen.refreshing}
            onRefresh={() => kitchen.fetchKitchenOrders()}
          />
        </div>
        <KitchenSummaryCards summary={filters.summary} overdueMinutes={overdueMinutes} />
      </section>

      <KitchenFilters
        searchTerm={filters.searchTerm}
        categoryFilter={filters.categoryFilter}
        attentionFilter={filters.attentionFilter}
        categories={filters.categories}
        lastUpdatedAt={kitchen.lastUpdatedAt}
        onSearchChange={filters.setSearchTerm}
        onCategoryChange={filters.setCategoryFilter}
        onAttentionChange={filters.setAttentionFilter}
      />

      {kitchen.loading ? (
        <div className="flex h-64 items-center justify-center">
          <Loader2 className="animate-spin text-orange-500" size={42} />
        </div>
      ) : (
        <div className="mt-5 grid gap-5 xl:grid-cols-3">
          {KITCHEN_COLUMNS.map((column) => (
            <KitchenColumn
              key={column.status}
              {...column}
              items={filters.itemsByStatus[column.status]}
              onUpdateStatus={kitchen.updateStatus}
              processingItems={kitchen.processingItems}
            />
          ))}
        </div>
      )}
    </div>
  );
};

const KitchenBoard = () => (
  <ErrorBoundary>
    <KitchenBoardContent />
  </ErrorBoundary>
);

export default KitchenBoard;
