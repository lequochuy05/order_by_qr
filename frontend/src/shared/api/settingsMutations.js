import { useMutation } from '@tanstack/react-query';

import { queryClient } from './queryClient.js';
import { queryKeys } from './queryKeys.js';
import { settingsService } from './settingsService.js';

const invalidateSettings = () =>
  queryClient.invalidateQueries({ queryKey: queryKeys.settings.all });

export const useUpdateSettingsMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => settingsService.update(data),
    onSuccess: (...args) => {
      invalidateSettings();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
    onSettled: (...args) => {
      options.onSettled?.(...args);
    },
  });

export const useUploadLogoMutation = (options = {}) =>
  useMutation({
    mutationFn: (file) => settingsService.uploadLogo(file),
    onSuccess: (...args) => {
      invalidateSettings();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
