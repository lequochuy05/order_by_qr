import React, { useLayoutEffect, useRef } from 'react';

/**
 * FlyingImageOverlay — ảnh bay từ vị trí card xuống giỏ hàng.
 * Render trong component layout (CustomerMenu), auto biến mất sau animation.
 */
const FlyingImageOverlay = ({ item, onEnd }) => {
  const imgRef = useRef(null);

  useLayoutEffect(() => {
    const el = imgRef.current;
    if (!el) return undefined;

    const { startRect, endRect } = item;

    Object.assign(el.style, {
      transition: 'none',
      left: `${startRect.left}px`,
      top: `${startRect.top}px`,
      width: `${startRect.width}px`,
      height: `${startRect.height}px`,
      opacity: '1',
      transform: 'scale(1)',
      borderRadius: '12px',
    });

    el.getBoundingClientRect();

    const animationFrame = requestAnimationFrame(() => {
      Object.assign(el.style, {
        transition: 'all 500ms cubic-bezier(0.34, 1.56, 0.64, 1)',
        left: `${endRect.left}px`,
        top: `${endRect.top}px`,
        width: '36px',
        height: '36px',
        opacity: '0.25',
        transform: 'scale(0.3)',
        borderRadius: '50%',
      });
    });

    return () => cancelAnimationFrame(animationFrame);
  }, [item]);

  return (
    <img
      ref={imgRef}
      src={item.src}
      alt=""
      onTransitionEnd={() => onEnd()}
      onError={() => onEnd()}
      style={{
        position: 'fixed',
        zIndex: 9999,
        objectFit: 'cover',
        pointerEvents: 'none',
        boxShadow: '0 8px 32px rgba(0,0,0,0.25)',
        willChange: 'transform, left, top, width, height, opacity',
      }}
    />
  );
};

export default FlyingImageOverlay;
