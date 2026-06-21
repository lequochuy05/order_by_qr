import { useCallback, useMemo, useState } from 'react';

import { printInvoice } from '@entities/order/lib/invoiceGenerator.js';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { useReconcileOrderMutation } from '../api/orderMutations.js';
import { useOrderAnalyticsQuery, useOrdersHistoryQuery } from '../api/orderQueries.js';
import useOrderHistoryFilters from './useOrderHistoryFilters.js';

const useOrderHistory = () => {
  const [selectedOrder, setSelectedOrder] = useState(null);
  const filters = useOrderHistoryFilters();
  const ordersQuery = useOrdersHistoryQuery(filters.queryParams);
  const analyticsQuery = useOrderAnalyticsQuery(filters.filterParams);
  const reconcileMutation = useReconcileOrderMutation();

  const orders = ordersQuery.data?.content || [];
  const analytics = analyticsQuery.data || { totalOrders: 0, totalRevenue: 0 };
  const loading = ordersQuery.isFetching || analyticsQuery.isFetching;
  const averageOrder = useMemo(
    () => (analytics.totalOrders > 0 ? analytics.totalRevenue / analytics.totalOrders : 0),
    [analytics.totalOrders, analytics.totalRevenue],
  );

  const refreshData = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: queryKeys.orders.history() });
    queryClient.invalidateQueries({ queryKey: ['orders', 'analytics'] });
  }, []);

  const printOrder = useCallback((order) => {
    if (order.status !== 'COMPLETED') {
      showErrorToast('Chỉ có thể in hóa đơn cho đơn hàng đã hoàn tất');
      return;
    }

    printInvoice({
      order,
      table: order.table,
      paidBy: order.paidByName || 'Nhân viên',
      paidAt: order.paymentTime,
    });
  }, []);

  const reconcileOrder = useCallback(
    async (orderId) => {
      try {
        const reconciledOrder = await reconcileMutation.mutateAsync(orderId);
        showSuccessToast(`Tra soát thành công: #${reconciledOrder?.id || orderId}`);
      } catch (error) {
        showErrorToast(error);
      }
    },
    [reconcileMutation],
  );

  return {
    ...filters,
    selectedOrder,
    orders,
    analytics,
    averageOrder,
    loading,
    totalPages: ordersQuery.data?.totalPages || 0,
    totalElements: ordersQuery.data?.totalElements || 0,
    setSelectedOrder,
    refreshData,
    printOrder,
    reconcileOrder,
  };
};

export default useOrderHistory;
