import { useState } from 'react';

const getDefaultDateRange = () => {
  const to = new Date();
  const from = new Date(to);
  from.setDate(to.getDate() - 6);
  return { from, to };
};

const useDateRangeFilter = () => {
  const [dateRange, setDateRange] = useState(getDefaultDateRange);
  const [appliedDateRange, setAppliedDateRange] = useState(dateRange);

  const handleApplyFilters = (resetPage) => {
    setAppliedDateRange({
      from: new Date(dateRange.from),
      to: new Date(dateRange.to),
    });
    resetPage?.();
  };

  return { dateRange, setDateRange, appliedDateRange, handleApplyFilters };
};

export default useDateRangeFilter;
