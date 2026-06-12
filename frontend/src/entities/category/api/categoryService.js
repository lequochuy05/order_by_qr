import api from '@shared/api/httpClient.js';
import { createCachedRequest } from '@shared/lib/cacheUtils.js';

const CATEGORY_LIST_CACHE_MS = 15_000;

const normalizePageContent = (data) => {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    return [];
};

const { requestFn: getCategoryList, clearCache: clearCategoryListCache } = createCachedRequest(
    (options = {}) => api.get('/categories', { params: { size: 1000, sort: 'displayOrder,asc', ...options } }).then(normalizePageContent),
    CATEGORY_LIST_CACHE_MS
);

const toCategoryPayload = (data) => {
    if (typeof data === 'string') {
        return { name: data };
    }

    return {
        name: data.name,
        img: data.img || null,
        description: data.description?.trim() || null,
        active: data.active ?? true,
        displayOrder: Number(data.displayOrder) || 0
    };
};

export const categoryService = {

    getAll: getCategoryList,

    
    // Tìm kiếm và phân trang
    search: async (q, page = 0, size = 12) => {
        const params = { q, page, size, sort: 'name,asc' };
        const res = await api.get('/categories', { params });
        return res;
    },

    // Tạo mới danh mục
    create: async (data) => {
        clearCategoryListCache();
        const res = await api.post('/categories', toCategoryPayload(data));
        clearCategoryListCache();
        return res;
    },

    // Cập nhật danh mục
    update: async (id, data) => {
        clearCategoryListCache();
        const res = await api.put(`/categories/${id}`, toCategoryPayload(data));
        clearCategoryListCache();
        return res;
    },

    // Upload ảnh
    uploadImage: async (id, file) => {
        clearCategoryListCache();
        const formData = new FormData();
        formData.append('file', file);
        const res = await api.post(`/categories/${id}/image`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
        });
        clearCategoryListCache();
        return res;
    },

    // Xóa danh mục
    delete: async (id) => {
        clearCategoryListCache();
        await api.delete(`/categories/${id}`);
        clearCategoryListCache();
    }
};
