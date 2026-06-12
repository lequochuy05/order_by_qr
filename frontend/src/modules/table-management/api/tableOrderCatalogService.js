import api from '@shared/api/httpClient.js';

const CACHE_TTL_MS = 15_000;

let catalogRequest = null;
let catalogCache = {
    data: null,
    expiresAt: 0
};

export const tableOrderCatalogService = {
    getCatalog: async ({ force = false } = {}) => {
        const now = Date.now();
        if (!force && catalogCache.data && catalogCache.expiresAt > now) {
            return catalogCache.data;
        }

        if (!catalogRequest) {
            catalogRequest = api.get('/public/catalog', {
                skipAuth: true,
                headers: {
                    Authorization: undefined,
                    'Content-Type': undefined
                }
            })
                .then((data) => {
                    catalogCache = {
                        data,
                        expiresAt: Date.now() + CACHE_TTL_MS
                    };
                    return data;
                })
                .finally(() => {
                    catalogRequest = null;
                });
        }

        return catalogRequest;
    },

    clearCache: () => {
        catalogCache = {
            data: null,
            expiresAt: 0
        };
        catalogRequest = null;
    }
};
