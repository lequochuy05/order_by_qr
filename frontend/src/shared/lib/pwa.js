import { useEffect, useRef } from 'react';
import toast from 'react-hot-toast';
import { useRegisterSW as useViteRegisterSW } from 'virtual:pwa-register/react';

const OFFLINE_TOAST_ID = 'pwa-offline-ready';
const ERROR_TOAST_ID = 'pwa-register-error';

export const useRegisterSW = () => {
  const updateServiceWorkerRef = useRef(null);
  const updatingRef = useRef(false);

  useEffect(() => {
    if (!import.meta.env.DEV || !('serviceWorker' in navigator)) return;

    navigator.serviceWorker.getRegistrations().then((registrations) => {
      registrations.forEach((registration) => registration.unregister());
    });
  }, []);

  const registration = useViteRegisterSW({
    immediate: true,
    onNeedRefresh: async () => {
      if (updatingRef.current) return;
      updatingRef.current = true;

      try {
        await updateServiceWorkerRef.current?.(true);
      } finally {
        window.location.reload();
      }
    },
    onNeedReload: () => {
      window.location.reload();
    },
    onOfflineReady: () => {
      toast.success('Ứng dụng đã sẵn sàng để sử dụng khi ngoại tuyến.', {
        id: OFFLINE_TOAST_ID,
      });
    },
    onRegisterError: (error) => {
      console.error('Không thể đăng ký Service Worker:', error);
      toast.error('Không thể bật chế độ ngoại tuyến.', {
        id: ERROR_TOAST_ID,
      });
    },
  });

  useEffect(() => {
    updateServiceWorkerRef.current = registration.updateServiceWorker;
  }, [registration.updateServiceWorker]);

  return registration;
};
