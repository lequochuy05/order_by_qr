import React from 'react';
import { X, Image as ImageIcon, AlertCircle } from 'lucide-react';
import SharedModal from '@shared/ui/SharedModal.jsx';
import FormError from '@shared/ui/FormError.jsx';

const CategoryModal = ({
    isOpen, onClose, onSubmit, editId, formData, setFormData,
    preview, handleFileChange, isSubmitting, initialFormData,
    selectedFile, errors = {}, setErrors
}) => {

    const isChanged = React.useMemo(() => {
        if (!editId) return true;
        if (selectedFile) return true;

        return (
            formData.name !== initialFormData?.name ||
            (formData.description || '') !== (initialFormData?.description || '') ||
            (formData.active ?? true) !== (initialFormData?.active ?? true) ||
            Number(formData.displayOrder || 0) !== Number(initialFormData?.displayOrder || 0)
        );
    }, [editId, formData, initialFormData, selectedFile]);

    const updateField = (field, value) => {
        setFormData({ ...formData, [field]: value });
        if (errors[field]) {
            const newErrors = { ...errors };
            delete newErrors[field];
            setErrors(newErrors);
        }
    };

    if (!isOpen) return null;

    return (
        <SharedModal isOpen={isOpen} onClose={onClose} className="max-w-lg !p-0">

            {/* Header */}
            <div className="flex justify-between items-center px-6 py-5 border-b shrink-0 bg-gray-50/50">
                <h2 className="text-xl font-bold text-gray-800">
                    {editId ? 'Sửa Danh Mục' : 'Thêm Danh Mục'}
                </h2>
                <button
                    onClick={onClose}
                    className="p-2 hover:bg-gray-200 rounded-full text-gray-500 transition-colors"
                >
                    <X size={20} />
                </button>
            </div>

            {/* Body - Scrollable */}
            <form id="categoryForm" onSubmit={onSubmit} className="p-6 space-y-5 overflow-y-auto custom-scrollbar">
                {/* Tên danh mục */}
                <div>
                    <label className="block text-xs font-bold uppercase mb-1.5 text-gray-500 tracking-wider">
                        Tên danh mục <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        className={`w-full px-4 py-3 border rounded-xl outline-none text-sm transition-all ${errors.name
                            ? 'border-red-500 focus:ring-red-100'
                            : 'border-gray-200 focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500'
                            }`}
                        value={formData.name}
                        onChange={(e) => updateField('name', e.target.value)}
                        placeholder="Ví dụ: Đồ uống, Món khai vị..."
                    />
                    <FormError message={errors.name} />
                </div>

                <div>
                    <label className="block text-xs font-bold uppercase mb-1.5 text-gray-500 tracking-wider">
                        Mô tả
                    </label>
                    <textarea
                        rows={3}
                        maxLength={255}
                        className={`w-full px-4 py-3 border rounded-xl outline-none text-sm transition-all resize-none ${errors.description
                            ? 'border-red-500 focus:ring-red-100'
                            : 'border-gray-200 focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500'
                            }`}
                        value={formData.description || ''}
                        onChange={(e) => updateField('description', e.target.value)}
                        placeholder="Mô tả ngắn về nhóm món trong danh mục..."
                    />
                    <div className="flex justify-between items-start gap-3 mt-1">
                        <FormError message={errors.description} />
                        <span className="ml-auto text-[10px] font-medium text-gray-400">
                            {(formData.description || '').length}/255
                        </span>
                    </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <label className="block text-xs font-bold uppercase mb-1.5 text-gray-500 tracking-wider">
                            Thứ tự hiển thị
                        </label>
                        <input
                            type="number"
                            min="0"
                            className={`w-full px-4 py-3 border rounded-xl outline-none text-sm transition-all ${errors.displayOrder
                                ? 'border-red-500 focus:ring-red-100'
                                : 'border-gray-200 focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500'
                                }`}
                            value={formData.displayOrder ?? 0}
                            onChange={(e) => updateField('displayOrder', e.target.value)}
                        />
                        <FormError message={errors.displayOrder} />
                    </div>

                    <div>
                        <label className="block text-xs font-bold uppercase mb-1.5 text-gray-500 tracking-wider">
                            Trạng thái
                        </label>
                        <label className="flex items-center gap-3 px-4 h-[46px] bg-gray-50 hover:bg-gray-100 rounded-xl cursor-pointer border border-gray-200 transition-colors">
                            <input
                                type="checkbox"
                                className="w-4 h-4 rounded text-orange-500 focus:ring-orange-200 border-gray-300 cursor-pointer"
                                checked={formData.active ?? true}
                                onChange={(e) => updateField('active', e.target.checked)}
                            />
                            <span className="text-sm font-bold text-gray-700">
                                Hiển thị danh mục
                            </span>
                        </label>
                    </div>
                </div>

                {/* Hình ảnh */}
                <div>
                    <label className="block text-xs font-bold uppercase mb-1.5 text-gray-500 tracking-wider">
                        Hình ảnh
                    </label>
                    <div className={`relative border-2 border-dashed rounded-2xl p-4 text-center transition-all group ${preview ? 'border-orange-200 bg-orange-50/30' : 'border-gray-100 bg-gray-50/50 hover:border-orange-200'
                        }`}>
                        {preview ? (
                            <div className="relative inline-block">
                                <img
                                    src={preview}
                                    className="max-h-40 mx-auto rounded-xl shadow-sm border border-white"
                                    alt="Preview"
                                />
                                <div className="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity rounded-xl flex items-center justify-center">
                                    <label htmlFor="fileInput" className="cursor-pointer bg-white text-gray-700 px-3 py-1.5 rounded-lg text-xs font-bold shadow-lg">
                                        Thay đổi
                                    </label>
                                </div>
                            </div>
                        ) : (
                            <div className="py-4">
                                <ImageIcon className="mx-auto text-gray-300 mb-2" size={40} />
                                <p className="text-[10px] text-gray-400 font-medium">Click để tải ảnh lên (Max 5MB)</p>
                            </div>
                        )}

                        <input
                            type="file"
                            onChange={handleFileChange}
                            className="hidden"
                            id="fileInput"
                            accept="image/*"
                        />

                        {!preview && (
                            <label htmlFor="fileInput" className="absolute inset-0 cursor-pointer w-full h-full" />
                        )}
                    </div>
                </div>
            </form>

            {/* Footer */}
            <div className="px-6 py-5 border-t bg-gray-50/50 rounded-b-[2rem] flex gap-3 shrink-0">
                <button
                    type="button"
                    onClick={onClose}
                    className="flex-1 py-3 bg-white text-gray-600 border border-gray-200 rounded-xl font-bold text-sm hover:bg-gray-50 transition-all active:scale-95"
                >
                    Hủy bỏ
                </button>
                <button
                    form="categoryForm"
                    type="submit"
                    disabled={isSubmitting || !isChanged}
                    className={`flex-[2] py-3 rounded-xl font-bold text-sm shadow-lg transition-all active:scale-95 ${(isSubmitting || !isChanged)
                        ? 'bg-gray-300 text-gray-500 cursor-not-allowed shadow-none'
                        : 'bg-orange-500 text-white hover:bg-orange-600 shadow-orange-100'
                        }`}
                >
                    {isSubmitting ? (
                        <div className="flex items-center justify-center gap-2">
                            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                            <span>Đang lưu...</span>
                        </div>
                    ) : (
                        editId ? 'Lưu thay đổi' : 'Tạo danh mục'
                    )}
                </button>
            </div>
        </SharedModal>
    );
};

export default CategoryModal;
