import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { menuItemService } from './menuItemService.js';

export const useMenuPageQuery = ({ q, page = 0, size = 24, categoryId } = {}, options = {}) =>
  useQuery({
    queryKey: [...queryKeys.menu.all, 'page', { q, page, size, categoryId }],
    queryFn: () => {
      const params = { page, size };
      if (q?.trim()) params.q = q.trim();
      if (categoryId && categoryId !== 'ALL') params.categoryId = categoryId;
      return menuItemService.getPage(params);
    },
    ...options,
  });

export const useMenuDetailQuery = (id, options = {}) =>
  useQuery({
    queryKey: queryKeys.menu.detail(id),
    queryFn: () => menuItemService.getById(id),
    enabled: !!id,
    ...options,
  });

/**
 * A-fix: Fetch ALL active menu items for combo composition.
 * Uses React Query with staleTime=60s so ComboModal always gets fresh data
 * without redundant network calls within a minute.
 */
export const useMenuAllQuery = (options = {}) =>
  useQuery({
    queryKey: [...queryKeys.menu.all, 'all'],
    queryFn: () => menuItemService.getAll(),
    staleTime: 60_000,
    ...options,
  });
