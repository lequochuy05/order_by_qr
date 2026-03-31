import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { fmtVND } from '../../../utils/formatters.js';

const ItemOptionsModal = ({ item, isOpen, onClose, onConfirm }) => {
  const [selectedOptions, setSelectedOptions] = useState({});
  useEffect(() => {
    if (isOpen && item && item.itemOptions) {
      const defaultSelections = {};

      item.itemOptions.forEach(opt => {
        // Tự động lấy giá trị đầu tiên của mỗi nhóm để làm mặc định
        if (opt.optionValues && opt.optionValues.length > 0) {
          defaultSelections[opt.id] = opt.optionValues[0].id;
        }
      });

      setSelectedOptions(defaultSelections);
    } else {
      setSelectedOptions({}); // Clear state khi đóng
    }
  }, [isOpen, item]);

  if (!isOpen || !item) return null;

  const handleOptionSelect = (optionId, valueId, isRequired) => {
    setSelectedOptions(prev => ({
      ...prev,
      [optionId]: valueId
    }));
  };

  const calculateTotalPrice = () => {
    let total = item.price;
    item.itemOptions?.forEach(opt => {
      const selectedValueId = selectedOptions[opt.id];
      if (selectedValueId) {
        const val = opt.optionValues.find(v => v.id === selectedValueId);
        if (val && val.extraPrice) {
          total += val.extraPrice;
        }
      }
    });
    return total;
  };

  const handleConfirm = () => {
    // Validate required options
    const requiredOptions = item.itemOptions?.filter(o => o.isRequired) || [];
    for (const opt of requiredOptions) {
      if (!selectedOptions[opt.id]) {
        alert(`Vui lòng chọn ${opt.name}!`);
        return;
      }
    }

    const selectedValueIds = Object.values(selectedOptions).filter(Boolean);
    const selectedOptionObjs = item.itemOptions?.flatMap(opt => {
      const selectedValueId = selectedOptions[opt.id];
      if (selectedValueId) {
        const val = opt.optionValues.find(v => v.id === selectedValueId);
        return val ? [{ optionName: opt.name, valueName: val.name, extraPrice: val.extraPrice }] : [];
      }
      return [];
    }) || [];

    onConfirm(item, selectedValueIds, selectedOptionObjs, calculateTotalPrice());
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/60 flex items-end z-50">
      <div className="bg-white w-full max-w-md mx-auto rounded-t-[2rem] p-6 max-h-[85vh] flex flex-col shadow-2xl animate-in slide-in-from-bottom duration-300">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-black text-gray-800 tracking-tight">{item.name}</h3>
          <button onClick={onClose} className="p-2 bg-gray-100 rounded-full hover:bg-gray-200">
            <X size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto pr-2 space-y-6">
          {item.itemOptions?.map(opt => (
            <div key={opt.id} className="space-y-3">
              <div className="flex justify-between">
                <h4 className="text-sm font-bold text-gray-800 uppercase tracking-widest">{opt.name}</h4>
                {opt.isRequired && <span className="text-[10px] font-bold text-orange-500 bg-orange-50 px-2 py-0.5 rounded-full uppercase">Bắt buộc</span>}
              </div>
              <div className="space-y-2">
                {opt.optionValues?.map(val => (
                  <label key={val.id} className="flex items-center justify-between p-3 rounded-2xl border border-gray-100 bg-gray-50/50 cursor-pointer hover:border-orange-200 transition-colors">
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        name={`opt-${opt.id}`}
                        checked={selectedOptions[opt.id] === val.id}
                        onChange={() => handleOptionSelect(opt.id, val.id, opt.isRequired)}
                        className="w-4 h-4 text-orange-500 rounded-full border-gray-300 focus:ring-orange-500"
                      />
                      <span className="text-sm font-semibold text-gray-700">{val.name}</span>
                    </div>
                    {val.extraPrice > 0 && <span className="text-xs font-black text-orange-600">+{fmtVND(val.extraPrice)}</span>}
                  </label>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 pt-4 border-t border-gray-100">
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
