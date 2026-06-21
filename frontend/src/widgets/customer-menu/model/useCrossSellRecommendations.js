import { useCallback, useRef, useState } from 'react';

import { menuService } from '@features/customer-ordering';

const useCrossSellRecommendations = (hydrateRecommendationItems) => {
  const [crossSellItems, setCrossSellItems] = useState([]);
  const cacheRef = useRef(new Map());

  const clearCrossSell = useCallback(() => {
    cacheRef.current.clear();
    setCrossSellItems([]);
  }, []);

  const loadCrossSellRecommendations = useCallback(
    (itemId) => {
      if (!itemId) return;

      const cached = cacheRef.current.get(itemId);
      if (Array.isArray(cached)) {
        setCrossSellItems(cached);
        return;
      }

      if (cached?.then) {
        cached.then(setCrossSellItems).catch(() => {});
        return;
      }

      const request = menuService
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
      request.then(setCrossSellItems).catch(() => {});
    },
    [hydrateRecommendationItems],
  );

  return {
    crossSellItems,
    setCrossSellItems,
    clearCrossSell,
    loadCrossSellRecommendations,
  };
};

export default useCrossSellRecommendations;
