import { useCallback, useState } from 'react';

import { inventoryService } from '@features/inventory-management/api/inventoryService.js';
import {
  defaultInventoryForm,
  INVENTORY_UNITS,
} from '@features/inventory-management/lib/inventoryConstants.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

const useInventoryItemForm = ({ onSaved }) => {
  const [inventoryModalOpen, setInventoryModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [inventoryForm, setInventoryForm] = useState(defaultInventoryForm);
  const [inventoryErrors, setInventoryErrors] = useState({});
  const [isInventorySubmitting, setIsInventorySubmitting] = useState(false);

  const resetForm = useCallback(() => {
    setEditingItem(null);
    setInventoryForm(defaultInventoryForm());
    setInventoryErrors({});
  }, []);

  const validateInventoryForm = useCallback(() => {
    const nextErrors = {};
    const name = inventoryForm.name?.trim() || '';
    const unit = inventoryForm.unit?.trim() || '';
    const quantityOnHand = Number(inventoryForm.quantityOnHand);
    const lowStockThreshold = Number(inventoryForm.lowStockThreshold);

    if (!name) {
      nextErrors.name = 'Tên nguyên liệu không được để trống';
    } else if (name.length < 2) {
      nextErrors.name = 'Tên nguyên liệu phải có ít nhất 2 ký tự';
    }

    if (!unit) nextErrors.unit = 'Vui lòng chọn đơn vị tính';

    if (String(inventoryForm.quantityOnHand ?? '').trim() === '') {
      nextErrors.quantityOnHand = 'Tồn hiện tại không được để trống';
    } else if (!Number.isFinite(quantityOnHand) || quantityOnHand < 0) {
      nextErrors.quantityOnHand = 'Tồn hiện tại không hợp lệ';
    }

    if (String(inventoryForm.lowStockThreshold ?? '').trim() === '') {
      nextErrors.lowStockThreshold = 'Ngưỡng cảnh báo không được để trống';
    } else if (!Number.isFinite(lowStockThreshold) || lowStockThreshold < 0) {
      nextErrors.lowStockThreshold = 'Ngưỡng cảnh báo không hợp lệ';
    }

    setInventoryErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }, [inventoryForm]);

  const openCreate = useCallback(() => {
    resetForm();
    setInventoryModalOpen(true);
  }, [resetForm]);

  const openEdit = useCallback((item) => {
    setEditingItem(item);
    setInventoryForm({
      name: item.name || '',
      unit: item.unit || INVENTORY_UNITS[0],
      quantityOnHand: item.quantityOnHand,
      lowStockThreshold: item.lowStockThreshold,
      active: item.active !== false,
    });
    setInventoryErrors({});
    setInventoryModalOpen(true);
  }, []);

  const closeInventoryModal = useCallback(() => {
    if (isInventorySubmitting) return;
    setInventoryModalOpen(false);
    resetForm();
  }, [isInventorySubmitting, resetForm]);

  const handleSaveInventory = async (event) => {
    event.preventDefault();
    if (isInventorySubmitting || !validateInventoryForm()) return;

    const payload = {
      name: inventoryForm.name.trim(),
      unit: inventoryForm.unit.trim(),
      quantityOnHand: Number(inventoryForm.quantityOnHand),
      lowStockThreshold: Number(inventoryForm.lowStockThreshold),
      active: inventoryForm.active,
    };

    setIsInventorySubmitting(true);
    try {
      if (editingItem?.id) {
        await inventoryService.updateItem(editingItem.id, payload);
        const quantityDelta =
          Number(inventoryForm.quantityOnHand) - Number(editingItem.quantityOnHand || 0);
        if (quantityDelta !== 0) {
          await inventoryService.adjust(editingItem.id, {
            quantityDelta,
            note: 'Cập nhật tồn kho từ form nguyên liệu',
          });
        }
        showSuccessToast('Đã cập nhật nguyên liệu');
      } else {
        await inventoryService.createItem(payload);
        showSuccessToast('Đã thêm nguyên liệu');
      }

      setInventoryModalOpen(false);
      resetForm();
      onSaved?.();
    } catch (error) {
      if (
        error?.code === 'INVENTORY_ITEM_NAME_EXISTS' ||
        error?.message?.includes('Inventory item name already exists')
      ) {
        setInventoryErrors((current) => ({
          ...current,
          name: 'Tên nguyên liệu này đã tồn tại',
        }));
      } else {
        showErrorToast(error);
      }
    } finally {
      setIsInventorySubmitting(false);
    }
  };

  return {
    inventoryModalOpen,
    editingItem,
    inventoryForm,
    inventoryErrors,
    isInventorySubmitting,
    setInventoryForm,
    setInventoryErrors,
    openCreate,
    openEdit,
    closeInventoryModal,
    handleSaveInventory,
  };
};

export default useInventoryItemForm;
