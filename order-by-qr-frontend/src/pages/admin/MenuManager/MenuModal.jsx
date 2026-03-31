import { X, ImageIcon, Plus, Trash2, ChevronDown, ChevronUp } from 'lucide-react';

const MenuItemModal = ({ isOpen, onClose, onSubmit, categories, formData, setFormData, preview, handleFileChange, isSubmitting }) => {
  if (!isOpen) return null;

  const addOption = () => {
    const newOption = { name: '', isRequired: false, maxSelection: 1, optionValues: [] };
    setFormData({ ...formData, itemOptions: [...formData.itemOptions, newOption] });
  };

  const removeOption = (index) => {
    const newOptions = formData.itemOptions.filter((_, i) => i !== index);
    setFormData({ ...formData, itemOptions: newOptions });
  };

  const updateOption = (index, field, value) => {
    const newOptions = [...formData.itemOptions];
    newOptions[index] = { ...newOptions[index], [field]: value };
    setFormData({ ...formData, itemOptions: newOptions });
  };

  const addValue = (optIndex) => {
    const newValue = { name: '', extraPrice: 0 };
    const newOptions = [...formData.itemOptions];
    newOptions[optIndex].optionValues = [...newOptions[optIndex].optionValues, newValue];
    setFormData({ ...formData, itemOptions: newOptions });
  };

  const removeValue = (optIndex, valIndex) => {
    const newOptions = [...formData.itemOptions];
    newOptions[optIndex].optionValues = newOptions[optIndex].optionValues.filter((_, i) => i !== valIndex);
    setFormData({ ...formData, itemOptions: newOptions });
  };

  const updateValue = (optIndex, valIndex, field, value) => {
    const newOptions = [...formData.itemOptions];
    newOptions[optIndex].optionValues[valIndex] = { ...newOptions[optIndex].optionValues[valIndex], [field]: value };
    setFormData({ ...formData, itemOptions: newOptions });
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm overflow-y-auto">
      <div className="bg-white rounded-3xl w-full max-w-2xl shadow-2xl animate-in zoom-in duration-200 my-8">
        <div className="sticky top-0 bg-white/80 backdrop-blur-md z-10 px-8 py-5 border-b flex justify-between items-center rounded-t-3xl">
          <h2 className="text-xl font-black text-gray-800 tracking-tight">
            {formData.id ? 'Sửa Món Ăn' : ' Thêm Món Mới'}
          </h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-full text-gray-400 transition-colors">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={onSubmit} className="p-8 space-y-8">
          {/* Thông tin cơ bản */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <label className="text-xs font-bold text-gray-400 uppercase tracking-widest ml-1 mb-2 block">Tên món ăn</label>
                <input
                  type="text" placeholder="Ví dụ: Trà sữa thái xanh" required
                  className="w-full px-4 py-3 bg-gray-50 border-none rounded-2xl outline-none text-sm focus:ring-2 ring-orange-100 transition-all font-medium"
                  value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-gray-400 uppercase tracking-widest ml-1 mb-2 block">Giá bán</label>
                  <input
                    type="number" placeholder="0" required
                    className="w-full px-4 py-3 bg-gray-50 border-none rounded-2xl outline-none text-sm font-black text-orange-600 focus:ring-2 ring-orange-100 transition-all"
                    value={formData.price} onChange={e => setFormData({ ...formData, price: e.target.value })}
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-gray-400 uppercase tracking-widest ml-1 mb-2 block">Danh mục</label>
                  <select
                    className="w-full px-4 py-3 bg-gray-50 border-none rounded-2xl outline-none text-sm font-medium appearance-none focus:ring-2 ring-orange-100 transition-all"
                    value={formData.categoryId} onChange={e => setFormData({ ...formData, categoryId: e.target.value })}
                  >
                    {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                </div>
              </div>
            </div>

            <div className="relative group">
              <label className="text-xs font-bold text-gray-400 uppercase tracking-widest ml-1 mb-2 block">Hình ảnh</label>
              <div className="border-2 border-dashed border-gray-100 rounded-3xl p-4 text-center bg-gray-50/50 h-[140px] flex flex-col items-center justify-center relative overflow-hidden transition-all group-hover:border-orange-200">
                {preview ? (
                  <img src={preview} className="absolute inset-0 w-full h-full object-cover" alt="Preview" />
                ) : (
                  <div className="space-y-2 text-gray-300">
                    <ImageIcon className="mx-auto" size={40} />
                    <p className="text-[10px] uppercase font-bold tracking-tighter">Chưa có ảnh</p>
                  </div>
                )}
                <input type="file" id="itemFile" className="hidden" accept="image/*" onChange={handleFileChange} />
                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center gap-2">
                  <label htmlFor="itemFile" className="px-4 py-2 bg-white text-orange-600 rounded-full text-xs font-black cursor-pointer shadow-lg hover:scale-105 transition-transform">
                    {preview ? 'Đổi ảnh khác' : 'Tải ảnh lên'}
                  </label>
                  {preview && !formData.id && (
                    <button
                      type="button"
                      onClick={() => window.dispatchEvent(new CustomEvent('aiScan'))}
                      className="px-4 py-2 bg-blue-500 text-white rounded-full text-xs font-black shadow-lg hover:scale-105 transition-transform flex items-center gap-1"
                    >
                      ✨ Magic Scan
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Tùy chọn (Options) */}
          <div className="space-y-4">
            <div className="flex justify-between items-center px-1">
              <h3 className="text-sm font-black text-gray-800 uppercase tracking-widest flex items-center gap-2">
                Tùy Chọn Mở Rộng
                <span className="px-2 py-0.5 bg-orange-100 text-orange-600 rounded-full text-[10px]">{formData.itemOptions.length}</span>
              </h3>
              <button
                type="button" onClick={addOption}
                className="text-[10px] font-black uppercase tracking-tighter text-white bg-gray-900 px-4 py-2 rounded-full hover:bg-black transition-all flex items-center gap-1 shadow-lg active:scale-95"
              >
                <Plus size={14} /> Thêm Nhóm
              </button>
            </div>

            <div className="space-y-4">
              {formData.itemOptions.map((opt, optIdx) => (
                <div key={optIdx} className="bg-gray-50/50 rounded-3xl p-6 border border-gray-100 space-y-6 relative group/option animate-in slide-in-from-bottom-2 duration-300">
                  <button
                    type="button" onClick={() => removeOption(optIdx)}
                    className="absolute -top-3 -right-3 p-2 bg-white text-red-500 rounded-full shadow-md border hover:bg-red-50 transition-all opacity-0 group-hover/option:opacity-100 z-10"
                  >
                    <Trash2 size={16} />
                  </button>

                  <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
                    <div className="md:col-span-6">
                      <input
                        type="text" placeholder="Tên nhóm (Size,..)" required
                        className="w-full px-4 py-3 bg-white border-none rounded-2xl outline-none text-sm font-bold shadow-sm"
                        value={opt.name} onChange={e => updateOption(optIdx, 'name', e.target.value)}
                      />
                    </div>
                    <div className="md:col-span-3 flex items-center justify-center gap-2 bg-white rounded-2xl shadow-sm px-4">
                      <input
                        type="checkbox" id={`req-${optIdx}`}
                        className="w-4 h-4 rounded text-orange-500 focus:ring-orange-200 border-gray-200 cursor-pointer"
                        checked={opt.isRequired} onChange={e => updateOption(optIdx, 'isRequired', e.target.checked)}
                      />
                      <label htmlFor={`req-${optIdx}`} className="text-xs font-bold text-gray-600 cursor-pointer select-none">Bắt buộc</label>
                    </div>
                    <div className="md:col-span-3">
                      <input
                        type="number" placeholder="Max" title="Số lượng chọn tối đa"
                        className="w-full px-4 py-3 bg-white border-none rounded-2xl outline-none text-sm font-bold shadow-sm text-center"
                        value={opt.maxSelection} onChange={e => updateOption(optIdx, 'maxSelection', parseInt(e.target.value) || 1)}
                      />
                    </div>
                  </div>

                  {/* Giá trị của tùy chọn */}
                  <div className="space-y-3">
                    <div className="flex justify-between items-center px-1">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Danh sách giá trị</p>
                      <button
                        type="button" onClick={() => addValue(optIdx)}
                        className="text-[9px] font-black text-orange-500 hover:text-orange-600 uppercase flex items-center gap-1"
                      >
                        <Plus size={12} /> Thêm giá trị
                      </button>
                    </div>

                    <div className="grid grid-cols-1 gap-2">
                      {opt.optionValues.map((val, valIdx) => (
                        <div key={valIdx} className="grid grid-cols-12 gap-2 animate-in fade-in duration-200">
                          <div className="col-span-7">
                            <input
                              type="text" placeholder="Tên (ví dụ: M, L)" required
                              className="w-full px-4 py-2.5 bg-white/50 border border-gray-100 rounded-xl outline-none text-xs font-medium"
                              value={val.name} onChange={e => updateValue(optIdx, valIdx, 'name', e.target.value)}
                            />
                          </div>
                          <div className="col-span-4">
                            <input
                              type="number" placeholder="+0đ"
                              className="w-full px-3 py-2.5 bg-white/50 border border-gray-100 rounded-xl outline-none text-xs font-bold text-orange-600"
                              value={val.extraPrice} onChange={e => updateValue(optIdx, valIdx, 'extraPrice', parseFloat(e.target.value) || 0)}
                            />
                          </div>
                          <button
                            type="button" onClick={() => removeValue(optIdx, valIdx)}
                            className="col-span-1 flex items-center justify-center text-gray-300 hover:text-red-500 transition-colors"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      ))}
                      {opt.optionValues.length === 0 && (
                        <p className="text-[10px] text-gray-300 italic text-center py-2 bg-white/30 rounded-xl border border-dashed">Chưa có giá trị nào</p>
                      )}
                    </div>
                  </div>
                </div>
              ))}
              {formData.itemOptions.length === 0 && (
                <div className="text-center py-10 bg-gray-50 rounded-3xl border border-dashed border-gray-200">
                  <p className="text-xs text-gray-400 font-medium">Món này chưa có lựa chọn đi kèm</p>
                </div>
              )}
            </div>
          </div>

          <div className="pt-4 border-t border-gray-100 flex gap-4">
            <button
              type="button" onClick={onClose}
              className="flex-1 bg-gray-100 text-gray-600 py-4 rounded-2xl font-black transition-all text-xs uppercase tracking-widest hover:bg-gray-200"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className={`flex-[2] text-white py-4 rounded-2xl font-black transition-all text-xs uppercase tracking-widest shadow-xl shadow-orange-100 active:scale-95 ${isSubmitting ? 'bg-orange-300 cursor-not-allowed' : 'bg-orange-500 hover:bg-orange-600'}`}
            >
              {isSubmitting ? 'Đang đồng bộ...' : (formData.id ? 'Lưu thay đổi' : 'Tạo món ăn')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default MenuItemModal;