import { create } from 'zustand';
import { getAccessToken } from '@shared/api/httpClient.js';
import { settingsService } from '../api/settingsService.js';

export const defaultSettings = {
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
  allowSettlementWithUnfinishedItems: false,
  customerSelfCheckoutEnabled: false,
};

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
  allowSettlementWithUnfinishedItems: data.allowSettlementWithUnfinishedItems ?? false,
  customerSelfCheckoutEnabled: data.customerSelfCheckoutEnabled ?? false,
});

const useSettingsStore = create((set, get) => ({
  settings: defaultSettings,
  loaded: false,
  loadedScope: 'none',

  fetchSettings: async (options = false) => {
    const force = typeof options === 'boolean' ? options : Boolean(options?.force);
    const authenticated =
      typeof options === 'object' && options !== null && 'authenticated' in options
        ? Boolean(options.authenticated)
        : Boolean(getAccessToken());
    const targetScope = authenticated ? 'private' : 'public';

    if (get().loaded && !force && get().loadedScope === targetScope) {
      return get().settings;
    }

    try {
      let data;
      if (authenticated) {
        try {
          data = await settingsService.get({ force });
        } catch {
          data = await settingsService.getPublic({ force });
        }
      } else {
        data = await settingsService.getPublic({ force });
      }
      const merged = normalizeSettings(data);
      set({ settings: merged, loaded: true, loadedScope: targetScope });
      return merged;
    } catch (err) {
      console.error('Error fetching settings:', err);
      return get().settings;
    }
  },

  invalidateAndRefetch: async (options = {}) => {
    settingsService.clearCache();
    set({ loaded: false, loadedScope: 'none' });
    await get().fetchSettings({ ...options, force: true });
  },
}));

export default useSettingsStore;
