import React, { useState } from 'react';
import { X, QrCode, AlertCircle, Save, Loader2, RefreshCw } from 'lucide-react';
import SharedModal from '@shared/ui/SharedModal.jsx';
import { TABLE_STATUS } from '@entities/order/lib/orderStatus.js';

const TABLE_STATUS_OPTIONS = Object.entries(TABLE_STATUS).map(([value, meta]) => ({
    value,
    label: meta.label
}));

const TableFormModal = ({ isOpen, onClose, initialData, onSubmit, isSubmitting, onRegenerateQr, isRegeneratingQr }) => {
    const [formData, setFormData] = useState(initialData || { id: null, tableNumber: '', capacity: 4, status: 'AVAILABLE', qrCodeUrl: '' });
    const [errors, setErrors] = useState({});
    const [isQrPreviewOpen, setIsQrPreviewOpen] = useState(false);

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

    const handleRegenerateQr = async () => {
        if (!formData.id || !onRegenerateQr) return;
        const updatedTable = await onRegenerateQr(formData.id);
        if (updatedTable) {
            setFormData(updatedTable);
        }
    };

    if (!isOpen) return null;

    return (
        <SharedModal isOpen={isOpen} onClose={onClose} closeOnBackdrop={false} className="max-w-md !p-0">

            {/* Header */}
            <div className="px-8 py-6 border-b flex justify-between items-center shrink-0 bg-white rounded-t-[2rem]">
                <h2 className="text-xl font-black text-gray-800 tracking-tight">
                    {formData.id ? 'Cập Nhật Bàn' : 'Thêm Bàn Mới'}
                </h2>
                <button onClick={onClose} className="p-2.5 hover:bg-gray-100 rounded-full text-gray-400 transition-all active:scale-90">
                    <X size={20} />
                </button>
            </div>

            <form id="tableForm" onSubmit={handleSave} className="p-8 space-y-6">
                <div>
                    <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">Số bàn <span className="text-red-500">*</span></label>
                    <input
                        type="text"
                        className={`w-full px-5 py-3.5 bg-gray-50 border-2 rounded-2xl outline-none text-sm transition-all font-bold disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-500 disabled:border-gray-100 ${errors.tableNumber ? 'border-red-500 ring-red-50' : 'border-transparent focus:bg-white focus:ring-4 focus:ring-orange-500/10 focus:border-orange-500'
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
                                {TABLE_STATUS_OPTIONS.map(option => (
                                    <option key={option.value} value={option.value}>{option.label}</option>
                                ))}
                            </select>
                        </div>
                    )}
                </div>

                {formData.qrCodeUrl && (
                    <div className="relative group mt-2">
                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] ml-1 mb-2 block">QR Code của bàn</label>
                        <div className="bg-gray-50/50 rounded-3xl p-6 border-2 border-dashed border-gray-100 flex flex-col items-center justify-center space-y-3 transition-all group-hover:border-orange-200 group-hover:bg-orange-50/20">
                            <button
                                type="button"
                                onClick={() => setIsQrPreviewOpen(true)}
                                className="bg-white p-3 rounded-2xl shadow-sm border border-gray-100 transition-transform duration-300 ease-out hover:scale-125 hover:shadow-xl hover:z-10 cursor-zoom-in"
                            >
                                <img src={formData.qrCodeUrl} alt="QR" className="w-32 h-32 object-contain" />
                            </button>
                            <div className="flex items-center gap-2 text-gray-400 text-[10px] font-black uppercase tracking-tighter bg-white px-4 py-1.5 rounded-full shadow-sm">
                                <QrCode size={14} className="text-orange-500" />
                                <span>Quét để đặt món</span>
                            </div>
                            {formData.id && (
                                <button
                                    type="button"
                                    onClick={handleRegenerateQr}
                                    disabled={isRegeneratingQr}
                                    className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-orange-500 text-white text-[11px] font-black uppercase tracking-[0.12em] shadow-lg shadow-orange-100 hover:bg-orange-600 disabled:bg-gray-300 disabled:text-gray-500 disabled:shadow-none transition-all active:scale-95"
                                >
                                    {isRegeneratingQr ? (
                                        <Loader2 size={14} className="animate-spin" />
                                    ) : (
                                        <RefreshCw size={14} />
                                    )}
                                    <span>{isRegeneratingQr ? 'Đang tạo...' : 'Tạo lại QR'}</span>
                                </button>
                            )}
                        </div>
                    </div>
                )}
            </form>

            {/* Footer */}
            <div className="px-8 py-6 border-t bg-gray-50/50 flex gap-4 shrink-0 rounded-b-[2rem]">
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
                            <span>{formData.id ? 'Lưu thay đổi' : 'Thêm bàn'}</span>
                        </div>
                    )}
                </button>
            </div>
            {formData.qrCodeUrl && (
                <SharedModal
                    isOpen={isQrPreviewOpen}
                    onClose={() => setIsQrPreviewOpen(false)}
                    closeOnBackdrop={true}
                    className="max-w-lg !p-0 !bg-transparent !shadow-none"
                    backdropClassName="bg-black/75 backdrop-blur-sm"
                >
                    <img
                        src={formData.qrCodeUrl}
                        alt="QR"
                        className="w-full max-h-[82vh] object-contain rounded-2xl bg-white p-5 shadow-2xl"
                    />
                </SharedModal>
            )}
        </SharedModal>
    );
};

export default TableFormModal;
