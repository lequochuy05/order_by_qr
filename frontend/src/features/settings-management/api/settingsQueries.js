import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { settingsService } from '@shared/api/settingsService.js';

/**
 * Fetches system settings from the API.
 * Shares a single cached response across every component that calls this hook.
 */
export const useSettingsQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.settings.all,
    queryFn: () => settingsService.get(),
    ...options,
  });

export const usePublicSettingsQuery = (options = {}) =>
  useQuery({
    queryKey: [...queryKeys.settings.all, 'public'],
    queryFn: () => settingsService.getPublic(),
    ...options,
  });
