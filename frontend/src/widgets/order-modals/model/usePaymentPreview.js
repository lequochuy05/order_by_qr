import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderServiceChargeAmount,
  getOrderServiceChargePercent,
  getOrderSubtotalAmount,
  getOrderTaxAmount,
  getOrderTaxPercent,
  isBillableOrderItem,
  orderService,
} from '@entities/order';
import { translateErrorMessage } from '@shared/lib/errorMessages.js';

const normalizeVoucherCode = (value) => value.trim().toUpperCase();

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
      const normalizedCode = (code || '').trim();
      const billableOrderItems = (order?.orderItems || []).filter(isBillableOrderItem);

      if (!normalizedCode) {
        setAppliedVoucherCode('');
        setError('');
        paymentDraft.clearPaymentDraft();
      }

      if (billableOrderItems.length === 0) {
        setPreviewData(null);
        return null;
      }

      // Đơn mang về không có bàn (table.id rỗng) — vẫn preview bình thường với tableId = null
      const isTakeaway = !table?.id || order?.orderType === 'TAKEAWAY';

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

      if (items.length === 0 && combos.length === 0) {
        setPreviewData(null);
        return null;
      }

      setLoading(true);
      try {
        const response = await orderService.previewOrder({
          tableId: isTakeaway ? null : table.id,
          ...(isTakeaway ? { orderType: 'TAKEAWAY' } : {}),
          items,
          combos,
          voucherCode: normalizedCode || null,
        });
        setPreviewData(response);

        if (normalizedCode) {
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
        }
        return response;
      } catch {
        if (normalizedCode) {
          setError('Lỗi kiểm tra voucher');
        }
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
      if (
        value.trim() &&
        normalizeVoucherCode(value) !== normalizeVoucherCode(appliedVoucherCode)
      ) {
        setPreviewData((prev) =>
          prev
            ? {
                ...prev,
                voucherValid: false,
                voucherMessage: '',
                discountAmount: prev.discountPromotion ?? 0,
                finalAmount: Math.max(
                  0,
                  (prev.subtotalAmount ?? 0) - (prev.discountPromotion ?? 0),
                ),
              }
            : null,
        );
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
    const subtotalAmount =
      previewData?.subtotalAmount ??
      (previewItemsSubtotal > 0 ? previewItemsSubtotal : null) ??
      getOrderSubtotalAmount(order) ??
      0;
    const discountAmount = previewData?.discountAmount ?? getOrderDiscountAmount(order) ?? 0;
    const discountPromotion =
      previewData?.discountPromotion ??
      (!order?.voucherCode ? (getOrderDiscountAmount(order) ?? 0) : 0);
    const voucherDiscountAmount = Math.max(0, discountAmount - discountPromotion);
    const serviceChargePercent =
      previewData?.serviceChargePercent ?? getOrderServiceChargePercent(order) ?? 0;
    const serviceChargeAmount =
      previewData?.serviceChargeAmount ?? getOrderServiceChargeAmount(order) ?? 0;
    const taxPercent = previewData?.taxPercent ?? getOrderTaxPercent(order) ?? 0;
    const taxAmount = previewData?.taxAmount ?? getOrderTaxAmount(order) ?? 0;
    const finalAmount = previewData?.finalAmount ?? getOrderFinalAmount(order) ?? 0;
    return {
      subtotalAmount,
      discountAmount,
      discountPromotion,
      voucherDiscountAmount,
      serviceChargePercent,
      serviceChargeAmount,
      taxPercent,
      taxAmount,
      finalAmount,
    };
  }, [order, previewData]);

  const hasVoucherInput = voucherCode.trim().length > 0;
  const hasAppliedVoucher = appliedVoucherCode.trim().length > 0 && previewData?.voucherValid;
  const voucherApplied =
    !hasVoucherInput ||
    (hasAppliedVoucher &&
      normalizeVoucherCode(voucherCode) === normalizeVoucherCode(appliedVoucherCode));
  const voucherRequiresApply = hasVoucherInput && !voucherApplied;
  const payableVoucherCode =
    voucherApplied && hasAppliedVoucher
      ? appliedVoucherCode
      : !hasVoucherInput && order?.voucherCode
        ? ''
        : null;

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
