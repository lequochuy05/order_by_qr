const PAYMENT_CREATING_STATUS = 'CREATING';

export const getSelfCheckoutStorageKey = (tableCode, orderId) =>
  tableCode && orderId ? `qros:self-checkout:${tableCode}:${orderId}` : null;

export const VOUCHER_STATUS_ERROR_MESSAGES = Object.freeze({
  NOT_FOUND: 'Mã giảm giá không tồn tại.',
  INACTIVE: 'Mã giảm giá hiện đang tạm ngừng.',
  NOT_YET_ACTIVE: 'Mã giảm giá chưa đến thời gian áp dụng.',
  EXPIRED: 'Mã giảm giá đã hết hạn.',
  EXHAUSTED: 'Mã giảm giá đã hết lượt sử dụng.',
});

export const getVoucherValidationError = (validationResult) => {
  if (validationResult?.applicable) return '';
  return (
    VOUCHER_STATUS_ERROR_MESSAGES[validationResult?.status] ||
    'Mã giảm giá không hợp lệ.'
  );
};

export const getBillableOrderItemLineTotal = (item) => {
  if (!item) return 0;
  if (item.lineTotal != null) return Number(item.lineTotal);
  if (item.combo?.price != null) return Number(item.combo.price) * Number(item.quantity || 0);
  return Number(item.unitPrice || 0) * Number(item.quantity || 0);
};

export const selectBestActivePromotion = (promotions) => {
  if (!Array.isArray(promotions) || promotions.length === 0) return null;

  return promotions.reduce((best, candidate) => {
    if (!candidate || candidate.active === false) return best;
    const candidatePercent = Number(candidate.discountPercent || 0);
    if (!Number.isFinite(candidatePercent) || candidatePercent <= 0) return best;
    if (!best) return candidate;
    return candidatePercent > Number(best.discountPercent || 0) ? candidate : best;
  }, null);
};

export const calculateSelfCheckoutBreakdown = (
  order,
  appliedVoucher = null,
  activePromotion = null,
  payosData = null,
) => {
  const items = order?.items || order?.orderItems || [];
  const computedItemsSubtotal = items
    .filter((item) => item?.status !== 'CANCELLED')
    .reduce((sum, item) => sum + getBillableOrderItemLineTotal(item), 0);

  const subtotalAmount = Math.max(
    0,
    Number(
      payosData?.subtotalAmount ??
        (order?.subtotalAmount != null
          ? Number(order.subtotalAmount)
          : computedItemsSubtotal > 0
            ? computedItemsSubtotal
            : Number(order?.finalAmount ?? 0)),
    ),
  );

  const promotionPercent = Number(activePromotion?.discountPercent || 0);
  const promotionName = activePromotion?.name || '';
  const livePromoDiscount =
    promotionPercent > 0
      ? Math.min(subtotalAmount, Math.round((subtotalAmount * promotionPercent) / 100))
      : 0;
  const storedPromoDiscount = !order?.voucherCode
    ? Math.max(0, Number(order?.discountAmount ?? 0))
    : livePromoDiscount;

  let promotionDiscountAmount = Math.min(
    subtotalAmount,
    Math.max(storedPromoDiscount, livePromoDiscount),
  );

  let voucherDiscountAmount = 0;
  if (appliedVoucher?.applicable) {
    voucherDiscountAmount = Math.max(0, Number(appliedVoucher.appliedDiscountAmount ?? 0));
  } else if (order?.voucherCode) {
    voucherDiscountAmount = Math.max(
      0,
      Number(order.discountAmount ?? 0) - promotionDiscountAmount,
    );
  }

  const hasPayosAmounts =
    payosData &&
    (payosData.discountAmount != null ||
      payosData.finalAmount != null ||
      payosData.amount != null);

  if (hasPayosAmounts) {
    const totalDiscount = Math.min(
      subtotalAmount,
      Math.max(
        0,
        Number(payosData.discountAmount ?? promotionDiscountAmount + voucherDiscountAmount),
      ),
    );
    const finalAmount = Math.max(
      0,
      Number(payosData.finalAmount ?? payosData.amount ?? subtotalAmount - totalDiscount),
    );

    if (!payosData.voucherCode && !appliedVoucher?.applicable) {
      promotionDiscountAmount = totalDiscount;
      voucherDiscountAmount = 0;
    } else {
      promotionDiscountAmount = Math.min(totalDiscount, promotionDiscountAmount);
      voucherDiscountAmount = Math.max(0, totalDiscount - promotionDiscountAmount);
    }

    return {
      subtotalAmount,
      discountAmount: totalDiscount,
      promotionDiscountAmount,
      voucherDiscountAmount,
      promotionName,
      promotionPercent,
      finalAmount,
    };
  }

  const discountAmount = Math.min(subtotalAmount, promotionDiscountAmount + voucherDiscountAmount);
  voucherDiscountAmount = Math.max(0, discountAmount - promotionDiscountAmount);
  const finalAmount = Math.max(0, subtotalAmount - discountAmount);

  return {
    subtotalAmount,
    discountAmount,
    promotionDiscountAmount,
    voucherDiscountAmount,
    promotionName,
    promotionPercent,
    finalAmount,
  };
};

export const calculateSelfCheckoutSummary = (order, appliedVoucher = null) => {
  const items = order?.items || order?.orderItems || [];
  const computedItemsSubtotal = items
    .filter((item) => item?.status !== 'CANCELLED')
    .reduce((sum, item) => sum + getBillableOrderItemLineTotal(item), 0);

  const subtotalAmount =
    order?.subtotalAmount != null
      ? Number(order.subtotalAmount)
      : computedItemsSubtotal > 0
        ? computedItemsSubtotal
        : Number(order?.finalAmount ?? 0);

  const basePromotionDiscount = !order?.voucherCode
    ? Math.max(0, Number(order?.discountAmount ?? 0))
    : 0;

  if (appliedVoucher?.applicable) {
    const voucherDiscount = Math.max(0, Number(appliedVoucher.appliedDiscountAmount ?? 0));
    const discountAmount = Math.min(subtotalAmount, basePromotionDiscount + voucherDiscount);
    const finalAmount =
      basePromotionDiscount === 0 && appliedVoucher.finalAmount != null
        ? Math.max(0, Number(appliedVoucher.finalAmount))
        : Math.max(0, subtotalAmount - discountAmount);
    return { subtotalAmount, discountAmount, finalAmount };
  }

  if (order?.voucherCode || Number(order?.discountAmount ?? 0) > 0) {
    const discountAmount = Math.min(subtotalAmount, Math.max(0, Number(order?.discountAmount ?? 0)));
    const finalAmount =
      order.finalAmount != null
        ? Math.max(0, Number(order.finalAmount))
        : Math.max(0, subtotalAmount - discountAmount);
    return { subtotalAmount, discountAmount, finalAmount };
  }

  return {
    subtotalAmount,
    discountAmount: 0,
    finalAmount: Math.max(0, subtotalAmount),
  };
};

export const hasUnfinishedOrderItems = (order) => {
  const items = order?.items || order?.orderItems || [];
  return items.some(
    (item) => item && item.status !== 'FINISHED' && item.status !== 'CANCELLED',
  );
};

export const canCustomerRequestPayment = (order) => {
  const items = order?.items || order?.orderItems || [];
  const billableItems = items.filter((item) => item && item.status !== 'CANCELLED');
  return billableItems.length > 0 && billableItems.every((item) => item.status === 'FINISHED');
};

export const isCustomerSelfCheckoutAvailable = (restaurantSettings) =>
  Boolean(restaurantSettings?.customerSelfCheckoutEnabled) &&
  restaurantSettings?.onlinePaymentEnabled !== false;

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

export const createSelfCheckoutWithRetry = async ({
  createFn,
  tableCode,
  sessionToken,
  voucherCode = null,
  idempotencyKey,
  waitForRetry = wait,
}) => {
  const maxAttempts = 5;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const response = await createFn(
      tableCode,
      {
        sessionToken,
        voucherCode: voucherCode || null,
        idempotencyKey,
      },
      sessionToken,
    );

    if (
      response?.status !== PAYMENT_CREATING_STATUS ||
      attempt === maxAttempts
    ) {
      return response;
    }
    await waitForRetry(400);
  }

  return null;
};
