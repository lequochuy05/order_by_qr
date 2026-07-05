import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { orderService } from '@entities/order/api/orderService.js';
import { ORDER_ITEM_STATUSES } from '@entities/order/lib/orderStatus.js';
import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderSubtotalAmount,
} from '@entities/order/lib/orderMoney.js';
import { translateErrorMessage } from '@shared/lib/errorMessages.js';

const normalizeVoucherCode = (value) => value.trim().toUpperCase();

const isBillableOrderItem = (item) => item?.status !== ORDER_ITEM_STATUSES.CANCELLED;

const usePaymentPreview = ({ isOpen, order, table, paymentDraft }) => {
  const [voucherCode, setVoucherCode] = useState('');
  const [appliedVoucherCode, setAppliedVoucherCode] = useState('');
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
      const normalizedCode = code.trim();

      // Đơn đã tồn tại — dùng số tiền từ order, không cần preview lại
      if (!normalizedCode) {
        setPreviewData(null);
        setAppliedVoucherCode('');
        setError('');
        paymentDraft.clearPaymentDraft();
        return null;
      }

      // Chỉ gọi preview khi có voucher để kiểm tra
      if (!table?.id) {
        setError('Không thể áp dụng voucher khi thiếu thông tin bàn');
        return null;
      }

      setLoading(true);
      try {
        const billableOrderItems = order.orderItems.filter(isBillableOrderItem);
        const items = billableOrderItems
          .filter((item) => item.menuItem)
          .map((item) => ({
            menuItemId: item.menuItem.id,
            quantity: item.quantity,
            notes: item.notes,
            selectedOptionValueIds:
              item.options?.map((option) => option.valueId).filter((id) => id != null) || [],
          }));
        const combos = billableOrderItems
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
          voucherCode: normalizedCode,
        });
        setPreviewData(response);

        if (!response.voucherValid) {
          setAppliedVoucherCode('');
          paymentDraft.clearPaymentDraft();
          setError(
            response.voucherMessage
              ? translateErrorMessage(response.voucherMessage)
              : 'Voucher không hợp lệ',
          );
        } else {
          setAppliedVoucherCode(normalizedCode);
          setError('');
          if (persistDraft) paymentDraft.savePaymentDraft(normalizedCode, response);
        }
        return response;
      } catch {
        setError('Lỗi kiểm tra voucher');
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
      if (draft.previewData?.voucherValid) setAppliedVoucherCode(draft.voucherCode || '');
    }
    if (initialVoucher) setAppliedVoucherCode(initialVoucher);
    if (!initialVoucher) {
      setPreviewData(null);
      setAppliedVoucherCode('');
    }
    setError('');
    // Không cần gọi preview cho đơn đã tồn tại — số tiền có sẵn trong order
    if (initialVoucher) {
      loadPreview(initialVoucher, {
        persistDraft: Boolean(!order?.voucherCode),
      });
    }
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
      if (
        value.trim() &&
        normalizeVoucherCode(value) !== normalizeVoucherCode(appliedVoucherCode)
      ) {
        setPreviewData(null);
        paymentDraft.clearPaymentDraft();
        setError('');
      }
    },
    [appliedVoucherCode, loadPreview, paymentDraft, setVoucher],
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

  const hasVoucherInput = voucherCode.trim().length > 0;
  const hasAppliedVoucher = appliedVoucherCode.trim().length > 0 && previewData?.voucherValid;
  const voucherApplied =
    !hasVoucherInput ||
    (hasAppliedVoucher &&
      normalizeVoucherCode(voucherCode) === normalizeVoucherCode(appliedVoucherCode));
  const voucherRequiresApply = hasVoucherInput && !voucherApplied;
  const payableVoucherCode = voucherApplied && hasAppliedVoucher ? appliedVoucherCode : null;

  return {
    voucherCode,
    appliedVoucherCode,
    previewData,
    loading,
    error,
    amounts,
    voucherApplied,
    voucherRequiresApply,
    payableVoucherCode,
    setVoucher,
    setAppliedVoucherCode,
    setPreviewData,
    handleInputChange,
    handleApplyVoucher,
  };
};

export default usePaymentPreview;
