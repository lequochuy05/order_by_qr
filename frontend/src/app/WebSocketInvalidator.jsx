import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { playNotificationSound } from '@shared/lib/notificationSound.js';
import { clearAnalyticsCache } from '@entities/analytics/api/analyticsService.js';
import { categoryService } from '@entities/category';
import { comboService } from '@entities/combo';
import { inventoryService } from '@entities/inventory';
import { menuItemService } from '@entities/menu-item';
import { userService } from '@entities/user';
import { voucherService } from '@entities/voucher';
import { useAuth } from '@features/auth';
import { useLocation } from 'react-router-dom';
import { useEffect, useRef } from 'react';
import useSettingsStore from '@shared/model/settingsStore.js';
import { canSubscribeToOperations, isCustomerMenuPath } from './webSocketAccess.js';

/**
 * WebSocket Invalidator Layer (Sprint 3D)
 * Hứng toàn bộ sự kiện từ WebSocket và quyết định invalidate query nào.
 * Giải quyết bài toán Realtime phân mảnh ở các Component nhỏ.
 */
export const WebSocketInvalidator = () => {
  const { user } = useAuth();
  const location = useLocation();
  const canSubscribeOperations = canSubscribeToOperations(user, location.pathname);
  const canSubscribeProtectedTopics = Boolean(user) && canSubscribeOperations;
  const canSubscribePublicCatalog = isCustomerMenuPath(location.pathname);
  const canSubscribeCatalog = Boolean(user) || canSubscribePublicCatalog;
  const canSubscribeSettings = Boolean(user) || canSubscribePublicCatalog;
  const settings = useSettingsStore((state) => state.settings);
  const fetchSettings = useSettingsStore((state) => state.fetchSettings);
  const refreshSettings = useSettingsStore((state) => state.invalidateAndRefetch);
  const tableInvalidationTimerRef = useRef(null);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  useEffect(
    () => () => {
      if (tableInvalidationTimerRef.current) {
        window.clearTimeout(tableInvalidationTimerRef.current);
      }
    },
    [],
  );

  const scheduleTableInvalidation = () => {
    if (tableInvalidationTimerRef.current) return;
    tableInvalidationTimerRef.current = window.setTimeout(() => {
      tableInvalidationTimerRef.current = null;
      queryClient.invalidateQueries({ queryKey: queryKeys.tables.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.tables.board });
      queryClient.invalidateQueries({ queryKey: queryKeys.orders.active });
    }, 300);
  };

  // Lắng nghe /topic/tables trong khu vực quản trị
  useWebSocket(
    canSubscribeOperations ? '/topic/tables' : null,
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        // Gom các event gần nhau như UPDATED + PAYMENT_SUCCESS thành một lượt refetch.
        scheduleTableInvalidation();
      }
    },
    { scope: 'admin' },
  );

  // Lắng nghe /topic/orders (Cập nhật Trạng thái đơn, Lịch sử)
  useWebSocket(
    canSubscribeOperations ? '/topic/orders' : null,
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        // Có đơn mới / duyệt đơn -> Báo chuông cho Admin
        if (settings.newOrderNotificationEnabled !== false) {
          playNotificationSound();
        }

        // Cập nhật lại toàn bộ Lịch sử đơn hàng, Active orders, Table board
        queryClient.invalidateQueries({ queryKey: queryKeys.orders.all });
        scheduleTableInvalidation();
        queryClient.invalidateQueries({ queryKey: queryKeys.analytics.all });
        clearAnalyticsCache();
      }
    },
    { scope: 'admin' },
  );

  // Lắng nghe cập nhật catalog công khai cho trang khách hàng
  const handleCatalogUpdate = (message) => {
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      categoryService.clearCache?.();
      menuItemService.clearCache?.();
      comboService.clearCache?.();
      queryClient.invalidateQueries({ queryKey: queryKeys.menu.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.combos.all });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    }
  };

  useWebSocket(canSubscribeCatalog ? '/topic/menu' : null, handleCatalogUpdate, {
    scope: 'public',
  });
  useWebSocket(canSubscribeCatalog ? '/topic/combos' : null, handleCatalogUpdate, {
    scope: 'public',
  });
  useWebSocket(canSubscribeCatalog ? '/topic/categories' : null, handleCatalogUpdate, {
    scope: 'public',
  });
  useWebSocket(
    canSubscribeProtectedTopics ? '/topic/users' : null,
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        userService.clearCache?.();
        queryClient.invalidateQueries({ queryKey: queryKeys.users.all });
      }
    },
    { scope: 'admin' },
  );

  useWebSocket(
    canSubscribeProtectedTopics ? '/topic/vouchers' : null,
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        voucherService.clearCache?.();
        queryClient.invalidateQueries({ queryKey: queryKeys.vouchers.all });
      }
    },
    { scope: 'admin' },
  );

  useWebSocket(
    canSubscribeProtectedTopics ? '/topic/promotions' : null,
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        queryClient.invalidateQueries({ queryKey: queryKeys.promotions.all });
      }
    },
    { scope: 'admin' },
  );

  useWebSocket(
    canSubscribeProtectedTopics ? '/topic/inventory' : null,
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        inventoryService.clearCache?.();
        queryClient.invalidateQueries({ queryKey: queryKeys.inventory.all });
        handleCatalogUpdate(message);
      }
    },
    { scope: 'admin' },
  );

  useWebSocket(
    canSubscribeSettings ? '/topic/settings' : null,
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        queryClient.invalidateQueries({ queryKey: queryKeys.settings.all });
        refreshSettings();
        if (canSubscribePublicCatalog) handleCatalogUpdate(message);
      }
    },
    { scope: canSubscribePublicCatalog ? 'public' : 'admin' },
  );

  return null;
};
