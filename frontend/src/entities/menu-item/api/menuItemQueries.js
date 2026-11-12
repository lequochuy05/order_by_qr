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
