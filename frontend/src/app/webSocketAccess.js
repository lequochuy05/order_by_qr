export const canSubscribeToOperations = (user, pathname = '') =>
  Boolean(user) && pathname.startsWith('/admin');
