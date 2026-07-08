import { ImageIcon } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const MenuItemCard = ({ item, onEdit, onDelete, isEditing = false }) => (
  <div className="group flex h-full flex-col rounded-xl border border-slate-100 bg-white p-3 shadow-sm transition-all hover:border-orange-500 hover:shadow-md dark:border-slate-800 dark:bg-slate-900">
    <div className="relative mb-3 h-32 flex-shrink-0 overflow-hidden rounded-lg bg-slate-50 dark:bg-slate-950">
      {item.img ? (
        <img
          src={item.img}
          alt={item.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
        />
      ) : (
        <div className="flex h-full items-center justify-center text-slate-300 dark:text-slate-600">
          <ImageIcon size={32} />
        </div>
      )}
      <div className="absolute right-2 top-2 rounded border border-orange-100 bg-white/90 px-2 py-0.5 text-[10px] font-bold text-orange-600 shadow-sm backdrop-blur dark:border-orange-500/20 dark:bg-slate-900/90 dark:text-orange-300">
        {item.category?.name}
      </div>
    </div>

    <div className="flex-grow">
      <h3 className="mb-1 line-clamp-1 text-sm font-bold text-slate-800 dark:text-slate-100">
        {item.name}
      </h3>
      {item.description && (
        <p className="mb-2 line-clamp-2 text-[11px] font-medium leading-snug text-slate-400 dark:text-slate-500">
          {item.description}
        </p>
      )}
      <p className="text-sm font-black text-orange-600 dark:text-orange-300">{fmtVND(item.price)}</p>
    </div>

    <EditDeleteActions
      onEdit={() => onEdit(item)}
      onDelete={() => onDelete(item.id)}
      editing={isEditing}
      className="mt-3"
    />
  </div>
);

export default MenuItemCard;
