import { create } from 'zustand';
import { settingsService } from '@shared/api/settingsService.js';

const defaultSettings = {
  restaurantName: 'QROS',
  restaurantAddress: '',
  restaurantPhone: '',
  restaurantEmail: '',
  logoUrl: '',
  wifiName: '',
  wifiPassword: '',
  openingTime: '',
  closingTime: '',
  currency: 'VND',
  taxPercent: 0,
  serviceChargePercent: 0,
  orderingEnabled: true,
  maintenanceMode: false,
  cashPaymentEnabled: true,
  onlinePaymentEnabled: true,
  paymentQrExpiresInMinutes: 20,
  autoConfirmOrders: false,
  kitchenOverdueThresholdMinutes: 20,
  showUnavailableItems: false,
  showRecommendations: true,
  showCombos: true,
  billTitle: 'HÓA ĐƠN THANH TOÁN',
  billFooterMessage: 'CẢM ƠN QUÝ KHÁCH & HẸN GẶP LẠI!',
  billPaperSize: '80',
  showWifiOnBill: true,
  autoPrintBill: true,
  newOrderNotificationEnabled: true,
  paymentNotificationEnabled: true,
  kitchenOverdueNotificationEnabled: true,
};

const normalizeSettings = (data = {}) => ({
  ...defaultSettings,
  ...data,
  logoUrl: data.logoUrl ?? data.restaurantLogoUrl ?? '',
  wifiName: data.wifiName ?? data.wifiSsid ?? '',
  taxPercent: data.taxPercent ?? data.vatRate ?? 0,
  serviceChargePercent: data.serviceChargePercent ?? 0,
  orderingEnabled: data.orderingEnabled ?? data.autoApproveOrders ?? true,
  maintenanceMode: data.maintenanceMode ?? false,
  wifiPassword: data.wifiPassword ?? '',
});

const useSettingsStore = create((set, get) => ({
  settings: defaultSettings,
  loaded: false,

  fetchSettings: async (force = false) => {
    if (get().loaded && !force) return get().settings;
    try {
      const data = await settingsService.getPublic();
      const merged = normalizeSettings(data);
      set({ settings: merged, loaded: true });
      return merged;
    } catch (err) {
      console.error('Error fetching settings:', err);
      return get().settings;
    }
  },

  invalidateAndRefetch: async () => {
    set({ loaded: false });
    await get().fetchSettings(true);
  },
}));

export default useSettingsStore;
