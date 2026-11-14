import MenuCard from './MenuCard.jsx';

const MenuItemGrid = ({ items, getCartItemQty, onAddToCart, onFly }) => (
  <>
    <div className="mb-4 mt-6 flex items-center justify-between">
      <h2 className="text-base font-black tracking-tight text-slate-900 transition-colors dark:text-white">
        Danh sách món ngon
      </h2>
      <span className="text-xs font-bold font-mono text-slate-400 dark:text-slate-500">
        {items.length} món
      </span>
    </div>
    <div className="grid grid-cols-2 gap-4">
      {items.map((item, index) => (
        <div
          key={item.id}
          className="animate-stagger-fade"
          style={{ animationDelay: `${index * 40}ms` }}
        >
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
    {items.length === 0 && (
      <div className="py-12 text-center text-xs italic text-slate-400 dark:text-slate-500">
        Danh mục này hiện tại chưa có món.
      </div>
    )}
  </>
);

export default MenuItemGrid;
