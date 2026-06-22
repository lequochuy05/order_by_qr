import { useCallback, useMemo, useState } from 'react';

import { getOrderHistoryDateRange } from '../lib/orderHistoryDates.js';

const useOrderHistoryFilters = (itemsPerPage = 10) => {
  const [currentPage, setCurrentPage] = useState(0);
  const [orderId, setOrderId] = useState('');
  const [tableNumber, setTableNumber] = useState('');
  const [status, setStatus] = useState('');
  const [datePreset, setDatePreset] = useState('today');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');
  const [appliedFilters, setAppliedFilters] = useState(() => {
    const range = getOrderHistoryDateRange('today');
    return {
      orderId: '',
      tableNumber: '',
      status: '',
      startDate: range.startDate,
      endDate: range.endDate,
    };
  });

  const filterParams = useMemo(
    () => ({
      ...(appliedFilters.orderId && { orderId: appliedFilters.orderId }),
      ...(appliedFilters.tableNumber && { tableNumber: appliedFilters.tableNumber }),
      ...(appliedFilters.status && { status: appliedFilters.status }),
      ...(appliedFilters.startDate && { from: appliedFilters.startDate }),
      ...(appliedFilters.endDate && { to: appliedFilters.endDate }),
    }),
    [appliedFilters],
  );

  const queryParams = useMemo(
    () => ({
      ...filterParams,
      page: currentPage,
      size: itemsPerPage,
    }),
    [currentPage, filterParams, itemsPerPage],
  );

  const applyFilters = useCallback(() => {
    const dateRange =
      datePreset === 'custom'
        ? { startDate: customStartDate, endDate: customEndDate }
        : getOrderHistoryDateRange(datePreset);

    setAppliedFilters({
      orderId: orderId.trim(),
      tableNumber: tableNumber.trim(),
      status,
      startDate: dateRange.startDate,
      endDate: dateRange.endDate,
    });
    setCurrentPage(0);
  }, [customEndDate, customStartDate, datePreset, orderId, status, tableNumber]);

  return {
    currentPage,
    orderId,
    tableNumber,
    status,
    datePreset,
    customStartDate,
    customEndDate,
    filterParams,
    queryParams,
    setCurrentPage,
    setOrderId,
    setTableNumber,
    setStatus,
    setDatePreset,
    setCustomStartDate,
    setCustomEndDate,
    applyFilters,
  };
};

export default useOrderHistoryFilters;
