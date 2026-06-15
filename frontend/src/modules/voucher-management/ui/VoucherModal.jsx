import React, { useState } from 'react';
import { X, Wand2, AlertCircle, Loader2 } from 'lucide-react';
import SharedModal from '@shared/ui/SharedModal.jsx';

const VoucherModal = ({ isOpen, onClose, onSubmit, initialData, isSubmitting }) => {
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

    const originalData = React.useMemo(() => {
        if (!initialData) return null;
        return {
            code: initialData.code || '',
            discountPercent: initialData.discountPercent || '',
            discountAmount: initialData.discountAmount || '',
            usageLimit: initialData.usageLimit || '',
            validFrom: initialData.validFrom ? initialData.validFrom.split('T')[0] : '',
            validTo: initialData.validTo ? initialData.validTo.split('T')[0] : '',
            active: initialData.active ?? true,
            type: initialData.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT',
        };
    }, [initialData]);

    const isChanged = React.useMemo(() => {
        if (!initialData) return true;
        const currentType = formData.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT';
        return JSON.stringify(originalData) !== JSON.stringify({
            code: formData.code,
            discountPercent: formData.discountPercent,
            discountAmount: formData.discountAmount,
            usageLimit: formData.usageLimit,
            validFrom: formData.validFrom,
            validTo: formData.validTo,
            active: formData.active,
            type: currentType,
        });
    }, [formData, originalData, initialData]);

    // Hàm tạo mã ngẫu nhiên
    const generateCode = () => {
        const randomStr = Math.random().toString(36).substring(2, 8).toUpperCase();
        setFormData(prev => ({ ...prev, code: `SALE_${randomStr}`, codeError: null }));
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

        // 2. Validate Giảm giá
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
        if (!formData.validFrom) {
            newErrors.validFrom = "Ngày bắt đầu không được để trống";
            isValid = false;
        }
        if (!formData.validTo) {
            newErrors.validTo = "Ngày kết thúc không được để trống";
            isValid = false;
        }

        if (formData.validFrom && formData.validTo) {
            const start = new Date(formData.validFrom);
            const end = new Date(formData.validTo);
            if (start > end) {
                newErrors.validTo = "Ngày kết thúc phải sau ngày bắt đầu";
                isValid = false;
            }
        }

        // 4. Validate Giới hạn (0 hoặc rỗng = không giới hạn → gửi null)
        const parsedLimit = formData.usageLimit === '' || formData.usageLimit === null
            ? null
            : parseInt(formData.usageLimit);
        if (parsedLimit !== null && parsedLimit < 0) {
            newErrors.usageLimit = "Giới hạn lượt dùng không được âm";
            isValid = false;
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
            usageLimit: (formData.usageLimit === '' || formData.usageLimit === '0' || parseInt(formData.usageLimit) === 0) ? null : parseInt(formData.usageLimit),
            // Đảm bảo gửi null nếu field rỗng
            discountPercent: formData.discountPercent ? parseFloat(formData.discountPercent) : null,
            discountAmount: formData.discountAmount ? parseFloat(formData.discountAmount) : null,
            type: formData.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT',
        };

        onSubmit(payload);
    };

    if (!isOpen) return null;

    return (
        <SharedModal isOpen={isOpen} onClose={onClose} className="max-w-lg p-6">
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
                    <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Mã Voucher <span className="text-red-500">*</span></label>
                    <div className="flex gap-2 relative">
                        <input
                            type="text"
                            placeholder="HELLO2026"
                            className={`w-full px-4 py-2 border rounded-xl outline-none uppercase font-mono font-bold text-gray-700 ${errors.code ? 'border-red-500 focus:ring-red-200' : 'focus:ring-2 focus:ring-orange-500'}`}
                            value={formData.code}
                            onChange={e => {
                                setFormData({ ...formData, code: e.target.value.toUpperCase().replace(/\s/g, '') }); // Tự động xóa khoảng trắng
                                if (errors.code) setErrors({ ...errors, code: null });
                            }}
                        />
                        <button type="button" onClick={generateCode} className="p-2 bg-gray-100 text-gray-600 rounded-xl hover:bg-gray-200" title="Tạo mã ngẫu nhiên">
                            <Wand2 size={20} />
                        </button>
                    </div>
                    {errors.code && <p className="text-red-500 text-xs mt-1 flex items-center gap-1"><AlertCircle size={12} />{errors.code}</p>}
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
                                onChange={e => setFormData({ ...formData, discountPercent: e.target.value, discountAmount: '' })}
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
                                onChange={e => setFormData({ ...formData, discountAmount: e.target.value, discountPercent: '' })}
                            />
                            {errors.discountAmount && <p className="text-red-500 text-[10px] mt-1">{errors.discountAmount}</p>}
                        </div>
                    </div>
                    {errors.discount && <p className="text-red-500 text-xs mt-2 font-medium text-center">{errors.discount}</p>}
                </div>

                {/* Giới hạn */}
                <div>
                    <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Giới hạn lượt dùng <span className="text-red-500">*</span></label>
                    <input
                        type="number" placeholder="Nhập 0 = Không giới hạn"
                        className={`w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-orange-500 ${errors.usageLimit ? 'border-red-500 focus:ring-red-200' : ''}`}
                        value={formData.usageLimit}
                        onChange={e => {
                            setFormData({ ...formData, usageLimit: e.target.value });
                            if (errors.usageLimit) setErrors({ ...errors, usageLimit: null });
                        }}
                    />
                    {errors.usageLimit && <p className="text-red-500 text-[10px] mt-1 flex items-center gap-1"><AlertCircle size={12} />{errors.usageLimit}</p>}
                </div>

                {/* Thời gian */}
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Từ ngày <span className="text-red-500">*</span></label>
                        <input
                            type="date"
                            className={`w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-orange-500 ${errors.validFrom ? 'border-red-500' : ''}`}
                            value={formData.validFrom}
                            onChange={e => {
                                setFormData({ ...formData, validFrom: e.target.value });
                                if (errors.validFrom) setErrors({ ...errors, validFrom: null });
                            }}
                        />
                        {errors.validFrom && <p className="text-red-500 text-[10px] mt-1 flex items-center gap-1"><AlertCircle size={12} />{errors.validFrom}</p>}
                    </div>
                    <div>
                        <label className="block text-xs font-bold uppercase mb-1 text-gray-500">Đến ngày <span className="text-red-500">*</span></label>
                        <input
                            type="date"
                            className={`w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-orange-500 ${errors.validTo ? 'border-red-500' : ''}`}
                            value={formData.validTo}
                            onChange={e => {
                                setFormData({ ...formData, validTo: e.target.value });
                                if (errors.validTo) setErrors({ ...errors, validTo: null });
                            }}
                        />
                        {errors.validTo && <p className="text-red-500 text-[10px] mt-1 flex items-center gap-1"><AlertCircle size={12} />{errors.validTo}</p>}
                    </div>
                </div>

                {/* Active Checkbox */}
                <label className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl cursor-pointer hover:bg-gray-100 transition-colors border border-transparent hover:border-gray-200">
                    <div className="relative flex items-center">
                        <input
                            type="checkbox"
                            className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-orange-500 checked:bg-orange-500"
                            checked={formData.active}
                            onChange={e => setFormData({ ...formData, active: e.target.checked })}
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
                <button
                    onClick={handleSubmit}
                    disabled={!isChanged || isSubmitting}
                    className={`flex-1 py-3 rounded-xl font-bold transition-all flex items-center justify-center gap-2 shadow-lg active:scale-95 ${(!isChanged || isSubmitting)
                        ? 'bg-gray-300 text-gray-500 cursor-not-allowed shadow-none'
                        : 'bg-orange-500 text-white hover:bg-orange-600 shadow-orange-100'
                        }`}
                >
                    {isSubmitting ? (
                        <>
                            <Loader2 size={16} className="animate-spin" />
                            Đang lưu...
                        </>
                    ) : (
                        initialData ? 'Cập nhật' : 'Tạo Voucher'
                    )}
                </button>
            </div>
        </SharedModal>
    );
};

export default VoucherModal;
