import React, { useState } from 'react';
import { X } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';
import SharedModal from './SharedModal.jsx';

const ItemOptionsModal = ({
  item,
  isOpen,
  onClose,
  onConfirm,
  onError,
  onFly,
  labels = {
    required: 'Bắt buộc',
    basePrice: 'Giá gốc',
    subtotal: 'Tạm tính',
    addToCart: 'Thêm vào giỏ',
    maxSelectionTitle: 'Vượt quá số lựa chọn',
    maxSelection: (max) => `Bạn chỉ được chọn tối đa ${max} mục cho phần này.`,
    requiredTitle: 'Thiếu lựa chọn bắt buộc',
    requiredMessage: (name) => `Vui lòng chọn ${name} trước khi thêm vào giỏ.`,
  },
}) => {
  const [selectedOptions, setSelectedOptions] = useState(() => {
    const defaultSelections = {};
    if (item?.itemOptions) {
      item.itemOptions.forEach((opt) => {
        // Chỉ tự động chọn nếu là Bắt buộc và chỉ cho phép chọn 1 (Radio)
        if (opt.required && opt.maxSelection === 1 && opt.optionValues?.length > 0) {
          defaultSelections[opt.id] = opt.optionValues[0].id;
        } else {
          // Nếu chọn nhiều (Checkbox) hoặc không bắt buộc, để trống/mảng rỗng
          defaultSelections[opt.id] = opt.maxSelection > 1 ? [] : null;
        }
      });
    }
    return defaultSelections;
  });
  const [submitted, setSubmitted] = useState(false);

  if (!isOpen || !item) return null;

  const isOptionSelected = (optionId, valueId, maxSelection) => {
    const selection = selectedOptions[optionId];
    return maxSelection > 1 ? (selection || []).includes(valueId) : selection === valueId;
  };

  const hasRequiredSelection = (opt) => {
    const selection = selectedOptions[opt.id];
    return Array.isArray(selection) ? selection.length > 0 : !!selection;
  };

  const handleOptionSelect = (optionId, valueId, maxSelection) => {
    setSelectedOptions((prev) => {
      if (maxSelection > 1) {
        // Logic Checkbox (Chọn nhiều)
        const currentSelected = Array.isArray(prev[optionId]) ? prev[optionId] : [];
        if (currentSelected.includes(valueId)) {
          return { ...prev, [optionId]: currentSelected.filter((id) => id !== valueId) };
        } else {
          if (currentSelected.length >= maxSelection) {
            onError?.(labels.maxSelection(maxSelection), labels.maxSelectionTitle);
            return prev;
          }
          return { ...prev, [optionId]: [...currentSelected, valueId] };
        }
      } else {
        // Logic Radio (Chọn 1)
        // Nếu click lại cái đang chọn thì bỏ chọn (nếu không bắt buộc)
        const required = item.itemOptions?.find((o) => o.id === optionId)?.required;
        if (prev[optionId] === valueId && !required) {
          return { ...prev, [optionId]: null };
        }
        return { ...prev, [optionId]: valueId };
      }
    });
  };

  const calculateTotalPrice = () => {
    let total = item.price;
    item.itemOptions?.forEach((opt) => {
      const selection = selectedOptions[opt.id];
      if (Array.isArray(selection)) {
        selection.forEach((valId) => {
          const val = opt.optionValues.find((v) => v.id === valId);
          if (val?.extraPrice) total += val.extraPrice;
        });
      } else if (selection) {
        const val = opt.optionValues.find((v) => v.id === selection);
        if (val?.extraPrice) total += val.extraPrice;
      }
    });
    return total;
  };

  const handleConfirm = () => {
    setSubmitted(true);
    const requiredOptions = item.itemOptions?.filter((o) => o.required) || [];
    for (const opt of requiredOptions) {
      if (!hasRequiredSelection(opt)) {
        onError?.(labels.requiredMessage(opt.name), labels.requiredTitle);
        return;
      }
    }

    // Flatten all selected IDs
    const selectedValueIds = [];
    Object.values(selectedOptions).forEach((val) => {
      if (Array.isArray(val)) selectedValueIds.push(...val);
      else if (val) selectedValueIds.push(val);
    });

    const selectedOptionObjs =
      item.itemOptions?.flatMap((opt) => {
        const selection = selectedOptions[opt.id];
        if (Array.isArray(selection)) {
          return selection.map((valId) => {
            const val = opt.optionValues.find((v) => v.id === valId);
            return { optionName: opt.name, valueName: val.name, extraPrice: val.extraPrice };
          });
        } else if (selection) {
          const val = opt.optionValues.find((v) => v.id === selection);
          return val
            ? [{ optionName: opt.name, valueName: val.name, extraPrice: val.extraPrice }]
            : [];
        }
        return [];
      }) || [];

    onConfirm(item, selectedValueIds, selectedOptionObjs, calculateTotalPrice());
    if (onFly && item?.img) {
      const el = document.querySelector(`[data-modal-img="${item.id}"]`);
      if (el) {
        const rect = el.getBoundingClientRect();
        onFly(rect, item.img);
      }
    }
    onClose();
  };

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      placement="bottom"
      closeOnBackdrop
      ariaLabel={`Tùy chọn món ${item.name}`}
      className="!max-h-[88vh] !max-w-md !overflow-hidden !p-0"
    >
        <div className="sticky top-0 z-10 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md px-5 pt-5 pb-4 border-b border-gray-100 dark:border-slate-800 transition-colors">
          <div className="flex items-start gap-3">
            {item.img && (
              <img
                src={item.img}
                alt={item.name}
                data-modal-img={item.id}
                className="h-14 w-14 shrink-0 rounded-2xl object-cover bg-gray-100 dark:bg-slate-800"
              />
            )}
            <div className="min-w-0 flex-1">
              <h3 className="text-base font-black text-gray-900 dark:text-white leading-tight line-clamp-2 transition-colors">
                {item.name}
              </h3>
              <p className="mt-1 text-[11px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 transition-colors">
                {labels.basePrice}
              </p>
              <p className="text-sm font-black font-mono text-indigo-600 dark:text-indigo-400 transition-colors">
                {fmtVND(item.price)}
              </p>
            </div>
            <button
              onClick={onClose}
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 transition-colors"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5">
          {item.itemOptions?.map((opt) => {
            const invalid = submitted && opt.required && !hasRequiredSelection(opt);
            return (
              <div
                key={opt.id}
                className={`space-y-3 rounded-2xl border p-3.5 transition-colors ${
                  invalid
                    ? 'border-red-200 bg-red-50/70 dark:border-red-500/30 dark:bg-red-500/10'
                    : 'border-slate-100 bg-slate-50/70 dark:border-slate-800 dark:bg-slate-950/40'
                }`}
              >
                <div className="flex items-center justify-between gap-3">
                  <h4 className="text-xs font-black text-slate-800 dark:text-white uppercase tracking-widest transition-colors">
                    {opt.name}
                  </h4>
                  {opt.required && (
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider transition-colors ${
                        invalid
                          ? 'bg-rose-100 text-rose-600 dark:bg-rose-500/15 dark:text-rose-300'
                          : 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300'
                      }`}
                    >
                      {labels.required}
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-2">
                  {opt.optionValues?.map((val) => (
                    <button
                      key={val.id}
                      type="button"
                      onClick={() => handleOptionSelect(opt.id, val.id, opt.maxSelection)}
                      className={`min-h-14 rounded-2xl border px-3 py-2 text-left transition-all active:scale-[0.98] ${
                        isOptionSelected(opt.id, val.id, opt.maxSelection)
                          ? 'border-indigo-500 bg-indigo-50/80 text-indigo-700 shadow-sm dark:border-indigo-400 dark:bg-indigo-950/40 dark:text-indigo-300'
                          : 'border-slate-200/80 bg-white text-slate-700 hover:border-indigo-200 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-indigo-500/50'
                      }`}
                    >
                      <span className="block text-xs font-black leading-tight line-clamp-2">
                        {val.name}
                      </span>
                      {val.extraPrice > 0 && (
                        <span className="mt-1 block text-[11px] font-bold font-mono text-indigo-600 dark:text-indigo-400">
                          +{fmtVND(val.extraPrice)}
                        </span>
                      )}
                    </button>
                  ))}
                </div>
              </div>
            );
          })}
        </div>

        <div
          className="sticky bottom-0 z-10 border-t border-slate-100 bg-white/95 px-5 py-4 backdrop-blur-md transition-colors dark:border-slate-800 dark:bg-slate-900/95"
          style={{ paddingBottom: 'calc(1rem + var(--safe-area-inset-bottom))' }}
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {labels.subtotal}
            </span>
            <span className="text-xl font-black font-mono text-indigo-600 dark:text-indigo-400">
              {fmtVND(calculateTotalPrice())}
            </span>
          </div>
          <button
            onClick={handleConfirm}
            className="w-full py-3.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-lg shadow-indigo-600/25 active:scale-95 transition-all flex items-center justify-center"
          >
            <span>{labels.addToCart}</span>
          </button>
        </div>
    </SharedModal>
  );
};

export default ItemOptionsModal;
