import MenuCard from './MenuCard.jsx';

const MenuItemGrid = ({ items, getCartItemQty, onAddToCart, onFly }) => (
  <>
    <h2 className="mb-4 mt-6 text-sm font-black uppercase tracking-tight text-gray-800 transition-colors dark:text-white">
      Thực đơn
    </h2>
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
      <div className="py-10 text-center text-xs italic text-gray-400">
        Danh mục này hiện tại chưa có món.
      </div>
    )}
  </>
);

export default MenuItemGrid;
