import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { inventoryService } from './inventoryService.js';

export const useInventoryPageQuery = ({ keyword, stockFilter, page, size } = {}, options = {}) =>
  useQuery({
    queryKey: [...queryKeys.inventory.all, 'page', { keyword, stockFilter, page, size }],
    queryFn: () => inventoryService.getItemPage({ keyword, stockFilter, page, size }),
    placeholderData: keepPreviousData,
    ...options,
  });

export const useInventoryItemsQuery = (params = {}, options = {}) =>
  useQuery({
    queryKey: [...queryKeys.inventory.all, 'items', params],
    queryFn: () => inventoryService.getItems(params),
    ...options,
  });

export const useInventorySummaryQuery = (options = {}) =>
  useQuery({
    queryKey: [...queryKeys.inventory.all, 'summary'],
    queryFn: () => inventoryService.getSummary(),
    ...options,
  });

export const useStockMovementsQuery = (params = {}, options = {}) =>
  useQuery({
    queryKey: [...queryKeys.inventory.all, 'movements', params],
    queryFn: () => inventoryService.getMovements(params),
    ...options,
  });

export const useRecipeQuery = (menuItemId, options = {}) =>
  useQuery({
    queryKey: [...queryKeys.inventory.all, 'recipe', menuItemId],
    queryFn: () => inventoryService.getRecipe(menuItemId),
    enabled: !!menuItemId,
    ...options,
  });
