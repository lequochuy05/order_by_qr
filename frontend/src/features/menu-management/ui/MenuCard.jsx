import { ImageIcon } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const MenuItemCard = ({ item, onEdit, onDelete, isEditing = false }) => (
  <div className="bg-white rounded-xl p-3 shadow-sm border border-gray-100 group hover:border-orange-500 hover:shadow-md transition-all flex flex-col h-full">
    <div className="relative h-32 mb-3 bg-gray-50 rounded-lg overflow-hidden flex-shrink-0">
      {item.img ? (
        <img
          src={item.img}
          alt={item.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
        />
      ) : (
        <div className="flex items-center justify-center h-full text-gray-300">
          <ImageIcon size={32} />
        </div>
      )}
      <div className="absolute top-2 right-2 bg-white/90 backdrop-blur px-2 py-0.5 rounded text-[10px] font-bold text-orange-600 shadow-sm border border-orange-100">
        {item.category?.name}
      </div>
    </div>

    <div className="flex-grow">
      <h3 className="font-bold text-gray-800 text-sm line-clamp-1 mb-1">{item.name}</h3>
      {item.description && (
        <p className="mb-2 line-clamp-2 text-[11px] font-medium leading-snug text-gray-400">
          {item.description}
        </p>
      )}
      <p className="text-orange-600 font-black text-sm">{fmtVND(item.price)}</p>
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
