import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Loader2, Layers } from 'lucide-react';

// Import Hook
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';

// Import Service
import { categoryService } from '@entities/category/api/categoryService.js';

// Import Component
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import CategoryCard from './CategoryCard';
import CategoryModal from './CategoryModal';
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';

const emptyCategoryForm = {
    id: null,
    name: '',
    img: '',
    description: '',
    active: true,
    displayOrder: 0
};

const toCategoryFormData = (category = {}) => ({
    id: category.id || null,
    name: category.name || '',
    img: category.img || '',
    description: category.description || '',
    active: category.active ?? true,
    displayOrder: category.displayOrder ?? 0
});

const CategoryManager = () => {
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');

    // State cho Modal nhập liệu
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editId, setEditId] = useState(null);
    const [formData, setFormData] = useState(emptyCategoryForm);
    const [initialFormData, setInitialFormData] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [selectedFile, setSelectedFile] = useState(null);
    const [preview, setPreview] = useState('');
    const [errors, setErrors] = useState({});
    const isMountedRef = useRef(true);
    const fetchSeqRef = useRef(0);

    // === Hook Status Modal ===
    const { showSuccess, showError } = useStatusModal();
    const { confirm } = useConfirmModal();

    // === Tải dữ liệu ===
    const fetchCategories = useCallback(async (showLoading = false, { force = false } = {}) => {
        const fetchSeq = ++fetchSeqRef.current;
        if (showLoading) setLoading(true);
        try {
            const data = await categoryService.getAll({ force });
            if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
            setCategories(data.content || data);
        } catch (err) {
            if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
            console.error("Lỗi tải danh mục:", err);
        } finally {
            if (isMountedRef.current && fetchSeq === fetchSeqRef.current) {
                setLoading(false);
            }
        }
    }, []);

    // === WebSocket ===
    useWebSocket('/topic/categories', (message) => {
        if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
            playNotificationSound();
            fetchCategories(false, { force: true });
        }
    });

    useEffect(() => {
        isMountedRef.current = true;
        return () => {
            isMountedRef.current = false;
        };
    }, []);

    useEffect(() => { fetchCategories(true); }, [fetchCategories]);

    // === Validate ===
    const validateForm = () => {
        const newErrors = {};
        if (!formData.name || !formData.name.trim()) {
            newErrors.name = "Tên danh mục không được để trống";
        } else if (formData.name.trim().length < 2) {
            newErrors.name = "Tên danh mục phải có ít nhất 2 ký tự";
        } else if (/^\d/.test(formData.name.trim())) {
            newErrors.name = "Tên danh mục không được bắt đầu bằng số";
        }

        if ((formData.description || '').length > 255) {
            newErrors.description = "Mô tả không được vượt quá 255 ký tự";
        }

        if (Number(formData.displayOrder) < 0) {
            newErrors.displayOrder = "Thứ tự hiển thị không được âm";
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    // === Lưu (Thêm/Sửa) ===
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validateForm()) return;
        if (isSubmitting) return;
        setIsSubmitting(true);
        try {
            const payload = {
                ...formData,
                name: formData.name.trim(),
                img: formData.img || null,
                description: formData.description?.trim() || null,
                active: formData.active ?? true,
                displayOrder: Number(formData.displayOrder) || 0
            };

            let result;
            if (editId) {
                result = await categoryService.update(editId, payload);
                showSuccess("Cập nhật danh mục thành công!");
            } else {
                result = await categoryService.create(payload);
                showSuccess("Thêm mới danh mục thành công!");
            }
            if (selectedFile) {
                await categoryService.uploadImage(result?.id || editId, selectedFile);
            }

            setIsModalOpen(false);
        } catch (err) {
            const errorMsg = err.message || '';
            if (errorMsg.toLowerCase().includes('category name already exists')) {
                setErrors({ ...errors, name: "Tên danh mục này đã tồn tại" });
            } else {
                showError(err);
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    // === Xóa ===
    const handleDelete = async (id) => {
        const confirmed = await confirm('Xóa danh mục', 'Bạn có chắc chắn muốn xóa danh mục này? Hành động này không thể hoàn tác.');
        if (!confirmed) return;

        try {
            await categoryService.delete(id);
            showSuccess("Đã xóa danh mục thành công!");
        } catch (err) {
            showError(err);
        }
    };

    // Modal
    const openModal = (cat = null) => {
        if (cat) {
            const categoryFormData = toCategoryFormData(cat);
            setEditId(cat.id);
            setFormData(categoryFormData);
            setInitialFormData(categoryFormData);
            setPreview(cat.img || '');
        } else {
            setEditId(null);
            setFormData(emptyCategoryForm);
            setInitialFormData(null);
            setPreview('');
        }
        setSelectedFile(null);
        setErrors({});
        setIsModalOpen(true);
    };

    const closeModal = () => { setIsModalOpen(false); setSelectedFile(null); };

    const filteredCategories = categories.filter(c =>
        c.name.toLowerCase().includes(searchTerm.toLowerCase())
    );



    return (
        <div className="p-4 space-y-4">
            <ManagementHeader
                searchPlaceholder="Tìm danh mục..."
                searchTerm={searchTerm}
                setSearchTerm={setSearchTerm}
                onAddClick={() => openModal()}
                addButtonText="Thêm mới"
            />

            {loading ? (
                <div className="flex justify-center p-20"><Loader2 className="animate-spin text-orange-500" size={40} /></div>
            ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
                    {filteredCategories.map(cat => (
                        <CategoryCard
                            key={cat.id}
                            category={cat}
                            onEdit={() => openModal(cat)}
                            onDelete={() => handleDelete(cat.id)}
                        />
                    ))}
                </div>
            )}

            {!loading && filteredCategories.length === 0 && (
                <div className="text-center py-20 text-gray-400 italic bg-white rounded-3xl border border-dashed">
                    <Layers size={40} className="mx-auto mb-4 opacity-30" />
                    Không tìm thấy danh mục phù hợp hoặc chưa có dữ liệu.
                </div>
            )}

            {/* Modal Nhập liệu */}
            <CategoryModal
                isOpen={isModalOpen} onClose={closeModal} onSubmit={handleSubmit}
                editId={editId} formData={formData} setFormData={setFormData}
                preview={preview} isSubmitting={isSubmitting}
                initialFormData={initialFormData} selectedFile={selectedFile}
                errors={errors} setErrors={setErrors}
                handleFileChange={(e) => {
                    const f = e.target.files[0];
                    if (f) {
                        setSelectedFile(f);
                        setPreview(URL.createObjectURL(f));
                    }
                }}
            />
        </div>
    );
};

export default CategoryManager;
