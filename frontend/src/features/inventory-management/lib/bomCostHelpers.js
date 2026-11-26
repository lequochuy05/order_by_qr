const MARGIN_STATUS_META = {
  NO_RECIPE: {
    vi: 'Chưa có công thức',
    en: 'No BOM recipe',
    badgeClass:
      'border-slate-200 bg-slate-100 text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300',
    barClass: 'bg-slate-400',
  },
  MISSING_COST: {
    vi: 'Thiếu đơn giá nhập',
    en: 'Missing unit cost',
    badgeClass:
      'border-amber-300 bg-amber-100 text-amber-800 dark:border-amber-500/40 dark:bg-amber-500/20 dark:text-amber-200',
    barClass: 'bg-amber-500',
  },
  HEALTHY: {
    vi: 'Biên lợi nhuận tốt (<=35%)',
    en: 'Healthy margin (<=35%)',
    badgeClass:
      'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/15 dark:text-emerald-300',
    barClass: 'bg-emerald-500',
  },
  WARNING: {
    vi: 'Giá vốn cần lưu ý (>35%)',
    en: 'Elevated cost (>35%)',
    badgeClass:
      'border-amber-300 bg-amber-100 text-amber-800 dark:border-amber-500/40 dark:bg-amber-500/20 dark:text-amber-200',
    barClass: 'bg-amber-500',
  },
  CRITICAL: {
    vi: 'Rủi ro lỗ (>45%)',
    en: 'Critical cost (>45%)',
    badgeClass:
      'border-rose-300 bg-rose-100 text-rose-800 dark:border-rose-500/40 dark:bg-rose-500/20 dark:text-rose-200',
    barClass: 'bg-rose-500',
  },
};

export const getMarginStatusMeta = (status, language = 'vi') => {
  const entry = MARGIN_STATUS_META[status] || MARGIN_STATUS_META.NO_RECIPE;
  const lang = language === 'en' ? 'en' : 'vi';
  return {
    label: entry[lang],
    badgeClass: entry.badgeClass,
    barClass: entry.barClass,
  };
};

export const buildDraftRecipePayloadLines = (recipeRows = []) =>
  (recipeRows || [])
    .map((row) => ({
      inventoryItemId: Number(row.inventoryItemId),
      quantityRequired: Number(row.quantityRequired),
    }))
    .filter(
      (row) =>
        Number.isInteger(row.inventoryItemId) &&
        row.inventoryItemId > 0 &&
        Number.isFinite(row.quantityRequired) &&
        row.quantityRequired > 0,
    );

export const mapSuggestedItemsToRecipeRows = (suggestedItems = []) =>
  (suggestedItems || [])
    .filter((item) => item?.inventoryItemId && Number(item.quantityRequired) > 0)
    .map((item) => ({
      inventoryItemId: String(item.inventoryItemId),
      quantityRequired: String(Number(Number(item.quantityRequired).toFixed(3))),
    }));

export const calculateLocalBomAnalysis = (sellingPrice, recipeRows = [], inventoryItems = []) => {
  const safePrice = Math.max(0, Number(sellingPrice || 0));
  const inventoryById = new Map((inventoryItems || []).map((item) => [Number(item.id), item]));
  const validRows = buildDraftRecipePayloadLines(recipeRows);

  if (validRows.length === 0) {
    return {
      totalFoodCost: 0,
      foodCostPercent: null,
      grossProfit: safePrice,
      recommendedMinPrice: 0,
      missingUnitCostCount: 0,
      marginStatus: 'NO_RECIPE',
    };
  }

  let totalFoodCost = 0;
  let missingUnitCostCount = 0;

  for (const row of validRows) {
    const inv = inventoryById.get(row.inventoryItemId);
    const unitCost = Math.max(0, Number(inv?.unitCost || 0));
    if (!inv || unitCost <= 0) {
      missingUnitCostCount += 1;
    }
    totalFoodCost += unitCost * row.quantityRequired;
  }

  totalFoodCost = Math.round(totalFoodCost * 100) / 100;
  const grossProfit = Math.round((safePrice - totalFoodCost) * 100) / 100;
  const foodCostPercent =
    safePrice > 0 ? Math.round((totalFoodCost / safePrice) * 1000) / 10 : null;
  const recommendedMinPrice =
    totalFoodCost > 0 ? Math.ceil(totalFoodCost / 0.35 / 1000) * 1000 : 0;

  let marginStatus = 'HEALTHY';
  if (missingUnitCostCount > 0) {
    marginStatus = 'MISSING_COST';
  } else if (foodCostPercent !== null && foodCostPercent > 45) {
    marginStatus = 'CRITICAL';
  } else if (foodCostPercent !== null && foodCostPercent > 35) {
    marginStatus = 'WARNING';
  }

  return {
    totalFoodCost,
    foodCostPercent,
    grossProfit,
    recommendedMinPrice,
    missingUnitCostCount,
    marginStatus,
  };
};
