import api from '@shared/api/httpClient.js';

export const subscriptionService = {
  getCurrent: () => api.get('/subscription/current'),
  getPlans: () => api.get('/subscription/plans'),
  getInvoices: () => api.get('/subscription/invoices'),
  activateTrial: () => api.post('/subscription/trial'),
  createCheckout: ({ billingCycle, returnPath = '/admin/billing', idempotencyKey }) =>
    api.post(
      '/subscription/checkout',
      { billingCycle, returnPath },
      idempotencyKey ? { headers: { 'Idempotency-Key': idempotencyKey } } : undefined,
    ),
  syncInvoice: (invoiceId) => api.post(`/subscription/invoices/${invoiceId}/sync`),
  cancelInvoice: (invoiceId) => api.post(`/subscription/invoices/${invoiceId}/cancel`),
};
