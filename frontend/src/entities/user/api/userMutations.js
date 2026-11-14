import { useMutation } from '@tanstack/react-query';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { userService } from './userService.js';

const invalidateUsers = () => queryClient.invalidateQueries({ queryKey: queryKeys.users.all });

export const useCreateUserMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => userService.create(data),
    onSuccess: (...args) => {
      invalidateUsers();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateUserMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => userService.update(id, data),
    onSuccess: (...args) => {
      invalidateUsers();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUploadUserAvatarMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, file }) => userService.uploadAvatar(id, file),
    onSuccess: (...args) => {
      invalidateUsers();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useDeleteUserMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => userService.delete(id),
    onSuccess: (...args) => {
      invalidateUsers();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
