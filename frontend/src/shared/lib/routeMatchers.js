export const isAdminRoute = (pathname = '') => pathname.startsWith('/admin');

export const isCustomerMenuPath = (pathname = '') => {
  const normalized = pathname.replace(/\/+$/, '') || '/';
  if (normalized === '/menu') return true;
  if (normalized === '/admin/menu') return false;
  return /^\/[^/]+\/menu$/.test(normalized);
};
