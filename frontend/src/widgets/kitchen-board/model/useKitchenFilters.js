import { useMemo, useState } from 'react';

import {
  buildKitchenItems,
  groupKitchenItems,
  summarizeKitchenItems,
} from '../lib/kitchenItems.js';

const useKitchenFilters = (orders, now, overdueMinutes) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [attentionFilter, setAttentionFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');

  const kitchenItems = useMemo(
    () => buildKitchenItems(orders, now, overdueMinutes),
    [now, orders, overdueMinutes],
  );
  const categories = useMemo(
    () =>
      [...new Set(kitchenItems.map((item) => item.category))].sort((a, b) =>
        a.localeCompare(b, 'vi'),
      ),
    [kitchenItems],
  );
  const filteredItems = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLocaleLowerCase('vi');
    return kitchenItems.filter((item) => {
      const searchableText = [
        item.itemName,
        item.tableName,
        item.orderId,
        item.notes,
        ...(item.options || []).flatMap((option) => [option.optionName, option.optionValueName]),
      ]
        .join(' ')
        .toLocaleLowerCase('vi');

      return (
        (!normalizedSearch || searchableText.includes(normalizedSearch)) &&
        (categoryFilter === 'ALL' || item.category === categoryFilter) &&
        (attentionFilter === 'ALL' ||
          (attentionFilter === 'OVERDUE' && item.isOverdue) ||
          (attentionFilter === 'NOTES' && item.hasNotes))
      );
    });
  }, [attentionFilter, categoryFilter, kitchenItems, searchTerm]);

  return {
    searchTerm,
    attentionFilter,
    categoryFilter,
    categories,
    itemsByStatus: useMemo(() => groupKitchenItems(filteredItems), [filteredItems]),
    summary: useMemo(() => summarizeKitchenItems(kitchenItems), [kitchenItems]),
    setSearchTerm,
    setAttentionFilter,
    setCategoryFilter,
  };
};

export default useKitchenFilters;
