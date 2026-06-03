/**
 * Browser Notification API utility.
 * Requests permission on first call and shows notifications when document is hidden.
 */

let permissionGranted = false;

const requestPermission = async () => {
  if (permissionGranted) return true;
  if (!('Notification' in window)) return false;

  if (Notification.permission === 'granted') {
    permissionGranted = true;
    return true;
  }

  if (Notification.permission === 'denied') return false;

  const result = await Notification.requestPermission();
  permissionGranted = result === 'granted';
  return permissionGranted;
};

/**
 * Show a browser notification only when the tab is not visible.
 * @param {string} title
 * @param {object} options - { body, icon, tag }
 */
export const showBrowserNotification = async (title, options = {}) => {
  if (!document.hidden) return;

  const allowed = await requestPermission();
  if (!allowed) return;

  try {
    const notification = new Notification(title, {
      body: options.body || '',
      icon: options.icon || '/favicon.ico',
      tag: options.tag || 'order-notification',
      silent: true,
    });

    setTimeout(() => notification.close(), 5000);
  } catch {
    // Notification API not supported in this context
  }
};
