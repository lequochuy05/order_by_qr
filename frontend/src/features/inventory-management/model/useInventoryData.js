import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { useState, useEffect } from 'react';
import { INVENTORY_PAGE_SIZE } from '../lib/inventoryConstants.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { playNotificationSound } from '@shared/lib/notificationSound.js';
import {
  useInventoryPageQuery,
  useInventorySummaryQuery,
  useStockMovementsQuery,
} from '@entities/inventory';

const useInventoryData = ({ onInventoryUpdated } = {}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [stockFilter, setStockFilter] = useState('ALL');
  const [activeTab, setActiveTab] = useState('items');
  const [currentPage, setCurrentPage] = useState(0);

  const debouncedSearchTerm = useDebouncedValue(searchTerm);

  const { data: inventoryData, isFetching: loading } = useInventoryPageQuery({
    page: currentPage,
    size: INVENTORY_PAGE_SIZE,
    keyword: debouncedSearchTerm.trim() || undefined,
    stockFilter,
  });

  const { data: summary } = useInventorySummaryQuery();

  const { data: movementsData, isFetching: movementsLoading } = useStockMovementsQuery(
    {},
    { enabled: activeTab === 'history' },
  );

  useEffect(() => {
    // eslint-disable-next-line
    setCurrentPage(0);
  }, [debouncedSearchTerm, stockFilter]);

  const refreshAfterMutation = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.inventory.all });
  };

  const refreshInventoryView = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.inventory.all });
  };

  useWebSocket(
    '/topic/inventory',
    (message) => {
      if (message !== 'UPDATED' && (typeof message !== 'object' || message === null)) return;
      playNotificationSound();
      refreshAfterMutation();
      onInventoryUpdated?.();
    },
    { scope: 'admin' },
  );

  return {
    items: inventoryData?.content || [],
    summary,
    movements: movementsData || [],
    searchTerm,
    stockFilter,
    activeTab,
    loading,
    movementsLoading,
    currentPage,
    totalPages: inventoryData?.totalPages || 0,
    totalElements: inventoryData?.totalElements || 0,
    setSearchTerm,
    setStockFilter,
    setActiveTab,
    setCurrentPage,
    refreshAfterMutation,
    refreshInventoryView,
  };
};

export default useInventoryData;
