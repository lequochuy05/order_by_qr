import { useCallback, useEffect, useRef, useState } from 'react';

import { orderService } from '@entities/order/api/orderService.js';
import { paymentService } from '@entities/payment/api/paymentService.js';
import { VOUCHER_STATUSES } from '@entities/voucher/lib/voucherStatus.js';
import { calculatePayosTimeLeft } from '@entities/payment/lib/paymentExpiry.js';
import { PAYMENT_METHODS, PAYMENT_TRANSACTION_STATUSES } from '@entities/payment/lib/paymentStatus.js';
import { ORDER_PAYMENT_STATUSES } from '@entities/order/lib/orderStatus.js';
import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderSubtotalAmount,
} from '@entities/order/lib/orderMoney.js';
import { printInvoice } from '@entities/order/lib/invoiceGenerator.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { showBrowserNotification } from '@shared/lib/browserNotification.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { playNotificationSound } from '@shared/lib/notificationSound.js';
import { showErrorToast } from '@shared/lib/toast.js';
import useSettingsStore from '@shared/model/settingsStore.js';
import usePayosTimer from './usePayosTimer.js';
import useCashPayment from './useCashPayment.js';

const PAYOS_PRINT_DELAY_MS = 300;
const PAYOS_FIRST_SYNC_DELAY_MS = 2500;
const PAYOS_SYNC_INTERVAL_MS = 8000;

const sameId = (left, right) =>
  left != null && right != null && String(left) === String(right);

const getPaymentEventOrderId = (message) =>
  message?.orderId ?? message?.id ?? message?.data?.orderId;

const getPaymentEventTransactionId = (message) =>
  message?.transactionId ?? message?.data?.transactionId;

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
  const settings = useSettingsStore((state) => state.settings);
  const cashPaymentEnabled = settings.cashPaymentEnabled !== false;
  const onlinePaymentEnabled = settings.onlinePaymentEnabled !== false;
  const defaultPaymentMethod = cashPaymentEnabled ? PAYMENT_METHODS.CASH : PAYMENT_METHODS.PAYOS;
  const [paymentMethod, setPaymentMethod] = useState(defaultPaymentMethod);
  const [payosLoading, setPayosLoading] = useState(false);
  const [payosData, setPayosData] = useState(null);
  const [payosStatus, setPayosStatus] = useState('idle');
  const [timeLeft, setTimeLeft] = useState(0);
  const [payosError, setPayosError] = useState('');
  const finishingRef = useRef(false);
  const paymentSuccessHandledRef = useRef(false);
  const payosSyncingRef = useRef(false);

  useEffect(() => {
    if (!isOpen) return undefined;
    const timeout = window.setTimeout(() => {
      const draft = paymentDraft.readPaymentDraft();
      const savedPayosData = onlinePaymentEnabled ? draft?.payosData : null;

      setPayosError('');
      finishingRef.current = false;
      paymentSuccessHandledRef.current = false;

      if (savedPayosData?.transactionId && savedPayosData?.qrCode) {
        const restoredTimeLeft = calculatePayosTimeLeft(savedPayosData.expiresAt);
        setPaymentMethod(PAYMENT_METHODS.PAYOS);
        setPayosData(savedPayosData);
        setPayosStatus(restoredTimeLeft > 0 ? 'waiting' : 'expired');
        setTimeLeft(restoredTimeLeft);
        return;
      }

      setPaymentMethod(defaultPaymentMethod);
      setPayosStatus('idle');
      setPayosData(null);
      setTimeLeft(0);
    }, 0);
    return () => window.clearTimeout(timeout);
  }, [defaultPaymentMethod, isOpen, onlinePaymentEnabled, order?.id, paymentDraft]);

  useEffect(() => {
    if (paymentMethod === PAYMENT_METHODS.CASH && !cashPaymentEnabled && onlinePaymentEnabled) {
      setPaymentMethod(PAYMENT_METHODS.PAYOS);
    } else if (paymentMethod === PAYMENT_METHODS.PAYOS && !onlinePaymentEnabled && cashPaymentEnabled) {
      setPaymentMethod(PAYMENT_METHODS.CASH);
    }
  }, [cashPaymentEnabled, onlinePaymentEnabled, paymentMethod]);

  usePayosTimer({
    payosStatus,
    expiresAt: payosData?.expiresAt,
    setTimeLeft,
    setPayosStatus,
  });

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
        paymentStatus: latestOrder?.paymentStatus ?? ORDER_PAYMENT_STATUSES.PAID,
        paymentTime: latestOrder?.paymentTime ?? new Date().toISOString(),
        paidByName: latestOrder?.paidByName ?? currentUser?.fullName,
      };
    },
    [currentUser?.fullName, order, paymentMethod, payosData, preview.previewData],
  );

  const finishPayment = useCallback(
    async (methodOverride = paymentMethod, latestOrder = null, { skipReconcile = false } = {}) => {
      if (finishingRef.current) return;
      finishingRef.current = true;

      let invoiceSourceOrder = latestOrder;
      if (!invoiceSourceOrder && !skipReconcile) {
        try {
          if (order?.id) invoiceSourceOrder = await orderService.reconcileOrder(order.id);
        } catch (error) {
          console.warn('Could not load latest paid order before printing invoice:', error);
        }
      }

      const invoiceOrder = buildInvoiceOrder(invoiceSourceOrder, methodOverride);
      if (settings.autoPrintBill !== false) {
        printInvoice({
          order: invoiceOrder,
          table,
          paidBy: invoiceOrder.paidByName || currentUser?.fullName || 'Admin',
          paidAt: invoiceOrder.paymentTime || new Date(),
        });
      }
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
      settings.autoPrintBill,
      table,
    ],
  );

  const handlePaymentSuccess = useCallback((latestOrder = null) => {
    if (paymentSuccessHandledRef.current) return;
    paymentSuccessHandledRef.current = true;
    setPayosStatus('success');
    setPaymentMethod(PAYMENT_METHODS.PAYOS);
    if (settings.paymentNotificationEnabled !== false) {
      playNotificationSound();
      showBrowserNotification('Thanh toán thành công', {
        body: `Đơn bàn ${table?.tableNumber || ''} đã được PayOS xác nhận.`,
        tag: `payment-${order?.id}`,
      });
    }
    window.setTimeout(
      () => finishPayment(PAYMENT_METHODS.PAYOS, latestOrder, { skipReconcile: true }),
      PAYOS_PRINT_DELAY_MS,
    );
  }, [finishPayment, order?.id, settings.paymentNotificationEnabled, table?.tableNumber]);

  useEffect(() => {
    if (payosStatus !== 'waiting' || !payosData?.transactionId || !order?.id) return undefined;
    let stopped = false;

    const syncPaymentStatus = async () => {
      if (payosSyncingRef.current || finishingRef.current) return;
      payosSyncingRef.current = true;
      try {
        const latestTransaction = await paymentService.syncPaymentStatus(payosData.transactionId);
        if (!stopped && latestTransaction?.status === PAYMENT_TRANSACTION_STATUSES.PAID) {
          handlePaymentSuccess();
        } else if (!stopped && latestTransaction?.status === PAYMENT_TRANSACTION_STATUSES.EXPIRED) {
          setPayosStatus('expired');
        } else if (!stopped && latestTransaction?.status === PAYMENT_TRANSACTION_STATUSES.CANCELLED) {
          setPayosStatus('idle');
          setPayosData(null);
          paymentDraft.clearPayosDraft();
        }
      } catch (error) {
        console.warn('Could not sync PayOS payment status:', error);
      } finally {
        payosSyncingRef.current = false;
      }
    };

    const firstSync = window.setTimeout(syncPaymentStatus, PAYOS_FIRST_SYNC_DELAY_MS);
    const interval = window.setInterval(syncPaymentStatus, PAYOS_SYNC_INTERVAL_MS);
    return () => {
      stopped = true;
      window.clearTimeout(firstSync);
      window.clearInterval(interval);
    };
  }, [handlePaymentSuccess, order?.id, paymentDraft, payosData?.transactionId, payosStatus]);

  useWebSocket(
    '/topic/tables',
    (message) => {
      const eventOrderId = getPaymentEventOrderId(message);
      const eventTransactionId = getPaymentEventTransactionId(message);
      if (
        message?.event === 'PAYMENT_SUCCESS' &&
        (sameId(eventOrderId, order?.id) || sameId(eventTransactionId, payosData?.transactionId))
      ) {
        handlePaymentSuccess();
      }
    },
    { scope: 'admin' },
  );

  const handleCreatePayosQR = useCallback(async () => {
    if (!onlinePaymentEnabled) return;
    if (preview.voucherRequiresApply) {
      setPayosError('Vui lòng áp dụng hoặc xóa mã voucher trước khi tạo QR.');
      setPayosStatus('error');
      return;
    }
    setPayosLoading(true);
    setPayosError('');
    try {
      const data = await paymentService.createPayment(
        order.id,
        PAYMENT_METHODS.PAYOS,
        preview.payableVoucherCode,
      );
      setPayosData(data);
      setPayosStatus('waiting');
      setPaymentMethod(PAYMENT_METHODS.PAYOS);
      setTimeLeft(calculatePayosTimeLeft(data.expiresAt));
      paymentDraft.savePayosDraft(data);

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
        voucherMessage: data.voucherCode
          ? VOUCHER_STATUSES.ACTIVE
          : preview.previewData?.voucherMessage,
      };
      preview.setPreviewData(nextPreview);
      if (data.voucherCode) {
        preview.setVoucher(data.voucherCode);
        preview.setAppliedVoucherCode(data.voucherCode);
        paymentDraft.savePaymentDraft(data.voucherCode, nextPreview);
      }
    } catch (error) {
      setPayosError(buildErrorMessage(error, { includeDetails: false }));
      setPayosStatus('error');
    } finally {
      setPayosLoading(false);
    }
  }, [onlinePaymentEnabled, order, paymentDraft, preview]);

  const handleCancelPayos = useCallback(async () => {
    if (!payosData) return;
    try {
      await paymentService.cancelPaymentLink(
        payosData.transactionId,
        'Customer changed payment method',
      );
      setPayosStatus('idle');
      setPayosData(null);
      paymentDraft.clearPayosDraft();
    } catch (error) {
      showErrorToast(error);
    }
  }, [paymentDraft, payosData]);

  const { handleConfirmCashPay } = useCashPayment({
    cashPaymentEnabled,
    order,
    preview,
    table,
    finishPayment,
  });

  const selectCash = useCallback(() => {
    if (!cashPaymentEnabled) return;
    if (payosStatus === 'waiting') return;
    setPaymentMethod(PAYMENT_METHODS.CASH);
    setPayosStatus('idle');
    setPayosData(null);
    paymentDraft.clearPayosDraft();
  }, [cashPaymentEnabled, paymentDraft, payosStatus]);

  return {
    paymentMethod,
    payosLoading,
    payosData,
    payosStatus,
    timeLeft,
    payosError,
    cashPaymentEnabled,
    onlinePaymentEnabled,
    setPaymentMethod,
    selectCash,
    handleCreatePayosQR,
    handleCancelPayos,
    handleConfirmCashPay,
  };
};

export default usePayosPayment;
