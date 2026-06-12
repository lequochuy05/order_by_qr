import api from '@shared/api/httpClient.js';
import { createCachedRequest } from '@shared/lib/cacheUtils.js';

const STAFF_LIST_CACHE_MS = 15_000;

const { requestFn: getStaffList, clearCache: clearStaffCache } = createCachedRequest(
    () => api.get('/users'),
    STAFF_LIST_CACHE_MS
);

export const staffService = {
    // Lấy danh sách
    getAll: getStaffList,

    // Tạo mới (trả về UserDto có id)
    create: async (data) => {
        clearStaffCache();
        const res = await api.post('/users', data);
        clearStaffCache();
        return res;
    },

    // Cập nhật thông tin
    update: async (id, data) => {
        clearStaffCache();
        const res = await api.put(`/users/${id}`, data);
        clearStaffCache();
        return res;
    },

    // Upload Avatar (API riêng biệt)
    uploadAvatar: async (id, file) => {
        clearStaffCache();
        const formData = new FormData();
        formData.append('file', file);

        const res = await api.post(`/users/${id}/avatar`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        clearStaffCache();
        return res;
    },

    // Xóa
    delete: async (id) => {
        clearStaffCache();
        await api.delete(`/users/${id}`);
        clearStaffCache();
    }
};
