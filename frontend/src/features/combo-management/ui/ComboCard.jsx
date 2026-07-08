import React from 'react';
import { Power, PowerOff, Package } from 'lucide-react';
import { fmtVND, fmtStatus } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const ComboCard = ({ combo, onEdit, onDelete, onToggle, isEditing = false }) => (
  <div className="group relative flex h-full flex-col overflow-hidden rounded-3xl border border-slate-100 bg-white p-6 shadow-sm transition-all hover:border-orange-500 hover:shadow-xl dark:border-slate-800 dark:bg-slate-900">
    {/* Trang trí góc thẻ */}
    <div className="absolute -right-6 -top-6 h-20 w-20 rounded-full bg-orange-50 transition-colors group-hover:bg-orange-100 dark:bg-orange-500/10 dark:group-hover:bg-orange-500/20" />

    <div className="flex items-center justify-between mb-5 relative z-10">
      <div
        className={`rounded-2xl p-3 ${combo.active ? 'bg-orange-500 text-white' : 'bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-500'}`}
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
      <h3 className="mb-2 text-xl font-bold text-slate-900 transition-colors group-hover:text-orange-600 dark:text-slate-100 dark:group-hover:text-orange-300">
        {combo.name}
      </h3>
      {combo.description && (
        <p className="mb-3 line-clamp-2 text-sm font-medium leading-snug text-slate-400 dark:text-slate-500">
          {combo.description}
        </p>
      )}
      <div className="text mb-4 font-black tracking-tight text-orange-600 dark:text-orange-300">
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
