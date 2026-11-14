import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { analyticsService } from './analyticsService.js';

export const useRevenueQuery = ({ from, to } = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.analytics.revenue({ from, to }),
    queryFn: () => analyticsService.getRevenue(from, to),
    ...options,
  });

export const useUserPerformanceQuery = ({ from, to } = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.analytics.userPerformance({ from, to }),
    queryFn: () => analyticsService.getUsers(from, to),
    ...options,
  });

export const useTopDishesQuery = ({ from, to } = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.analytics.topItems({ from, to }),
    queryFn: () => analyticsService.getTopDishes(from, to),
    ...options,
  });

export const useSalesTrendQuery = ({ from, to } = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.analytics.salesTrend({ from, to }),
    queryFn: () => analyticsService.getDishTrend(from, to),
    ...options,
  });

export const useRevenueForecastQuery = (options = {}) =>
  useQuery({
    queryKey: [...queryKeys.analytics.all, 'revenue-forecast'],
    queryFn: () => analyticsService.getRevenueForecast(),
    ...options,
  });

export const usePopularDishesForecastQuery = (options = {}) =>
  useQuery({
    queryKey: [...queryKeys.analytics.all, 'popular-dishes-forecast'],
    queryFn: () => analyticsService.getPopularDishesForecast(),
    ...options,
  });
