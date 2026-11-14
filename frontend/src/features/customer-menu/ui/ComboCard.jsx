import { useRef } from 'react';
import { Plus, Minus, Flame } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const ComboCard = ({ combo, onAddToCart, quantity = 0, onFly }) => {
  const cardRef = useRef(null);
  const available = combo.available !== false;

  const handlePlus = () => {
    if (onFly) {
      const rect = cardRef.current?.getBoundingClientRect();
      if (rect) onFly(rect, combo.img);
    }
    onAddToCart(combo, quantity + 1);
  };

  return (
    <div
      ref={cardRef}
      className={`p-5 bg-gradient-to-br from-indigo-600 via-indigo-700 to-violet-800 text-white rounded-3xl shadow-xl shadow-indigo-600/25 relative overflow-hidden transition-all duration-300 active:scale-[0.98] h-full flex flex-col justify-between ${available ? '' : 'opacity-70'}`}
    >
      {/* Decorative flame */}
      <div className="absolute -right-4 -top-8 opacity-10 rotate-12 scale-150 pointer-events-none">
        <Flame size={120} />
      </div>
      <div className="absolute -left-12 -bottom-10 opacity-10 -rotate-12 scale-125 pointer-events-none">
        <Flame size={100} />
      </div>

      <div className="relative z-10">
        <div className="flex items-center gap-2 mb-2">
          <span className="bg-white/20 backdrop-blur-md text-white text-[10px] px-2.5 py-0.5 rounded-full uppercase font-bold tracking-widest border border-white/20">
            COMBO
          </span>
          {!available && (
            <span className="bg-white text-rose-600 text-xs px-2.5 py-0.5 rounded-full uppercase font-bold shadow-sm">
              Hết hàng
            </span>
          )}
        </div>
        <h3 className="font-black text-lg leading-tight drop-shadow-sm mb-1">{combo.name}</h3>
        <p className="text-xs text-white/80 font-medium line-clamp-2 leading-relaxed">
          {combo.description?.trim() || 'Combo ưu đãi'}
        </p>
      </div>

      <div className="relative z-10 mt-6 flex items-end justify-between">
        <div>
          <span className="font-black font-mono text-xl sm:text-2xl drop-shadow-md">{fmtVND(combo.price)}</span>
        </div>

        <div className="flex items-center gap-2.5 bg-white/20 backdrop-blur-md rounded-full px-2 py-1 shadow-lg border border-white/30">
          <button
            onClick={() => onAddToCart(combo, quantity - 1)}
            disabled={quantity === 0}
            className="w-7 h-7 rounded-full flex items-center justify-center text-white disabled:opacity-30 hover:bg-white/20 transition-all font-bold active:scale-95"
          >
            <Minus size={13} />
          </button>
          <span className="text-xs font-mono font-bold w-4 text-center">{quantity}</span>
          <button
            onClick={handlePlus}
            disabled={!available}
            className="w-7 h-7 rounded-full flex items-center justify-center bg-white text-indigo-600 hover:scale-105 transition-all shadow-sm font-bold active:scale-95 disabled:bg-slate-200 disabled:text-slate-400"
          >
            <Plus size={13} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default ComboCard;
