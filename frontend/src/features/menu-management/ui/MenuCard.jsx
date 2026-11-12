import { ImageIcon } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const MenuItemCard = ({ item, onEdit, onDelete, isEditing = false }) => (
  <div className="group flex h-full flex-col rounded-2xl border border-[#E8DCD1] bg-white p-3.5 shadow-sm transition-all hover:border-[#C17C4A] hover:shadow-md dark:border-[#352C26] dark:bg-[#1F1A17]">
    <div className="relative mb-3 h-32 flex-shrink-0 overflow-hidden rounded-xl bg-[#F7F0E7] dark:bg-[#2A221E]">
      {item.img ? (
        <img
          src={item.img}
          alt={item.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
        />
      ) : (
        <div className="flex h-full items-center justify-center text-[#9C8E85] dark:text-[#82756B]">
          <ImageIcon size={32} />
        </div>
      )}
      <div className="absolute right-2 top-2 rounded-lg border border-[#C17C4A]/30 bg-white/95 px-2 py-0.5 text-[10px] font-bold text-[#56372B] shadow-sm backdrop-blur dark:border-[#C17C4A]/30 dark:bg-[#1F1A17]/95 dark:text-[#D8986A]">
        {item.category?.name}
      </div>
    </div>

    <div className="flex-grow">
      <h3 className="mb-1 line-clamp-1 text-sm font-bold text-[#2D1D16] dark:text-[#F7F0E7]">
        {item.name}
      </h3>
      {item.description && (
        <p className="mb-2 line-clamp-2 text-[11px] font-medium leading-snug text-[#9C8E85] dark:text-[#82756B]">
          {item.description}
        </p>
      )}
      <p className="text-sm font-black text-[#56372B] dark:text-[#D8986A]">{fmtVND(item.price)}</p>
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
