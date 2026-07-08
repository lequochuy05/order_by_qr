import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { userService } from '@entities/user/api/userService.js';

export const useUserPageQuery = (params = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.users.list(params),
    queryFn: () => userService.getPage(params),
    placeholderData: keepPreviousData,
    ...options,
  });
