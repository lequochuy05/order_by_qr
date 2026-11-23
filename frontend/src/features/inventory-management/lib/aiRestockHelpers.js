export const FORECAST_DAY_OPTIONS = [3, 7, 14];

export const RESTOCK_URGENCY_META = {
  CRITICAL: {
    vi: 'Khẩn cấp',
    en: 'Critical',
    badgeClass:
      'border-rose-300 bg-rose-100 text-rose-800 dark:border-rose-500/40 dark:bg-rose-500/20 dark:text-rose-200',
    rowClass:
      'border-rose-200/80 bg-rose-50/50 dark:border-rose-500/25 dark:bg-rose-950/20',
  },
  HIGH: {
    vi: 'Thiếu hụt',
    en: 'Shortfall',
    badgeClass:
      'border-amber-300 bg-amber-100 text-amber-800 dark:border-amber-500/40 dark:bg-amber-500/20 dark:text-amber-200',
    rowClass:
      'border-amber-200/80 bg-amber-50/40 dark:border-amber-500/25 dark:bg-amber-950/20',
  },
  MEDIUM: {
    vi: 'Sắp chạm ngưỡng',
    en: 'Low threshold',
    badgeClass:
      'border-indigo-200 bg-indigo-50 text-indigo-700 dark:border-indigo-500/30 dark:bg-indigo-500/15 dark:text-indigo-300',
    rowClass:
      'border-slate-200/80 bg-white dark:border-slate-800 dark:bg-slate-900/70',
  },
  OK: {
    vi: 'Đủ dùng',
    en: 'Sufficient',
    badgeClass:
      'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/15 dark:text-emerald-300',
    rowClass:
      'border-slate-200/80 bg-white dark:border-slate-800 dark:bg-slate-900/70',
  },
};

export const getUrgencyMeta = (urgency, language = 'vi') => {
  const entry = RESTOCK_URGENCY_META[urgency] || RESTOCK_URGENCY_META.OK;
  const lang = language === 'en' ? 'en' : 'vi';
  return {
    label: entry[lang],
    badgeClass: entry.badgeClass,
    rowClass: entry.rowClass,
  };
};

export const findLiveInventoryTarget = (items = [], proposal) => {
  if (!proposal) return null;
  const existing = (items || []).find((item) => Number(item.id) === Number(proposal.inventoryItemId));
  if (existing) {
    return existing;
  }
  return {
    id: proposal.inventoryItemId,
    name: proposal.name,
    unit: proposal.unit,
    quantityOnHand: Number(proposal.availableQuantity ?? 0),
    lowStockThreshold: Number(proposal.lowStockThreshold ?? 0),
    unitCost: Number(proposal.unitCost ?? 0),
  };
};

export const buildStockInPreset = (proposal, language = 'vi') => {
  if (!proposal) {
    return { quantity: '', note: '' };
  }
  const qty = Number(proposal.suggestedRestockQty ?? 0);
  const rounded = Number.isFinite(qty) && qty > 0 ? String(Number(qty.toFixed(3))) : '';
  const prefix = language === 'en' ? 'AI Restock Forecast' : 'Nhập nhanh theo dự báo AI';
  const note = proposal.reason ? `${prefix}: ${proposal.reason}` : prefix;
  return {
    quantity: rounded,
    note: note.slice(0, 200),
  };
};
