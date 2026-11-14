export const RECOMMENDATION_REASON_LABELS = {
  'Popular items': 'Món đang được gọi nhiều',
  'Frequently ordered together': 'Thường được gọi cùng món này',
  'Similar items in the same category': 'Cùng danh mục với món bạn đang xem',
  'Suitable for the morning': 'Phù hợp buổi sáng',
  'Suitable for lunch': 'Phù hợp bữa trưa',
  'Suitable for the afternoon': 'Phù hợp buổi chiều',
  'Suitable for dinner': 'Phù hợp bữa tối',
  'Recommended for you': 'Gợi ý phù hợp cho bạn',
};

const DEFAULT_RESTAURANT_SETTINGS = {
  restaurantName: 'Sắc Màu Quán',
  restaurantPhone: '',
  logoUrl: '',
  orderingEnabled: true,
  maintenanceMode: false,
  enableAiAssistant: true,
  showRecommendations: true,
  showCombos: true,
  showUnavailableItems: false,
};

export const translateRecommendationReason = (reason) =>
  RECOMMENDATION_REASON_LABELS[reason] || reason;

export const normalizeRestaurantSettings = (settings = {}) => ({
  ...DEFAULT_RESTAURANT_SETTINGS,
  ...settings,
  logoUrl: settings.logoUrl ?? settings.restaurantLogoUrl ?? '',
  orderingEnabled: settings.orderingEnabled ?? true,
  maintenanceMode: settings.maintenanceMode ?? false,
  enableAiAssistant: settings.enableAiAssistant ?? true,
  showRecommendations: settings.showRecommendations ?? true,
  showCombos: settings.showCombos ?? true,
  showUnavailableItems: settings.showUnavailableItems ?? false,
});

export const getTimeContext = (date = new Date()) => {
  const hour = date.getHours();
  if (hour < 11) return 'Sáng';
  if (hour < 14) return 'Trưa';
  if (hour < 18) return 'Chiều';
  return 'Tối';
};

export const getSessionStorageKey = (tableCode) => `qros:table-session:${tableCode}`;
