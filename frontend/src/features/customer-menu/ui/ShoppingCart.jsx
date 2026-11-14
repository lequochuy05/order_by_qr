import { forwardRef, useRef, useEffect, useState } from 'react';
import { ShoppingCart } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const ShoppingCartButton = forwardRef(({ cart, onOpenCart }, ref) => {
  const [bumping, setBumping] = useState(false);
  const prevTotal = useRef(0);

  const totalItems =
    Object.values(cart.items || {}).reduce((sum, item) => sum + item.qty, 0) +
    Object.values(cart.combos || {}).reduce((sum, combo) => sum + combo.qty, 0);

  const cartAmount =
    Object.values(cart.items || {}).reduce((sum, item) => sum + item.qty * item.price, 0) +
    Object.values(cart.combos || {}).reduce((sum, combo) => sum + combo.qty * combo.price, 0);

  useEffect(() => {
    if (totalItems > prevTotal.current && prevTotal.current > 0) {
      const startTimer = setTimeout(() => setBumping(true), 0);
      const stopTimer = setTimeout(() => setBumping(false), 300);
      prevTotal.current = totalItems;
      return () => {
        clearTimeout(startTimer);
        clearTimeout(stopTimer);
      };
    }
    prevTotal.current = totalItems;
  }, [totalItems]);

  if (totalItems === 0) return null;

  return (
    <div
      ref={ref}
      className="fixed left-0 right-0 z-50 px-4"
      style={{ bottom: 'calc(1.5rem + var(--safe-area-inset-bottom))' }}
    >
      <button
        onClick={onOpenCart}
        className={`max-w-md mx-auto w-full bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white p-4 rounded-2xl shadow-xl shadow-indigo-600/30 flex items-center justify-between transition-all active:scale-95 ${bumping ? 'animate-cart-bump' : ''}`}
      >
        <div className="flex items-center gap-3">
          <div className="relative">
            <ShoppingCart size={22} />
            <span className="absolute -top-2 -right-2.5 bg-white text-indigo-600 text-[11px] font-black font-mono w-5 h-5 rounded-full flex items-center justify-center border-2 border-indigo-600 shadow-sm">
              {totalItems}
            </span>
          </div>
          <span className="font-bold text-xs uppercase tracking-wider">Xem giỏ hàng</span>
        </div>
        <span className="font-black font-mono text-base sm:text-lg">{fmtVND(cartAmount)}</span>
      </button>
    </div>
  );
});

export default ShoppingCartButton;
ShoppingCartButton.displayName = 'ShoppingCartButton';
