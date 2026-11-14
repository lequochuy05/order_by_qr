import { useEffect } from 'react';
import { calculatePayosTimeLeft } from '@entities/payment/lib/paymentExpiry.js';

/**
 * Quản lý countdown timer cho PayOS QR.
 * Cập nhật timeLeft mỗi giây, set status 'expired' khi hết giờ.
 */
const usePayosTimer = ({ payosStatus, expiresAt, setTimeLeft, setPayosStatus }) => {
  useEffect(() => {
    if (payosStatus !== 'waiting' || !expiresAt) return undefined;

    const updateTimeLeft = () => {
      const remaining = calculatePayosTimeLeft(expiresAt);
      setTimeLeft(remaining);
      if (remaining <= 0) setPayosStatus('expired');
    };
    updateTimeLeft();
    const interval = window.setInterval(updateTimeLeft, 1000);
    return () => window.clearInterval(interval);
  }, [expiresAt, payosStatus, setPayosStatus, setTimeLeft]);
};

export default usePayosTimer;
