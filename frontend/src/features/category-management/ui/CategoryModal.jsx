import React from 'react';
import {
  CheckboxCard,
  ImageUploadField,
  ModalActions,
  ModalHeader,
  SharedModal,
  TextareaField,
  TextField,
} from '@shared/ui';

const CategoryModal = ({
  isOpen,
  onClose,
  onSubmit,
  editId,
  formData,
  setFormData,
  preview,
  handleFileChange,
  isSubmitting,
  initialFormData,
  selectedFile,
  errors = {},
  setErrors,
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
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-lg !p-0"
      ariaLabel={editId ? 'Sửa danh mục' : 'Thêm danh mục'}
    >
      <ModalHeader
        title={editId ? 'Sửa Danh Mục' : 'Thêm Danh Mục'}
        subtitle="Tổ chức thực đơn"
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="categoryForm"
        onSubmit={onSubmit}
        className="custom-scrollbar space-y-6 overflow-y-auto p-6 sm:p-8"
      >
        <TextField
          label="Tên danh mục"
          required
          value={formData.name}
          onChange={(value) => updateField('name', value)}
          error={errors.name}
          placeholder="Ví dụ: Đồ uống, Món khai vị..."
        />

        <TextareaField
          label="Mô tả"
          rows={3}
          maxLength={255}
          value={formData.description || ''}
          onChange={(value) => updateField('description', value)}
          error={errors.description}
          placeholder="Mô tả ngắn về nhóm món trong danh mục..."
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField
            label="Thứ tự hiển thị"
            type="number"
            min="0"
            value={formData.displayOrder ?? 0}
            onChange={(value) => updateField('displayOrder', value)}
            error={errors.displayOrder}
          />

          <div>
            <p className="mb-2 ml-1 text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">
              Trạng thái
            </p>
            <CheckboxCard
              checked={formData.active ?? true}
              onChange={(checked) => updateField('active', checked)}
              label="Hiển thị danh mục"
            />
          </div>
        </div>

        <ImageUploadField preview={preview} onChange={handleFileChange} inputId="category-image" />
      </form>

      <ModalActions
        onClose={onClose}
        formId="categoryForm"
        submitLabel={editId ? 'Lưu thay đổi' : 'Tạo danh mục'}
        isSubmitting={isSubmitting}
        disabled={!isChanged}
      />
    </SharedModal>
  );
};

export default CategoryModal;
