import { useEffect, useState } from 'react';
import { WifiOff } from 'lucide-react';

const getInitialOnlineState = () => (typeof navigator === 'undefined' ? true : navigator.onLine);

const OfflineIndicator = () => {
  const [isOnline, setIsOnline] = useState(getInitialOnlineState);

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (isOnline) return null;

  return (
    <div
      className="fixed left-1/2 z-[100] flex w-[calc(100%_-_1rem)] max-w-md -translate-x-1/2 items-center justify-center gap-2 rounded-b-2xl bg-red-600 px-4 py-2.5 text-center text-xs font-bold text-white shadow-lg"
      style={{ top: 'var(--safe-area-inset-top)' }}
      role="status"
      aria-live="assertive"
    >
      <WifiOff size={16} className="shrink-0" />
      <span>Mất kết nối Internet. Dữ liệu có thể chưa được cập nhật.</span>
    </div>
  );
};

export default OfflineIndicator;
