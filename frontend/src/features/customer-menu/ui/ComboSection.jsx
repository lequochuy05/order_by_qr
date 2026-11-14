import ComboCard from './ComboCard.jsx';

const ComboSection = ({ combos, cart, onAddToCart, onFly }) => {
  if (combos.length === 0) return null;

  return (
    <div className="mb-2 mt-4">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-base font-black text-slate-900 transition-colors dark:text-white tracking-tight">
          <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-gradient-to-tr from-indigo-600 to-violet-500 text-white text-[11px] font-bold shadow-sm shadow-indigo-500/25">
            %
          </span>
          Combo Tiết Kiệm
        </h2>
        <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
          Ưu đãi
        </span>
      </div>
      <div className="flex gap-3 overflow-x-auto pb-2 no-scrollbar animate-in fade-in duration-500">
        {combos.map((combo) => (
          <div key={combo.id} className="min-w-[85vw] sm:min-w-[280px]">
            <ComboCard
              combo={combo}
              quantity={cart.combos[combo.id]?.qty || 0}
              onAddToCart={(item, quantity) => onAddToCart(item, quantity, true)}
              onFly={onFly}
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default ComboSection;
