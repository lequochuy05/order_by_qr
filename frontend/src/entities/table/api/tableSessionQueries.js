import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';
import { isTerminalSessionError } from '../lib/sessionErrors.js';

export const useTableSessionQuery = (tableCode, sessionToken, options = {}) =>
  useQuery({
    queryKey: ['tableSession', tableCode, sessionToken || 'anonymous'],
    queryFn: async () => {
      if (!tableCode) return null;

      const state = await publicMenuService.getSessionState(tableCode);
      let currentOrder = null;
      let sessionError = null;

      if (sessionToken && state?.hasOpenSession) {
        try {
          currentOrder = await publicMenuService.getCurrentOrderByTableCode(tableCode, sessionToken);
        } catch (error) {
          if (!isTerminalSessionError(error)) {
            throw error;
          }

          sessionError = {
            code: error.code,
            message: error.message,
          };
        }
      }

      return {
        tableInfo: state
          ? {
              tableCode: state.tableCode,
              tableNumber: state.tableNumber,
              status: state.tableStatus,
              hasOpenSession: state.hasOpenSession,
              canStartSession: state.canStartSession,
              canOrder: state.canOrder,
            }
          : null,
        sessionState: state,
        currentOrder,
        sessionEnded: Boolean(sessionToken && state && !state.hasOpenSession),
        sessionError,
      };
    },
    enabled: !!tableCode,
    retry: false,
    staleTime: 15 * 1000,
    placeholderData: keepPreviousData,
    ...options,
  });
