import { Plus, Minus, Flame } from 'lucide-react';
import { fmtVND } from '../../../utils/formatters.js';

const ComboCard = ({ combo, onAddToCart, quantity = 0, labels = { badge: 'COMBO', helper: 'Tiết kiệm hơn' } }) => {
  return (
    <div className="h-32 p-3 bg-gradient-to-br from-blue-500 via-blue-400 to-blue-500 text-white rounded-2xl shadow-[0_8px_20px_rgb(249,115,22,0.18)] relative overflow-hidden transition-all duration-300 active:scale-[0.98] flex flex-col justify-between">
      {/* Decorative flame */}
      <div className="absolute -right-5 -top-7 opacity-10 rotate-12 scale-110">
        <Flame size={76} />
      </div>
      <div className="absolute -left-9 -bottom-9 opacity-10 -rotate-12">
        <Flame size={68} />
      </div>

      <div className="relative z-10">
        <div className="flex items-center gap-2 mb-1">
          <span className="bg-white/20 backdrop-blur-md text-white text-[9px] px-2 py-0.5 rounded-full uppercase font-bold tracking-widest border border-white/20">{labels.badge}</span>
        </div>
        <h3 className="font-black text-sm leading-tight drop-shadow-sm line-clamp-1">{combo.name}</h3>
        <p className="mt-0.5 text-[10px] text-white font-medium">{labels.helper}</p>
      </div>

      <div className="relative z-10 flex items-end justify-between gap-2">
        <div>
          <span className="font-black text-base drop-shadow-md">
            {fmtVND(combo.price)}
          </span>
        </div>

        <div className="flex items-center gap-1.5 bg-white/20 backdrop-blur-md rounded-full px-1 py-0.5 shadow-lg border border-white/30">
          <button
            onClick={() => onAddToCart(combo, quantity - 1)}
            disabled={quantity === 0}
            className="w-6 h-6 rounded-full flex items-center justify-center text-white disabled:opacity-30 hover:bg-white/20 transition-colors font-bold"
          >
            <Minus size={12} />
          </button>
          <span className="text-xs font-black w-3 text-center">{quantity}</span>
          <button
            onClick={() => onAddToCart(combo, quantity + 1)}
            className="w-6 h-6 rounded-full flex items-center justify-center bg-white text-black hover:scale-105 transition-all shadow-sm font-bold"
          >
            <Plus size={12} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default ComboCard;
