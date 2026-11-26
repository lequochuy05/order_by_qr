import { useCallback, useEffect, useRef, useState } from 'react';

import {
  ORDER_PAYMENT_STATUSES,
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderServiceChargeAmount,
  getOrderServiceChargePercent,
  getOrderSubtotalAmount,
  getOrderTaxAmount,
  getOrderTaxPercent,
  orderService,
  printInvoice,
} from '@entities/order';
import {
  PAYMENT_METHODS,
  PAYMENT_TRANSACTION_STATUSES,
  calculatePayosTimeLeft,
  paymentService,
  usePayosTransactionMonitor,
} from '@entities/payment';
import { VOUCHER_STATUSES } from '@entities/voucher';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { showBrowserNotification } from '@shared/lib/browserNotification.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { playNotificationSound } from '@shared/lib/notificationSound.js';
import { showErrorToast } from '@shared/lib/toast.js';
import { useSettingsStore } from '@entities/settings';
import useCashPayment from './useCashPayment.js';

const PAYOS_PRINT_DELAY_MS = 300;
const PAYOS_FIRST_SYNC_DELAY_MS = 2500;
const PAYOS_SYNC_INTERVAL_MS = 8000;

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
  const { confirm } = useConfirmModal();
  const cashPaymentEnabled = settings.cashPaymentEnabled !== false;
  const onlinePaymentEnabled = settings.onlinePaymentEnabled !== false;
  const defaultPaymentMethod = cashPaymentEnabled ? PAYMENT_METHODS.CASH : PAYMENT_METHODS.PAYOS;
  const [paymentMethod, setPaymentMethod] = useState(defaultPaymentMethod);
  const [payosLoading, setPayosLoading] = useState(false);
  const [payosData, setPayosData] = useState(null);
  const [payosStatus, setPayosStatus] = useState('idle');
  const [payosError, setPayosError] = useState('');
  const finishingRef = useRef(false);
  const paymentSuccessHandledRef = useRef(false);

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
        return;
      }

      setPaymentMethod(defaultPaymentMethod);
      setPayosStatus('idle');
      setPayosData(null);
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
        serviceChargePercent:
          latestOrder?.serviceChargePercent ??
          preview.previewData?.serviceChargePercent ??
          getOrderServiceChargePercent(order),
        serviceChargeAmount:
          latestOrder?.serviceChargeAmount ??
          payosData?.serviceChargeAmount ??
          preview.previewData?.serviceChargeAmount ??
          getOrderServiceChargeAmount(order),
        taxPercent:
          latestOrder?.taxPercent ??
          preview.previewData?.taxPercent ??
          getOrderTaxPercent(order),
        taxAmount:
          latestOrder?.taxAmount ??
          payosData?.taxAmount ??
          preview.previewData?.taxAmount ??
          getOrderTaxAmount(order),
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
          settings,
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
      settings,
      table,
    ],
  );

  const handlePaymentSuccess = useCallback(
    (latestOrder = null) => {
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
    },
    [finishPayment, order?.id, settings.paymentNotificationEnabled, table?.tableNumber],
  );

  const handlePayosExpired = useCallback(() => {
    setPayosStatus('expired');
  }, []);

  const handleRemoteCancelled = useCallback(() => {
    setPayosStatus('idle');
    setPayosData(null);
    paymentDraft.clearPayosDraft();
  }, [paymentDraft]);

  usePayosTransactionMonitor({
    isWaiting: payosStatus === 'waiting' && Boolean(order?.id),
    subscribeWebSocket: Boolean(isOpen),
    orderId: order?.id,
    transactionId: payosData?.transactionId,
    wsScope: 'admin',
    firstSyncDelayMs: PAYOS_FIRST_SYNC_DELAY_MS,
    syncIntervalMs: PAYOS_SYNC_INTERVAL_MS,
    syncTransaction: (txId) => paymentService.syncPaymentStatus(txId),
    shouldSkipSync: () => finishingRef.current,
    onPaid: handlePaymentSuccess,
    onExpired: handlePayosExpired,
    onCancelled: handleRemoteCancelled,
    onSyncError: (error) => console.warn('Could not sync PayOS payment status:', error),
  });

  const handleCreatePayosQR = useCallback(async () => {
    if (!onlinePaymentEnabled) return;
    if (preview.voucherRequiresApply) {
      setPayosError('Vui lòng áp dụng hoặc xóa mã voucher trước khi tạo QR.');
      setPayosStatus('error');
      return;
    }

    const orderItems = order?.orderItems || order?.items || [];
    const unfinished = orderItems.filter(
      (item) => item.status === 'PENDING' || item.status === 'COOKING',
    );
    let confirmUnfinishedItems = false;
    if (unfinished.length > 0) {
      const targetLabel =
        table?.displayName || (table?.tableNumber ? `Bàn ${table.tableNumber}` : 'Đơn mang về');
      const unfinishedNames = unfinished
        .map((i) => i.itemNameSnapshot || i.menuItem?.name || 'Món')
        .slice(0, 3)
        .join(', ');
      const suffix = unfinished.length > 3 ? ` và ${unfinished.length - 3} món khác` : '';
      const proceed = await confirm(
        'Bếp vẫn đang chế biến!',
        `${targetLabel} còn ${unfinished.length} món chưa hoàn thành (${unfinishedNames}${suffix}).\n\nBạn có chắc chắn muốn tạo mã thanh toán trước không?`,
      );
      if (!proceed) return;
      confirmUnfinishedItems = true;
    }

    setPayosLoading(true);
    setPayosError('');
    try {
      const data = await paymentService.createPayment(
        order.id,
        PAYMENT_METHODS.PAYOS,
        preview.payableVoucherCode,
        { confirmUnfinishedItems },
      );

      if (data?.status === PAYMENT_TRANSACTION_STATUSES.PAID) {
        handlePaymentSuccess({
          ...order,
          id: data.orderId ?? order.id,
          subtotalAmount: data.subtotalAmount ?? preview.amounts?.subtotalAmount ?? order.subtotalAmount,
          discountAmount: data.discountAmount ?? preview.amounts?.discountAmount ?? order.discountAmount,
          serviceChargePercent: preview.amounts?.serviceChargePercent ?? order.serviceChargePercent ?? 0,
          serviceChargeAmount: data.serviceChargeAmount ?? preview.amounts?.serviceChargeAmount ?? order.serviceChargeAmount ?? 0,
          taxPercent: preview.amounts?.taxPercent ?? order.taxPercent ?? 0,
          taxAmount: data.taxAmount ?? preview.amounts?.taxAmount ?? order.taxAmount ?? 0,
          finalAmount: data.finalAmount ?? data.amount ?? 0,
          voucherCode: data.voucherCode ?? preview.payableVoucherCode ?? order.voucherCode,
          paymentMethod: PAYMENT_METHODS.PAYOS,
          paymentStatus: ORDER_PAYMENT_STATUSES.PAID,
          paymentTime: data.createdAt ?? new Date().toISOString(),
        });
        return;
      }

      setPayosData(data);
      setPayosStatus(calculatePayosTimeLeft(data.expiresAt) > 0 ? 'waiting' : 'expired');
      setPaymentMethod(PAYMENT_METHODS.PAYOS);
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
        serviceChargeAmount:
          data.serviceChargeAmount ??
          preview.previewData?.serviceChargeAmount ??
          getOrderServiceChargeAmount(order),
        taxAmount:
          data.taxAmount ??
          preview.previewData?.taxAmount ??
          getOrderTaxAmount(order),
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
  }, [confirm, handlePaymentSuccess, onlinePaymentEnabled, order, paymentDraft, preview, table?.displayName, table?.tableNumber]);

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

  const effectiveFinalAmount =
    preview.previewData?.finalAmount ??
    payosData?.finalAmount ??
    payosData?.amount ??
    preview.amounts.finalAmount ??
    getOrderFinalAmount(order);

  const {
    cashReceived,
    setCashReceived,
    changeAmount,
    isInsufficient,
    hasUnfinishedItems,
    isSubmittingPayment,
    handleConfirmCashPay,
  } = useCashPayment({
    cashPaymentEnabled,
    order,
    preview,
    table,
    finishPayment,
    finalAmount: effectiveFinalAmount,
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
    payosError,
    cashPaymentEnabled,
    onlinePaymentEnabled,
    cashReceived,
    setCashReceived,
    changeAmount,
    isInsufficient,
    hasUnfinishedItems,
    isSubmittingPayment,
    setPaymentMethod,
    selectCash,
    handleCreatePayosQR,
    handleCancelPayos,
    handleConfirmCashPay,
    handlePayosExpired,
  };
};

export default usePayosPayment;
