import React, { useState, useRef, useCallback } from 'react';

/**
 * FlyingImageOverlay — ảnh bay từ vị trí card xuống giỏ hàng.
 * Render trong component layout (CustomerMenu), auto biến mất sau animation.
 */
const FlyingImageOverlay = ({ item, onEnd }) => {
  const imgRef = useRef(null);
  const started = useRef(false);

  if (!started.current) {
    started.current = true;

    // Queue the animation to start after render paints initial position
    queueMicrotask(() => {
      const el = imgRef.current;
      if (!el) return;

      const { startRect, endRect } = item;

      // Set start position
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

      // Force layout
      el.getBoundingClientRect();

      // Animate to end position
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
  }

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

/**
 * Hook quản lý hiệu ứng "fly to cart".
 *
 * @returns {{ flyItem, cartRef, fly }}
 *   flyItem: object or null — để render FlyingImageOverlay
 *   cartRef: ref gắn vào ShoppingCart button
 *   fly(sourceRect, imageUrl): gọi khi click "+" để trigger animation
 */
const useFlyToCart = () => {
  const [flyItem, setFlyItem] = useState(null);
  const cartRef = useRef(null);

  const fly = useCallback((sourceRect, imageUrl) => {
    let endRect;

    if (cartRef.current) {
      const r = cartRef.current.getBoundingClientRect();
      endRect = {
        left: r.left + r.width / 2 - 18,
        top: r.top + r.height / 2 - 18,
        width: 36,
        height: 36,
      };
    } else {
      // Fallback: bottom center màn hình
      endRect = {
        left: window.innerWidth / 2 - 18,
        top: window.innerHeight - 90,
        width: 36,
        height: 36,
      };
    }

    setFlyItem({ src: imageUrl, startRect: sourceRect, endRect });
  }, []);

  const clearFly = useCallback(() => {
    setFlyItem(null);
  }, []);

  return { flyItem, cartRef, fly, clearFly };
};

export { FlyingImageOverlay };
export default useFlyToCart;
