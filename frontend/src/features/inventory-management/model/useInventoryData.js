import { useCallback, useEffect, useRef, useState } from 'react';

import { inventoryService } from '@features/inventory-management/api/inventoryService.js';
import { INVENTORY_PAGE_SIZE } from '@features/inventory-management/lib/inventoryConstants.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { playNotificationSound } from '@shared/lib/notificationSound.js';
import { showErrorToast } from '@shared/lib/toast.js';

const useInventoryData = ({ onInventoryUpdated } = {}) => {
  const [items, setItems] = useState([]);
  const [summary, setSummary] = useState(null);
  const [movements, setMovements] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [stockFilter, setStockFilter] = useState('ALL');
  const [activeTab, setActiveTab] = useState('items');
  const [loading, setLoading] = useState(false);
  const [movementsLoading, setMovementsLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const debouncedSearchTerm = useDebouncedValue(searchTerm);

  const isMountedRef = useRef(true);
  const inventoryFetchSeqRef = useRef(0);
  const movementFetchSeqRef = useRef(0);

  const fetchInventoryItems = useCallback(
    async (showLoading = false, { force = false } = {}) => {
      const fetchSeq = ++inventoryFetchSeqRef.current;
      if (showLoading) setLoading(true);

      try {
        const inventoryData = await inventoryService.getItemPage({
          page: currentPage,
          size: INVENTORY_PAGE_SIZE,
          keyword: debouncedSearchTerm.trim() || undefined,
          stockFilter,
          force,
        });
        if (!isMountedRef.current || fetchSeq !== inventoryFetchSeqRef.current) return;

        setItems(inventoryData.content || []);
        setTotalPages(inventoryData.totalPages || 0);
        setTotalElements(inventoryData.totalElements || 0);
      } catch (error) {
        if (!isMountedRef.current || fetchSeq !== inventoryFetchSeqRef.current) return;
        showErrorToast(error);
      } finally {
        if (isMountedRef.current && fetchSeq === inventoryFetchSeqRef.current) {
          setLoading(false);
        }
      }
    },
    [currentPage, debouncedSearchTerm, stockFilter],
  );

  const fetchInventorySummary = useCallback(async ({ force = false } = {}) => {
    try {
      const data = await inventoryService.getSummary({ force });
      if (isMountedRef.current) setSummary(data);
    } catch (error) {
      if (isMountedRef.current) showErrorToast(error);
    }
  }, []);

  const fetchMovements = useCallback(async (showLoading = false, { force = false } = {}) => {
    const fetchSeq = ++movementFetchSeqRef.current;
    if (showLoading) setMovementsLoading(true);

    try {
      const movementData = await inventoryService.getMovements({ force });
      if (!isMountedRef.current || fetchSeq !== movementFetchSeqRef.current) return;
      setMovements(movementData || []);
    } catch (error) {
      if (!isMountedRef.current || fetchSeq !== movementFetchSeqRef.current) return;
      showErrorToast(error);
    } finally {
      if (isMountedRef.current && fetchSeq === movementFetchSeqRef.current) {
        setMovementsLoading(false);
      }
    }
  }, []);

  const refreshAfterMutation = useCallback(() => {
    fetchInventoryItems(false, { force: true });
    fetchInventorySummary({ force: true });
    if (activeTab === 'history' || movements.length > 0) {
      fetchMovements(false, { force: true });
    }
  }, [activeTab, fetchInventoryItems, fetchInventorySummary, fetchMovements, movements.length]);

  const refreshInventoryView = useCallback(
    (showLoading = true) => {
      fetchInventoryItems(showLoading, { force: true });
      fetchInventorySummary({ force: true });
      if (activeTab === 'history') {
        fetchMovements(showLoading, { force: true });
      }
    },
    [activeTab, fetchInventoryItems, fetchInventorySummary, fetchMovements],
  );

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    fetchInventoryItems(true);
  }, [fetchInventoryItems]);

  useEffect(() => {
    fetchInventorySummary({ force: true });
  }, [fetchInventorySummary]);

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearchTerm, stockFilter]);

  useEffect(() => {
    if (activeTab === 'history' && movements.length === 0) {
      fetchMovements(true);
    }
  }, [activeTab, fetchMovements, movements.length]);

  useWebSocket('/topic/inventory', (message) => {
    if (message !== 'UPDATED' && (typeof message !== 'object' || message === null)) return;

    playNotificationSound();
    refreshAfterMutation();
    onInventoryUpdated?.();
  });

  return {
    items,
    summary,
    movements,
    searchTerm,
    stockFilter,
    activeTab,
    loading,
    movementsLoading,
    currentPage,
    totalPages,
    totalElements,
    setSearchTerm,
    setStockFilter,
    setActiveTab,
    setCurrentPage,
    refreshAfterMutation,
    refreshInventoryView,
  };
};

export default useInventoryData;
