import { useState, useRef, useCallback } from 'react';

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

export default useFlyToCart;
