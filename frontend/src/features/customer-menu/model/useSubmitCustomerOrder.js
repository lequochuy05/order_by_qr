import { useCallback, useRef, useState } from 'react';

import { useSubmitOrderMutation } from '@entities/order';
import { queryClient } from '@shared/api/queryClient.js';

const PENDING_ORDER_REQUEST_KEY = 'qros:pending-order-request';

const createClientRequestId = () =>
  crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

const buildOrderPayload = (tableCode, sessionToken, cart) => ({
  tableCode,
  sessionToken,
  items: Object.entries(cart.items)
    .map(([id, item]) => ({
      menuItemId: item.actualId || parseInt(id),
      quantity: item.qty,
      notes: item.note,
      selectedOptionValueIds: item.selectedOptionValueIds || [],
    }))
    .sort((left, right) => left.menuItemId - right.menuItemId),
  combos: Object.entries(cart.combos)
    .map(([id, combo]) => ({
      comboId: parseInt(id),
      quantity: combo.qty,
      notes: combo.note,
    }))
    .sort((left, right) => left.comboId - right.comboId),
});

const requestFingerprint = (payload) => JSON.stringify(payload);

const loadPendingRequestId = (fingerprint) => {
  try {
    const pending = JSON.parse(globalThis.sessionStorage?.getItem(PENDING_ORDER_REQUEST_KEY) || 'null');
    return pending?.fingerprint === fingerprint ? pending.clientRequestId : null;
  } catch {
    return null;
  }
};

const persistPendingRequest = (fingerprint, clientRequestId) => {
  try {
    globalThis.sessionStorage?.setItem(
      PENDING_ORDER_REQUEST_KEY,
      JSON.stringify({ fingerprint, clientRequestId }),
    );
  } catch {
    // The in-memory ref still preserves retry identity for this page lifetime.
  }
};

const clearPendingRequest = () => {
  try {
    globalThis.sessionStorage?.removeItem(PENDING_ORDER_REQUEST_KEY);
  } catch {
    // Storage may be unavailable in a restricted browser context.
  }
};

const useSubmitCustomerOrder = ({
  tableCode,
  sessionData,
  cart,
  orderingUnavailable,
  orderPaymentLocked,
  restaurantSettings,
  paymentLockedMessage,
  ensureSessionToken,
  clearSessionToken,
  setPaymentInProgress,
  resetCart,
  closeCart,
  showSuccess,
  showError,
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const submittingRef = useRef(false);
  const pendingRequestRef = useRef(null);
  const submitOrderMutation = useSubmitOrderMutation();

  const handleSubmitOrder = useCallback(async () => {
    if (orderPaymentLocked) {
      showError(paymentLockedMessage, 'Bàn đang thanh toán');
      return;
    }
    if (orderingUnavailable) {
      showError(
        restaurantSettings.maintenanceMode
          ? 'Quán đang bảo trì, vui lòng thử lại sau.'
          : 'Quán đang tạm ngưng nhận đơn mới.',
        'Chưa thể đặt món',
      );
      return;
    }
    if (!tableCode) {
      showError('Vui lòng quét mã QR trên bàn để đặt món.', 'Chưa xác định bàn');
      return;
    }
    if (isSubmitting || submittingRef.current) return;
    if (Object.keys(cart.items).length === 0 && Object.keys(cart.combos).length === 0) {
      showError('Giỏ hàng của bạn đang trống. Hãy chọn món trước khi đặt.', 'Chưa có món');
      return;
    }

    submittingRef.current = true;
    setIsSubmitting(true);
    try {
      const activeSessionToken = await ensureSessionToken();
      const payload = buildOrderPayload(tableCode, activeSessionToken, cart);
      const fingerprint = requestFingerprint(payload);
      const clientRequestId =
        pendingRequestRef.current?.fingerprint === fingerprint
          ? pendingRequestRef.current.clientRequestId
          : loadPendingRequestId(fingerprint) || createClientRequestId();
      pendingRequestRef.current = { fingerprint, clientRequestId };
      persistPendingRequest(fingerprint, clientRequestId);

      const createdOrder = await submitOrderMutation.mutateAsync({ ...payload, clientRequestId });
      pendingRequestRef.current = null;
      clearPendingRequest();
      queryClient.setQueryData(
        ['tableSession', tableCode, activeSessionToken],
        (previousSessionData) => ({
          ...(previousSessionData || sessionData || {}),
          currentOrder: createdOrder,
          sessionEnded: false,
          sessionError: null,
        }),
      );
      setPaymentInProgress(false);
      resetCart();
      closeCart();
      showSuccess('Đơn hàng của bạn đã được gửi đến quán.', 'Đặt món thành công');
    } catch (error) {
      const errorMessage = error?.message || '';
      if (
        error?.status === 404 &&
        (errorMessage.includes('thông tin bàn') || errorMessage.includes('Table Code'))
      ) {
        showError(error, 'Không tìm thấy thông tin bàn');
      } else if (
        error?.code === 'TABLE_SESSION_EXPIRED' ||
        error?.code === 'TABLE_SESSION_INVALID'
      ) {
        clearSessionToken();
        showError(error, 'Phiên bàn đã hết hạn');
      } else if (error?.code === 'ORDER_PAYMENT_IN_PROGRESS') {
        setPaymentInProgress(true);
        showError(error, 'Bàn đang thanh toán');
      } else {
        showError(error, 'Không thể gửi đơn hàng');
      }
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  }, [
    cart,
    clearSessionToken,
    closeCart,
    ensureSessionToken,
    isSubmitting,
    orderPaymentLocked,
    orderingUnavailable,
    paymentLockedMessage,
    resetCart,
    restaurantSettings.maintenanceMode,
    sessionData,
    setPaymentInProgress,
    showError,
    showSuccess,
    submitOrderMutation,
    tableCode,
  ]);

  return { isSubmitting, handleSubmitOrder };
};

export default useSubmitCustomerOrder;
