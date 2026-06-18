import { useCallback, useEffect, useState } from 'react';

export const adminPreferencesStorageKey = 'admin_preferences';
export const adminPreferencesEventName = 'admin-preferences-change';

export const defaultAdminPreferences = {
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

const persistAdminPreferences = (preferences) => {
  localStorage.setItem(adminPreferencesStorageKey, JSON.stringify(preferences));
  window.dispatchEvent(new CustomEvent(adminPreferencesEventName, { detail: preferences }));
};

export const useAdminPreferences = () => {
  const [preferences, setPreferenceState] = useState(() => readAdminPreferences());

  const setPreferences = useCallback((updater) => {
    setPreferenceState((prev) => {
      const next = typeof updater === 'function' ? updater(prev) : updater;
      persistAdminPreferences(next);
      return next;
    });
  }, []);

  useEffect(() => {
    const handlePreferenceChange = (event) => {
      setPreferenceState(event.detail || readAdminPreferences());
    };
    const handleStorageChange = (event) => {
      if (event.key === adminPreferencesStorageKey) {
        setPreferenceState(readAdminPreferences());
      }
    };

    window.addEventListener(adminPreferencesEventName, handlePreferenceChange);
    window.addEventListener('storage', handleStorageChange);

    return () => {
      window.removeEventListener(adminPreferencesEventName, handlePreferenceChange);
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', Boolean(preferences.darkMode));
  }, [preferences.darkMode]);

  return [preferences, setPreferences];
};
