import React from 'react';
import { X, ImageIcon } from 'lucide-react';

const MenuItemModal = ({ isOpen, onClose, onSubmit, categories, formData, setFormData, preview, handleFileChange }) => {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm p-6 shadow-2xl animate-in zoom-in duration-200">
        <div className="flex justify-between items-center mb-5">
          <h2 className="text-lg font-bold text-gray-800">{formData.id ? 'Sửa Món Ăn' : 'Thêm Món Mới'}</h2>
          <button onClick={onClose} className="p-1.5 hover:bg-gray-100 rounded-full text-gray-400"><X size={18} /></button>
        </div>
        
        <form onSubmit={onSubmit} className="space-y-3">
          <input 
            type="text" placeholder="Tên món ăn" required
            className="w-full px-4 py-2 border rounded-lg outline-none text-sm"
            value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})}
          />
          <input 
            type="number" placeholder="Giá bán (VNĐ)" required
            className="w-full px-4 py-2 border rounded-lg outline-none text-sm font-bold text-orange-600"
            value={formData.price} onChange={e => setFormData({...formData, price: e.target.value})}
          />
          <select 
            className="w-full px-4 py-2 border rounded-lg outline-none text-sm bg-white"
            value={formData.categoryId} onChange={e => setFormData({...formData, categoryId: e.target.value})}
          >
            {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>

          <div className="border-2 border-dashed border-gray-100 rounded-lg p-3 text-center bg-gray-50/50">
            {preview ? <img src={preview} className="max-h-24 mx-auto rounded mb-2" alt="P" /> : <ImageIcon className="mx-auto text-gray-300" size={32} />}
            <input type="file" id="itemFile" className="hidden" accept="image/*" onChange={handleFileChange} />
            <label htmlFor="itemFile" className="text-xs font-bold text-orange-500 cursor-pointer">Tải ảnh lên</label>
          </div>

          <button type="submit" className="w-full bg-orange-500 text-white py-2.5 rounded-lg font-bold hover:bg-orange-600 transition-all text-sm mt-2">Lưu</button>
        </form>
      </div>
    </div>
  );
};

export default MenuItemModal;