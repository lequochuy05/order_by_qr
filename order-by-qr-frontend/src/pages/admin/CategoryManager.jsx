import React, { useState, useEffect, useCallback } from 'react';
import { Loader2 } from 'lucide-react';

// Import Hook
import { useWebSocket } from '../../hooks/useWebSocket';
import { useStatusModal } from '../../hooks/useStatusModal';

// Import Service
import { categoryService } from '../../services/admin/categoryService';

// Import Component
import ManagementHeader from '../../components/admin/common/ManagementHeader';
import CategoryCard from '../../components/admin/categories/CategoryCard';
import CategoryModal from '../../components/admin/categories/CategoryModal';
import StatusModal from '../../components/admin/common/StatusModal';

const CategoryManager = () => {
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    
    // State cho Modal nhập liệu
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editId, setEditId] = useState(null);
    const [catName, setCatName] = useState('');
    const [selectedFile, setSelectedFile] = useState(null);
    const [preview, setPreview] = useState('');

    // === 1. Hook Status Modal ===
    const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

    // === 2. Tải dữ liệu ===
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

    // === 3. WebSocket ===
    useWebSocket('/topic/categories', (message) => {
        const signal = typeof message === 'string' ? message : message.body;
        if (signal === 'UPDATED') fetchCategories();
    });

    useEffect(() => { fetchCategories(true); }, [fetchCategories]);

    // === 4. Xử lý Lưu (Thêm/Sửa) ===
    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // 1. Lưu thông tin cơ bản
            let result;
            if (editId) {
                result = await categoryService.update(editId, catName);
                showSuccess("Cập nhật danh mục thành công!");
            } else {
                result = await categoryService.create(catName);
                showSuccess("Thêm mới danh mục thành công!");
            }

            // 2. Upload ảnh (nếu có)
            if (selectedFile) {
                await categoryService.uploadImage(result?.id || editId, selectedFile);
            }
            
            setIsModalOpen(false);
        } catch (err) { 
            showError(err); 
        }
    };

    // === 5. Xử lý Xóa ===
    const handleDelete = async (id) => {
        if (!window.confirm("Bạn có chắc chắn muốn xóa danh mục này? Hành động này không thể hoàn tác.")) return;
        
        try {
            await categoryService.delete(id);
            showSuccess("Đã xóa danh mục thành công!");
        } catch (err) {
            showError(err);
        }
    };

    // Các hàm phụ trợ Modal
    const openModal = (cat = null) => {
        if (cat) { 
            setEditId(cat.id); 
            setCatName(cat.name); 
            setPreview(cat.img || ''); // Fix hiển thị ảnh
        } else { 
            setEditId(null); 
            setCatName(''); 
            setPreview(''); 
        }
        setSelectedFile(null);
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

            {/* Modal Nhập liệu */}
            <CategoryModal 
                isOpen={isModalOpen} onClose={closeModal} onSubmit={handleSubmit}
                editId={editId} catName={catName} setCatName={setCatName}
                preview={preview}
                handleFileChange={(e) => {
                    const f = e.target.files[0];
                    if(f) { setSelectedFile(f); setPreview(URL.createObjectURL(f)); }
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
        </div>
    );
};

export default CategoryManager;