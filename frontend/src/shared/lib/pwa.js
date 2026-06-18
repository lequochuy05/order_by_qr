import { createElement, useEffect, useRef } from 'react';
import toast from 'react-hot-toast';
import { useRegisterSW as useViteRegisterSW } from 'virtual:pwa-register/react';

const UPDATE_TOAST_ID = 'pwa-update-available';
const OFFLINE_TOAST_ID = 'pwa-offline-ready';
const ERROR_TOAST_ID = 'pwa-register-error';

const showUpdateToast = (onUpdate) => {
  toast(
    (currentToast) =>
      createElement(
        'div',
        { className: 'flex items-center gap-3' },
        createElement('span', null, 'Đã có phiên bản mới. Làm mới để cập nhật.'),
        createElement(
          'button',
          {
            type: 'button',
            className:
              'shrink-0 rounded-lg bg-orange-600 px-3 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-orange-700',
            onClick: async () => {
              toast.dismiss(currentToast.id);
              await onUpdate();
            },
          },
          'Cập nhật',
        ),
      ),
    {
      id: UPDATE_TOAST_ID,
      duration: Infinity,
      icon: '🔄',
    },
  );
};

export const useRegisterSW = () => {
  const updateServiceWorkerRef = useRef(null);

  useEffect(() => {
    if (!import.meta.env.DEV || !('serviceWorker' in navigator)) return;

    navigator.serviceWorker.getRegistrations().then((registrations) => {
      registrations.forEach((registration) => registration.unregister());
    });
  }, []);

  const registration = useViteRegisterSW({
    immediate: true,
    onNeedRefresh: () => {
      showUpdateToast(() => updateServiceWorkerRef.current?.(true));
    },
    onNeedReload: () => {
      showUpdateToast(() => window.location.reload());
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
