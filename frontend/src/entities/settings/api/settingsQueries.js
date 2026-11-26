import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { settingsService } from './settingsService.js';

export const useSettingsQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.settings.all,
    queryFn: () => settingsService.get(),
    ...options,
  });
