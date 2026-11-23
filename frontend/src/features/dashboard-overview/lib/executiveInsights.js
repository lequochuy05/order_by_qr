export const INSIGHTS_PRESETS = ['today', '7d', '30d'];

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

const shiftIsoDate = (isoDate, deltaDays) => {
  const [year, month, day] = isoDate.split('-').map(Number);
  const utc = new Date(Date.UTC(year, month - 1, day + deltaDays));
  return [
    utc.getUTCFullYear(),
    String(utc.getUTCMonth() + 1).padStart(2, '0'),
    String(utc.getUTCDate()).padStart(2, '0'),
  ].join('-');
};

export const computeInsightsDateRange = (businessToday, preset = '7d') => {
  const safeEnd = typeof businessToday === 'string' && ISO_DATE_PATTERN.test(businessToday)
    ? businessToday
    : '2026-01-07';

  if (preset === 'today') {
    return { from: safeEnd, to: safeEnd };
  }
  if (preset === '30d') {
    return { from: shiftIsoDate(safeEnd, -29), to: safeEnd };
  }
  return { from: shiftIsoDate(safeEnd, -6), to: safeEnd };
};

export const formatDeltaPercent = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return null;
  }
  const num = Number(value);
  const rounded = Math.round(num * 10) / 10;
  return `${rounded >= 0 ? '+' : ''}${rounded}%`;
};

export const getSeverityMeta = (severity, language = 'vi') => {
  const normalized = typeof severity === 'string' ? severity.trim().toUpperCase() : 'INFO';
  const isEn = language === 'en';

  if (normalized === 'HIGH') {
    return {
      key: 'HIGH',
      label: isEn ? 'High risk' : 'Ưu tiên cao',
      badgeClass:
        'bg-rose-100 text-rose-800 border-rose-300/80 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-500/40',
      containerClass:
        'border-rose-200/90 bg-rose-50/70 dark:border-rose-500/30 dark:bg-rose-950/25',
    };
  }
  if (normalized === 'MEDIUM') {
    return {
      key: 'MEDIUM',
      label: isEn ? 'Watch' : 'Cần theo dõi',
      badgeClass:
        'bg-amber-100 text-amber-800 border-amber-300/80 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-500/40',
      containerClass:
        'border-amber-200/90 bg-amber-50/70 dark:border-amber-500/30 dark:bg-amber-950/25',
    };
  }
  return {
    key: 'INFO',
    label: isEn ? 'Note' : 'Thông tin',
    badgeClass:
      'bg-sky-100 text-sky-800 border-sky-300/80 dark:bg-sky-950/60 dark:text-sky-300 dark:border-sky-500/40',
    containerClass:
      'border-sky-200/90 bg-sky-50/70 dark:border-sky-500/30 dark:bg-sky-950/25',
  };
};

export const normalizeExecutiveSummary = (payload) => {
  if (!payload || typeof payload !== 'object') {
    return null;
  }

  const metricsRaw = payload.metrics && typeof payload.metrics === 'object' ? payload.metrics : {};
  return {
    from: payload.from || '',
    to: payload.to || '',
    language: payload.language === 'en' ? 'en' : 'vi',
    headline: typeof payload.headline === 'string' ? payload.headline.trim() : '',
    highlights: Array.isArray(payload.highlights)
      ? payload.highlights.filter((item) => typeof item === 'string' && item.trim())
      : [],
    anomalies: Array.isArray(payload.anomalies)
      ? payload.anomalies
          .filter((item) => item && typeof item === 'object' && item.title && item.detail)
          .map((item) => ({
            severity: String(item.severity || 'INFO').toUpperCase(),
            title: String(item.title).trim(),
            detail: String(item.detail).trim(),
          }))
      : [],
    recommendations: Array.isArray(payload.recommendations)
      ? payload.recommendations.filter((item) => typeof item === 'string' && item.trim())
      : [],
    aiGenerated: Boolean(payload.aiGenerated),
    generatedAt: payload.generatedAt || null,
    metrics: {
      revenue: Number(metricsRaw.revenue ?? 0),
      previousRevenue: Number(metricsRaw.previousRevenue ?? 0),
      revenueChangePercent:
        metricsRaw.revenueChangePercent === null || metricsRaw.revenueChangePercent === undefined
          ? null
          : Number(metricsRaw.revenueChangePercent),
      completedOrders: Number(metricsRaw.completedOrders ?? 0),
      previousCompletedOrders: Number(metricsRaw.previousCompletedOrders ?? 0),
      cancelledOrders: Number(metricsRaw.cancelledOrders ?? 0),
      averageOrderValue: Number(metricsRaw.averageOrderValue ?? 0),
      totalItemsSold: Number(metricsRaw.totalItemsSold ?? 0),
      occupiedTables: Number(metricsRaw.occupiedTables ?? 0),
      totalTables: Number(metricsRaw.totalTables ?? 0),
      activeKitchenItems: Number(metricsRaw.activeKitchenItems ?? 0),
      overdueKitchenItems: Number(metricsRaw.overdueKitchenItems ?? 0),
      maxKitchenWaitMinutes: Number(metricsRaw.maxKitchenWaitMinutes ?? 0),
      totalServiceRequests: Number(metricsRaw.totalServiceRequests ?? 0),
      openServiceRequests: Number(metricsRaw.openServiceRequests ?? 0),
      overdueServiceRequests: Number(metricsRaw.overdueServiceRequests ?? 0),
      topServiceRequestReason: metricsRaw.topServiceRequestReason || null,
      lowStockItems: Number(metricsRaw.lowStockItems ?? 0),
      outOfStockItems: Number(metricsRaw.outOfStockItems ?? 0),
      lowStockItemNames: Array.isArray(metricsRaw.lowStockItemNames)
        ? metricsRaw.lowStockItemNames
        : [],
      topDishes: Array.isArray(metricsRaw.topDishes) ? metricsRaw.topDishes : [],
    },
  };
};
