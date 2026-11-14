import { Sparkles } from 'lucide-react';

import MenuCard from './MenuCard.jsx';

const RecommendationSection = ({
  recommendations,
  selectedCategory,
  timeContext,
  getCartItemQty,
  onAddToCart,
  onFly,
}) => {
  if (recommendations.length === 0 || selectedCategory !== 'all') return null;

  return (
    <div className="-mx-4 mb-4 mt-8 border-b border-t border-indigo-100/60 bg-gradient-to-r from-indigo-50/70 via-indigo-50/30 to-transparent p-4 backdrop-blur-sm transition-colors dark:border-indigo-900/40 dark:from-indigo-950/40 dark:to-transparent">
      <h2 className="mb-3 flex items-center gap-2 px-4 text-xs font-bold uppercase tracking-wider text-indigo-900 transition-colors dark:text-indigo-300">
        <Sparkles size={15} className="fill-indigo-600 text-indigo-600 dark:fill-indigo-400 dark:text-indigo-400" /> Gợi ý cho bạn (
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
              onFly={onFly}
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default RecommendationSection;
