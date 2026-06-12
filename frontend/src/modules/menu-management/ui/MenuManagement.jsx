import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
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
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';

const emptyMenuForm = {
  id: null,
  name: '',
  description: '',
  img: '',
  price: '',
  categoryId: '',
  itemOptions: [],
  active: true,
  available: true,
  displayOrder: 0
};

const normalizeItemOptions = (itemOptions = []) => itemOptions.map(option => ({
  id: option.id,
  name: option.name || '',
  required: option.required ?? option.isRequired ?? false,
  maxSelection: option.maxSelection ?? 1,
  optionValues: (option.optionValues || []).map(value => ({
    id: value.id,
    name: value.name || '',
    extraPrice: Number(value.extraPrice) || 0
  }))
}));

const toMenuFormData = (item) => ({
  id: item.id,
  name: item.name || '',
  description: item.description || '',
  img: item.img || '',
  price: item.price ?? '',
  categoryId: item.category?.id || '',
  itemOptions: normalizeItemOptions(item.itemOptions || []),
  active: item.active ?? true,
  available: item.available ?? true,
  displayOrder: item.displayOrder ?? 0
});

const MenuManager = () => {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filterCate, setFilterCate] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [aiScanning, setAiScanning] = useState(false);
  const [detailLoadingId, setDetailLoadingId] = useState(null);

  const [formData, setFormData] = useState(emptyMenuForm);
  const [initialFormData, setInitialFormData] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState('');
  const [errors, setErrors] = useState({});
  const isMountedRef = useRef(true);
  const fetchSeqRef = useRef(0);

  // Hook Status Modal
  const { showSuccess, showError } = useStatusModal();
  const { confirm } = useConfirmModal();

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // Load Categories
  useEffect(() => {
    let cancelled = false;

    const loadCategories = async () => {
      try {
        const data = await categoryService.getAll();
        if (cancelled || !isMountedRef.current) return;

        const cats = data.content || data || [];
        setCategories(cats);
        if (cats.length > 0) {
          setFormData(prev => ({ ...prev, categoryId: prev.categoryId || cats[0].id }));
        }
      } catch (err) {
        console.error("Lỗi tải danh mục món ăn:", err);
      }
    };

    loadCategories();
    return () => {
      cancelled = true;
    };
  }, []);

  // Fetch Items
  const fetchItems = useCallback(async (showLoading = false, { force = false } = {}) => {
    const fetchSeq = ++fetchSeqRef.current;
    if (showLoading) setLoading(true);
    try {
      const data = await menuItemService.getAll(undefined, { force });
      if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
      setItems(data);
    } catch (err) {
      if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
      console.error("Lỗi đồng bộ thực đơn:", err);
    } finally {
      if (isMountedRef.current && fetchSeq === fetchSeqRef.current) {
        setLoading(false);
      }
    }
  }, []);

  const filteredItems = useMemo(() => {
    if (filterCate === 'ALL') return items;
    return items.filter(item => String(item.category?.id) === String(filterCate));
  }, [items, filterCate]);

  // WebSocket
  useWebSocket('/topic/menu', (message) => {
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      playNotificationSound();
      fetchItems(false, { force: true });
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
        name: formData.name.trim(),
        description: formData.description?.trim() || null,
        img: formData.img || null,
        price: Number(formData.price),
        categoryId: Number(formData.categoryId),
        itemOptions: normalizeItemOptions(formData.itemOptions),
        active: formData.active ?? true,
        available: formData.available ?? true,
        displayOrder: Number(formData.displayOrder) || 0
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
      fetchItems(false, { force: true });
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
      fetchItems(false, { force: true });
    } catch (err) {
      showError(err);
    }
  };

  const handleEditItem = async (item) => {
    setDetailLoadingId(item.id);
    try {
      const detail = await menuItemService.getById(item.id);
      if (!isMountedRef.current) return;

      const data = toMenuFormData(detail);
      setFormData(data);
      setInitialFormData(data);
      setErrors({});
      setSelectedFile(null);
      setPreview(detail.img || '');
      setIsModalOpen(true);
    } catch (err) {
      showError(err);
    } finally {
      if (isMountedRef.current) {
        setDetailLoadingId(null);
      }
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
          const emptyForm = { ...emptyMenuForm, categoryId: categories[0]?.id || '' };
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
          {filteredItems.map(it => (
            <MenuCard
              key={it.id}
              item={it}
              onEdit={handleEditItem}
              onDelete={() => handleDelete(it.id)}
              isEditing={detailLoadingId === it.id}
            />
          ))}
        </div>
      )}

      {!loading && filteredItems.length === 0 && (
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
    </div>
  );
};

export default MenuManager;
