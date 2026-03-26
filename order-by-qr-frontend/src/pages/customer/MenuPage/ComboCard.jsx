import { Plus, Minus, Flame } from 'lucide-react';
import { fmtVND } from '../../../utils/formatters.js';

const ComboCard = ({ combo, onAddToCart, quantity = 0 }) => {
  return (
    <div className="p-4 bg-gradient-to-r from-orange-50 to-white rounded-2xl border border-orange-100 shadow-sm mb-3 relative overflow-hidden">
      <div className="absolute -right-2 -top-2 opacity-10 text-orange-500">
        <Flame size={60} />
      </div>

      <div className="relative z-10">
        <div className="flex items-center gap-2">
          <h3 className="font-bold text-gray-800 text-base">{combo.name}</h3>
          <span className="bg-orange-500 text-white text-[9px] px-1.5 py-0.5 rounded-full uppercase font-bold">Hot Combo</span>
        </div>
        <p className="text-[11px] text-gray-500 mt-1 italic">{'Ưu đãi đặc biệt'}</p>

        <div className="mt-4 flex items-center justify-between">
          <span className="text-orange-600 font-black text-lg">
            {fmtVND(combo.price)}
          </span>

          <div className="flex items-center gap-3 bg-white rounded-full px-2 py-1 shadow-sm border border-orange-100">
            <button
              onClick={() => onAddToCart(combo, quantity - 1)}
              disabled={quantity === 0}
              className="w-6 h-6 text-orange-500 disabled:text-gray-300 font-bold"
            > - </button>
            <span className="text-sm font-bold w-4 text-center">{quantity}</span>
            <button
              onClick={() => onAddToCart(combo, quantity + 1)}
              className="w-6 h-6 text-orange-600 font-bold"
            > + </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ComboCard;