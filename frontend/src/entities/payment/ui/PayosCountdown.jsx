import React, { useEffect, useRef, useState } from 'react';
import { Loader2 } from 'lucide-react';
import { calculatePayosTimeLeft } from '../lib/paymentExpiry.js';

const formatCountdown = (seconds) => {
  const safe = Math.max(0, Number(seconds || 0));
  const minutes = Math.floor(safe / 60);
  const remaining = safe % 60;
  return `${minutes.toString().padStart(2, '0')}:${remaining.toString().padStart(2, '0')}`;
};

/**
 * Isolated 1-second countdown badge for PayOS VietQR expiration.
 * Keeping the 1s tick inside this leaf component prevents parent payment modals
 * and customer order sheets from re-rendering every second.
 */
const PayosCountdown = ({ expiresAt, active = false, onExpire }) => {
  const [timeLeft, setTimeLeft] = useState(() =>
    expiresAt ? calculatePayosTimeLeft(expiresAt) : 0,
  );
  const expiredNotifiedRef = useRef(false);
  const onExpireRef = useRef(onExpire);

  useEffect(() => {
    onExpireRef.current = onExpire;
  }, [onExpire]);

  useEffect(() => {
    expiredNotifiedRef.current = false;
    if (!active || !expiresAt) {
      return undefined;
    }

    const tick = () => {
      const remaining = calculatePayosTimeLeft(expiresAt);
      setTimeLeft(remaining);
      if (remaining <= 0 && !expiredNotifiedRef.current) {
        expiredNotifiedRef.current = true;
        onExpireRef.current?.();
      }
    };

    const immediateTimer = window.setTimeout(tick, 0);
    const intervalId = window.setInterval(tick, 1000);
    return () => {
      window.clearTimeout(immediateTimer);
      window.clearInterval(intervalId);
    };
  }, [active, expiresAt]);

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 font-mono text-xs font-bold ${
        timeLeft < 60
          ? 'animate-pulse bg-rose-100 text-rose-600 dark:bg-rose-500/20 dark:text-rose-400'
          : 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300'
      }`}
    >
      <Loader2 size={12} className={active ? 'animate-spin' : ''} />
      Hết hạn sau: {formatCountdown(timeLeft)}
    </span>
  );
};

export default PayosCountdown;
