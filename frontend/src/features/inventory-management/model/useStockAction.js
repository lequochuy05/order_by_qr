import { useCallback, useState } from 'react';

import { inventoryService } from '@features/inventory-management/api/inventoryService.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

const EMPTY_STOCK_FORM = {
  quantity: '',
  quantityOnHand: '',
  note: '',
};

const useStockAction = ({ onSaved }) => {
  const [stockAction, setStockAction] = useState(null);
  const [stockForm, setStockForm] = useState(EMPTY_STOCK_FORM);
  const [isStockSubmitting, setIsStockSubmitting] = useState(false);

  const openStockAction = useCallback((item, type) => {
    setStockAction({ item, type });
    setStockForm({
      quantity: '',
      quantityOnHand: item.quantityOnHand,
      note: '',
    });
  }, []);

  const openStockIn = useCallback(
    (item) => {
      openStockAction(item, 'stock-in');
    },
    [openStockAction],
  );

  const openStockAdjust = useCallback(
    (item) => {
      openStockAction(item, 'adjust');
    },
    [openStockAction],
  );

  const closeStockModal = useCallback(() => {
    if (!isStockSubmitting) setStockAction(null);
  }, [isStockSubmitting]);

  const handleStockSubmit = async (event) => {
    event.preventDefault();
    if (!stockAction?.item || isStockSubmitting) return;

    setIsStockSubmitting(true);
    try {
      if (stockAction.type === 'stock-in') {
        await inventoryService.stockIn(stockAction.item.id, {
          quantity: Number(stockForm.quantity),
          note: stockForm.note,
        });
        showSuccessToast('Đã nhập kho');
      } else {
        const quantityDelta =
          Number(stockForm.quantityOnHand) - Number(stockAction.item.quantityOnHand || 0);
        if (quantityDelta === 0) {
          setStockAction(null);
          showSuccessToast('Tồn kho không thay đổi');
          return;
        }
        await inventoryService.adjust(stockAction.item.id, {
          quantityDelta,
          note: stockForm.note,
        });
        showSuccessToast('Đã cập nhật kiểm kê');
      }

      setStockAction(null);
      onSaved?.();
    } catch (error) {
      showErrorToast(error);
    } finally {
      setIsStockSubmitting(false);
    }
  };

  return {
    stockAction,
    stockForm,
    isStockSubmitting,
    setStockForm,
    openStockIn,
    openStockAdjust,
    closeStockModal,
    handleStockSubmit,
  };
};

export default useStockAction;
