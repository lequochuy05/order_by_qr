import { Sparkles } from 'lucide-react';

import MenuCard from '../MenuCard.jsx';

const RecommendationSection = ({
  recommendations,
  selectedCategory,
  timeContext,
  getCartItemQty,
  onAddToCart,
}) => {
  if (recommendations.length === 0 || selectedCategory !== 'all') return null;

  return (
    <div className="-mx-4 mb-4 mt-8 border-b border-t border-orange-100/50 bg-gradient-to-r from-orange-50/80 to-transparent p-4 backdrop-blur-sm transition-colors dark:border-slate-800/50 dark:from-slate-800/80 dark:to-transparent">
      <h2 className="mb-3 flex items-center gap-2 px-4 text-sm font-bold uppercase tracking-wider text-orange-900 transition-colors dark:text-orange-300">
        <Sparkles size={16} className="fill-orange-500 text-orange-500" /> Gợi ý cho bạn (
        {timeContext})
      </h2>
      <div className="flex gap-3 overflow-x-auto px-4 pb-2 no-scrollbar">
        {recommendations.map((item) => (
          <div key={item.id} className="min-w-[140px]">
            <MenuCard
              item={item}
              quantity={getCartItemQty(item)}
              onAddToCart={(product, quantity, needsOptions) =>
                onAddToCart(product, quantity, false, needsOptions)
              }
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default RecommendationSection;
