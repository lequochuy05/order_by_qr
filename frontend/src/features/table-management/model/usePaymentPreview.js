import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { orderService } from '@entities/order/api/orderService.js';
import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderSubtotalAmount,
} from '@entities/order/lib/orderMoney.js';
import { translateErrorMessage } from '@shared/lib/errorMessages.js';

const usePaymentPreview = ({ isOpen, order, table, paymentDraft }) => {
  const [voucherCode, setVoucherCode] = useState('');
  const [previewData, setPreviewData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const voucherCodeRef = useRef('');

  const setVoucher = useCallback((value) => {
    voucherCodeRef.current = value;
    setVoucherCode(value);
  }, []);

  const loadPreview = useCallback(
    async (code, { persistDraft = false } = {}) => {
      setLoading(true);
      try {
        const items = order.orderItems
          .filter((item) => item.menuItem)
          .map((item) => ({
            menuItemId: item.menuItem.id,
            quantity: item.quantity,
            notes: item.notes,
            selectedOptionValueIds:
              item.options?.map((option) => option.valueId).filter((id) => id != null) || [],
          }));
        const combos = order.orderItems
          .filter((item) => item.combo)
          .map((item) => ({
            comboId: item.combo.id,
            quantity: item.quantity,
            notes: item.notes,
          }));

        const response = await orderService.previewOrder({
          tableId: table.id,
          items,
          combos,
          voucherCode: code || null,
        });
        setPreviewData(response);

        if (code && !response.voucherValid) {
          paymentDraft.clearPaymentDraft();
          setError(
            response.voucherMessage
              ? translateErrorMessage(response.voucherMessage)
              : 'Voucher không hợp lệ',
          );
        } else {
          setError('');
          if (persistDraft) paymentDraft.savePaymentDraft(code, response);
        }
        if (!code) paymentDraft.clearPaymentDraft();
        return response;
      } catch {
        setError('Lỗi tính toán hóa đơn');
        return null;
      } finally {
        setLoading(false);
      }
    },
    [order, paymentDraft, table?.id],
  );

  const initializePreview = useCallback(() => {
    const draft = paymentDraft.readPaymentDraft();
    const initialVoucher = order?.voucherCode || draft?.voucherCode || '';

    setVoucher(initialVoucher);
    if (draft?.previewData && !order?.voucherCode) {
      setPreviewData(draft.previewData);
    }
    setError('');
    loadPreview(initialVoucher, {
      persistDraft: Boolean(initialVoucher && !order?.voucherCode),
    });
  }, [loadPreview, order?.voucherCode, paymentDraft, setVoucher]);

  useEffect(() => {
    if (!isOpen) return undefined;
    const timeout = window.setTimeout(initializePreview, 0);
    return () => window.clearTimeout(timeout);
  }, [initializePreview, isOpen]);

  const handleInputChange = useCallback(
    (event) => {
      const value = event.target.value;
      setVoucher(value);
      if (value.trim() === '') loadPreview('');
    },
    [loadPreview, setVoucher],
  );

  const handleApplyVoucher = useCallback(() => {
    if (voucherCodeRef.current.trim()) {
      loadPreview(voucherCodeRef.current, { persistDraft: true });
    }
  }, [loadPreview]);

  const amounts = useMemo(() => {
    const previewItemsSubtotal =
      (previewData?.subtotalItems || 0) + (previewData?.subtotalCombos || 0);
    return {
      subtotalAmount:
        previewData?.subtotalAmount ??
        (previewItemsSubtotal > 0 ? previewItemsSubtotal : null) ??
        getOrderSubtotalAmount(order) ??
        0,
      discountAmount: previewData?.discountAmount ?? getOrderDiscountAmount(order) ?? 0,
      finalAmount: previewData?.finalAmount ?? getOrderFinalAmount(order) ?? 0,
    };
  }, [order, previewData]);

  return {
    voucherCode,
    previewData,
    loading,
    error,
    amounts,
    setVoucher,
    setPreviewData,
    handleInputChange,
    handleApplyVoucher,
  };
};

export default usePaymentPreview;
