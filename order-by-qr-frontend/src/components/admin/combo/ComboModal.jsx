import React, { useState, useEffect } from 'react';
import { X, Search } from 'lucide-react';

const ComboModal = ({ isOpen, onClose, onSubmit, menuItems, initialData }) => {
  const [formData, setFormData] = useState({ name: '', price: 0, active: true, items: [] });

  useEffect(() => {
    if (initialData) setFormData(initialData);
    else setFormData({ name: '', price: 0, active: true, items: [] });
  }, [initialData, isOpen]);

  // Logic tự động tính giá ưu đãi
  const calculateAutoPrice = (selectedItems) => {
    const total = selectedItems.reduce((sum, item) => {
      const menu = menuItems.find(m => m.id === item.menuItemId);
      return sum + (menu ? menu.price * item.quantity : 0);
    }, 0);
    return Math.round(total * 0.9); // Giảm 10% mặc định
  };

  const handleToggleItem = (menuId) => {
    let newItems = [...formData.items];
    const index = newItems.findIndex(i => i.menuItemId === menuId);
    
    if (index > -1) newItems.splice(index, 1);
    else newItems.push({ menuItemId: menuId, quantity: 1 });

    setFormData({ ...formData, items: newItems, price: calculateAutoPrice(newItems) });
  };

  const handleUpdateQty = (menuId, qty) => {
    const newItems = formData.items.map(i => i.menuItemId === menuId ? { ...i, quantity: Math.max(1, qty) } : i);
    setFormData({ ...formData, items: newItems, price: calculateAutoPrice(newItems) });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-xl p-6 shadow-2xl animate-in zoom-in duration-200 flex flex-col max-h-[90vh]">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-bold text-gray-800">{initialData?.id ? 'Sửa Combo' : 'Tạo Combo Mới'}</h2>
          <button onClick={onClose} className="p-1.5 hover:bg-gray-100 rounded-full text-gray-400"><X size={18} /></button>
        </div>

        <form onSubmit={(e) => { e.preventDefault(); onSubmit(formData); }} className="space-y-4 overflow-y-auto pr-2">
          <div>
            <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Tên Combo</label>
            <input 
              type="text" required className="w-full px-4 py-2 border rounded-lg outline-none text-sm"
              placeholder='Nhập tên...'
              value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})}
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Chọn món trong combo</label>
            <div className="border rounded-lg divide-y max-h-48 overflow-y-auto bg-gray-50/50">
              {menuItems.map(m => {
                const selected = formData.items.find(i => i.menuItemId === m.id);
                return (
                  <div key={m.id} className="flex items-center justify-between p-2">
                    <label className="flex items-center gap-2 cursor-pointer flex-1">
                      <input type="checkbox" checked={!!selected} onChange={() => handleToggleItem(m.id)} className="w-4 h-4 rounded" />
                      <span className="text-sm">{m.name} <small className="text-gray-400">({m.price.toLocaleString()}đ)</small></span>
                    </label>
                    {selected && (
                      <input 
                        type="number" className="w-16 px-2 py-1 border rounded text-center text-sm"
                        value={selected.quantity} onChange={e => handleUpdateQty(m.id, parseInt(e.target.value))}
                      />
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Giá bán (Đã giảm 10%)</label>
              <input 
                type="number" required className="w-full px-4 py-2 border rounded-lg outline-none text-sm font-bold text-orange-600"
                value={formData.price} onChange={e => setFormData({...formData, price: parseInt(e.target.value)})}
              />
            </div>
            <div className="flex items-end">
              <label className="flex items-center gap-2 p-2 bg-gray-100 rounded-lg cursor-pointer w-full">
                <input type="checkbox" checked={formData.active} onChange={e => setFormData({...formData, active: e.target.checked})} />
                <span className="text-sm font-bold text-gray-600">Đang kinh doanh</span>
              </label>
            </div>
          </div>

          <button type="submit" className="w-full bg-orange-500 text-white py-3 rounded-xl font-bold hover:bg-orange-600 transition-all shadow-lg shadow-orange-100 mt-2">
            Lưu
          </button>
        </form>
      </div>
    </div>
  );
};

export default ComboModal;