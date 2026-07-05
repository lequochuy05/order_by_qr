import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { playNotificationSound } from '@shared/lib/notificationSound.js';
import { clearAnalyticsCache } from '@entities/analytics/api/analyticsService.js';
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
  const canSubscribePublicCatalog = isCustomerMenuPath(location.pathname);
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
      queryClient.invalidateQueries({ queryKey: queryKeys.menu.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.combos.all });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    }
  };

  useWebSocket(canSubscribePublicCatalog ? '/topic/menu' : null, handleCatalogUpdate, {
    scope: 'public',
  });
  useWebSocket(canSubscribePublicCatalog ? '/topic/combos' : null, handleCatalogUpdate, {
    scope: 'public',
  });
  useWebSocket(canSubscribePublicCatalog ? '/topic/categories' : null, handleCatalogUpdate, {
    scope: 'public',
  });
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
