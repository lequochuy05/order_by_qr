import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { settingsService } from '@shared/api/settingsService.js';

/**
 * Mutation to update system settings.
 * On success the settings cache is invalidated so every consumer gets fresh data.
 */
export const useUpdateSettingsMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => settingsService.update(data),
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.settings.all });
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
    onSettled: (...args) => {
      options.onSettled?.(...args);
    },
  });
