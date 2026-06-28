import { useCallback, useEffect, useRef, useState } from 'react';

import { useAuth } from '@features/auth';
import { orderService } from '@entities/order/api/orderService.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';

const useKitchenOrders = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [processingItems, setProcessingItems] = useState(new Set());
  const [now, setNow] = useState(() => Date.now());
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
  const { user } = useAuth();
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
        showError('Không thể cập nhật trạng thái món');
        fetchKitchenOrders({ silent: true });
      } finally {
        setProcessingItems((current) => {
          const next = new Set(current);
          next.delete(itemId);
          return next;
        });
      }
    },
    [fetchKitchenOrders, processingItems, showError, user?.userId],
  );

  return {
    orders,
    loading,
    refreshing,
    processingItems,
    now,
    lastUpdatedAt,
    fetchKitchenOrders,
    updateStatus,
  };
};

export default useKitchenOrders;
