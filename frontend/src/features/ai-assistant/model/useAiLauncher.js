import { useCallback, useEffect, useRef, useState } from 'react';

import {
  clampLauncherPosition,
  getInitialLauncherPosition,
  LAUNCHER_STORAGE_KEY,
} from '../lib/launcherPosition.js';

const useAiLauncher = ({ hidden, onOpen }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isAnimating, setIsAnimating] = useState(false);
  const [position, setPosition] = useState(getInitialLauncherPosition);
  const [isDragging, setIsDragging] = useState(false);
  const ignoreBackdropClickUntilRef = useRef(0);
  const closeTimerRef = useRef(null);
  const dragStateRef = useRef({
    pointerId: null,
    startX: 0,
    startY: 0,
    originX: 0,
    originY: 0,
    moved: false,
  });

  const open = useCallback(() => {
    ignoreBackdropClickUntilRef.current = Date.now() + 250;
    setIsAnimating(true);
    setIsOpen(true);
    onOpen();
  }, [onOpen]);

  const close = useCallback(() => {
    setIsAnimating(false);
    window.clearTimeout(closeTimerRef.current);
    closeTimerRef.current = window.setTimeout(() => setIsOpen(false), 200);
  }, []);

  useEffect(
    () => () => {
      window.clearTimeout(closeTimerRef.current);
    },
    [],
  );

  useEffect(() => {
    const handleResize = () => setPosition((current) => clampLauncherPosition(current));
    window.addEventListener('resize', handleResize);
    window.addEventListener('orientationchange', handleResize);
    window.visualViewport?.addEventListener('resize', handleResize);
    window.visualViewport?.addEventListener('scroll', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('orientationchange', handleResize);
      window.visualViewport?.removeEventListener('resize', handleResize);
      window.visualViewport?.removeEventListener('scroll', handleResize);
    };
  }, []);

  useEffect(() => {
    localStorage.setItem(LAUNCHER_STORAGE_KEY, JSON.stringify(position));
  }, [position]);

  useEffect(() => {
    if (!hidden || !isOpen) return undefined;
    const timeout = window.setTimeout(() => {
      setIsAnimating(false);
      setIsOpen(false);
    }, 0);
    return () => window.clearTimeout(timeout);
  }, [hidden, isOpen]);

  const handleBackdropClick = useCallback(() => {
    if (Date.now() >= ignoreBackdropClickUntilRef.current) close();
  }, [close]);

  const handlePointerDown = useCallback(
    (event) => {
      if (event.button !== undefined && event.button !== 0) return;
      dragStateRef.current = {
        pointerId: event.pointerId,
        startX: event.clientX,
        startY: event.clientY,
        originX: position.x,
        originY: position.y,
        moved: false,
      };
      event.currentTarget.setPointerCapture?.(event.pointerId);
      setIsDragging(true);
    },
    [position],
  );

  const handlePointerMove = useCallback((event) => {
    if (dragStateRef.current.pointerId !== event.pointerId) return;
    const deltaX = event.clientX - dragStateRef.current.startX;
    const deltaY = event.clientY - dragStateRef.current.startY;
    if (Math.abs(deltaX) + Math.abs(deltaY) > 6) dragStateRef.current.moved = true;
    setPosition(
      clampLauncherPosition({
        x: dragStateRef.current.originX + deltaX,
        y: dragStateRef.current.originY + deltaY,
      }),
    );
  }, []);

  const finishPointer = useCallback(
    (event) => {
      if (dragStateRef.current.pointerId !== event.pointerId) return;
      event.currentTarget.releasePointerCapture?.(event.pointerId);
      setIsDragging(false);
      if (!dragStateRef.current.moved) open();
      dragStateRef.current.pointerId = null;
    },
    [open],
  );

  const handleKeyDown = useCallback(
    (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        open();
      }
    },
    [open],
  );

  return {
    isOpen,
    isAnimating,
    position,
    isDragging,
    open,
    close,
    handleBackdropClick,
    handlePointerDown,
    handlePointerMove,
    finishPointer,
    handleKeyDown,
  };
};

export default useAiLauncher;
