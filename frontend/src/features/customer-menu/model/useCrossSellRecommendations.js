import { useCallback, useRef, useState } from 'react';

import { publicMenuService } from '@entities/public-menu';

const useCrossSellRecommendations = (hydrateRecommendationItems, enabled = true) => {
  const [crossSellItems, setCrossSellItems] = useState([]);
  const cacheRef = useRef(new Map());
  // H4 fix: sequence counter to discard stale in-flight responses.
  // If loadCrossSellRecommendations is called again before a previous request
  // resolves, the older response will be silently dropped.
  const requestIdRef = useRef(0);

  const clearCrossSell = useCallback(() => {
    cacheRef.current.clear();
    setCrossSellItems([]);
  }, []);

  const loadCrossSellRecommendations = useCallback(
    (itemId) => {
      if (!enabled || !itemId) return;

      const myRequestId = ++requestIdRef.current;

      const cached = cacheRef.current.get(itemId);
      if (Array.isArray(cached)) {
        if (myRequestId === requestIdRef.current) setCrossSellItems(cached);
        return;
      }

      if (cached?.then) {
        cached
          .then((items) => {
            if (myRequestId === requestIdRef.current) setCrossSellItems(items);
          })
          .catch(() => {});
        return;
      }

      const request = publicMenuService
        .getCrossSellRecommendations(itemId)
        .then((response) => {
          const items = hydrateRecommendationItems(response);
          cacheRef.current.set(itemId, items);
          return items;
        })
        .catch((error) => {
          cacheRef.current.delete(itemId);
          throw error;
        });

      cacheRef.current.set(itemId, request);
      request
        .then((items) => {
          if (myRequestId === requestIdRef.current) setCrossSellItems(items);
        })
        .catch(() => {});
    },
    [enabled, hydrateRecommendationItems],
  );

  return {
    crossSellItems: enabled ? crossSellItems : [],
    setCrossSellItems,
    clearCrossSell,
    loadCrossSellRecommendations,
  };
};

export default useCrossSellRecommendations;
