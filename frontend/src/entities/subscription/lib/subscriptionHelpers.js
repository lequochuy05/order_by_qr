export const PLAN_TIERS = {
  FREE: 'FREE',
  PRO: 'PRO',
};

export const PLAN_FEATURES = {
  AI_OPERATIONS_SUITE: 'AI_OPERATIONS_SUITE',
  AI_CUSTOMER_ASSISTANT: 'AI_CUSTOMER_ASSISTANT',
  INVENTORY_BOM: 'INVENTORY_BOM',
  UNLIMITED_RESOURCES: 'UNLIMITED_RESOURCES',
};

export const BILLING_CYCLES = {
  TRIAL: 'TRIAL',
  MONTHLY: 'MONTHLY',
  YEARLY: 'YEARLY',
};

export const isProActive = (subscription) => {
  if (!subscription) return false;
  if (subscription.enforcementEnabled === false) return true;
  return Boolean(subscription.proActive);
};

export const hasPlanFeature = (subscription, feature) => {
  if (!feature) return true;
  if (!subscription) return true;
  if (subscription.enforcementEnabled === false) return true;
  if (Array.isArray(subscription.enabledFeatures)) {
    return subscription.enabledFeatures.includes(feature);
  }
  return Boolean(subscription.proActive);
};

export const getRemainingDays = (expiresAt, nowMs = Date.now()) => {
  if (!expiresAt) return null;
  const expiryMs = new Date(expiresAt).getTime();
  if (!Number.isFinite(expiryMs)) return null;
  const diffMs = expiryMs - nowMs;
  if (diffMs <= 0) return 0;
  return Math.ceil(diffMs / (24 * 60 * 60 * 1000));
};

export const getUsagePercent = (current, max) => {
  if (max == null || max <= 0) return 0;
  const value = Math.max(0, Number(current || 0));
  return Math.min(100, Math.round((value / max) * 100));
};

export const isPlanLimitReached = (current, max) => {
  if (max == null || max <= 0) return false;
  return Number(current || 0) >= Number(max);
};

export const formatBillingCycleLabel = (cycle, language = 'vi') => {
  const isEn = language === 'en';
  switch (cycle) {
    case BILLING_CYCLES.TRIAL:
      return isEn ? '14-day Free Trial' : 'Dùng thử 14 ngày';
    case BILLING_CYCLES.MONTHLY:
      return isEn ? 'Pro Monthly (30 days)' : 'Pro Tháng (30 ngày)';
    case BILLING_CYCLES.YEARLY:
      return isEn ? 'Pro Yearly (365 days)' : 'Pro Năm (365 ngày)';
    default:
      return isEn ? 'Free Plan' : 'Gói Miễn phí';
  }
};

export const getFeaturePaywallCopy = (feature, language = 'vi') => {
  const isEn = language === 'en';
  switch (feature) {
    case PLAN_FEATURES.AI_OPERATIONS_SUITE:
      return {
        title: isEn ? 'Unlock AI Operations Suite (Pro)' : 'Mở khóa Bộ trợ lý AI Vận hành (Pro)',
        description: isEn
          ? 'Automate shift executive summaries, smart restock forecasts, Gemini Vision invoice stock-in, BOM costing checks, allergy triage, and combo/promotion suggestions.'
          : 'Mở khóa AI Tóm tắt ca điều hành, Dự báo nhập kho BOM, Quét hóa đơn nhập kho bằng Gemini Vision, Kiểm tra giá vốn BOM, Cảnh báo dị ứng và Đề xuất Combo/Khuyến mãi.',
      };
    case PLAN_FEATURES.INVENTORY_BOM:
      return {
        title: isEn ? 'Unlock Inventory & BOM Controls (Pro)' : 'Mở khóa Quản trị Kho & Định lượng BOM (Pro)',
        description: isEn
          ? 'Free plan keeps your existing stock records read-only. Upgrade to Pro to add ingredients, perform stock-ins, adjust inventory, and edit dish BOM recipes.'
          : 'Gói Miễn phí vẫn giữ nguyên dữ liệu kho ở chế độ chỉ đọc. Nâng cấp Pro để thêm/sửa nguyên liệu, nhập kho, kiểm kê và thiết lập định mức món ăn (BOM).',
      };
    case PLAN_FEATURES.UNLIMITED_RESOURCES:
      return {
        title: isEn ? 'Unlock Unlimited Tables & Staff (Pro)' : 'Mở không giới hạn Bàn & Nhân sự (Pro)',
        description: isEn
          ? 'You have reached the Free plan capacity limit. Upgrade to Pro for unlimited dining tables and internal staff accounts.'
          : 'Quán đã chạm hạn mức của gói Miễn phí. Nâng cấp lên Pro để tạo không giới hạn số lượng bàn ăn và tài khoản nhân sự.',
      };
    default:
      return {
        title: isEn ? 'Upgrade to QROS Pro' : 'Nâng cấp gói QROS Pro',
        description: isEn
          ? 'Unlock AI assistants, BOM inventory control, and unlimited restaurant scaling.'
          : 'Mở khóa trọn bộ AI nhà hàng, quản trị kho định lượng BOM và không giới hạn quy mô bàn/nhân sự.',
      };
  }
};
