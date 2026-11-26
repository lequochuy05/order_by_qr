import { useEffect } from 'react';
import { create } from 'zustand';

const adminPreferencesStorageKey = 'admin_preferences';

const defaultAdminPreferences = {
  notificationSound: true,
  loudSound: false,
  darkMode: false,
  language: 'vi',
};

export const readAdminPreferences = () => {
  try {
    return {
      ...defaultAdminPreferences,
      ...JSON.parse(localStorage.getItem(adminPreferencesStorageKey) || '{}'),
    };
  } catch {
    return defaultAdminPreferences;
  }
};

const useAdminPreferencesStore = create((set) => ({
  preferences: readAdminPreferences(),
  setPreferences: (updater) => {
    set((state) => {
      const next = typeof updater === 'function' ? updater(state.preferences) : updater;
      try {
        localStorage.setItem(adminPreferencesStorageKey, JSON.stringify(next));
      } catch {
        // Storage not available
      }
      return { preferences: next };
    });
  },
  syncPreferences: () => set({ preferences: readAdminPreferences() }),
}));

if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key === adminPreferencesStorageKey) {
      useAdminPreferencesStore.getState().syncPreferences();
    }
  });
}

export const useAdminPreferences = () => {
  const preferences = useAdminPreferencesStore((state) => state.preferences);
  const setPreferences = useAdminPreferencesStore((state) => state.setPreferences);

  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.classList.toggle('dark', Boolean(preferences.darkMode));
    }
  }, [preferences.darkMode]);

  return [preferences, setPreferences];
};
