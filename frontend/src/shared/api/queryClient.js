import { QueryClient } from '@tanstack/react-query';
import { DEFAULT_QUERY_OPTIONS } from './queryOptions.js';

/**
 * Singleton QueryClient exported for use in:
 * - QueryClientProvider (providers.jsx)
 * - WebSocketProvider (Sprint 3) for external invalidation
 * - Auth logout flow for queryClient.clear()
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: DEFAULT_QUERY_OPTIONS,
  },
});
