import React from 'react';
import { Image as ImageIcon } from 'lucide-react';
import { EditDeleteActions } from '@shared/ui';

const CategoryCard = ({ category, onEdit, onDelete }) => (
  <div className="group flex h-full flex-col rounded-2xl border border-[#E8DCD1] bg-white p-3.5 shadow-sm transition-all hover:border-[#C17C4A] hover:shadow-md dark:border-[#352C26] dark:bg-[#1F1A17]">
    <div className="relative mb-3 h-32 flex-shrink-0 overflow-hidden rounded-xl bg-[#F7F0E7] dark:bg-[#2A221E]">
      {category.img ? (
        <img
          src={category.img}
          alt={category.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
        />
      ) : (
        <div className="flex h-full items-center justify-center text-[#9C8E85] dark:text-[#82756B]">
          <ImageIcon size={32} />
        </div>
      )}
    </div>
    <h3 className="mb-3 line-clamp-1 flex-grow text-center text-sm font-bold text-[#2D1D16] dark:text-[#F7F0E7]">
      {category.name}
    </h3>
    <EditDeleteActions
      onEdit={() => onEdit(category)}
      onDelete={() => onDelete(category.id)}
    />
  </div>
);

export default CategoryCard;
