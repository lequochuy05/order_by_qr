export const DATE_PRESETS = [
  { label: 'Tất cả', value: 'all' },
  { label: 'Hôm nay', value: 'today' },
  { label: 'Hôm qua', value: 'yesterday' },
  { label: '7 ngày', value: '7days' },
  { label: '30 ngày', value: '30days' },
];

const formatLocalDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const getOrderHistoryDateRange = (preset, today = new Date()) => {
  switch (preset) {
    case 'today':
      return { startDate: formatLocalDate(today), endDate: formatLocalDate(today) };
    case 'yesterday': {
      const date = new Date(today);
      date.setDate(date.getDate() - 1);
      return { startDate: formatLocalDate(date), endDate: formatLocalDate(date) };
    }
    case '7days': {
      const date = new Date(today);
      date.setDate(date.getDate() - 6);
      return { startDate: formatLocalDate(date), endDate: formatLocalDate(today) };
    }
    case '30days': {
      const date = new Date(today);
      date.setDate(date.getDate() - 29);
      return { startDate: formatLocalDate(date), endDate: formatLocalDate(today) };
    }
    default:
      return { startDate: '', endDate: '' };
  }
};
