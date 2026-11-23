import { useCallback, useEffect, useRef, useState } from 'react';

import { useAuth } from '@features/auth';
import { comboService } from '@entities/combo';
import { menuItemService } from '@entities/menu-item';
import { orderService } from '@entities/order/api/orderService.js';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useTranslation } from '@shared/lib/i18n';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

export const getAvailabilityTargetKey = (target) => {
  if (!target) return null;
  if (target.comboId) return `combo-${target.comboId}`;
  if (target.menuItemId) return `menu-${target.menuItemId}`;
  return null;
};

const useKitchenOrders = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [processingItems, setProcessingItems] = useState(new Set());
  const [togglingAvailabilityKeys, setTogglingAvailabilityKeys] = useState(new Set());
  const [now, setNow] = useState(() => Date.now());
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
  const { user } = useAuth();
  const { isEn } = useTranslation('kitchen');
  const { showError } = useStatusModal();
  const isMountedRef = useRef(true);
  const fetchSeqRef = useRef(0);

  const fetchKitchenOrders = useCallback(
    async ({ silent = false } = {}) => {
      const fetchSeq = ++fetchSeqRef.current;
      if (!silent) setRefreshing(true);

      try {
        const data = await orderService.getKitchenOrders();
        if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
        setOrders(Array.isArray(data) ? data : []);
        setLastUpdatedAt(new Date());
      } catch (error) {
        if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
        console.error('Failed to fetch kitchen orders:', error);
        showError('Không thể tải danh sách đơn hàng nhà bếp');
      } finally {
        if (isMountedRef.current && fetchSeq === fetchSeqRef.current) {
          setLoading(false);
          setRefreshing(false);
        }
      }
    },
    [showError],
  );

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    fetchKitchenOrders({ silent: true });
  }, [fetchKitchenOrders]);

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(intervalId);
  }, []);

  const updateStatus = useCallback(
    async (itemId, newStatus) => {
      if (processingItems.has(itemId)) return;
      setProcessingItems((current) => new Set(current).add(itemId));
      setOrders((current) =>
        current.map((order) => ({
          ...order,
          orderItems: order.orderItems.map((item) =>
            item.id === itemId
              ? { ...item, status: newStatus, updatedAt: new Date().toISOString() }
              : item,
          ),
        })),
      );

      try {
        await orderService.updateItemStatus(itemId, newStatus, user?.userId);
      } catch {
        // H6+H7 fix: guard state updates and rollback fetch against unmounted component
        if (isMountedRef.current) {
          showError('Không thể cập nhật trạng thái món');
          fetchKitchenOrders({ silent: true });
        }
      } finally {
        if (isMountedRef.current) {
          setProcessingItems((current) => {
            const next = new Set(current);
            next.delete(itemId);
            return next;
          });
        }
      }
    },
    [fetchKitchenOrders, processingItems, showError, user?.userId],
  );

  const batchUpdateStatus = useCallback(
    async (itemIds, newStatus) => {
      if (!itemIds || itemIds.length === 0) return;
      const idsToProcess = itemIds.filter((id) => !processingItems.has(id));
      if (idsToProcess.length === 0) return;

      const idSet = new Set(idsToProcess);
      setProcessingItems((current) => {
        const next = new Set(current);
        idsToProcess.forEach((id) => next.add(id));
        return next;
      });

      setOrders((current) =>
        current.map((order) => ({
          ...order,
          orderItems: order.orderItems.map((item) =>
            idSet.has(item.id)
              ? { ...item, status: newStatus, updatedAt: new Date().toISOString() }
              : item,
          ),
        })),
      );

      try {
        await orderService.batchUpdateItemStatus(idsToProcess, newStatus, user?.userId);
      } catch {
        // H6+H7 fix: guard state updates and rollback fetch against unmounted component
        if (isMountedRef.current) {
          showError('Không thể cập nhật trạng thái các món');
          fetchKitchenOrders({ silent: true });
        }
      } finally {
        if (isMountedRef.current) {
          setProcessingItems((current) => {
            const next = new Set(current);
            idsToProcess.forEach((id) => next.delete(id));
            return next;
          });
        }
      }
    },
    [fetchKitchenOrders, processingItems, showError, user?.userId],
  );

  const cancelItem = useCallback(
    async (itemId, reason) => {
      if (processingItems.has(itemId)) return;
      setProcessingItems((current) => new Set(current).add(itemId));
      setOrders((current) =>
        current
          .map((order) => ({
            ...order,
            orderItems: order.orderItems.filter((item) => item.id !== itemId),
          }))
          .filter((order) => order.orderItems.length > 0),
      );

      try {
        await orderService.cancelKitchenItem(itemId, reason);
      } catch {
        // H6+H7 fix: guard state updates and rollback fetch against unmounted component
        if (isMountedRef.current) {
          showError('Không thể hủy món ăn');
          fetchKitchenOrders({ silent: true });
        }
      } finally {
        if (isMountedRef.current) {
          setProcessingItems((current) => {
            const next = new Set(current);
            next.delete(itemId);
            return next;
          });
        }
      }
    },
    [fetchKitchenOrders, processingItems, showError],
  );

  const handleToggleAvailability = useCallback(
    async (target) => {
      if (!target) return null;
      const { menuItemId, comboId, itemName, isAvailable, version } = target;
      const targetKey = getAvailabilityTargetKey({ menuItemId, comboId });
      if (!targetKey || togglingAvailabilityKeys.has(targetKey)) return null;

      setTogglingAvailabilityKeys((current) => new Set(current).add(targetKey));

      try {
        const res = comboId
          ? await comboService.toggleAvailable(comboId, version)
          : await menuItemService.toggleAvailable(menuItemId, version);
        const nextAvailable = res?.available ?? !isAvailable;

        if (isMountedRef.current) {
          setOrders((current) =>
            current.map((order) => ({
              ...order,
              orderItems: (order.orderItems || []).map((orderItem) => {
                if (comboId && orderItem.combo?.id === comboId) {
                  return {
                    ...orderItem,
                    combo: { ...orderItem.combo, available: nextAvailable },
                  };
                }
                if (menuItemId && orderItem.menuItem?.id === menuItemId) {
                  return {
                    ...orderItem,
                    menuItem: { ...orderItem.menuItem, available: nextAvailable },
                  };
                }
                return orderItem;
              }),
            })),
          );
        }

        queryClient.invalidateQueries({ queryKey: queryKeys.menu.all });
        queryClient.invalidateQueries({ queryKey: queryKeys.combos.all });
        fetchKitchenOrders({ silent: true });

        const displayName = itemName || (isEn ? 'Item' : 'Món');
        showSuccessToast(
          nextAvailable
            ? isEn
              ? `Re-enabled "${displayName}" on the menu.`
              : `Đã mở bán lại "${displayName}".`
            : isEn
              ? `Marked "${displayName}" as out of stock.`
              : `Đã báo tạm hết món: "${displayName}".`,
        );

        return nextAvailable;
      } catch (error) {
        showErrorToast(error);
        return null;
      } finally {
        if (isMountedRef.current) {
          setTogglingAvailabilityKeys((current) => {
            const next = new Set(current);
            next.delete(targetKey);
            return next;
          });
        }
      }
    },
    [fetchKitchenOrders, isEn, togglingAvailabilityKeys],
  );

  return {
    orders,
    loading,
    refreshing,
    processingItems,
    togglingAvailabilityKeys,
    now,
    lastUpdatedAt,
    fetchKitchenOrders,
    updateStatus,
    batchUpdateStatus,
    cancelItem,
    handleToggleAvailability,
  };
};

export default useKitchenOrders;
