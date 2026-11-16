import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import { translations } from './translations.js';

export const useTranslation = (namespace = 'common') => {
  const [preferences] = useAdminPreferences();
  const language = preferences?.language === 'en' ? 'en' : 'vi';
  const isEn = language === 'en';

  const t = (key, fallback) => {
    const val = translations[language]?.[namespace]?.[key] ?? translations.vi?.[namespace]?.[key];
    return val !== undefined ? val : (fallback ?? key);
  };

  const copy = translations[language]?.[namespace] || translations.vi?.[namespace] || {};

  return { language, isEn, t, copy };
};

export { translations };
