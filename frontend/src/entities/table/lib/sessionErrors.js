const TERMINAL_SESSION_ERROR_CODES = new Set([
  'TABLE_SESSION_EXPIRED',
  'TABLE_SESSION_INVALID',
  'TABLE_SESSION_NOT_FOUND',
]);

export const isTerminalSessionError = (error) => TERMINAL_SESSION_ERROR_CODES.has(error?.code);
