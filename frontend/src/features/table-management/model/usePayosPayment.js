import { useCallback, useEffect, useRef, useState } from 'react';

import { orderService } from '@features/order-management';
import { paymentService } from '@features/payment';
import { calculatePayosTimeLeft } from '@features/payment/lib/paymentExpiry.js';
import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderSubtotalAmount,
} from '@entities/order/lib/orderMoney.js';
import { printInvoice } from '@entities/order/lib/invoiceGenerator.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { showErrorToast } from '@shared/lib/toast.js';

const usePayosPayment = ({
  isOpen,
  order,
  table,
  currentUser,
  onPaymentSuccess,
  onClose,
  preview,
  paymentDraft,
}) => {
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [payosLoading, setPayosLoading] = useState(false);
  const [payosData, setPayosData] = useState(null);
  const [payosStatus, setPayosStatus] = useState('idle');
  const [timeLeft, setTimeLeft] = useState(0);
  const [payosError, setPayosError] = useState('');
  const finishingRef = useRef(false);
  const paymentSuccessHandledRef = useRef(false);
  const payosSyncingRef = useRef(false);
  const { confirm } = useConfirmModal();

  useEffect(() => {
    if (!isOpen) return undefined;
    const timeout = window.setTimeout(() => {
      setPaymentMethod('CASH');
      setPayosStatus('idle');
      setPayosData(null);
      setPayosError('');
      setTimeLeft(0);
      finishingRef.current = false;
      paymentSuccessHandledRef.current = false;
    }, 0);
    return () => window.clearTimeout(timeout);
  }, [isOpen, order?.id]);

  useEffect(() => {
    if (payosStatus !== 'waiting' || !payosData?.expiresAt) return undefined;

    const updateTimeLeft = () => {
      const remaining = calculatePayosTimeLeft(payosData.expiresAt);
      setTimeLeft(remaining);
      if (remaining <= 0) setPayosStatus('expired');
    };
    updateTimeLeft();
    const interval = window.setInterval(updateTimeLeft, 1000);
    return () => window.clearInterval(interval);
  }, [payosData?.expiresAt, payosStatus]);

  const buildInvoiceOrder = useCallback(
    (latestOrder, method) => {
      const sourceOrder = latestOrder || order;
      return {
        ...sourceOrder,
        subtotalAmount:
          latestOrder?.subtotalAmount ??
          payosData?.subtotalAmount ??
          preview.previewData?.subtotalAmount ??
          getOrderSubtotalAmount(order),
        discountAmount:
          latestOrder?.discountAmount ??
          payosData?.discountAmount ??
          preview.previewData?.discountAmount ??
          getOrderDiscountAmount(order),
        finalAmount:
          latestOrder?.finalAmount ??
          payosData?.finalAmount ??
          payosData?.amount ??
          preview.previewData?.finalAmount ??
          getOrderFinalAmount(order),
        voucherCode: latestOrder?.voucherCode ?? payosData?.voucherCode ?? order?.voucherCode,
        paymentMethod: method || latestOrder?.paymentMethod || paymentMethod,
        paymentStatus: latestOrder?.paymentStatus ?? 'PAID',
        paymentTime: latestOrder?.paymentTime ?? new Date().toISOString(),
        paidByName: latestOrder?.paidByName ?? currentUser?.fullName,
      };
    },
    [currentUser?.fullName, order, paymentMethod, payosData, preview.previewData],
  );

  const finishPayment = useCallback(
    async (methodOverride = paymentMethod) => {
      if (finishingRef.current) return;
      finishingRef.current = true;

      let latestOrder = null;
      try {
        if (order?.id) latestOrder = await orderService.reconcileOrder(order.id);
      } catch (error) {
        console.warn('Could not load latest paid order before printing invoice:', error);
      }

      const invoiceOrder = buildInvoiceOrder(latestOrder, methodOverride);
      printInvoice({
        order: invoiceOrder,
        table,
        paidBy: invoiceOrder.paidByName || currentUser?.fullName || 'Admin',
        paidAt: invoiceOrder.paymentTime || new Date(),
      });
      paymentDraft.clearPaymentDraft();
      onPaymentSuccess();
      onClose();
    },
    [
      buildInvoiceOrder,
      currentUser?.fullName,
      onClose,
      onPaymentSuccess,
      order?.id,
      paymentDraft,
      paymentMethod,
      table,
    ],
  );

  const handlePaymentSuccess = useCallback(() => {
    if (paymentSuccessHandledRef.current) return;
    paymentSuccessHandledRef.current = true;
    setPayosStatus('success');
    setPaymentMethod('PAYOS');
    window.setTimeout(() => finishPayment('PAYOS'), 1500);
  }, [finishPayment]);

  useEffect(() => {
    if (payosStatus !== 'waiting' || !payosData?.transactionId || !order?.id) return undefined;
    let stopped = false;

    const syncPaymentStatus = async () => {
      if (payosSyncingRef.current || finishingRef.current) return;
      payosSyncingRef.current = true;
      try {
        const latestOrder = await orderService.reconcileOrder(order.id);
        if (
          !stopped &&
          (latestOrder?.paymentStatus === 'PAID' || latestOrder?.status === 'COMPLETED')
        ) {
          handlePaymentSuccess();
        }
      } catch (error) {
        console.warn('Could not sync PayOS payment status:', error);
      } finally {
        payosSyncingRef.current = false;
      }
    };

    const firstSync = window.setTimeout(syncPaymentStatus, 1500);
    const interval = window.setInterval(syncPaymentStatus, 3000);
    return () => {
      stopped = true;
      window.clearTimeout(firstSync);
      window.clearInterval(interval);
    };
  }, [handlePaymentSuccess, order?.id, payosData?.transactionId, payosStatus]);

  useWebSocket('/topic/tables', (message) => {
    const eventOrderId = message?.orderId ?? message?.id;
    const eventTransactionId = message?.transactionId ?? message?.data;
    if (
      message?.event === 'PAYMENT_SUCCESS' &&
      (eventOrderId === order?.id || eventTransactionId === payosData?.transactionId)
    ) {
      handlePaymentSuccess();
    }
  });

  const handleCreatePayosQR = useCallback(async () => {
    setPayosLoading(true);
    setPayosError('');
    try {
      const data = await paymentService.createPayment(
        order.id,
        'PAYOS',
        preview.voucherCode || null,
      );
      setPayosData(data);
      setPayosStatus('waiting');
      setPaymentMethod('PAYOS');
      setTimeLeft(calculatePayosTimeLeft(data.expiresAt));

      const nextPreview = {
        ...preview.previewData,
        subtotalAmount:
          data.subtotalAmount ??
          preview.previewData?.subtotalAmount ??
          getOrderSubtotalAmount(order),
        discountAmount:
          data.discountAmount ??
          preview.previewData?.discountAmount ??
          getOrderDiscountAmount(order),
        finalAmount:
          data.finalAmount ??
          data.amount ??
          preview.previewData?.finalAmount ??
          getOrderFinalAmount(order),
        voucherValid: Boolean(data.voucherCode || preview.previewData?.voucherValid),
        voucherMessage: data.voucherCode ? 'ACTIVE' : preview.previewData?.voucherMessage,
      };
      preview.setPreviewData(nextPreview);
      if (data.voucherCode) {
        preview.setVoucher(data.voucherCode);
        paymentDraft.savePaymentDraft(data.voucherCode, nextPreview);
      }
    } catch (error) {
      setPayosError(buildErrorMessage(error, { includeDetails: false }));
      setPayosStatus('error');
    } finally {
      setPayosLoading(false);
    }
  }, [order, paymentDraft, preview]);

  const handleCancelPayos = useCallback(async () => {
    if (!payosData) return;
    try {
      await paymentService.cancelPaymentLink(
        payosData.transactionId,
        'Customer changed payment method',
      );
      setPayosStatus('idle');
      setPayosData(null);
    } catch (error) {
      showErrorToast(error);
    }
  }, [payosData]);

  const handleConfirmCashPay = useCallback(async () => {
    const confirmed = await confirm(
      'Xác nhận thanh toán',
      `Xác nhận thanh toán TIỀN MẶT cho bàn ${table.tableNumber}?`,
    );
    if (!confirmed) return;

    try {
      const voucherCode = preview.voucherCode.trim() || null;
      await paymentService.createPayment(order.id, 'CASH', voucherCode);
      await finishPayment('CASH');
    } catch (error) {
      showErrorToast(error);
    }
  }, [confirm, finishPayment, order?.id, preview.voucherCode, table?.tableNumber]);

  const selectCash = useCallback(() => {
    setPaymentMethod('CASH');
    setPayosStatus('idle');
    setPayosData(null);
  }, []);

  return {
    paymentMethod,
    payosLoading,
    payosData,
    payosStatus,
    timeLeft,
    payosError,
    setPaymentMethod,
    selectCash,
    handleCreatePayosQR,
    handleCancelPayos,
    handleConfirmCashPay,
  };
};

export default usePayosPayment;
