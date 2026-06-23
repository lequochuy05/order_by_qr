import React from 'react';
import { Image as ImageIcon } from 'lucide-react';
import { EditDeleteActions } from '@shared/ui';

const CategoryCard = ({ category, onEdit, onDelete }) => (
  <div className="bg-white rounded-xl p-3 shadow-sm border border-gray-100 group hover:border-orange-500 hover:shadow-md transition-all flex flex-col h-full">
    <div className="relative h-32 mb-3 bg-gray-50 rounded-lg overflow-hidden flex-shrink-0">
      {category.img ? (
        <img
          src={category.img}
          alt={category.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
        />
      ) : (
        <div className="flex items-center justify-center h-full text-gray-300">
          <ImageIcon size={32} />
        </div>
      )}
    </div>
    <h3 className="font-bold text-gray-800 text-sm mb-3 text-center line-clamp-1 flex-grow">
      {category.name}
    </h3>
    <EditDeleteActions
      onEdit={() => onEdit(category)}
      onDelete={() => onDelete(category.id)}
    />
  </div>
);

export default CategoryCard;
