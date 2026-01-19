import React from 'react';
import { Pencil, Trash2, ImageIcon } from 'lucide-react';

const MenuItemCard = ({ item, onEdit, onDelete }) => (
  <div className="bg-white rounded-xl p-3 shadow-sm border border-gray-100 group hover:border-orange-500 hover:shadow-md transition-all flex flex-col h-full">
    <div className="relative h-32 mb-3 bg-gray-50 rounded-lg overflow-hidden flex-shrink-0">
      {item.img ? (
        <img src={item.img} alt={item.name} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" />
      ) : (
        <div className="flex items-center justify-center h-full text-gray-300"><ImageIcon size={32} /></div>
      )}
      <div className="absolute top-2 right-2 bg-white/90 backdrop-blur px-2 py-0.5 rounded text-[10px] font-bold text-orange-600 shadow-sm border border-orange-100">
        {item.category?.name}
      </div>
    </div>
    
    <div className="flex-grow">
      <h3 className="font-bold text-gray-800 text-sm line-clamp-1 mb-1">{item.name}</h3>
      <p className="text-orange-600 font-black text-sm">{item.price.toLocaleString('vi-VN')}đ</p>
    </div>
    
    <div className="flex gap-2 mt-3 group-hover:opacity-100 transition-opacity">
      <button onClick={() => onEdit(item)} className="flex-1 py-1.5 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-600 hover:text-white transition-all flex justify-center border border-blue-100">
        <Pencil size={14} />
      </button>
      <button onClick={() => onDelete(item.id)} className="flex-1 py-1.5 bg-red-50 text-red-600 rounded-lg hover:bg-red-600 hover:text-white transition-all flex justify-center border border-red-100">
        <Trash2 size={14} />
      </button>
    </div>
  </div>
);

export default MenuItemCard;