import React, { useState } from 'react';
import { X, QrCode, AlertCircle, Save, Loader2 } from 'lucide-react';

const TableFormModal = ({ isOpen, onClose, initialData, onSubmit, isSubmitting }) => {
    const [formData, setFormData] = useState(initialData || { id: null, tableNumber: '', capacity: 4, status: 'AVAILABLE', qrCodeUrl: '' });
    const [errors, setErrors] = useState({});

    const isChanged = React.useMemo(() => {
        if (!initialData) return true;
        if (formData.tableNumber !== initialData.tableNumber) return true;
        if (formData.capacity !== initialData.capacity) return true;
        if (formData.status !== initialData.status) return true;
        return false;
    }, [formData, initialData]);

    const validateForm = () => {
        const newErrors = {};
        if (!formData.tableNumber || !formData.tableNumber.trim()) {
            newErrors.tableNumber = "Số bàn không được để trống";
        }
        if (formData.capacity <= 0) {
            newErrors.capacity = "Sức chứa phải lớn hơn 0";
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSave = (e) => {
        e.preventDefault();
        if (validateForm()) {
            onSubmit(formData);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white rounded-[2rem] w-full max-w-md shadow-2xl animate-in zoom-in duration-300 flex flex-col overflow-hidden">

                {/* Header */}
                <div className="px-8 py-6 border-b flex justify-between items-center shrink-0 bg-white">
                    <div>
                        <h2 className="text-xl font-black text-gray-800 tracking-tight">
                            {formData.id ? 'Cập Nhật Bàn' : 'Thêm Bàn Mới'}
                        </h2>
                        <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest mt-0.5">Sơ đồ nhà hàng</p>
                    </div>
                    <button onClick={onClose} className="p-2.5 hover:bg-gray-100 rounded-full text-gray-400 transition-all active:scale-90">
                        <X size={20} />
                    </button>
                </div>

                <form id="tableForm" onSubmit={handleSave} className="p-8 space-y-6">
                    <div>
                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">Số bàn <span className="text-red-500">*</span></label>
                        <input
                            type="text"
                            className={`w-full px-5 py-3.5 bg-gray-50 border-2 rounded-2xl outline-none text-sm transition-all font-bold ${errors.tableNumber ? 'border-red-500 ring-red-50' : 'border-transparent focus:bg-white focus:ring-4 focus:ring-orange-500/10 focus:border-orange-500'
                                }`}
                            value={formData.tableNumber}
                            onChange={e => {
                                setFormData({ ...formData, tableNumber: e.target.value });
                                if (errors.tableNumber) setErrors({ ...errors, tableNumber: null });
                            }}
                            placeholder="Ví dụ: 101, Bàn 01..."
                            disabled={!!formData.id}
                        />
                        {errors.tableNumber && (
                            <p className="text-red-500 text-[11px] mt-1.5 flex items-center gap-1.5 font-bold animate-in slide-in-from-top-1">
                                <AlertCircle size={12} /> {errors.tableNumber}
                            </p>
                        )}
                    </div>

                    <div className="grid grid-cols-2 gap-6">
                        <div>
                            <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">Sức chứa <span className="text-red-500">*</span></label>
                            <input
                                type="number"
                                className={`w-full px-5 py-3.5 bg-gray-50 border-2 rounded-2xl outline-none text-sm font-black transition-all ${errors.capacity ? 'border-red-500' : 'border-transparent focus:bg-white focus:ring-4 focus:ring-orange-500/10 focus:border-orange-500 text-orange-600'
                                    }`}
                                value={formData.capacity}
                                onChange={e => {
                                    setFormData({ ...formData, capacity: parseInt(e.target.value) || 0 });
                                    if (errors.capacity) setErrors({ ...errors, capacity: null });
                                }}
                            />
                            {errors.capacity && (
                                <p className="text-red-500 text-[10px] mt-1.5 font-bold">{errors.capacity}</p>
                            )}
                        </div>

                        {formData.id && (
                            <div>
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">Trạng thái</label>
                                <select
                                    className="w-full px-4 py-3.5 bg-gray-50 border-2 border-transparent focus:bg-white focus:ring-4 focus:ring-orange-500/10 focus:border-orange-500 outline-none rounded-2xl text-sm font-bold appearance-none cursor-pointer"
                                    value={formData.status}
                                    onChange={e => setFormData({ ...formData, status: e.target.value })}
                                >
                                    <option value="AVAILABLE">Sẵn sàng</option>
                                    <option value="OCCUPIED">Đang ngồi</option>
                                    <option value="WAITING_FOR_PAYMENT">Chờ trả tiền</option>
                                </select>
                            </div>
                        )}
                    </div>

                    {formData.qrCodeUrl && (
                        <div className="relative group mt-2">
                            <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">QR Code của bàn</label>
                            <div className="bg-gray-50/50 rounded-3xl p-6 border-2 border-dashed border-gray-100 flex flex-col items-center justify-center space-y-3 transition-all group-hover:border-orange-200 group-hover:bg-orange-50/20">
                                <div className="bg-white p-3 rounded-2xl shadow-sm border border-gray-100">
                                    <img src={formData.qrCodeUrl} alt="QR" className="w-32 h-32 object-contain" />
                                </div>
                                <div className="flex items-center gap-2 text-gray-400 text-[10px] font-black uppercase tracking-tighter bg-white px-4 py-1.5 rounded-full shadow-sm">
                                    <QrCode size={14} className="text-orange-500" />
                                    <span>Quét để đặt món</span>
                                </div>
                            </div>
                        </div>
                    )}
                </form>

                {/* Footer */}
                <div className="px-8 py-6 border-t bg-gray-50/50 flex gap-4 shrink-0">
                    <button
                        type="button"
                        onClick={onClose}
                        className="flex-1 bg-white text-gray-600 py-4 rounded-2xl font-black transition-all text-[11px] uppercase tracking-[0.1em] border border-gray-200 hover:bg-gray-100 active:scale-95"
                    >
                        Hủy bỏ
                    </button>
                    <button
                        form="tableForm"
                        type="submit"
                        disabled={isSubmitting || !isChanged}
                        className={`flex-[2] text-white py-4 rounded-2xl font-black transition-all text-[11px] uppercase tracking-[0.2em] shadow-xl active:scale-95 ${(isSubmitting || !isChanged)
                                ? 'bg-gray-300 text-gray-500 cursor-not-allowed shadow-none'
                                : 'bg-orange-500 hover:bg-orange-600 shadow-orange-200'
                            }`}
                    >
                        {isSubmitting ? (
                            <div className="flex items-center justify-center gap-2">
                                <Loader2 size={16} className="animate-spin" />
                                <span>Đang lưu...</span>
                            </div>
                        ) : (
                            <div className="flex items-center justify-center gap-2">
                                <Save size={16} />
                                <span>{formData.id ? 'Cập nhật' : 'Thêm bàn'}</span>
                            </div>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default TableFormModal;