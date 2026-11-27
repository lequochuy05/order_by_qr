import { useEffect, useMemo, useRef, useState } from 'react';
import { Loader2 } from 'lucide-react';

import { useAuth } from '@entities/session';
import { ErrorBoundary } from '@shared/ui';
import { AiNoteTriageCard } from '@entities/ai';
import { showBrowserNotification } from '@shared/lib/browserNotification.js';
import { playLoudSound, playNotificationSound } from '@shared/lib/notificationSound.js';
import { useSettingsStore } from '@entities/settings';
import KitchenColumn from './KitchenColumn.jsx';
import { aggregateBatchCookingItems, KITCHEN_COLUMNS } from '../lib/kitchenItems.js';
import { useTranslation } from '@shared/lib/i18n';
import useKitchenFilters from '../model/useKitchenFilters.js';
import useKitchenNotifications from '../model/useKitchenNotifications.js';
import useKitchenOrders, { getAvailabilityTargetKey } from '../model/useKitchenOrders.js';
import AiStaffPanel from './AiStaffPanel.jsx';
import BatchCookingView from './BatchCookingView.jsx';
import KitchenFilters from './KitchenFilters.jsx';
import KitchenQuick86Modal from './KitchenQuick86Modal.jsx';
import KitchenRecipeModal from './KitchenRecipeModal.jsx';
import KitchenSummaryCards from './KitchenSummaryCards.jsx';
import KitchenToolbar from './KitchenToolbar.jsx';

const KitchenBoardContent = () => {
  const { user } = useAuth();
  const { isEn } = useTranslation('kitchen');
  const [viewMode, setViewMode] = useState('kanban');
  const [recipeTarget, setRecipeTarget] = useState(null);
  const [isQuick86Open, setIsQuick86Open] = useState(false);
  const [lastAvailabilityUpdate, setLastAvailabilityUpdate] = useState(null);
  const settings = useSettingsStore((state) => state.settings);
  const overdueMinutes = Number(settings.kitchenOverdueThresholdMinutes || 20);
  const kitchen = useKitchenOrders();
  const filters = useKitchenFilters(kitchen.orders, kitchen.now, overdueMinutes);
  const notifications = useKitchenNotifications(
    () => kitchen.fetchKitchenOrders({ silent: true }),
    settings.newOrderNotificationEnabled !== false,
  );
  const previousOverdueCountRef = useRef(null);

  const handleToggleDishAvailability = async (target) => {
    const nextAvailable = await kitchen.handleToggleAvailability(target);
    if (nextAvailable !== null) {
      const targetKey = getAvailabilityTargetKey(target);
      if (targetKey) {
        setLastAvailabilityUpdate({ key: targetKey, isAvailable: nextAvailable, seq: Date.now() });
      }
      setRecipeTarget((current) => {
        if (!current) return current;
        const matchesCombo = target.comboId && current.comboId === target.comboId;
        const matchesMenu =
          !target.comboId && target.menuItemId && current.menuItemId === target.menuItemId;
        return matchesCombo || matchesMenu
          ? { ...current, isAvailable: nextAvailable }
          : current;
      });
    }
    return nextAvailable;
  };

  const recipeAvailabilityKey = getAvailabilityTargetKey(recipeTarget);

  const localizedColumns = useMemo(
    () =>
      KITCHEN_COLUMNS.map((column) => {
        if (column.status === 'PENDING') {
          return {
            ...column,
            title: isEn ? 'Pending' : column.title,
            subtitle: isEn ? 'Priority by longest wait' : column.subtitle,
          };
        }
        if (column.status === 'COOKING') {
          return {
            ...column,
            title: isEn ? 'Cooking' : column.title,
            subtitle: isEn ? 'Items being prepared' : column.subtitle,
          };
        }
        if (column.status === 'FINISHED') {
          return {
            ...column,
            title: isEn ? 'Finished' : column.title,
            subtitle: isEn ? 'Ready to serve or pay' : column.subtitle,
          };
        }
        return column;
      }),
    [isEn],
  );

  const batchCount = useMemo(
    () => aggregateBatchCookingItems(filters.filteredItems).length,
    [filters.filteredItems],
  );

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
      showBrowserNotification(isEn ? 'Kitchen Alert' : 'Cảnh báo bếp', {
        body: isEn
          ? `${filters.summary.overdue} items have been waiting over ${overdueMinutes} mins.`
          : `${filters.summary.overdue} món đã chờ quá ${overdueMinutes} phút.`,
        tag: 'kitchen-overdue',
      });
    }
  }, [filters.summary.overdue, isEn, overdueMinutes, settings.kitchenOverdueNotificationEnabled]);

  return (
    <div className="w-full min-w-0 space-y-6 text-slate-900 dark:text-slate-100">
      <section className="overflow-hidden rounded-3xl border border-[#1E293B] bg-white/90 px-5 py-6 shadow-sm backdrop-blur-sm dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:text-slate-100 sm:px-7">
        <div className="flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-end">
          <KitchenToolbar
            wakeLock={notifications.wakeLock}
            soundEnabled={notifications.preferences.notificationSound}
            onToggleSound={notifications.toggleSound}
            refreshing={kitchen.refreshing}
            onRefresh={() => kitchen.fetchKitchenOrders()}
            onOpenQuick86={() => setIsQuick86Open(true)}
            viewMode={viewMode}
            onViewModeChange={setViewMode}
            batchCount={batchCount}
          />
        </div>
        <KitchenSummaryCards summary={filters.summary} overdueMinutes={overdueMinutes} />
      </section>

      {['ADMIN', 'MANAGER', 'CHEF'].includes(user?.role) && (
        <div className="space-y-4">
          <AiStaffPanel />
          <AiNoteTriageCard namespace="kitchen" />
        </div>
      )}

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
          <Loader2 className="animate-spin text-indigo-600 dark:text-indigo-400" size={42} />
        </div>
      ) : viewMode === 'batch' ? (
        <BatchCookingView
          items={filters.filteredItems}
          onBatchUpdateStatus={kitchen.batchUpdateStatus}
          onUpdateStatus={kitchen.updateStatus}
          onCancelItem={kitchen.cancelItem}
          onOpenRecipe={setRecipeTarget}
          onToggleAvailability={handleToggleDishAvailability}
          processingItems={kitchen.processingItems}
          togglingAvailabilityKeys={kitchen.togglingAvailabilityKeys}
        />
      ) : (
        <div className="mt-5 grid gap-5 xl:grid-cols-3">
          {localizedColumns.map((column) => (
            <KitchenColumn
              key={column.status}
              {...column}
              items={filters.itemsByStatus[column.status]}
              onUpdateStatus={kitchen.updateStatus}
              onCancelItem={kitchen.cancelItem}
              onOpenRecipe={setRecipeTarget}
              onToggleAvailability={handleToggleDishAvailability}
              processingItems={kitchen.processingItems}
              togglingAvailabilityKeys={kitchen.togglingAvailabilityKeys}
            />
          ))}
        </div>
      )}

      <KitchenQuick86Modal
        isOpen={isQuick86Open}
        onClose={() => setIsQuick86Open(false)}
        onToggleAvailability={handleToggleDishAvailability}
        onOpenRecipe={setRecipeTarget}
        togglingAvailabilityKeys={kitchen.togglingAvailabilityKeys}
        lastAvailabilityUpdate={lastAvailabilityUpdate}
      />

      <KitchenRecipeModal
        isOpen={Boolean(recipeTarget)}
        onClose={() => setRecipeTarget(null)}
        target={recipeTarget}
        onToggleAvailability={handleToggleDishAvailability}
        isTogglingAvailability={Boolean(
          recipeAvailabilityKey && kitchen.togglingAvailabilityKeys.has(recipeAvailabilityKey),
        )}
      />
    </div>
  );
};

const KitchenBoard = () => (
  <ErrorBoundary>
    <KitchenBoardContent />
  </ErrorBoundary>
);

export default KitchenBoard;
