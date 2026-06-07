import React, { useState, useEffect, useCallback } from 'react';
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
import StatusModal from '@shared/ui/StatusModal.jsx';
import ConfirmModal from '@shared/ui/ConfirmModal.jsx';
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';

const CategoryManager = () => {
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');

    // State cho Modal nhập liệu
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editId, setEditId] = useState(null);
    const [catName, setCatName] = useState('');
    const [initialCatName, setInitialCatName] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [selectedFile, setSelectedFile] = useState(null);
    const [preview, setPreview] = useState('');
    const [errors, setErrors] = useState({});

    // === Hook Status Modal ===
    const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();
    const { confirmModal, confirm, closeConfirm } = useConfirmModal();

    // === Tải dữ liệu ===
    const fetchCategories = useCallback(async (showLoading = false) => {
        if (showLoading) setLoading(true);
        try {
            const data = await categoryService.getAll();
            setCategories(data.content || data);
        } catch (err) {
            console.error("Lỗi tải danh mục:", err);
        } finally {
            setLoading(false);
        }
    }, []);

    // === WebSocket ===
    useWebSocket('/topic/categories', (message) => {
        if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
            playNotificationSound();
            fetchCategories();
        }
    });

    useEffect(() => { fetchCategories(true); }, [fetchCategories]);

    // === Validate ===
    const validateForm = () => {
        const newErrors = {};
        if (!catName || !catName.trim()) {
            newErrors.name = "Tên danh mục không được để trống";
        } else if (catName.length < 2) {
            newErrors.name = "Tên danh mục phải có ít nhất 2 ký tự";
        } else if (/^\d/.test(catName)) {
            newErrors.name = "Tên danh mục không được bắt đầu bằng số";
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
            let result;
            if (editId) {
                result = await categoryService.update(editId, catName);
                showSuccess("Cập nhật danh mục thành công!");
            } else {
                result = await categoryService.create(catName);
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
            setEditId(cat.id);
            setCatName(cat.name);
            setInitialCatName(cat.name);
            setPreview(cat.img || '');
        } else {
            setEditId(null);
            setCatName('');
            setInitialCatName('');
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
                editId={editId} catName={catName} setCatName={setCatName}
                preview={preview} isSubmitting={isSubmitting}
                initialCatName={initialCatName} selectedFile={selectedFile}
                errors={errors} setErrors={setErrors}
                handleFileChange={(e) => {
                    const f = e.target.files[0];
                    if (f) {
                        setSelectedFile(f);
                        setPreview(URL.createObjectURL(f));
                    }
                }}
            />

            {/* Modal Thông báo (Dùng Hook) */}
            <StatusModal
                isOpen={statusModal.isOpen}
                onClose={closeStatusModal}
                type={statusModal.type}
                title={statusModal.title}
                message={statusModal.message}
            />
            <ConfirmModal
                isOpen={confirmModal.isOpen}
                onClose={closeConfirm}
                onConfirm={confirmModal.onConfirm}
                title={confirmModal.title}
                message={confirmModal.message}
            />
        </div>
    );
};

export default CategoryManager;
