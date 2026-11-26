import api from '@shared/api/httpClient.js';
import { sessionHeaderConfig } from '@shared/api/sessionHeaders.js';

export const serviceRequestService = {
  getCustomerRequests: (tableCode, sessionToken) =>
    api.get(`/public/tables/${tableCode}/service-requests`, {
      params: { sessionToken },
      skipAuth: true,
      ...sessionHeaderConfig(sessionToken),
    }),

  createCustomerRequest: (tableCode, payload) =>
    api.post(`/public/tables/${tableCode}/service-requests`, payload, {
      skipAuth: true,
      ...sessionHeaderConfig(payload?.sessionToken),
    }),

  cancelCustomerRequest: (tableCode, requestId, payload) =>
    api.post(`/public/tables/${tableCode}/service-requests/${requestId}/cancel`, payload, {
      skipAuth: true,
      ...sessionHeaderConfig(payload?.sessionToken),
    }),

  getStaffQueue: ({ status = 'OPEN', mineOnly = false, tableId } = {}) =>
    api.get('/table-service-requests', {
      params: {
        status,
        mineOnly,
        ...(tableId != null && tableId !== '' ? { tableId } : {}),
      },
    }),

  claimRequest: (id) => api.patch(`/table-service-requests/${id}/claim`),

  resolveRequest: (id) => api.patch(`/table-service-requests/${id}/resolve`),

  cancelStaffRequest: (id, reason) =>
    api.patch(`/table-service-requests/${id}/cancel`, reason ? { reason } : {}),
};
