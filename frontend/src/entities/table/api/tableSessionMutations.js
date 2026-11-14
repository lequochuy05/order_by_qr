import { useMutation } from '@tanstack/react-query';
import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';
import { startTableSessionOnce } from './sessionStartCoordinator.js';

export const useStartTableSessionMutation = (options = {}) =>
  useMutation({
    mutationFn: (param) => {
      const tableCode = typeof param === 'object' && param !== null ? param.tableCode : param;
      const sig = typeof param === 'object' && param !== null ? param.sig : undefined;
      return startTableSessionOnce(tableCode, (code) => publicMenuService.startSession(code, sig));
    },
    onSuccess: (...args) => {
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
