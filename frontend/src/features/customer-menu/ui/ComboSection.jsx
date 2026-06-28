import ComboCard from './ComboCard.jsx';

const ComboSection = ({ combos, cart, onAddToCart, onFly }) => {
  if (combos.length === 0) return null;

  return (
    <div className="mb-2 mt-4">
      <h2 className="mb-3 flex items-center gap-2 text-sm font-bold text-gray-800 transition-colors dark:text-white">
        Combo Khuyến Mãi
      </h2>
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
