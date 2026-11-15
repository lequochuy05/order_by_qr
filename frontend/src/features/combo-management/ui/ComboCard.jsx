import React from 'react';
import { Power, PowerOff, Package, ShoppingBag, ShoppingCart } from 'lucide-react';
import { fmtVND, fmtStatus } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

// K-fix: added onToggleAvailable prop + available toggle button.
// Previously admins had to open the full edit modal to change stock availability.
const ComboCard = ({ combo, onEdit, onDelete, onToggle, onToggleAvailable, isEditing = false }) => (
  <div className="group relative flex h-full flex-col overflow-hidden rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-xl dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:hover:border-indigo-500/40">
    <div className="flex items-center justify-between mb-5">
      <div
        className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-transform duration-300 group-hover:scale-110 shadow-lg ${
          combo.active
            ? 'bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-indigo-500/25'
            : 'bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-500 shadow-none'
        }`}
      >
        <Package size={22} />
      </div>
      <div className="flex flex-col items-end gap-1">
        <span
          className={`text-[10px] font-black px-3 py-1 rounded-full uppercase tracking-wider ${fmtStatus('active', combo.active).color}`}
        >
          {fmtStatus('active', combo.active).label}
        </span>
        {combo.active && (
          <span
            className={`text-[10px] font-black px-2.5 py-0.5 rounded-full uppercase tracking-wider ${
              combo.available !== false
                ? 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-300'
                : 'bg-amber-50 text-amber-600 dark:bg-amber-950/40 dark:text-amber-300'
            }`}
          >
            {combo.available !== false ? 'Còn hàng' : 'Hết hàng'}
          </span>
        )}
      </div>
    </div>

    <div className="flex-grow">
      <h3 className="mb-2 text-xl font-black text-slate-900 transition-colors group-hover:text-indigo-600 dark:text-white dark:group-hover:text-indigo-400">
        {combo.name}
      </h3>
      {combo.description && (
        <p className="mb-4 line-clamp-2 text-xs font-medium leading-relaxed text-slate-500 dark:text-slate-400">
          {combo.description}
        </p>
      )}
      <div className="mb-5 text-2xl font-black font-mono tracking-tight text-slate-900 dark:text-white">
        {fmtVND(combo.price)}
      </div>
    </div>

    <div className="mt-auto space-y-2">
      <button
        type="button"
        onClick={() => onToggle(combo.id)}
        className={`flex w-full items-center justify-center gap-2 rounded-xl border py-2.5 text-xs font-bold transition-all active:scale-95 ${
          combo.active
            ? 'border-rose-200/60 bg-rose-50 text-rose-600 hover:bg-rose-600 hover:text-white dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300 dark:hover:bg-rose-600 dark:hover:text-white'
            : 'border-emerald-200/60 bg-emerald-50 text-emerald-600 hover:bg-emerald-600 hover:text-white dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300 dark:hover:bg-emerald-600 dark:hover:text-white'
        }`}
      >
        {combo.active ? <PowerOff size={15} /> : <Power size={15} />}
        {combo.active ? 'Tạm ngưng' : 'Kích hoạt'}
      </button>

      {/* K-fix: quick toggle available without opening the full edit modal */}
      {combo.active && onToggleAvailable && (
        <button
          type="button"
          onClick={() => onToggleAvailable(combo.id, combo.version)}
          className={`flex w-full items-center justify-center gap-2 rounded-xl border py-2 text-xs font-bold transition-all active:scale-95 ${
            combo.available !== false
              ? 'border-amber-200/60 bg-amber-50 text-amber-600 hover:bg-amber-500 hover:text-white dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-300 dark:hover:bg-amber-500 dark:hover:text-white'
              : 'border-emerald-200/60 bg-emerald-50 text-emerald-600 hover:bg-emerald-500 hover:text-white dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300 dark:hover:bg-emerald-500 dark:hover:text-white'
          }`}
        >
          {combo.available !== false ? <ShoppingCart size={14} /> : <ShoppingBag size={14} />}
          {combo.available !== false ? 'Đánh dấu hết hàng' : 'Còn hàng trở lại'}
        </button>
      )}

      <EditDeleteActions
        onEdit={() => onEdit(combo.id)}
        onDelete={() => onDelete(combo.id)}
        editing={isEditing}
        className="pt-2"
      />
    </div>
  </div>
);

export default ComboCard;
