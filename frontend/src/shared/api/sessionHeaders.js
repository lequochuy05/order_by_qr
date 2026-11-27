export const sessionHeaderConfig = (sessionToken) =>
  sessionToken ? { headers: { 'X-Session-Token': sessionToken } } : undefined;
