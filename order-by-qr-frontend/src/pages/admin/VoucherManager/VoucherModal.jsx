import React, { useState } from 'react';
import { X, Wand2, AlertCircle } from 'lucide-react';

const VoucherModal = ({ isOpen, onClose, onSubmit, initialData }) => {
    // State dữ liệu form
    const [formData, setFormData] = useState(() => {
        if (initialData) {
            return {
                ...initialData,
                validFrom: initialData.validFrom ? initialData.validFrom.split('T')[0] : '',
                validTo: initialData.validTo ? initialData.validTo.split('T')[0] : '',
                discountPercent: initialData.discountPercent || '',
                discountAmount: initialData.discountAmount || '',
                usageLimit: initialData.usageLimit || ''
            };
        }
        return {
            code: '', discountPercent: '', discountAmount: '', usageLimit: '',
            validFrom: '', validTo: '', active: true,
        };
    });

    // State lưu lỗi validation
    const [errors, setErrors] = useState({});

    // Hàm tạo mã ngẫu nhiên
    const generateCode = () => {
        const randomStr = Math.random().toString(36).substring(2, 8).toUpperCase();
        setFormData(prev => ({ ...prev, code: `SALE_${randomStr}`, codeError: null }));
        // Xóa lỗi liên quan đến code nếu có
        setErrors(prev => ({ ...prev, code: null }));
    };

    // Hàm Validate chi tiết
    const validateForm = () => {
        const newErrors = {};
        let isValid = true;

        // 1. Validate Mã
        if (!formData.code || !formData.code.trim()) {
            newErrors.code = "Mã voucher không được để trống";
            isValid = false;
        } else if (/\s/.test(formData.code)) {
            newErrors.code = "Mã voucher không được chứa khoảng trắng";
            isValid = false;
        }

        // 2. Validate Giảm giá (Chỉ được chọn 1 và phải hợp lệ)
        const hasPercent = formData.discountPercent && parseFloat(formData.discountPercent) > 0;
        const hasAmount = formData.discountAmount && parseFloat(formData.discountAmount) > 0;

        if (!hasPercent && !hasAmount) {
            newErrors.discount = "Phải nhập ít nhất một loại giảm giá";
            isValid = false;
        } else if (hasPercent && hasAmount) {
            newErrors.discount = "Chỉ được chọn 1 loại giảm giá";
            isValid = false;
        }

        if (hasPercent) {
            const p = parseFloat(formData.discountPercent);
            if (p <= 0 || p > 100) {
                newErrors.discountPercent = "% giảm phải từ 1 đến 100";
                isValid = false;
            }
        }

        if (hasAmount) {
            const a = parseFloat(formData.discountAmount);
            if (a < 1000) {
                newErrors.discountAmount = "Số tiền giảm tối thiểu là 1.000đ";
                isValid = false;
            }
        }

        // 3. Validate Ngày tháng
        if (formData.validFrom && formData.validTo) {
            const start = new Date(formData.validFrom);
            const end = new Date(formData.validTo);
            if (start > end) {
                newErrors.validTo = "Ngày kết thúc phải sau ngày bắt đầu";
                isValid = false;
            }
        }

        setErrors(newErrors);
        return isValid;
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        
        // Chạy validate trước khi submit
        if (!validateForm()) return;

        // Chuẩn bị payload sạch
        const payload = {
            ...formData,
            code: formData.code.trim().toUpperCase(),
            validFrom: formData.validFrom ? `${formData.validFrom}T00:00:00` : null,
            validTo: formData.validTo ? `${formData.validTo}T23:59:59` : null,
            usageLimit: formData.usageLimit === '' ? 0 : parseInt(formData.usageLimit),
            // Đảm bảo gửi null nếu field rỗng
            discountPercent: formData.discountPercent ? parseFloat(formData.discountPercent) : null,
            discountAmount: formData.discountAmount ? parseFloat(formData.discountAmount) : null,
        };

        onSubmit(payload);
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/60 z-[60] flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white rounded-3xl w-full max-w-lg p-6 shadow-2xl animate-in zoom-in duration-200 flex flex-col max-h-[90vh]">
                
                {/* Header */}
                <div className="flex justify-between items-center mb-5 shrink-0">
                    <h2 className="text-xl font-bold text-gray-800">
                        {initialData ? 'Sửa Voucher' : 'Thêm Voucher Mới'}
                    </h2>
                    <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-full text-gray-400 transition-colors">
                        <X size={20} />
                    </button>
                </div>

                {/* Form Body - Có scroll nếu dài */}
                <form onSubmit={handleSubmit} className="space-y-4 overflow-y-auto pr-2 custom-scrollbar">
                    
                    {/* Mã Voucher */}
                    <div>
                        <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Mã Voucher <span class="text-red-500">*</span></label>
                        <div className="flex gap-2 relative">
                            <input 
                                type="text" 
                                placeholder="HELLO2026" 
                                className={`w-full px-4 py-2 border rounded-xl outline-none uppercase font-mono font-bold text-gray-700 ${errors.code ? 'border-red-500 focus:ring-red-200' : 'focus:ring-2 focus:ring-orange-500'}`}
                                value={formData.code} 
                                onChange={e => {
                                    setFormData({...formData, code: e.target.value.toUpperCase().replace(/\s/g, '')}); // Tự động xóa khoảng trắng
                                    if(errors.code) setErrors({...errors, code: null});
                                }}
                            />
                            <button type="button" onClick={generateCode} className="p-2 bg-gray-100 text-gray-600 rounded-xl hover:bg-gray-200" title="Tạo mã ngẫu nhiên">
                                <Wand2 size={20} />
                            </button>
                        </div>
                        {errors.code && <p className="text-red-500 text-xs mt-1 flex items-center gap-1"><AlertCircle size={10}/> {errors.code}</p>}
                    </div>

                    {/* Giảm giá (Logic Disable ô còn lại) */}
                    <div className="p-4 bg-orange-50 rounded-2xl border border-orange-100">
                        <p className="text-xs font-bold uppercase text-orange-600 mb-3">Hình thức giảm giá</p>
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-medium text-gray-500 mb-1">Giảm theo %</label>
                                <input 
                                    type="number" placeholder="0-100" 
                                    className={`w-full px-3 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-orange-500 ${formData.discountAmount ? 'bg-gray-100 cursor-not-allowed text-gray-400' : 'bg-white'}`}
                                    value={formData.discountPercent || ''}
                                    disabled={!!formData.discountAmount} // Disable nếu đã nhập tiền
                                    onChange={e => setFormData({...formData, discountPercent: e.target.value, discountAmount: ''})}
                                />
                                {errors.discountPercent && <p className="text-red-500 text-[10px] mt-1">{errors.discountPercent}</p>}
                            </div>
                            
                            <div className="relative">
                                {/* Vạch ngăn cách OR */}
                                <div className="absolute top-1/2 -left-2 -translate-x-1/2 -translate-y-1/2 bg-white px-1 text-[10px] text-gray-400 font-bold z-10">OR</div>
                                
                                <label className="block text-xs font-medium text-gray-500 mb-1">Giảm số tiền</label>
                                <input 
                                    type="number" placeholder="VNĐ" 
                                    className={`w-full px-3 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-orange-500 ${formData.discountPercent ? 'bg-gray-100 cursor-not-allowed text-gray-400' : 'bg-white'}`}
                                    value={formData.discountAmount || ''} 
                                    disabled={!!formData.discountPercent} // Disable nếu đã nhập %
                                    onChange={e => setFormData({...formData, discountAmount: e.target.value, discountPercent: ''})}
                                />
                                {errors.discountAmount && <p className="text-red-500 text-[10px] mt-1">{errors.discountAmount}</p>}
                            </div>
                        </div>
                        {errors.discount && <p className="text-red-500 text-xs mt-2 font-medium text-center">{errors.discount}</p>}
                    </div>

                    {/* Giới hạn */}
                    <div>
                        <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Giới hạn lượt dùng</label>
                        <input 
                            type="number" placeholder="Nhập 0 = Không giới hạn"
                            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-orange-500"
                            value={formData.usageLimit} 
                            onChange={e => setFormData({...formData, usageLimit: e.target.value})}
                        />
                    </div>

                    {/* Thời gian */}
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Từ ngày</label>
                            <input 
                                type="date" 
                                className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-orange-500"
                                value={formData.validFrom} 
                                onChange={e => setFormData({...formData, validFrom: e.target.value})}
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Đến ngày</label>
                            <input 
                                type="date" 
                                className={`w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-orange-500 ${errors.validTo ? 'border-red-500' : ''}`}
                                value={formData.validTo} 
                                onChange={e => {
                                    setFormData({...formData, validTo: e.target.value});
                                    if(errors.validTo) setErrors({...errors, validTo: null});
                                }}
                            />
                            {errors.validTo && <p className="text-red-500 text-[10px] mt-1">{errors.validTo}</p>}
                        </div>
                    </div>

                    {/* Active Checkbox */}
                    <label className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl cursor-pointer hover:bg-gray-100 transition-colors border border-transparent hover:border-gray-200">
                        <div className="relative flex items-center">
                            <input 
                                type="checkbox" 
                                className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-orange-500 checked:bg-orange-500"
                                checked={formData.active} 
                                onChange={e => setFormData({...formData, active: e.target.checked})} 
                            />
                            <div className="pointer-events-none absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-white opacity-0 peer-checked:opacity-100">
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" /></svg>
                            </div>
                        </div>
                        <span className="text-sm font-bold text-gray-700 select-none">Kích hoạt Voucher ngay</span>
                    </label>

                </form>

                {/* Footer Buttons */}
                <div className="flex gap-3 pt-4 border-t mt-2 shrink-0">
                    <button type="button" onClick={onClose} className="flex-1 py-3 bg-gray-100 text-gray-600 rounded-xl font-bold hover:bg-gray-200 transition-all">
                        Hủy bỏ
                    </button>
                    <button onClick={handleSubmit} className="flex-1 py-3 bg-orange-500 text-white rounded-xl font-bold hover:bg-orange-600 shadow-lg shadow-orange-100 transition-all active:scale-95">
                        {initialData ? 'Cập nhật' : 'Tạo Voucher'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default VoucherModal;