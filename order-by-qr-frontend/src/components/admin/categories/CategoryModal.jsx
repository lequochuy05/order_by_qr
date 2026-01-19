import React from 'react';
import { X, Image as ImageIcon } from 'lucide-react';

const CategoryModal = ({ isOpen, onClose, onSubmit, editId, catName, setCatName, preview, handleFileChange }) => {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm p-6 shadow-2xl animate-in zoom-in duration-200">
        <div className="flex justify-between items-center mb-5">
          <h2 className="text-lg font-bold text-gray-800">{editId ? 'Sửa Danh Mục' : 'Thêm Danh Mục'}</h2>
          <button onClick={onClose} className="p-1.5 hover:bg-gray-100 rounded-full text-gray-400"><X size={18} /></button>
        </div>
        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase mb-1.5 text-gray-500">Tên danh mục</label>
            <input 
              type="text" 
              className="w-full px-4 py-2 border border-gray-200 rounded-lg outline-none text-sm"
              value={catName}
              onChange={(e) => setCatName(e.target.value)}
              placeholder="Nhập tên..."
              required
            />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1.5 text-gray-500">Hình ảnh</label>
            <div className="border-2 border-dashed border-gray-100 rounded-lg p-3 text-center bg-gray-50/50">
              {preview ? (
                <img src={preview} className="max-h-32 mx-auto rounded-lg" alt="Preview" />
              ) : (
                <ImageIcon className="mx-auto text-gray-300 mb-2" size={32} />
              )}
              <input type="file" onChange={handleFileChange} className="hidden" id="fileInput" accept="image/*" />
              <label htmlFor="fileInput" className="block mt-2 text-xs font-bold text-orange-500 cursor-pointer">
                {preview ? 'Chọn lại ảnh khác' : 'Tải ảnh lên'}
              </label>
            </div>
          </div>
          <button type="submit" className="w-full bg-orange-500 text-white py-2.5 rounded-lg font-bold hover:bg-orange-600 transition-all text-sm">
            Lưu
          </button>
        </form>
      </div>
    </div>
  );
};

export default CategoryModal;