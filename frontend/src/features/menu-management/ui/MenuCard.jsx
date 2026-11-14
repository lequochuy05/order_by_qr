import { ImageIcon } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const MenuItemCard = ({ item, onEdit, onDelete, isEditing = false }) => (
  <div className="group flex h-full flex-col rounded-3xl border border-[#1E293B] bg-white/90 p-4 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-xl dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:hover:border-indigo-500/40">
    <div className="relative mb-3.5 h-36 flex-shrink-0 overflow-hidden rounded-2xl bg-slate-100 dark:bg-slate-800/60 border border-[#1E293B]/20 dark:border-[#1E293B]/60">
      {item.img ? (
        <img
          src={item.img}
          alt={item.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
      ) : (
        <div className="flex h-full items-center justify-center text-slate-300 dark:text-slate-600">
          <ImageIcon size={32} />
        </div>
      )}
      {item.category?.name && (
        <div className="absolute right-2.5 top-2.5 rounded-lg border border-indigo-200/60 bg-white/90 px-2 py-0.5 text-[10px] font-bold text-indigo-700 shadow-sm backdrop-blur-md dark:border-indigo-800/40 dark:bg-slate-900/90 dark:text-indigo-300">
          {item.category?.name}
        </div>
      )}
    </div>

    <div className="flex-grow">
      <h3 className="mb-1 line-clamp-1 text-sm font-bold text-slate-900 dark:text-white">
        {item.name}
      </h3>
      {item.description && (
        <p className="mb-2 line-clamp-2 text-xs font-medium leading-relaxed text-slate-500 dark:text-slate-400">
          {item.description}
        </p>
      )}
      <p className="text-base font-black font-mono tracking-tight text-slate-900 dark:text-white">
        {fmtVND(item.price)}
      </p>
    </div>

    <EditDeleteActions
      onEdit={() => onEdit(item)}
      onDelete={() => onDelete(item.id)}
      editing={isEditing}
      className="mt-3.5"
    />
  </div>
);

export default MenuItemCard;
