import { useEffect, useRef, useState } from 'react';
import { ArrowDown, Loader2 } from 'lucide-react';

const DEFAULT_THRESHOLD = 80;
const DEFAULT_MAX_PULL = 120;
const REFRESH_HOLD_DISTANCE = 56;

const PullToRefresh = ({
  children,
  onRefresh,
  threshold = DEFAULT_THRESHOLD,
  maxPull = DEFAULT_MAX_PULL,
  disabled = false,
  className = '',
  contentClassName = '',
}) => {
  const scrollRef = useRef(null);
  const gestureRef = useRef({
    tracking: false,
    direction: null,
    startX: 0,
    startY: 0,
  });
  const pullDistanceRef = useRef(0);
  const refreshingRef = useRef(false);
  const [pullDistance, setPullDistance] = useState(0);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const updatePullDistance = (distance) => {
    pullDistanceRef.current = distance;
    setPullDistance(distance);
  };

  useEffect(() => {
    const scrollElement = scrollRef.current;
    if (!scrollElement) return undefined;

    const resetGesture = () => {
      gestureRef.current.tracking = false;
      gestureRef.current.direction = null;
    };

    const handleTouchStart = (event) => {
      if (
        disabled ||
        refreshingRef.current ||
        event.touches.length !== 1 ||
        scrollElement.scrollTop > 0
      ) {
        resetGesture();
        return;
      }

      const touch = event.touches[0];
      gestureRef.current = {
        tracking: true,
        direction: null,
        startX: touch.clientX,
        startY: touch.clientY,
      };
    };

    const handleTouchMove = (event) => {
      const gesture = gestureRef.current;
      if (!gesture.tracking || event.touches.length !== 1) return;

      const touch = event.touches[0];
      const deltaX = touch.clientX - gesture.startX;
      const deltaY = touch.clientY - gesture.startY;

      if (!gesture.direction && Math.abs(deltaX) + Math.abs(deltaY) >= 8) {
        gesture.direction = Math.abs(deltaY) > Math.abs(deltaX) ? 'vertical' : 'horizontal';
      }

      if (gesture.direction === 'horizontal') {
        resetGesture();
        return;
      }

      if (deltaY <= 0 || scrollElement.scrollTop > 0) {
        updatePullDistance(0);
        resetGesture();
        return;
      }

      if (gesture.direction === 'vertical') {
        event.preventDefault();
        updatePullDistance(Math.min(maxPull, deltaY * 0.5));
      }
    };

    const finishGesture = async () => {
      const shouldRefresh =
        gestureRef.current.tracking &&
        gestureRef.current.direction === 'vertical' &&
        pullDistanceRef.current >= threshold;

      resetGesture();

      if (!shouldRefresh || refreshingRef.current || disabled) {
        updatePullDistance(0);
        return;
      }

      refreshingRef.current = true;
      setIsRefreshing(true);
      updatePullDistance(REFRESH_HOLD_DISTANCE);

      try {
        await onRefresh?.();
      } finally {
        refreshingRef.current = false;
        setIsRefreshing(false);
        updatePullDistance(0);
      }
    };

    const cancelGesture = () => {
      resetGesture();
      if (!refreshingRef.current) updatePullDistance(0);
    };

    scrollElement.addEventListener('touchstart', handleTouchStart, { passive: true });
    scrollElement.addEventListener('touchmove', handleTouchMove, { passive: false });
    scrollElement.addEventListener('touchend', finishGesture, { passive: true });
    scrollElement.addEventListener('touchcancel', cancelGesture, { passive: true });

    return () => {
      scrollElement.removeEventListener('touchstart', handleTouchStart);
      scrollElement.removeEventListener('touchmove', handleTouchMove);
      scrollElement.removeEventListener('touchend', finishGesture);
      scrollElement.removeEventListener('touchcancel', cancelGesture);
    };
  }, [disabled, maxPull, onRefresh, threshold]);

  const readyToRefresh = pullDistance >= threshold;
  const indicatorText = isRefreshing
    ? 'Đang làm mới...'
    : readyToRefresh
      ? 'Thả để làm mới'
      : 'Kéo để làm mới';

  return (
    <div className={`relative min-h-0 overflow-hidden ${className}`}>
      <div
        aria-live="polite"
        className="pointer-events-none absolute left-1/2 top-0 z-20 flex h-11 items-center gap-2 rounded-full bg-white/95 px-3 text-xs font-bold text-orange-600 shadow-md backdrop-blur dark:bg-slate-800/95 dark:text-orange-300"
        style={{
          opacity: pullDistance > 4 || isRefreshing ? 1 : 0,
          transform: `translate(-50%, ${pullDistance - 48}px)`,
          transition: gestureRef.current.tracking
            ? 'none'
            : 'transform 180ms ease, opacity 180ms ease',
        }}
      >
        {isRefreshing ? (
          <Loader2 size={16} className="animate-spin" />
        ) : (
          <ArrowDown
            size={16}
            className={readyToRefresh ? 'rotate-180 transition-transform' : 'transition-transform'}
          />
        )}
        <span>{indicatorText}</span>
      </div>

      <div
        ref={scrollRef}
        className={`h-full overflow-y-auto overscroll-y-contain ${contentClassName}`}
        style={{
          transform: `translateY(${pullDistance}px)`,
          transition: gestureRef.current.tracking ? 'none' : 'transform 180ms ease',
          willChange: pullDistance > 0 ? 'transform' : 'auto',
        }}
      >
        {children}
      </div>
    </div>
  );
};

export default PullToRefresh;
