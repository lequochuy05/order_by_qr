import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { userService } from '@entities/user/api/userService.js';

const invalidateUsers = () =>
  queryClient.invalidateQueries({ queryKey: queryKeys.users.all });

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
