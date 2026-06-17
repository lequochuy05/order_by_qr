export const createCachedRequest = (fetchFn, cacheMs = 0) => {
  let pendingRequest = null;
  let cache = { data: null, expiresAt: 0 };

  const clearCache = () => {
    pendingRequest = null;
    cache = { data: null, expiresAt: 0 };
  };

  const requestFn = (options = {}) => {
    const { force = false } = options ?? {};
    const now = Date.now();
    if (!force && cacheMs > 0 && cache.data && cache.expiresAt > now) {
      return Promise.resolve(cache.data);
    }

    if (!pendingRequest) {
      pendingRequest = fetchFn()
        .then((res) => {
          if (cacheMs > 0) {
            cache = {
              data: res,
              expiresAt: Date.now() + cacheMs,
            };
          }
          return res;
        })
        .finally(() => {
          pendingRequest = null;
        });
    }

    return pendingRequest;
  };

  return { requestFn, clearCache };
};

export const createKeyedCachedRequest = (fetchFn, cacheMs = 0, keyFn = (args) => String(args)) => {
  const pendingRequests = new Map();
  const cacheMap = new Map();

  const clearCache = () => {
    pendingRequests.clear();
    cacheMap.clear();
  };

  const requestFn = (args, { force = false } = {}) => {
    const key = keyFn(args);
    const now = Date.now();

    if (!force && cacheMs > 0 && cacheMap.has(key)) {
      const cached = cacheMap.get(key);
      if (cached.expiresAt > now) {
        return Promise.resolve(cached.data);
      }
    }

    if (!pendingRequests.has(key)) {
      const request = fetchFn(args)
        .then((res) => {
          if (cacheMs > 0) {
            cacheMap.set(key, {
              data: res,
              expiresAt: Date.now() + cacheMs,
            });
          }
          return res;
        })
        .finally(() => {
          pendingRequests.delete(key);
        });
      pendingRequests.set(key, request);
    }

    return pendingRequests.get(key);
  };

  return { requestFn, clearCache };
};
