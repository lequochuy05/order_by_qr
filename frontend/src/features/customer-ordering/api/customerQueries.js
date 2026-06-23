import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { menuService } from './menuService.js';

const TERMINAL_SESSION_ERROR_CODES = new Set([
  'TABLE_SESSION_EXPIRED',
  'TABLE_SESSION_INVALID',
  'TABLE_SESSION_NOT_FOUND',
]);

export const isTerminalSessionError = (error) => TERMINAL_SESSION_ERROR_CODES.has(error?.code);

const toRecommendationContext = (timeContext) => {
  switch (timeContext) {
    case 'Sáng':
      return 'MORNING';
    case 'Trưa':
      return 'LUNCH';
    case 'Chiều':
      return 'AFTERNOON';
    case 'Tối':
      return 'DINNER';
    default:
      return 'ANY';
  }
};

// Aggregate query for Customer Menu with 10 minutes staleTime
export const useCustomerMenuQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.menu.customer,
    queryFn: async () => {
      const [categoriesRes, menuRes, combosRes, settingsRes] = await Promise.all([
        menuService.getCategories(),
        menuService.getAllMenuItems(),
        menuService.getCombos(),
        menuService.getSettings(),
      ]);

      return {
        categories: Array.isArray(categoriesRes) ? categoriesRes : [],
        menuItems: Array.isArray(menuRes) ? menuRes : [],
        combos: (Array.isArray(combosRes) ? combosRes : []).filter((c) => c.active !== false),
        settings: settingsRes,
      };
    },
    staleTime: 10 * 60 * 1000, // 10 minutes
    ...options,
  });

export const useTableSessionQuery = (tableCode, sessionToken, options = {}) =>
  useQuery({
    queryKey: ['tableSession', tableCode, sessionToken || 'anonymous'],
    queryFn: async () => {
      if (!tableCode) return null;

      const state = await menuService.getSessionState(tableCode);
      let currentOrder = null;
      let sessionError = null;

      if (sessionToken && state?.hasOpenSession) {
        try {
          currentOrder = await menuService.getCurrentOrderByTableCode(tableCode, sessionToken);
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

export const useRecommendationsQuery = (timeContext, weather, options = {}) =>
  useQuery({
    queryKey: ['recommendations', timeContext, weather],
    queryFn: async () => {
      try {
        const res = await menuService.getPersonalizedRecommendations(
          toRecommendationContext(timeContext),
          weather,
        );
        return Array.isArray(res) ? res : [];
      } catch {
        const fallback = await menuService.getPopularItems();
        return Array.isArray(fallback) ? fallback : [];
      }
    },
    ...options,
  });
