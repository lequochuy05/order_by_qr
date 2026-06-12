import React, { useState } from 'react';
import { X, Search, AlertCircle, ShoppingBasket } from 'lucide-react';
import SharedModal from '@shared/ui/SharedModal.jsx';
import { fmtVND } from '@shared/lib/formatters.js';

const ComboModal = ({ isOpen, onClose, onSubmit, menuItems, initialData, errors = {}, setErrors }) => {
  const [formData, setFormData] = useState(initialData || { name: '', description: '', price: 0, active: true, available: true, displayOrder: 0, items: [] });

  const isChanged = React.useMemo(() => {
    if (!initialData) return true;

    if (formData.name !== initialData.name) return true;
    if ((formData.description || '') !== (initialData.description || '')) return true;
    if (formData.price !== initialData.price) return true;
    if (formData.active !== initialData.active) return true;
    if ((formData.available ?? true) !== (initialData.available ?? true)) return true;
    if (Number(formData.displayOrder || 0) !== Number(initialData.displayOrder || 0)) return true;

    if (formData.items.length !== initialData.items.length) return true;

    const sortedFormItems = [...formData.items].sort((a, b) => a.menuItemId - b.menuItemId);
    const sortedInitialItems = [...initialData.items].sort((a, b) => a.menuItemId - b.menuItemId);

    for (let i = 0; i < sortedFormItems.length; i++) {
      if (sortedFormItems[i].menuItemId !== sortedInitialItems[i].menuItemId) return true;
      if (sortedFormItems[i].quantity !== sortedInitialItems[i].quantity) return true;
    }

    return false;
  }, [formData, initialData]);

  const validateForm = () => {
    const newErrors = {};
    if (!formData.name?.trim()) newErrors.name = "Tên combo không được để trống";
    if (formData.price < 0) newErrors.price = "Giá không hợp lệ";
    if (formData.items.length < 2) newErrors.items = "Vui lòng chọn ít nhất 2 món";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    if (validateForm()) {
      onSubmit(formData);
    }
  };

  const calculateAutoPrice = (selectedItems) => {
    const total = selectedItems.reduce((sum, item) => {
      const menu = menuItems.find(m => m.id === item.menuItemId);
      return sum + (menu ? menu.price * item.quantity : 0);
    }, 0);
    return Math.round(total * 0.9);
  };

  const handleToggleItem = (menuId) => {
    let newItems = [...formData.items];
    const index = newItems.findIndex(i => i.menuItemId === menuId);

    if (index > -1) newItems.splice(index, 1);
    else newItems.push({ menuItemId: menuId, quantity: 1 });

    setFormData({ ...formData, items: newItems, price: calculateAutoPrice(newItems) });
    if (errors.items) setErrors({ ...errors, items: null });
  };

  const handleUpdateQty = (menuId, qty) => {
    const newItems = formData.items.map(i => i.menuItemId === menuId ? { ...i, quantity: Math.max(1, qty) } : i);
    setFormData({ ...formData, items: newItems, price: calculateAutoPrice(newItems) });
  };

  if (!isOpen) return null;

  return (
    <SharedModal isOpen={isOpen} onClose={onClose} className="max-w-xl !p-0">
        {/* Header */}
        <div className="px-8 py-6 border-b flex justify-between items-center shrink-0">
          <div>
            <h2 className="text-xl font-black text-gray-800 tracking-tight">
              {initialData?.id ? 'Chỉnh Sửa Combo' : 'Tạo Combo Mới'}
            </h2>
            <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest mt-0.5">Tiết kiệm hơn cho khách hàng</p>
          </div>
          <button onClick={onClose} className="p-2.5 hover:bg-gray-100 rounded-full text-gray-400 transition-all active:scale-90">
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <form id="comboForm" onSubmit={handleFormSubmit} className="p-8 space-y-6 overflow-y-auto custom-scrollbar">
          <div>
            <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">Tên Combo <span className="text-red-500">*</span></label>
            <input
              type="text"
              className={`w-full px-5 py-3.5 bg-gray-50 border-2 rounded-2xl outline-none text-sm transition-all font-bold ${errors.name ? 'border-red-500 ring-red-50' : 'border-transparent focus:bg-white focus:ring-4 focus:ring-orange-500/10 focus:border-orange-500'
                }`}
              placeholder='Ví dụ: Combo Gia Đình, Combo Cuối Tuần...'
              value={formData.name}
              onChange={e => {
                setFormData({ ...formData, name: e.target.value });
                if (errors.name) setErrors({ ...errors, name: null });
              }}
            />
            {errors.name && (
              <p className="text-red-500 text-[11px] mt-1.5 flex items-center gap-1.5 font-bold animate-in slide-in-from-top-1">
                <AlertCircle size={12} /> {errors.name}
              </p>
            )}
          </div>

          <div>
            <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">Mô tả</label>
            <textarea
              rows={2}
              maxLength={500}
              className="w-full px-5 py-3.5 bg-gray-50 border-2 border-transparent rounded-2xl outline-none text-sm transition-all resize-none focus:bg-white focus:ring-4 focus:ring-orange-500/10 focus:border-orange-500"
              placeholder="Mô tả ngắn gọn về combo..."
              value={formData.description || ''}
              onChange={e => setFormData({ ...formData, description: e.target.value })}
            />
            <div className="text-right mt-1">
              <span className="text-[10px] font-medium text-gray-400">{(formData.description || '').length}/500</span>
            </div>
          </div>

          <div>
            <div className="flex justify-between items-center mb-2 px-1">
              <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] block">Thành phần Combo <span className="text-red-500">*</span></label>
              <span className="text-[10px] font-bold text-orange-500 bg-orange-50 px-2 py-0.5 rounded-lg">Chọn các món lẻ</span>
            </div>

            <div className={`border-2 rounded-[1.5rem] divide-y overflow-hidden transition-all ${errors.items ? 'border-red-500 bg-red-50/10' : 'border-gray-100 bg-gray-50/30'
              }`}>
              <div className="max-h-60 overflow-y-auto custom-scrollbar">
                {menuItems.map(m => {
                  const selected = formData.items.find(i => i.menuItemId === m.id);
                  return (
                    <div key={m.id} className={`flex items-center justify-between p-3.5 transition-colors ${selected ? 'bg-orange-50/50' : 'hover:bg-white'}`}>
                      <label className="flex items-center gap-3 cursor-pointer flex-1 group">
                        <div className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all ${selected ? 'bg-orange-500 border-orange-500 shadow-sm' : 'border-gray-200 bg-white group-hover:border-orange-300'
                          }`}>
                          {selected && <div className="w-2 h-2 bg-white rounded-full animate-in zoom-in duration-200" />}
                        </div>
                        <input type="checkbox" checked={!!selected} onChange={() => handleToggleItem(m.id)} className="hidden" />
                        <div>
                          <p className={`text-sm font-bold transition-colors ${selected ? 'text-gray-800' : 'text-gray-500 group-hover:text-gray-700'}`}>{m.name}</p>
                          <p className="text-[10px] text-gray-400 font-medium">{fmtVND(m.price)}</p>
                        </div>
                      </label>
                      {selected && (
                        <div className="flex items-center gap-2 animate-in slide-in-from-right-2 duration-300">
                          <input
                            type="number"
                            className="w-14 px-2 py-2 bg-white border border-orange-200 rounded-xl text-center text-xs font-black text-orange-600 outline-none shadow-sm"
                            value={selected.quantity}
                            onChange={e => handleUpdateQty(m.id, parseInt(e.target.value))}
                          />
                          <span className="text-[10px] font-bold text-gray-400">món</span>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
            {errors.items && (
              <p className="text-red-500 text-[11px] mt-1.5 flex items-center gap-1.5 font-bold">
                <AlertCircle size={12} /> {errors.items}
              </p>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">Giá Combo (Khuyến mãi) <span className="text-red-500">*</span></label>
              <div className="relative">
                <input
                  type="number"
                  className="w-full pl-5 pr-12 py-3.5 bg-gray-50 border-2 border-transparent focus:bg-white focus:ring-4 focus:ring-orange-500/10 focus:border-orange-500 outline-none rounded-2xl text-sm font-black text-orange-600 transition-all"
                  value={formData.price}
                  onChange={e => setFormData({ ...formData, price: parseInt(e.target.value) || 0 })}
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-gray-400 uppercase">VNĐ</span>
              </div>
            </div>
            <div>
              <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">Thứ tự hiển thị</label>
              <input
                type="number"
                min="0"
                className="w-full px-5 py-3.5 bg-gray-50 border-2 border-transparent focus:bg-white focus:ring-4 focus:ring-orange-500/10 focus:border-orange-500 outline-none rounded-2xl text-sm font-bold transition-all"
                value={formData.displayOrder ?? 0}
                onChange={e => setFormData({ ...formData, displayOrder: e.target.value })}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label className="flex items-center gap-3 px-5 h-[52px] bg-gray-50 hover:bg-gray-100 rounded-2xl cursor-pointer w-full transition-all border-2 border-transparent">
              <input
                type="checkbox"
                className="w-5 h-5 rounded-md text-orange-500 focus:ring-orange-200 border-gray-200 cursor-pointer"
                checked={formData.active}
                onChange={e => setFormData({ ...formData, active: e.target.checked })}
              />
              <span className="text-xs font-black text-gray-600 uppercase tracking-tight">Đang kinh doanh</span>
            </label>
            <label className="flex items-center gap-3 px-5 h-[52px] bg-gray-50 hover:bg-gray-100 rounded-2xl cursor-pointer w-full transition-all border-2 border-transparent">
              <input
                type="checkbox"
                className="w-5 h-5 rounded-md text-emerald-500 focus:ring-emerald-200 border-gray-200 cursor-pointer"
                checked={formData.available ?? true}
                onChange={e => setFormData({ ...formData, available: e.target.checked })}
              />
              <span className="text-xs font-black text-gray-600 uppercase tracking-tight">Còn hàng</span>
            </label>
          </div>
        </form>

        {/* Footer */}
        <div className="px-8 py-6 border-t bg-gray-50/50 rounded-b-[2rem] flex gap-4 shrink-0">
          <button
            type="button" onClick={onClose}
            className="flex-1 bg-white text-gray-600 py-4 rounded-2xl font-black transition-all text-[11px] uppercase tracking-[0.1em] border border-gray-200 hover:bg-gray-100 active:scale-95 shadow-sm"
          >
            Hủy bỏ
          </button>
          <button
            form="comboForm"
            type="submit"
            disabled={!isChanged}
            className={`flex-[2] py-4 rounded-2xl font-black transition-all text-[11px] uppercase tracking-[0.2em] shadow-xl active:scale-95 ${!isChanged
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed shadow-none'
              : 'bg-orange-500 text-white hover:bg-orange-600 shadow-orange-200'
              }`}
          >
            <div className="flex items-center justify-center gap-2">
              <ShoppingBasket size={16} />
              <span>{initialData?.id ? 'Lưu thay đổi' : 'Tạo Combo'}</span>
            </div>
          </button>
        </div>
    </SharedModal>
  );
};

export default ComboModal;