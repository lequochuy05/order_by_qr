import React, { useState } from 'react';
import { X, QrCode } from 'lucide-react';

const TableFormModal = ({ isOpen, onClose, initialData, onSubmit, isSubmitting }) => {
    const [formData, setFormData] = useState(initialData || { id: null, tableNumber: '', capacity: 4, status: 'AVAILABLE', qrCodeUrl: '' });

    const handleSave = () => {
        if (!formData.tableNumber || formData.capacity <= 0) {
            alert("Vui lòng nhập số bàn và sức chứa hợp lệ!");
            return;
        }
        onSubmit(formData);
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
                <div className="px-6 py-4 border-b flex justify-between items-center bg-gray-50">
                    <h3 className="font-bold text-lg">{formData.id ? 'Cập nhật bàn' : 'Thêm bàn mới'}</h3>
                    <button onClick={onClose}><X size={20} className="text-gray-500 hover:text-red-500" /></button>
                </div>

                <div className="p-6 space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Số bàn</label>
                        <input type="text" className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-orange-500 outline-none"
                            value={formData.tableNumber}
                            onChange={e => setFormData({ ...formData, tableNumber: e.target.value })}
                            placeholder="VD: 101"
                            disabled={!!formData.id}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Sức chứa (người)</label>
                        <input type="number" className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-orange-500 outline-none"
                            value={formData.capacity}
                            onChange={e => setFormData({ ...formData, capacity: parseInt(e.target.value) })}
                        />
                    </div>

                    {formData.id && (
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Trạng thái</label>
                            <select className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-orange-500 outline-none"
                                value={formData.status}
                                onChange={e => setFormData({ ...formData, status: e.target.value })}
                            >
                                <option value="AVAILABLE">Trống</option>
                                <option value="OCCUPIED">Đang phục vụ</option>
                                <option value="WAITING_FOR_PAYMENT">Chờ thanh toán</option>
                            </select>
                        </div>
                    )}

                    {formData.qrCodeUrl && (
                        <div className="flex flex-col items-center p-4 bg-gray-50 rounded-lg border border-dashed">
                            <img src={formData.qrCodeUrl} alt="QR" className="w-32 h-32 object-contain" />
                            <span className="text-xs text-gray-500 mt-2 flex items-center gap-1"><QrCode size={12} /> Quét để đặt món</span>
                        </div>
                    )}
                </div>

                <div className="px-6 py-4 border-t bg-gray-50 flex justify-end gap-2">
                    <button onClick={onClose} className="px-4 py-2 text-gray-600 hover:bg-gray-200 rounded-lg">Hủy</button>
                    <button onClick={handleSave} disabled={isSubmitting} className={`px-4 py-2 text-white font-bold rounded-lg ${isSubmitting ? 'bg-orange-300 cursor-not-allowed' : 'bg-orange-500 hover:bg-orange-600'}`}>{isSubmitting ? 'Đang lưu...' : 'Lưu'}</button>
                </div>
            </div>
        </div>
    );
};

export default TableFormModal;