import React, { useState, useEffect } from 'react';
import { Loader2, UtensilsCrossed } from 'lucide-react';

import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { useMenuPageQuery } from '../api/menuQueries.js';
import { useCategoriesPageQuery } from '@features/category-management/api/categoryQueries.js';

import { menuItemService } from '@features/menu-management/api/menuService.js';

import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import MenuCard from './MenuCard';
import MenuModal from './MenuModal';

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
  displayOrder: 0,
};

const normalizeItemOptions = (itemOptions = []) =>
  itemOptions.map((option) => ({
    id: option.id,
    name: option.name || '',
    required: option.required ?? false,
    maxSelection: option.maxSelection ?? 1,
    optionValues: (option.optionValues || []).map((value) => ({
      id: value.id,
      name: value.name || '',
      extraPrice: Number(value.extraPrice) || 0,
    })),
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
  displayOrder: item.displayOrder ?? 0,
});

const MenuManager = () => {
  const [filterCate, setFilterCate] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [detailLoadingId, setDetailLoadingId] = useState(null);

  const [formData, setFormData] = useState(emptyMenuForm);
  const [initialFormData, setInitialFormData] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState('');
  const [errors, setErrors] = useState({});
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 24;

  const { confirm } = useConfirmModal();
  const debouncedSearchTerm = useDebouncedValue(searchTerm);

  // React Query: Categories for filter dropdown
  const { data: categoryPageData } = useCategoriesPageQuery(
    { page: 0, size: 1000 },
    { staleTime: 5 * 60 * 1000 },
  );
  const categories = categoryPageData?.content || [];

  // React Query: Menu items
  const { data: pageData, isLoading: loading } = useMenuPageQuery({
    q: debouncedSearchTerm,
    page: currentPage,
    size: pageSize,
    categoryId: filterCate,
  });

  const items = pageData?.content || [];
  const totalPages = pageData?.totalPages || 0;
  const totalElements = pageData?.totalElements || 0;

  const invalidateMenu = () => queryClient.invalidateQueries({ queryKey: queryKeys.menu.all });

  // WebSocket
  // WebSocket update đã được chuyển sang WebSocketInvalidator (Sprint 3D)

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearchTerm, filterCate]);

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
        displayOrder: Number(formData.displayOrder) || 0,
      };

      let res;
      if (formData.id) {
        res = await menuItemService.update(formData.id, payload);
        showSuccessToast(`Đã cập nhật món ăn`);
      } else {
        res = await menuItemService.create(payload);
        showSuccessToast(`Đã thêm món ăn`);
      }

      if (selectedFile) {
        await menuItemService.uploadImage(res.id || formData.id, selectedFile);
      }

      setIsModalOpen(false);
      invalidateMenu();
    } catch (err) {
      console.error('Error saving menu item:', err);
      const errorMsg = err.message || '';

      if (
        err?.code === 'MENU_ITEM_NAME_EXISTS' ||
        errorMsg.toLowerCase().includes('item name already exists')
      ) {
        setErrors({ ...errors, name: 'Tên món ăn này đã tồn tại' });
      } else {
        showErrorToast(err);
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
      showSuccessToast('Đã xóa món ăn thành công');
      invalidateMenu();
    } catch (err) {
      showErrorToast(err);
    }
  };

  const handleEditItem = async (item) => {
    setDetailLoadingId(item.id);
    try {
      const detail = await menuItemService.getById(item.id);

      const data = toMenuFormData(detail);
      setFormData(data);
      setInitialFormData(data);
      setErrors({});
      setSelectedFile(null);
      setPreview(detail.img || '');
      setIsModalOpen(true);
    } catch (err) {
      showErrorToast(err);
    } finally {
      setDetailLoadingId(null);
    }
  };

  return (
    <div className="w-full min-w-0 space-y-4 p-0 sm:p-3 lg:p-4">
      <ManagementHeader
        searchPlaceholder="Tìm món ăn..."
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
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
        <div className="grid min-w-0 grid-cols-1 gap-4 min-[360px]:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6">
          {items.map((it) => (
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

      {!loading && items.length === 0 && (
        <div className="text-center py-20 text-gray-400 italic bg-white rounded-3xl border border-dashed">
          <UtensilsCrossed size={40} className="mx-auto mb-4 opacity-30" />
          Không tìm thấy món ăn phù hợp hoặc chưa có dữ liệu.
        </div>
      )}

      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        itemLabel="món"
        loading={loading}
        onPageChange={setCurrentPage}
      />

      <MenuModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmit}
        categories={categories}
        formData={formData}
        setFormData={setFormData}
        preview={preview}
        isSubmitting={isSubmitting}
        initialFormData={initialFormData}
        selectedFile={selectedFile}
        errors={errors}
        setErrors={setErrors}
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

export default MenuManager;
