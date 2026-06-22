export const calculatePayosTimeLeft = (expiresAt, now = Date.now()) => {
  if (!expiresAt) return 0;
  const expiryTime = new Date(expiresAt).getTime();
  if (!Number.isFinite(expiryTime)) return 0;
  return Math.max(0, Math.floor((expiryTime - now) / 1000));
};
