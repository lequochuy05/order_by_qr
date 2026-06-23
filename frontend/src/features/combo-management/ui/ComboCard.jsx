import React from 'react';
import { Power, PowerOff, Package } from 'lucide-react';
import { fmtVND, fmtStatus } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const ComboCard = ({ combo, onEdit, onDelete, onToggle, isEditing = false }) => (
  <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 group hover:border-orange-500 hover:shadow-xl transition-all flex flex-col h-full relative overflow-hidden">
    {/* Trang trí góc thẻ */}
    <div className="absolute -top-6 -right-6 w-20 h-20 bg-orange-50 rounded-full group-hover:bg-orange-100 transition-colors" />

    <div className="flex items-center justify-between mb-5 relative z-10">
      <div
        className={`p-3 rounded-2xl ${combo.active ? 'bg-orange-500 text-white' : 'bg-gray-100 text-gray-400'}`}
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
      <h3 className="font-bold text-gray-900 text-xl mb-2 group-hover:text-orange-600 transition-colors">
        {combo.name}
      </h3>
      {combo.description && (
        <p className="mb-3 line-clamp-2 text-sm font-medium leading-snug text-gray-400">
          {combo.description}
        </p>
      )}
      <div className="text font-black text-orange-600 mb-4 tracking-tight">
        {fmtVND(combo.price)}
      </div>
    </div>

    <div className="relative z-10 mt-auto">
      <button
        onClick={() => onToggle(combo.id)}
        className={`flex w-full items-center justify-center gap-2 rounded-xl border py-2.5 text-sm font-bold transition-all ${combo.active ? 'bg-red-50 text-red-600 border-red-100 hover:bg-red-600 hover:text-white' : 'bg-green-50 text-green-600 border-green-100 hover:bg-green-600 hover:text-white'}`}
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
