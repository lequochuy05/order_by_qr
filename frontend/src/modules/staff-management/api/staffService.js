import api from '@shared/api/httpClient.js';

const STAFF_LIST_CACHE_MS = 15_000;

const stableStringify = (value) => {
    if (!value) return 'all';
    if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
    if (typeof value === 'object') return `{${Object.keys(value).sort().map(k => `${k}:${stableStringify(value[k])}`).join(',')}}`;
    return String(value);
};

const staffRequests = new Map();
const staffCache = new Map();

const clearStaffCache = () => {
    staffRequests.clear();
    staffCache.clear();
};

export const staffService = {
    getPage: async (params = {}, { force = false } = {}) => {
        const page = Number(params.page || 0);
        const size = Number(params.size || 24);
        const requestParams = {
            page,
            size,
            sort: 'fullName,asc',
            ...params
        };
        const key = stableStringify(requestParams);
        const now = Date.now();
        const cached = staffCache.get(key);
        if (!force && cached && cached.expiresAt > now) {
            return cached.data;
        }

        if (!staffRequests.has(key)) {
            const request = api.get('/users', { params: requestParams })
                .then((res) => {
                    staffCache.set(key, {
                        data: res,
                        expiresAt: Date.now() + STAFF_LIST_CACHE_MS
                    });
                    return res;
                })
                .finally(() => {
                    staffRequests.delete(key);
                });
            staffRequests.set(key, request);
        }

        return staffRequests.get(key);
    },

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
