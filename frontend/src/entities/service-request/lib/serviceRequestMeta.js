export const SERVICE_REQUEST_SLA_MS = 5 * 60 * 1000;
export const SERVICE_REQUEST_COOLDOWN_SECONDS = 60;
export const MAX_OPEN_SERVICE_REQUESTS = 3;
export const MIN_OTHER_NOTE_LENGTH = 2;
export const MAX_SERVICE_REQUEST_NOTE_LENGTH = 200;

export const SERVICE_REQUEST_STATUSES = Object.freeze({
  PENDING: 'PENDING',
  CLAIMED: 'CLAIMED',
  RESOLVED: 'RESOLVED',
  CANCELLED: 'CANCELLED',
});

export const SERVICE_REQUEST_REASONS = Object.freeze([
  {
    code: 'WATER_ICE',
    icon: 'Droplets',
    labelVi: 'Thêm nước / Đá',
    labelEn: 'Water / Ice',
  },
  {
    code: 'UTENSILS_NAPKINS',
    icon: 'Utensils',
    labelVi: 'Chén đũa / Khăn giấy',
    labelEn: 'Utensils / Napkins',
  },
  {
    code: 'CLEAN_TABLE',
    icon: 'Sparkles',
    labelVi: 'Dọn bàn',
    labelEn: 'Clean table',
  },
  {
    code: 'ORDER_ASSISTANCE',
    icon: 'HandHelping',
    labelVi: 'Hỗ trợ gọi món',
    labelEn: 'Order assistance',
  },
  {
    code: 'REQUEST_BILL',
    icon: 'ReceiptText',
    labelVi: 'Gọi thanh toán',
    labelEn: 'Request bill',
  },
  {
    code: 'OTHER',
    icon: 'MessageSquarePlus',
    labelVi: 'Khác',
    labelEn: 'Other',
  },
]);

export const SERVICE_REQUEST_REASON_MAP = Object.freeze(
  Object.fromEntries(SERVICE_REQUEST_REASONS.map((reason) => [reason.code, reason])),
);

export const SERVICE_REQUEST_STATUS_META = Object.freeze({
  [SERVICE_REQUEST_STATUSES.PENDING]: {
    color: 'amber',
    labelVi: 'Chờ xử lý',
    labelEn: 'Pending',
  },
  [SERVICE_REQUEST_STATUSES.CLAIMED]: {
    color: 'indigo',
    labelVi: 'Đang xử lý',
    labelEn: 'Claimed',
  },
  [SERVICE_REQUEST_STATUSES.RESOLVED]: {
    color: 'emerald',
    labelVi: 'Đã xong',
    labelEn: 'Resolved',
  },
  [SERVICE_REQUEST_STATUSES.CANCELLED]: {
    color: 'slate',
    labelVi: 'Đã hủy',
    labelEn: 'Cancelled',
  },
});

export const getServiceRequestReasonMeta = (reasonCode, language = 'vi') => {
  const meta = SERVICE_REQUEST_REASON_MAP[reasonCode];
  if (!meta) {
    return {
      code: reasonCode,
      icon: 'BellRing',
      label: reasonCode || (language === 'en' ? 'Assistance' : 'Hỗ trợ'),
    };
  }
  return {
    ...meta,
    label: language === 'en' ? meta.labelEn : meta.labelVi,
  };
};

export const getServiceRequestStatusMeta = (status, language = 'vi') => {
  const meta = SERVICE_REQUEST_STATUS_META[status] || {
    color: 'slate',
    labelVi: status || 'Không rõ',
    labelEn: status || 'Unknown',
  };
  return {
    color: meta.color,
    label: language === 'en' ? meta.labelEn : meta.labelVi,
  };
};

export const parseServiceRequestTimestamp = (value) => {
  if (!value) return null;
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? null : parsed;
};

export const isServiceRequestOverdue = (request, nowMs = Date.now()) => {
  if (!request) return false;
  if (
    request.status !== SERVICE_REQUEST_STATUSES.PENDING &&
    request.status !== SERVICE_REQUEST_STATUSES.CLAIMED
  ) {
    return false;
  }
  const createdMs = parseServiceRequestTimestamp(request.createdAt);
  if (createdMs == null) return false;
  return nowMs - createdMs >= SERVICE_REQUEST_SLA_MS;
};

export const getServiceRequestWaitMinutes = (request, nowMs = Date.now()) => {
  const createdMs = parseServiceRequestTimestamp(request?.createdAt);
  if (createdMs == null) return 0;
  return Math.max(0, Math.floor((nowMs - createdMs) / 60000));
};

export const validateServiceRequestInput = ({ reason, note } = {}, language = 'vi') => {
  if (!reason || !SERVICE_REQUEST_REASON_MAP[reason]) {
    return {
      valid: false,
      error:
        language === 'en'
          ? 'Please select a service request reason.'
          : 'Vui lòng chọn lý do cần hỗ trợ.',
    };
  }

  const trimmedNote = typeof note === 'string' ? note.trim() : '';
  if (trimmedNote.length > MAX_SERVICE_REQUEST_NOTE_LENGTH) {
    return {
      valid: false,
      error:
        language === 'en'
          ? `Note cannot exceed ${MAX_SERVICE_REQUEST_NOTE_LENGTH} characters.`
          : `Ghi chú không được vượt quá ${MAX_SERVICE_REQUEST_NOTE_LENGTH} ký tự.`,
    };
  }

  if (reason === 'OTHER' && trimmedNote.length < MIN_OTHER_NOTE_LENGTH) {
    return {
      valid: false,
      error:
        language === 'en'
          ? 'Please enter at least 2 characters when selecting Other.'
          : 'Vui lòng nhập ghi chú tối thiểu 2 ký tự khi chọn lý do Khác.',
    };
  }

  return { valid: true, error: null, trimmedNote };
};

export const computeCooldownRemaining = (cooldownEndsAtMs, nowMs = Date.now()) => {
  if (!cooldownEndsAtMs || cooldownEndsAtMs <= nowMs) return 0;
  return Math.ceil((cooldownEndsAtMs - nowMs) / 1000);
};

export const sortServiceRequestQueue = (requests = [], statusFilter = 'OPEN') => {
  const items = Array.isArray(requests) ? [...requests] : [];
  const normalized = String(statusFilter || 'OPEN').toUpperCase();
  const isHistory =
    normalized === 'RESOLVED' || normalized === 'CANCELLED' || normalized === 'ALL';

  return items.sort((a, b) => {
    const aMs = parseServiceRequestTimestamp(a?.createdAt) ?? 0;
    const bMs = parseServiceRequestTimestamp(b?.createdAt) ?? 0;
    if (aMs !== bMs) {
      return isHistory ? bMs - aMs : aMs - bMs;
    }
    return isHistory ? (b?.id ?? 0) - (a?.id ?? 0) : (a?.id ?? 0) - (b?.id ?? 0);
  });
};

export const generateClientRequestId = () => {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID();
  }
  return `sr-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
};
