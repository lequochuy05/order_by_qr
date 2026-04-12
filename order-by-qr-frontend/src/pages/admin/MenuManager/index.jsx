import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, UtensilsCrossed } from 'lucide-react';

import { useWebSocket } from '../../../hooks/useWebSocket';
import { useStatusModal } from '../../../hooks/useStatusModal';

import { menuItemService } from '../../../services/admin/menuService';
import { categoryService } from '../../../services/admin/categoryService';
import { aiService } from '../../../services/admin/aiService';

import ManagementHeader from '../../../components/admin/common/ManagementHeader';
import MenuCard from './MenuCard';
import MenuModal from './MenuModal';
import StatusModal from '../../../components/admin/common/StatusModal';

const MenuManager = () => {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filterCate, setFilterCate] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [formData, setFormData] = useState({ id: null, name: '', price: '', categoryId: '', itemOptions: [] });
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState('');

  // Hook Status Modal
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

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
    const signal = typeof message === 'string' ? message : message.body;
    if (signal === 'UPDATED') fetchItems();
  });

  useEffect(() => { fetchItems(true); }, [fetchItems]);

  // AI Magic Scan
  const handleAiScan = useCallback(async () => {
    if (!selectedFile) return;
    setLoading(true);
    try {
      showSuccess("AI đang phân tích món ăn...");
      const result = await aiService.analyzeDish(selectedFile);
      if (result.error) throw new Error(result.error);

      setFormData(prev => ({
        ...prev,
        name: result.name || prev.name,
        price: result.price || prev.price
      }));
      showSuccess("Đã hoàn tất phân tích!");
    } catch (err) {
      showError("Lỗi AI: " + err.message);
    } finally {
      setLoading(false);
    }
  }, [selectedFile, showSuccess, showError]);

  useEffect(() => {
    window.addEventListener('aiScan', handleAiScan);
    return () => window.removeEventListener('aiScan', handleAiScan);
  }, [handleAiScan]);

  // SUBMIT 
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      const payload = {
        name: formData.name,
        price: Number(formData.price),
        category: { id: Number(formData.categoryId) },
        itemOptions: formData.itemOptions
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
      showError(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  // DELETE
  const handleDelete = async (id) => {
    if (!window.confirm("Xóa món ăn này?")) return;
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
          setFormData({ id: null, name: '', price: '', categoryId: categories[0]?.id || '', itemOptions: [] });
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
                setFormData({
                  id: item.id,
                  name: item.name,
                  price: item.price,
                  categoryId: item.category?.id,
                  itemOptions: item.itemOptions || []
                });
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
        handleFileChange={(e) => {
          const f = e.target.files[0];
          if (f) { setSelectedFile(f); setPreview(URL.createObjectURL(f)); }
        }}
      />

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

export default MenuManager;