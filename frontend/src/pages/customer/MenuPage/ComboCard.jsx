import { Plus, Minus, Flame } from 'lucide-react';
import { fmtVND } from '../../../utils/formatters.js';

const ComboCard = ({ combo, onAddToCart, quantity = 0 }) => {
  return (
    <div className="p-5 bg-gradient-to-br from-blue-500 via-blue-400 to-blue-500 text-white rounded-3xl shadow-[0_8px_20px_rgb(249,115,22,0.25)] relative overflow-hidden transition-all duration-300 active:scale-[0.98] h-full flex flex-col justify-between">
      {/* Decorative flame */}
      <div className="absolute -right-4 -top-8 opacity-10 rotate-12 scale-150">
        <Flame size={120} />
      </div>
      <div className="absolute -left-12 -bottom-10 opacity-10 -rotate-12 scale-125">
        <Flame size={100} />
      </div>

      <div className="relative z-10">
        <div className="flex items-center gap-2 mb-1">
          <span className="bg-white/20 backdrop-blur-md text-white text-[10px] px-2 py-0.5 rounded-full uppercase font-bold tracking-widest border border-white/20">COMBO</span>
        </div>
        <h3 className="font-black text-xl leading-tight drop-shadow-sm mb-1">{combo.name}</h3>
        <p className="text-xs text-white font-medium">{'Tiết kiệm hơn'}</p>
      </div>

      <div className="relative z-10 mt-6 flex items-end justify-between">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-widest text-white mb-0.5">Giá chỉ còn</p>
          <span className="font-black text-2xl drop-shadow-md">
            {fmtVND(combo.price)}
          </span>
        </div>

        <div className="flex items-center gap-3 bg-white/20 backdrop-blur-md rounded-full px-2 py-1 shadow-lg border border-white/30">
          <button
            onClick={() => onAddToCart(combo, quantity - 1)}
            disabled={quantity === 0}
            className="w-8 h-8 rounded-full flex items-center justify-center text-white disabled:opacity-30 hover:bg-white/20 transition-colors font-bold"
          >
            <Minus size={16} />
          </button>
          <span className="text-sm font-black w-3 text-center">{quantity}</span>
          <button
            onClick={() => onAddToCart(combo, quantity + 1)}
            className="w-8 h-8 rounded-full flex items-center justify-center bg-white text-black hover:scale-105 transition-all shadow-sm font-bold"
          >
            <Plus size={16} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default ComboCard;