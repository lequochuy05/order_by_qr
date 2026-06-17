import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { categoryService } from '@entities/category/api/categoryService.js';

/**
 * Fetches a paginated, optionally-searched list of categories.
 * Each unique combination of { q, page, size } gets its own cache entry,
 * but all share the `categories.all` prefix for bulk invalidation.
 */
export const useCategoriesPageQuery = ({ q, page = 0, size = 24 } = {}, options = {}) =>
  useQuery({
    queryKey: [...queryKeys.categories.all, 'page', { q, page, size }],
    queryFn: () => categoryService.getPage({ q, page, size }),
    ...options,
  });
