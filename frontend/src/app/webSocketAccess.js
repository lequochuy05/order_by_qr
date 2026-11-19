import { isAdminRoute } from '../shared/lib/routeMatchers.js';

export { isAdminRoute, isCustomerMenuPath } from '../shared/lib/routeMatchers.js';

export const canSubscribeToOperations = (user, pathname = '') =>
  Boolean(user) && isAdminRoute(pathname);

export const canSubscribeToUsers = (user, pathname = '') =>
  Boolean(user) && isAdminRoute(pathname) && ['ADMIN', 'MANAGER'].includes(user?.role);

export const canSubscribeToMarketing = (user, pathname = '') =>
  Boolean(user) &&
  isAdminRoute(pathname) &&
  ['ADMIN', 'MANAGER', 'CASHIER', 'STAFF'].includes(user?.role);

export const canSubscribeToKitchen = (user, pathname = '') =>
  Boolean(user) && isAdminRoute(pathname) && ['ADMIN', 'MANAGER', 'CHEF'].includes(user?.role);

