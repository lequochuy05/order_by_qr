import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { profileService } from '../api/profileService.js';

export const useProfileQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.users.profile(),
    queryFn: () => profileService.getMe(),
    ...options,
  });
