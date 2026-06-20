import { useCallback, useEffect, useRef, useState } from 'react';

export const useScreenWakeLock = (enabled = true) => {
  const sentinelRef = useRef(null);
  const enabledRef = useRef(enabled);
  const [isActive, setIsActive] = useState(false);
  const isSupported = typeof navigator !== 'undefined' && 'wakeLock' in navigator;

  const releaseWakeLock = useCallback(async () => {
    const sentinel = sentinelRef.current;
    sentinelRef.current = null;

    if (sentinel && !sentinel.released) {
      await sentinel.release();
    }

    setIsActive(false);
  }, []);

  const requestWakeLock = useCallback(async () => {
    if (
      !enabledRef.current ||
      !isSupported ||
      document.visibilityState !== 'visible' ||
      (sentinelRef.current && !sentinelRef.current.released)
    ) {
      return false;
    }

    try {
      const sentinel = await navigator.wakeLock.request('screen');

      if (!enabledRef.current) {
        await sentinel.release();
        return false;
      }

      sentinelRef.current = sentinel;
      setIsActive(true);

      sentinel.addEventListener('release', () => {
        if (sentinelRef.current === sentinel) {
          sentinelRef.current = null;
          setIsActive(false);
        }
      });

      return true;
    } catch (error) {
      if (import.meta.env.DEV) {
        console.warn('[WakeLock] Không thể giữ màn hình luôn bật:', error);
      }
      setIsActive(false);
      return false;
    }
  }, [isSupported]);

  useEffect(() => {
    enabledRef.current = enabled;

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && enabledRef.current) {
        requestWakeLock();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    queueMicrotask(() => {
      if (enabledRef.current) {
        requestWakeLock();
      } else {
        releaseWakeLock();
      }
    });

    return () => {
      enabledRef.current = false;
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      releaseWakeLock();
    };
  }, [enabled, releaseWakeLock, requestWakeLock]);

  return {
    isActive,
    isSupported,
    requestWakeLock,
  };
};
