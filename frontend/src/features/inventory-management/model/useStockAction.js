import { useCallback, useState } from 'react';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { useStockInMutation, useAdjustStockMutation } from '../api/inventoryMutations.js';

const EMPTY_STOCK_FORM = {
  quantity: '',
  quantityOnHand: '',
  note: '',
};

const useStockAction = ({ onSaved }) => {
  const [stockAction, setStockAction] = useState(null);
  const [stockForm, setStockForm] = useState(EMPTY_STOCK_FORM);

  const { mutateAsync: stockIn, isPending: isStockInSubmitting } = useStockInMutation();
  const { mutateAsync: adjustStock, isPending: isAdjustSubmitting } = useAdjustStockMutation();

  const isStockSubmitting = isStockInSubmitting || isAdjustSubmitting;

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

    try {
      if (stockAction.type === 'stock-in') {
        await stockIn({
          id: stockAction.item.id,
          data: {
            quantity: Number(stockForm.quantity),
            note: stockForm.note,
          },
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
        await adjustStock({
          id: stockAction.item.id,
          data: {
            quantityDelta,
            note: stockForm.note,
          },
        });
        showSuccessToast('Đã cập nhật kiểm kê');
      }

      setStockAction(null);
      onSaved?.();
    } catch (error) {
      showErrorToast(error);
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
