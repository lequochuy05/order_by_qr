import React from 'react';
import { Pencil, Trash2, Power, PowerOff, Package, ChevronRight } from 'lucide-react';
import { fmtVND, fmtStatus } from '../../../utils/formatters';

const ComboCard = ({ combo, onEdit, onDelete, onToggle }) => (
  <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 group hover:border-orange-500 hover:shadow-xl transition-all flex flex-col h-full relative overflow-hidden">
        {/* Trang trí góc thẻ */}
        <div className="absolute -top-6 -right-6 w-20 h-20 bg-orange-50 rounded-full group-hover:bg-orange-100 transition-colors" />
            
            <div className="flex items-center justify-between mb-5 relative z-10">
                <div className={`p-3 rounded-2xl ${combo.active ? 'bg-orange-500 text-white' : 'bg-gray-100 text-gray-400'}`}>
                    <Package size={24} />
                </div>
                <span className={`text-[11px] font-black px-3 py-1 rounded-full uppercase tracking-wider ${fmtStatus('active', combo.active).color}`}>
                    {fmtStatus('active', combo.active).label}
                </span>
            </div>

            <div className="flex-grow relative z-10">
                <h3 className="font-bold text-gray-900 text-xl mb-2 group-hover:text-orange-600 transition-colors">{combo.name}</h3>
                <div className="text font-black text-orange-600 mb-4 tracking-tight">
                    {fmtVND(combo.price)}
                </div>
        </div>

        <div className="flex gap-3 mt-auto relative z-10">
            <button onClick={() => onEdit(combo.id)} className="flex-1 py-3 bg-blue-50 text-blue-600 rounded-xl hover:bg-blue-600 hover:text-white transition-all flex justify-center border border-blue-100 font-bold">
                <Pencil size={18} />
            </button>
            <button 
                onClick={() => onToggle(combo.id)} 
                className={`flex-1 py-3 rounded-xl transition-all flex justify-center border font-bold ${combo.active ? 'bg-red-50 text-red-600 border-red-100 hover:bg-red-600 hover:text-white' : 'bg-green-50 text-green-600 border-green-100 hover:bg-green-600 hover:text-white'}`}
            >
                {combo.active ? <PowerOff size={18} /> : <Power size={18} />}
            </button>
            <button onClick={() => onDelete(combo.id)} className="p-3 bg-gray-50 text-gray-400 rounded-xl hover:bg-red-50 hover:text-red-600 transition-all border border-gray-100">
                <Trash2 size={18} />
            </button>
        </div>
  </div>
);

export default ComboCard;