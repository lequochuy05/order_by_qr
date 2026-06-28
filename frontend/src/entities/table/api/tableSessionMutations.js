import { useMutation } from '@tanstack/react-query';
import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';
import { startTableSessionOnce } from './sessionStartCoordinator.js';

export const useStartTableSessionMutation = (options = {}) =>
  useMutation({
    mutationFn: (tableCode) => startTableSessionOnce(tableCode, publicMenuService.startSession),
    onSuccess: (...args) => {
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
