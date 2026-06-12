import api from '@shared/api/httpClient.js';

const VOUCHER_LIST_CACHE_MS = 15_000;

let voucherListRequest = null;
let voucherListCache = {
    data: null,
    expiresAt: 0
};

const clearVoucherCache = () => {
    voucherListRequest = null;
    voucherListCache = {
        data: null,
        expiresAt: 0
    };
};

export const voucherService = {
    // Lấy toàn bộ voucher
    getAll: async ({ force = false } = {}) => {
        const now = Date.now();
        if (!force && voucherListCache.data && voucherListCache.expiresAt > now) {
            return voucherListCache.data;
        }

        if (!voucherListRequest) {
            voucherListRequest = api.get('/vouchers')
                .then((res) => {
                    voucherListCache = {
                        data: res,
                        expiresAt: Date.now() + VOUCHER_LIST_CACHE_MS
                    };
                    return res;
                })
                .finally(() => {
                    voucherListRequest = null;
                });
        }

        return voucherListRequest;
    },

    // Tạo mới voucher
    create: async (data) => {
        clearVoucherCache();
        const res = await api.post('/vouchers', data);
        clearVoucherCache();
        return res;
    },

    // Cập nhật voucher
    update: async (id, data) => {
        clearVoucherCache();
        const res = await api.put(`/vouchers/${id}`, data);
        clearVoucherCache();
        return res;
    },

    // Xóa voucher
    delete: async (id) => {
        clearVoucherCache();
        await api.delete(`/vouchers/${id}`);
        clearVoucherCache();
    }
};
