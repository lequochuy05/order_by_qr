import { ShoppingCart } from 'lucide-react';
import { fmtVND } from '../../../utils/formatters.js';

const ShoppingCartButton = ({ cart, onOpenCart }) => {
  const totalItems = Object.values(cart.items || {}).reduce((sum, item) => sum + item.qty, 0) +
    Object.values(cart.combos || {}).reduce((sum, combo) => sum + combo.qty, 0);

  const totalAmount = Object.values(cart.items || {}).reduce((sum, item) => sum + (item.qty * item.price), 0) +
    Object.values(cart.combos || {}).reduce((sum, combo) => sum + (combo.qty * combo.price), 0);

  if (totalItems === 0) return null;

  return (
    <div className="fixed bottom-6 left-0 right-0 px-4 z-50">
      <button
        onClick={onOpenCart}
        className="max-w-md mx-auto w-full bg-orange-500 hover:bg-orange-600 text-white p-4 rounded-2xl shadow-xl flex items-center justify-between transition-transform active:scale-95"
      >
        <div className="flex items-center gap-3">
          <div className="relative">
            <ShoppingCart size={24} />
            <span className="absolute -top-2 -right-2 bg-red-500 text-white text-[10px] font-bold w-5 h-5 rounded-full flex items-center justify-center border-2 border-blue-600">
              {totalItems}
            </span>
          </div>
          <span className="font-bold">Xem giỏ hàng</span>
        </div>
        <span className="font-black text-lg">
          {fmtVND(totalAmount)}
        </span>
      </button>
    </div>
  );
};

export default ShoppingCartButton;