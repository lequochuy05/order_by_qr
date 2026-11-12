import { useQuery } from '@tanstack/react-query';

import { queryKeys } from './queryKeys.js';
import { settingsService } from './settingsService.js';

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
