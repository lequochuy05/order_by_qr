import { create } from 'zustand';
import { settingsService } from '@shared/api/settingsService.js';

const defaultSettings = {
  restaurantName: 'Sắc Màu Quán',
  restaurantAddress: '',
  restaurantPhone: '',
  restaurantLogoUrl: '',
  wifiSsid: '',
  wifiPassword: '',
  vatRate: 0,
  autoApproveOrders: true,
  enableAiAssistant: true,
  enablePayos: true,
  enableCash: true,
};

const useSettingsStore = create((set, get) => ({
  settings: defaultSettings,
  loaded: false,

  fetchSettings: async (force = false) => {
    if (get().loaded && !force) return get().settings;
    try {
      const data = await settingsService.get();
      const merged = { ...defaultSettings, ...data };
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
