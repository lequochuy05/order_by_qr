import { useMemo } from 'react';

import { getInventoryUnitOptions } from '@features/inventory-management/lib/inventoryFormat.js';
import {
  CheckboxCard,
  ModalActions,
  ModalHeader,
  SelectField,
  SharedModal,
  TextField,
} from '@shared/ui';

const InventoryModal = ({
  item,
  form,
  setForm,
  errors = {},
  setErrors,
  onClose,
  onSubmit,
  isSubmitting = false,
}) => {
  const unitOptions = getInventoryUnitOptions(form.unit);
  const isChanged = useMemo(() => {
    if (!item?.id) return true;
    if ((form.name || '') !== (item.name || '')) return true;
    if ((form.unit || '') !== (item.unit || '')) return true;
    if (Number(form.quantityOnHand || 0) !== Number(item.quantityOnHand || 0)) return true;
    if (Number(form.lowStockThreshold || 0) !== Number(item.lowStockThreshold || 0)) return true;
    return (form.active !== false) !== (item.active !== false);
  }, [form, item]);

  const updateField = (field, value) => {
    setForm({ ...form, [field]: value });
    if (errors[field]) {
      setErrors({ ...errors, [field]: null });
    }
  };

  return (
    <SharedModal isOpen onClose={onClose} className="max-w-xl !p-0">
      <ModalHeader
        title={item?.id ? 'Sửa nguyên liệu' : 'Thêm nguyên liệu'}
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="inventoryForm"
        onSubmit={onSubmit}
        className="space-y-6 overflow-y-auto p-8 custom-scrollbar"
      >
        <TextField
          label="Tên nguyên liệu"
          value={form.name}
          onChange={(value) => updateField('name', value)}
          error={errors.name}
          placeholder="Ví dụ: Bột mì, Đường, Sữa tươi..."
          required
        />

        <SelectField
          label="Đơn vị tính"
          value={form.unit}
          onChange={(value) => updateField('unit', value)}
          error={errors.unit}
          options={unitOptions.map((unit) => ({ value: unit, label: unit }))}
          required
        />

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <TextField
            type="number"
            min="0"
            step="0.001"
            label="Tồn hiện tại"
            value={form.quantityOnHand}
            onChange={(value) => updateField('quantityOnHand', value)}
            error={errors.quantityOnHand}
            required
          />
          <TextField
            type="number"
            min="0"
            step="0.001"
            label="Ngưỡng cảnh báo"
            value={form.lowStockThreshold}
            onChange={(value) => updateField('lowStockThreshold', value)}
            error={errors.lowStockThreshold}
            required
          />
        </div>

        <CheckboxCard
          checked={form.active}
          onChange={(checked) => updateField('active', checked)}
          label="Đang sử dụng"
        />
      </form>

      <ModalActions
        formId="inventoryForm"
        onClose={onClose}
        submitLabel={item?.id ? 'Lưu thay đổi' : 'Thêm nguyên liệu'}
        isSubmitting={isSubmitting}
        disabled={!isChanged}
      />
    </SharedModal>
  );
};

export default InventoryModal;
