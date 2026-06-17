import MenuItemImage from './MenuItemImage.jsx';
import MenuItemPrice from './MenuItemPrice.jsx';

const MenuItemCard = ({ item, actions = null }) => {
  if (!item) return null;

  return (
    <article className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <MenuItemImage src={item.imageUrl} alt={item.name} className="aspect-[4/3]" />
      <div className="space-y-2 p-4">
        <div>
          <h3 className="font-bold text-slate-900 dark:text-slate-100">{item.name}</h3>
          {item.description && (
            <p className="line-clamp-2 text-sm text-slate-500">{item.description}</p>
          )}
        </div>
        <div className="flex items-center justify-between gap-3">
          <MenuItemPrice value={item.price} />
          {actions}
        </div>
      </div>
    </article>
  );
};

export default MenuItemCard;
