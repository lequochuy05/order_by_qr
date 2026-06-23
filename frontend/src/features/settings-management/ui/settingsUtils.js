import { defaultSettings } from './settingsConstants.js';

export const normalizeSettings = (data = {}) => ({
  ...defaultSettings,
  ...data,
  logoUrl: data.logoUrl ?? data.restaurantLogoUrl ?? '',
  wifiName: data.wifiName ?? data.wifiSsid ?? '',
  taxPercent: data.taxPercent ?? data.vatRate ?? 0,
  serviceChargePercent: data.serviceChargePercent ?? 0,
  currency: data.currency ?? 'VND',
  openingTime: data.openingTime ?? '',
  closingTime: data.closingTime ?? '',
  orderingEnabled: data.orderingEnabled ?? data.autoApproveOrders ?? true,
  maintenanceMode: data.maintenanceMode ?? false,
  wifiPassword: data.wifiPassword ?? '',
});

export const toSettingsPayload = (settings) => ({
  restaurantName: settings.restaurantName.trim(),
  restaurantAddress: settings.restaurantAddress?.trim() || null,
  restaurantPhone: settings.restaurantPhone?.trim() || null,
  restaurantEmail: settings.restaurantEmail?.trim() || null,
  logoUrl: settings.logoUrl?.trim() || null,
  wifiName: settings.wifiName?.trim() || null,
  wifiPassword: settings.wifiPassword?.trim() || null,
  openingTime: settings.openingTime || null,
  closingTime: settings.closingTime || null,
  currency: settings.currency?.trim().toUpperCase() || 'VND',
  taxPercent: Number(settings.taxPercent || 0),
  serviceChargePercent: Number(settings.serviceChargePercent || 0),
  orderingEnabled: Boolean(settings.orderingEnabled),
  maintenanceMode: Boolean(settings.maintenanceMode),
  cashPaymentEnabled: Boolean(settings.cashPaymentEnabled),
  onlinePaymentEnabled: Boolean(settings.onlinePaymentEnabled),
  paymentQrExpiresInMinutes: Number(settings.paymentQrExpiresInMinutes || 20),
  autoConfirmOrders: Boolean(settings.autoConfirmOrders),
  kitchenOverdueThresholdMinutes: Number(settings.kitchenOverdueThresholdMinutes || 20),
  showUnavailableItems: Boolean(settings.showUnavailableItems),
  showRecommendations: Boolean(settings.showRecommendations),
  showCombos: Boolean(settings.showCombos),
  billTitle: settings.billTitle?.trim() || 'HÓA ĐƠN THANH TOÁN',
  billFooterMessage: settings.billFooterMessage?.trim() || 'CẢM ƠN QUÝ KHÁCH & HẸN GẶP LẠI!',
  billPaperSize: String(settings.billPaperSize || '80'),
  showWifiOnBill: Boolean(settings.showWifiOnBill),
  autoPrintBill: Boolean(settings.autoPrintBill),
  newOrderNotificationEnabled: Boolean(settings.newOrderNotificationEnabled),
  paymentNotificationEnabled: Boolean(settings.paymentNotificationEnabled),
  kitchenOverdueNotificationEnabled: Boolean(settings.kitchenOverdueNotificationEnabled),
  version: settings.version,
});

export const hasSettingsChanges = (settings, serverSettings) => {
  const current = toSettingsPayload(settings);
  const saved = toSettingsPayload(serverSettings);
  const { version: currentVersion, ...currentValues } = current;
  const { version: savedVersion, ...savedValues } = saved;
  void currentVersion;
  void savedVersion;
  return JSON.stringify(currentValues) !== JSON.stringify(savedValues);
};

export const setSettingsValue = (setSettings, key, value) => {
  setSettings((prev) => ({ ...prev, [key]: value }));
};

export const setPreferenceValue = (setPreferences, key, value) => {
  setPreferences((prev) => ({ ...prev, [key]: value }));
};

export const escapeWifi = (value) => String(value).replace(/([\\;,":])/g, '\\$1');
