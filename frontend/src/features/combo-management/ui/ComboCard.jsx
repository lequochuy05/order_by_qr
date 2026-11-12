import React from 'react';
import { Power, PowerOff, Package } from 'lucide-react';
import { fmtVND, fmtStatus } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const ComboCard = ({ combo, onEdit, onDelete, onToggle, isEditing = false }) => (
  <div className="group relative flex h-full flex-col overflow-hidden rounded-3xl border border-[#E8DCD1] bg-white p-6 shadow-sm transition-all hover:border-[#C17C4A] hover:shadow-xl dark:border-[#352C26] dark:bg-[#1F1A17]">
    {/* Trang trí góc thẻ */}
    <div className="absolute -right-6 -top-6 h-20 w-20 rounded-full bg-[#F7F0E7] transition-colors group-hover:bg-[#EAE0D5] dark:bg-[#2A221E] dark:group-hover:bg-[#352C26]" />

    <div className="flex items-center justify-between mb-5 relative z-10">
      <div
        className={`rounded-2xl p-3 ${combo.active ? 'bg-[#56372B] text-white dark:bg-[#C17C4A]' : 'bg-[#F7F0E7] text-[#9C8E85] dark:bg-[#2A221E] dark:text-[#82756B]'}`}
      >
        <Package size={24} />
      </div>
      <span
        className={`text-[11px] font-black px-3 py-1 rounded-full uppercase tracking-wider ${fmtStatus('active', combo.active).color}`}
      >
        {fmtStatus('active', combo.active).label}
      </span>
    </div>

    <div className="flex-grow relative z-10">
      <h3 className="mb-2 text-xl font-bold text-[#2D1D16] transition-colors group-hover:text-[#C17C4A] dark:text-[#F7F0E7] dark:group-hover:text-[#D8986A]">
        {combo.name}
      </h3>
      {combo.description && (
        <p className="mb-3 line-clamp-2 text-sm font-medium leading-snug text-[#9C8E85] dark:text-[#82756B]">
          {combo.description}
        </p>
      )}
      <div className="mb-4 text-xl font-black tracking-tight text-[#56372B] dark:text-[#D8986A]">
        {fmtVND(combo.price)}
      </div>
    </div>

    <div className="relative z-10 mt-auto">
      <button
        onClick={() => onToggle(combo.id)}
        className={`flex w-full items-center justify-center gap-2 rounded-xl border py-2.5 text-sm font-bold transition-all ${
          combo.active
            ? 'border-red-100 bg-red-50 text-red-600 hover:bg-red-600 hover:text-white dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300'
            : 'border-green-100 bg-green-50 text-green-600 hover:bg-green-600 hover:text-white dark:border-green-500/20 dark:bg-green-500/10 dark:text-green-300'
        }`}
      >
        {combo.active ? <PowerOff size={18} /> : <Power size={18} />}
        {combo.active ? 'Tạm ngưng' : 'Kích hoạt'}
      </button>
      <EditDeleteActions
        onEdit={() => onEdit(combo.id)}
        onDelete={() => onDelete(combo.id)}
        editing={isEditing}
        className="mt-3"
      />
    </div>
  </div>
);

export default ComboCard;
