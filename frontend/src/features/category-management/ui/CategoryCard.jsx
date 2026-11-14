import React from 'react';
import { Image as ImageIcon } from 'lucide-react';
import { EditDeleteActions } from '@shared/ui';

const CategoryCard = ({ category, onEdit, onDelete }) => (
  <div className="group flex h-full flex-col rounded-3xl border border-[#1E293B] bg-white/90 p-4 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-xl dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:hover:border-indigo-500/40">
    <div className="relative mb-3.5 h-36 flex-shrink-0 overflow-hidden rounded-2xl bg-slate-100 dark:bg-slate-800/60 border border-[#1E293B]/20 dark:border-[#1E293B]/60">
      {category.img ? (
        <img
          src={category.img}
          alt={category.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
      ) : (
        <div className="flex h-full items-center justify-center text-slate-300 dark:text-slate-600">
          <ImageIcon size={32} />
        </div>
      )}
    </div>
    <h3 className="mb-3 line-clamp-1 flex-grow text-center text-sm font-bold text-slate-900 dark:text-white">
      {category.name}
    </h3>
    <EditDeleteActions
      onEdit={() => onEdit(category)}
      onDelete={() => onDelete(category.id)}
    />
  </div>
);

export default CategoryCard;
