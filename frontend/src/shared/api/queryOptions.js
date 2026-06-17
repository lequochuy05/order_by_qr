/**
 * Default React Query options for QROS.
 *
 * staleTime = 1 minute: safe default before WebSocket invalidation is wired up.
 * After Sprint 3 (WS invalidate), this can be increased to 5–10 min or Infinity.
 */
export const DEFAULT_QUERY_OPTIONS = {
  staleTime: 60 * 1000,
  gcTime: 10 * 60 * 1000,
  retry: 1,
  refetchOnWindowFocus: false,
  refetchOnReconnect: true,
  refetchOnMount: true,
};
