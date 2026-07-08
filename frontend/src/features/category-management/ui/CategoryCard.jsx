import React from 'react';
import { Image as ImageIcon } from 'lucide-react';
import { EditDeleteActions } from '@shared/ui';

const CategoryCard = ({ category, onEdit, onDelete }) => (
  <div className="group flex h-full flex-col rounded-xl border border-slate-100 bg-white p-3 shadow-sm transition-all hover:border-orange-500 hover:shadow-md dark:border-slate-800 dark:bg-slate-900">
    <div className="relative mb-3 h-32 flex-shrink-0 overflow-hidden rounded-lg bg-slate-50 dark:bg-slate-950">
      {category.img ? (
        <img
          src={category.img}
          alt={category.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
        />
      ) : (
        <div className="flex h-full items-center justify-center text-slate-300 dark:text-slate-600">
          <ImageIcon size={32} />
        </div>
      )}
    </div>
    <h3 className="mb-3 line-clamp-1 flex-grow text-center text-sm font-bold text-slate-800 dark:text-slate-100">
      {category.name}
    </h3>
    <EditDeleteActions
      onEdit={() => onEdit(category)}
      onDelete={() => onDelete(category.id)}
    />
  </div>
);

export default CategoryCard;
