import React, { useState } from 'react';
import { X } from 'lucide-react';
import { fmtVND } from '../../../utils/formatters.js';

const ItemOptionsModal = ({ item, isOpen, onClose, onConfirm }) => {
  const [selectedOptions, setSelectedOptions] = useState(() => {
    const defaultSelections = {};
    if (item?.itemOptions) {
      item.itemOptions.forEach(opt => {
        // Chỉ tự động chọn nếu là Bắt buộc và chỉ cho phép chọn 1 (Radio)
        if (opt.isRequired && opt.maxSelection === 1 && opt.optionValues?.length > 0) {
          defaultSelections[opt.id] = opt.optionValues[0].id;
        } else {
          // Nếu chọn nhiều (Checkbox) hoặc không bắt buộc, để trống/mảng rỗng
          defaultSelections[opt.id] = opt.maxSelection > 1 ? [] : null;
        }
      });
    }
    return defaultSelections;
  });

  if (!isOpen || !item) return null;

  const handleOptionSelect = (optionId, valueId, maxSelection) => {
    setSelectedOptions(prev => {
      if (maxSelection > 1) {
        // Logic Checkbox (Chọn nhiều)
        const currentSelected = Array.isArray(prev[optionId]) ? prev[optionId] : [];
        if (currentSelected.includes(valueId)) {
          return { ...prev, [optionId]: currentSelected.filter(id => id !== valueId) };
        } else {
          if (currentSelected.length >= maxSelection) {
            alert(`Bạn chỉ được chọn tối đa ${maxSelection} mục cho phần này!`);
            return prev;
          }
          return { ...prev, [optionId]: [...currentSelected, valueId] };
        }
      } else {
        // Logic Radio (Chọn 1)
        // Nếu click lại cái đang chọn thì bỏ chọn (nếu không bắt buộc)
        const isRequired = item.itemOptions?.find(o => o.id === optionId)?.isRequired;
        if (prev[optionId] === valueId && !isRequired) {
          return { ...prev, [optionId]: null };
        }
        return { ...prev, [optionId]: valueId };
      }
    });
  };

  const calculateTotalPrice = () => {
    let total = item.price;
    item.itemOptions?.forEach(opt => {
      const selection = selectedOptions[opt.id];
      if (Array.isArray(selection)) {
        selection.forEach(valId => {
          const val = opt.optionValues.find(v => v.id === valId);
          if (val?.extraPrice) total += val.extraPrice;
        });
      } else if (selection) {
        const val = opt.optionValues.find(v => v.id === selection);
        if (val?.extraPrice) total += val.extraPrice;
      }
    });
    return total;
  };

  const handleConfirm = () => {
    const requiredOptions = item.itemOptions?.filter(o => o.isRequired) || [];
    for (const opt of requiredOptions) {
      const selection = selectedOptions[opt.id];
      const hasSelection = Array.isArray(selection) ? selection.length > 0 : !!selection;
      if (!hasSelection) {
        alert(`Vui lòng chọn ${opt.name}!`);
        return;
      }
    }

    // Flatten all selected IDs
    const selectedValueIds = [];
    Object.values(selectedOptions).forEach(val => {
      if (Array.isArray(val)) selectedValueIds.push(...val);
      else if (val) selectedValueIds.push(val);
    });

    const selectedOptionObjs = item.itemOptions?.flatMap(opt => {
      const selection = selectedOptions[opt.id];
      if (Array.isArray(selection)) {
        return selection.map(valId => {
          const val = opt.optionValues.find(v => v.id === valId);
          return { optionName: opt.name, valueName: val.name, extraPrice: val.extraPrice };
        });
      } else if (selection) {
        const val = opt.optionValues.find(v => v.id === selection);
        return val ? [{ optionName: opt.name, valueName: val.name, extraPrice: val.extraPrice }] : [];
      }
      return [];
    }) || [];

    onConfirm(item, selectedValueIds, selectedOptionObjs, calculateTotalPrice());
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/60 flex items-end z-50">
      <div className="bg-white dark:bg-slate-900 w-full max-w-md mx-auto rounded-t-[2rem] p-6 max-h-[85vh] flex flex-col shadow-2xl animate-in slide-in-from-bottom duration-300 transition-colors">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-black text-gray-800 dark:text-white tracking-tight transition-colors">{item.name}</h3>
          <button onClick={onClose} className="p-2 bg-gray-100 dark:bg-slate-800 rounded-full hover:bg-gray-200 dark:hover:bg-slate-700 text-gray-600 dark:text-gray-300 transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto pr-2 space-y-6">
          {item.itemOptions?.map(opt => (
            <div key={opt.id} className="space-y-3">
              <div className="flex justify-between">
                <h4 className="text-sm font-bold text-gray-800 dark:text-white uppercase tracking-widest transition-colors">{opt.name}</h4>
                {opt.isRequired && <span className="text-[10px] font-bold text-orange-500 bg-orange-50 dark:bg-orange-500/10 px-2 py-0.5 rounded-full uppercase transition-colors">Bắt buộc</span>}
              </div>
              <div className="space-y-2">
                {opt.optionValues?.map(val => (
                  <label key={val.id} className="flex items-center justify-between p-3 rounded-2xl border border-gray-100 dark:border-slate-800 bg-gray-50/50 dark:bg-slate-800/80 cursor-pointer hover:border-orange-200 dark:hover:border-orange-500 transition-colors">
                    <div className="flex items-center gap-3">
                      <input
                        type={opt.maxSelection > 1 ? "checkbox" : "radio"}
                        name={`opt-${opt.id}`}
                        checked={opt.maxSelection > 1 
                          ? (selectedOptions[opt.id] || []).includes(val.id)
                          : selectedOptions[opt.id] === val.id}
                        onChange={() => handleOptionSelect(opt.id, val.id, opt.maxSelection)}
                        className={`w-4 h-4 text-orange-500 border-gray-300 focus:ring-orange-500 ${opt.maxSelection > 1 ? 'rounded' : 'rounded-full'}`}
                      />
                      <span className="text-sm font-semibold text-gray-700 dark:text-gray-200 transition-colors">{val.name}</span>
                    </div>
                    {val.extraPrice > 0 && <span className="text-xs font-black text-orange-600 dark:text-orange-400 transition-colors">+{fmtVND(val.extraPrice)}</span>}
                  </label>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 pt-4 border-t border-gray-100 dark:border-slate-800 transition-colors">
          <button
            onClick={handleConfirm}
            className="w-full py-4 bg-orange-500 text-white rounded-2xl font-black text-sm uppercase tracking-widest shadow-lg shadow-orange-200 active:scale-95 transition-all flex justify-between px-6"
          >
            <span>Thêm vào giỏ</span>
            <span>{fmtVND(calculateTotalPrice())}</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default ItemOptionsModal;
