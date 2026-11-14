import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { publicMenuService } from './publicMenuService.js';

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

export const useCustomerMenuQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.menu.customer,
    queryFn: async () => {
      const [categoriesRes, menuRes, combosRes, settingsRes] = await Promise.all([
        publicMenuService.getCategories(),
        publicMenuService.getAllMenuItems(),
        publicMenuService.getCombos(),
        publicMenuService.getSettings(),
      ]);

      return {
        categories: Array.isArray(categoriesRes) ? categoriesRes : [],
        menuItems: Array.isArray(menuRes) ? menuRes : [],
        combos: (Array.isArray(combosRes) ? combosRes : []).filter((c) => c.active !== false),
        settings: settingsRes,
      };
    },
    staleTime: 10 * 60 * 1000,
    ...options,
  });

export const useRecommendationsQuery = (timeContext, options = {}) =>
  useQuery({
    queryKey: ['recommendations', timeContext],
    queryFn: async () => {
      try {
        const res = await publicMenuService.getPersonalizedRecommendations(
          toRecommendationContext(timeContext),
        );
        return Array.isArray(res) ? res : [];
      } catch {
        const fallback = await publicMenuService.getPopularItems();
        return Array.isArray(fallback) ? fallback : [];
      }
    },
    ...options,
  });
