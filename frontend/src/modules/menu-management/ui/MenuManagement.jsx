import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, UtensilsCrossed } from 'lucide-react';

import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';

import { menuItemService } from '@modules/menu-management/api/menuService.js';
import { categoryService } from '@entities/category/api/categoryService.js';
import { aiLocalService } from '@modules/ai-assistant/api/aiLocalService.js';

import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import MenuCard from './MenuCard';
import MenuModal from './MenuModal';
import StatusModal from '@shared/ui/StatusModal.jsx';
import ConfirmModal from '@shared/ui/ConfirmModal.jsx';
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';

const MenuManager = () => {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filterCate, setFilterCate] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [aiScanning, setAiScanning] = useState(false);

  const [formData, setFormData] = useState({ id: null, name: '', price: '', categoryId: '', itemOptions: [] });
  const [initialFormData, setInitialFormData] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState('');
  const [errors, setErrors] = useState({});

  // Hook Status Modal
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();
  const { confirmModal, confirm, closeConfirm } = useConfirmModal();

  // Load Categories
  useEffect(() => {
    categoryService.search('').then(data => {
      const cats = data.content || [];
      setCategories(cats);
      if (cats.length > 0) setFormData(prev => ({ ...prev, categoryId: cats[0].id }));
    });
  }, []);

  // Fetch Items
  const fetchItems = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true);
    try {
      const data = await menuItemService.getAll(filterCate);
      setItems(data);
    } catch (err) {
      console.error("Lỗi đồng bộ thực đơn:", err);
    } finally {
      setLoading(false);
    }
  }, [filterCate]);

  // WebSocket
  useWebSocket('/topic/menu', (message) => {
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      playNotificationSound();
      fetchItems();
    }
  });

  useEffect(() => { fetchItems(true); }, [fetchItems]);

  // AI Magic Scan
  const handleAiScan = useCallback(async () => {
    if (!preview) return;
    setAiScanning(true);
    try {
      const result = await aiLocalService.analyzeDish(preview);

      if (result.confidence < 0.4) {
        showError(`AI không chắc chắn (${(result.confidence * 100).toFixed(0)}%). Hãy thử ảnh rõ hơn.`);
        return;
      }

      setFormData(prev => ({
        ...prev,
        name: result.name,
        categoryId: result.categoryId,
        price: result.price
      }));
      showSuccess(`Nhận diện: ${result.name}`);
    } catch (err) {
      showError("Lỗi AI: " + err.message);
    } finally {
      setAiScanning(false);
    }
  }, [preview, showSuccess, showError]);

  // SUBMIT 
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      const payload = {
        name: formData.name,
        price: Number(formData.price),
        categoryId: Number(formData.categoryId),
        itemOptions: formData.itemOptions,
        active: formData.active ?? true
      };

      let res;
      if (formData.id) {
        res = await menuItemService.update(formData.id, payload);
        showSuccess(`Đã cập nhật món ăn`);
      } else {
        res = await menuItemService.create(payload);
        showSuccess(`Đã thêm món ăn`);
      }

      if (selectedFile) {
        await menuItemService.uploadImage(res.id || formData.id, selectedFile);
      }

      setIsModalOpen(false);
    } catch (err) {
      console.error('Error saving menu item:', err);
      const errorMsg = err.message || '';

      if (errorMsg.toLowerCase().includes('item name already exists')) {
        setErrors({ ...errors, name: "Tên món ăn này đã tồn tại" });
      } else {
        showError(err);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // DELETE
  const handleDelete = async (id) => {
    const confirmed = await confirm('Xóa món ăn', 'Bạn có chắc chắn muốn xóa món ăn này?');
    if (!confirmed) return;
    try {
      await menuItemService.delete(id);
      showSuccess("Đã xóa món ăn thành công");
    } catch (err) {
      showError(err);
    }
  };

  return (
    <div className="p-4 space-y-4">
      <ManagementHeader
        showFilter={true}
        filterValue={filterCate}
        setFilterValue={setFilterCate}
        filterOptions={categories}
        onAddClick={() => {
          const emptyForm = { id: null, name: '', price: '', categoryId: categories[0]?.id || '', itemOptions: [] };
          setFormData(emptyForm);
          setInitialFormData(null);
          setErrors({});
          setPreview('');
          setSelectedFile(null);
          setIsModalOpen(true);
        }}
        addButtonText="Thêm mới"
      />

      {loading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-orange-500" size={40} />
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
          {items.map(it => (
            <MenuCard
              key={it.id}
              item={it}
              onEdit={(item) => {
                const data = {
                  id: item.id,
                  name: item.name,
                  price: item.price,
                  categoryId: item.category?.id,
                  itemOptions: item.itemOptions || []
                };
                setFormData(data);
                setInitialFormData(data);
                setErrors({});
                setPreview(item.img || '');
                setIsModalOpen(true);
              }}
              onDelete={() => handleDelete(it.id)}
            />
          ))}
        </div>
      )}

      {!loading && items.length === 0 && (
        <div className="text-center py-20 text-gray-400 italic bg-white rounded-3xl border border-dashed">
          <UtensilsCrossed size={40} className="mx-auto mb-4 opacity-30" />
          Không tìm thấy món ăn phù hợp hoặc chưa có dữ liệu.
        </div>
      )}

      <MenuModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmit}
        categories={categories}
        formData={formData}
        setFormData={setFormData}
        preview={preview}
        isSubmitting={isSubmitting}
        aiScanning={aiScanning}
        initialFormData={initialFormData}
        selectedFile={selectedFile}
        errors={errors}
        setErrors={setErrors}
        handleFileChange={(e) => {
          const f = e.target.files[0];
          if (f) { setSelectedFile(f); setPreview(URL.createObjectURL(f)); }
        }}
        onAiScan={handleAiScan}
      />

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

export default MenuManager;
