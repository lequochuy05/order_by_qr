import { isAdminRoute } from '../shared/lib/routeMatchers.js';

export { isAdminRoute, isCustomerMenuPath } from '../shared/lib/routeMatchers.js';

export const canSubscribeToOperations = (user, pathname = '') =>
  Boolean(user) && isAdminRoute(pathname);
