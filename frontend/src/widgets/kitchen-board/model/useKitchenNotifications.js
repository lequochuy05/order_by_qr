import { useCallback } from 'react';

import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import { useScreenWakeLock } from '@shared/hooks/useScreenWakeLock.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { showBrowserNotification } from '@shared/lib/browserNotification.js';
import { playLoudSound, playNotificationSound } from '@shared/lib/notificationSound.js';

const useKitchenNotifications = (onRealtimeUpdate) => {
  const [preferences, setPreferences] = useAdminPreferences();
  const wakeLock = useScreenWakeLock(true);

  useWebSocket('/topic/kitchen', (message) => {
    if (message !== 'UPDATED' && (typeof message !== 'object' || message === null)) return;
    playNotificationSound();
    playLoudSound();
    showBrowserNotification('Đơn hàng nhà bếp', {
      body: 'Có cập nhật mới cho nhà bếp!',
    });
    onRealtimeUpdate();
  });

  const toggleSound = useCallback(() => {
    setPreferences((current) => ({
      ...current,
      notificationSound: !current.notificationSound,
    }));
  }, [setPreferences]);

  return { preferences, wakeLock, toggleSound };
};

export default useKitchenNotifications;
