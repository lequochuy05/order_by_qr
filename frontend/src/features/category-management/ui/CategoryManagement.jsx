import React, { useState, useEffect, useRef } from 'react';
import { Loader2, Layers } from 'lucide-react';

import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useCrudModal } from '@shared/hooks/useCrudModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

// Import React Query
import { categoryService, useCategoriesPageQuery } from '@entities/category';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';

// Import Component
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import CategoryCard from './CategoryCard';
import CategoryModal from './CategoryModal';

const emptyCategoryForm = {
  id: null,
  version: null,
  name: '',
  img: '',
  description: '',
  active: true,
  displayOrder: 0,
};

const toCategoryFormData = (category = {}) => ({
  id: category.id || null,
  version: category.version ?? null,
  name: category.name || '',
  img: category.img || '',
  description: category.description || '',
  active: category.active ?? true,
  displayOrder: category.displayOrder ?? 0,
});

const toImageFormData = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
};

const CategoryManager = () => {
  const [searchTerm, setSearchTerm] = useState('');

  // State cho Modal nhập liệu
  const [formData, setFormData] = useState(emptyCategoryForm);
  const [initialFormData, setInitialFormData] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [detailLoadingId, setDetailLoadingId] = useState(null);
  const objectPreviewUrlRef = useRef(null);
  const pageSize = 24;

  const crud = useCrudModal();
  const editId = crud.editingItem;
  const { confirm } = useConfirmModal();
  const debouncedSearchTerm = useDebouncedValue(searchTerm);

  const handleSearchChange = (value) => {
    setSearchTerm(value);
    setCurrentPage(0);
  };

  // === React Query: Tải dữ liệu ===
  const { data: pageData, isLoading: loading } = useCategoriesPageQuery({
    q: debouncedSearchTerm,
    page: currentPage,
    size: pageSize,
  });

  const categories = pageData?.content || [];
  const totalPages = pageData?.totalPages || 0;
  const totalElements = pageData?.totalElements || 0;

  const invalidateCategories = () =>
    queryClient.invalidateQueries({ queryKey: queryKeys.categories.all });

  const clearObjectPreview = () => {
    if (objectPreviewUrlRef.current) {
      URL.revokeObjectURL(objectPreviewUrlRef.current);
      objectPreviewUrlRef.current = null;
    }
  };

  useEffect(
    () => () => {
      if (objectPreviewUrlRef.current) {
        URL.revokeObjectURL(objectPreviewUrlRef.current);
      }
    },
    [],
  );

  // === Validate ===
  const validateForm = () => {
    const newErrors = {};
    if (!formData.name || !formData.name.trim()) {
      newErrors.name = 'Tên danh mục không được để trống';
    } else if (formData.name.trim().length < 2) {
      newErrors.name = 'Tên danh mục phải có ít nhất 2 ký tự';
    } else if (formData.name.trim().length > 50) {
      newErrors.name = 'Tên danh mục không được vượt quá 50 ký tự';
    }

    if ((formData.description || '').length > 500) {
      newErrors.description = 'Mô tả không được vượt quá 500 ký tự';
    }

    if (Number(formData.displayOrder) < 0) {
      newErrors.displayOrder = 'Thứ tự hiển thị không được âm';
    }

    crud.setFormErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // === Lưu (Thêm/Sửa) ===
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;
    if (crud.isSubmitting) return;
    crud.setSubmitting(true);
    try {
      const payload = {
        ...formData,
        name: formData.name.trim(),
        img: formData.img || null,
        description: formData.description?.trim() || null,
        active: formData.active ?? true,
        displayOrder: Number(formData.displayOrder) || 0,
      };

      let result;
      if (editId) {
        result = await categoryService.update(editId, payload);
      } else {
        result = await categoryService.create(payload);
      }

      if (selectedFile) {
        const uploadVersion = result?.version ?? formData.version;
        try {
          await categoryService.uploadImage(
            result?.id || editId,
            uploadVersion,
            toImageFormData(selectedFile),
          );
        } catch {
          setFormData((prev) => ({
            ...prev,
            id: result?.id || editId,
            version: result?.version ?? prev.version,
          }));
          if (!editId && result?.id) {
            crud.setEditingItem(result.id);
          }
          invalidateCategories();
          showErrorToast('Đã lưu danh mục, nhưng chưa tải được ảnh. Vui lòng cập nhật ảnh lại.');
          return;
        }
      }

      closeModal();
      invalidateCategories();
      showSuccessToast(editId ? 'Cập nhật danh mục thành công!' : 'Thêm mới danh mục thành công!');
    } catch (err) {
      const errorMsg = err.message || '';
      if (
        err?.code === 'CATEGORY_NAME_EXISTS' ||
        errorMsg.toLowerCase().includes('category name already exists')
      ) {
        crud.setFormErrors((current) => ({ ...current, name: 'Tên danh mục này đã tồn tại' }));
      } else {
        showErrorToast(err);
      }
    } finally {
      crud.setSubmitting(false);
    }
  };

  // === Xóa ===
  const handleDelete = async (id, version) => {
    if (crud.deletingId) return;
    const confirmed = await confirm(
      'Xóa danh mục',
      'Bạn có chắc chắn muốn xóa danh mục này? Hành động này không thể hoàn tác.',
    );
    if (!confirmed) return;

    crud.setDeletingId(id);
    try {
      await categoryService.delete(id, version);
      showSuccessToast('Đã xóa danh mục thành công!');
      invalidateCategories();
    } catch (err) {
      showErrorToast(err);
    } finally {
      crud.setDeletingId(null);
    }
  };

  // Modal
  const openModal = async (cat = null) => {
    clearObjectPreview();
    if (cat) {
      setDetailLoadingId(cat.id);
      try {
        const freshCat = await categoryService.getById(cat.id);
        const categoryFormData = toCategoryFormData(freshCat);
        setFormData(categoryFormData);
        setInitialFormData(categoryFormData);
        setPreview(freshCat.img || '');
        setSelectedFile(null);
        crud.openEdit(freshCat.id);
      } catch (err) {
        showErrorToast(err);
      } finally {
        setDetailLoadingId(null);
      }
    } else {
      setFormData(emptyCategoryForm);
      setInitialFormData(null);
      setPreview('');
      setSelectedFile(null);
      crud.openCreate();
    }
  };

  const closeModal = () => {
    clearObjectPreview();
    crud.close();
    setSelectedFile(null);
  };

  return (
    <div className="w-full min-w-0 space-y-4 p-0 sm:p-3 lg:p-4">
      <ManagementHeader
        searchPlaceholder="Tìm danh mục..."
        searchTerm={searchTerm}
        setSearchTerm={handleSearchChange}
        onAddClick={() => openModal()}
        addButtonText="Thêm mới"
      />

      {loading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-indigo-600 dark:text-indigo-400" size={40} />
        </div>
      ) : (
        <div className="grid min-w-0 grid-cols-1 gap-4 min-[360px]:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6">
          {categories.map((cat) => (
            <CategoryCard
              key={cat.id}
              category={cat}
              onEdit={() => openModal(cat)}
              onDelete={() => handleDelete(cat.id, cat.version)}
              isEditing={detailLoadingId === cat.id}
              isDeleting={crud.deletingId === cat.id}
            />
          ))}
        </div>
      )}

      {!loading && categories.length === 0 && (
        <div className="rounded-3xl border border-dashed border-slate-200 bg-white py-20 text-center italic text-slate-400 transition-colors dark:border-slate-800 dark:bg-slate-900 dark:text-slate-500">
          <Layers size={40} className="mx-auto mb-4 opacity-30" />
          Không tìm thấy danh mục phù hợp hoặc chưa có dữ liệu.
        </div>
      )}

      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        itemLabel="danh mục"
        loading={loading}
        onPageChange={setCurrentPage}
      />

      {/* Modal Nhập liệu */}
      <CategoryModal
        isOpen={crud.isOpen}
        onClose={closeModal}
        onSubmit={handleSubmit}
        editId={editId}
        formData={formData}
        setFormData={setFormData}
        preview={preview}
        isSubmitting={crud.isSubmitting}
        initialFormData={initialFormData}
        selectedFile={selectedFile}
        errors={crud.formErrors}
        setErrors={crud.setFormErrors}
        handleFileChange={(e) => {
          const f = e.target.files[0];
          if (f) {
            clearObjectPreview();
            const objectUrl = URL.createObjectURL(f);
            objectPreviewUrlRef.current = objectUrl;
            setSelectedFile(f);
            setPreview(objectUrl);
          }
        }}
      />
    </div>
  );
};

export default CategoryManager;
