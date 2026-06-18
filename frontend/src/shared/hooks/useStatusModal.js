import { create } from 'zustand';

const tryParseJson = (value) => {
  if (typeof value !== 'string') return value;

  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
};

const getErrorPayload = (err) => {
  if (!err) return {};
  if (err.response?.data) return tryParseJson(err.response.data);
  if (err.data) return tryParseJson(err.data);
  if (typeof err === 'string') return tryParseJson(err);
  return err;
};

const translateHttpTitle = (title) => {
  const titles = {
    'Bad Request': 'Yêu cầu không hợp lệ',
    Unauthorized: 'Chưa đăng nhập',
    Forbidden: 'Không có quyền truy cập',
    'Not Found': 'Không tìm thấy',
    Conflict: 'Dữ liệu bị xung đột',
    'Internal Server Error': 'Lỗi máy chủ',
  };

  return titles[title] || title;
};

const translateErrorMessage = (msg) => {
  if (!msg) return msg;

  const messageMap = {
    'Secure Table Code invalid':
      'Không tìm thấy thông tin bàn. Mã QR này có thể đã được tạo lại hoặc không còn hiệu lực.',
    'Table ID invalid': 'Không tìm thấy thông tin bàn.',
    'Table identification required for order creation': 'Vui lòng quét mã QR trên bàn để đặt món.',
    'Order content cannot be empty': 'Giỏ hàng của bạn đang trống. Hãy chọn món trước khi đặt.',
    'Menu item not found': 'Không tìm thấy món ăn.',
    'Combo not found': 'Không tìm thấy combo.',
    'Network Error': 'Không thể kết nối đến máy chủ!',
  };

  if (messageMap[msg]) return messageMap[msg];
  if (msg.includes('already exists')) return 'Dữ liệu này đã tồn tại trong hệ thống!';
  return msg;
};

const buildErrorMessage = (err) => {
  const data = getErrorPayload(err);
  let msg = 'Có lỗi xảy ra, vui lòng thử lại.';
  const details = [];

  if (data && typeof data === 'object') {
    const payload = data;

    msg = payload.message || data.message || err?.message || msg;

    const validationDetails = payload.details;
    if (validationDetails && typeof validationDetails === 'object') {
      Object.entries(validationDetails).forEach(([field, message]) => {
        details.push(`${field}: ${message}`);
      });
    }

    const status = data.status || err?.status || err?.response?.status;
    const code = payload.code || data.code || err?.code;
    const title = data.title || err?.response?.statusText;
    const translatedTitle = translateHttpTitle(title);
    msg = translateErrorMessage(msg);

    if (status && !msg.includes('Không tìm thấy thông tin bàn')) {
      details.push(`Mã lỗi: ${code || status}${translatedTitle ? ` - ${translatedTitle}` : ''}`);
    }
  } else if (typeof data === 'string' && data.trim()) {
    msg = data;
  } else if (err?.message) {
    msg = err.message;
  }

  msg = translateErrorMessage(msg);

  return details.length > 0 ? `${msg}\n\n${details.join('\n')}` : msg;
};

export const useStatusModalStore = create((set) => ({
  isOpen: false,
  type: 'success',
  title: '',
  message: '',

  showSuccess: (msg, title = 'Thành công!') =>
    set({
      isOpen: true,
      type: 'success',
      title: title,
      message: msg,
    }),

  showError: (err, title = 'Thao tác thất bại') =>
    set({
      isOpen: true,
      type: 'error',
      title: title,
      message: buildErrorMessage(err),
    }),

  closeStatusModal: () => set({ isOpen: false }),
}));

export const useStatusModal = () => {
  const store = useStatusModalStore();

  return {
    statusModal: {
      isOpen: store.isOpen,
      type: store.type,
      title: store.title,
      message: store.message,
    },
    showSuccess: store.showSuccess,
    showError: store.showError,
    closeStatusModal: store.closeStatusModal,
  };
};
