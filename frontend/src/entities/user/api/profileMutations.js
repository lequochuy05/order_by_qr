import { useMutation } from '@tanstack/react-query';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { profileService } from './profileService.js';

const invalidateProfile = () => {
  queryClient.invalidateQueries({ queryKey: queryKeys.users.profile() });
  queryClient.invalidateQueries({ queryKey: queryKeys.users.all });
};

export const useUpdateProfileMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => profileService.updateMe(data),
    onSuccess: (...args) => {
      invalidateProfile();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useChangePasswordMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => profileService.changePassword(data),
    onSuccess: (...args) => {
      invalidateProfile();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUploadProfileAvatarMutation = (options = {}) =>
  useMutation({
    mutationFn: (file) => profileService.uploadAvatar(file),
    onSuccess: (...args) => {
      invalidateProfile();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
