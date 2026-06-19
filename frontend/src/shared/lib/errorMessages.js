const tryParseJson = (value) => {
  if (typeof value !== 'string') return value;

  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
};

export const getErrorPayload = (error) => {
  if (!error) return {};
  if (error.response?.data) return tryParseJson(error.response.data);
  if (error.data) return tryParseJson(error.data);
  if (typeof error === 'string') return tryParseJson(error);
  return error;
};

export const getErrorDetails = (error) => {
  const payload = getErrorPayload(error);
  return payload &&
    typeof payload === 'object' &&
    payload.details &&
    typeof payload.details === 'object'
    ? payload.details
    : {};
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

const translateErrorMessage = (message) => {
  if (!message) return message;

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

  if (messageMap[message]) return messageMap[message];
  if (message.includes('already exists')) return 'Dữ liệu này đã tồn tại trong hệ thống!';
  return message;
};

export const buildErrorMessage = (error, { includeDetails = true } = {}) => {
  const data = getErrorPayload(error);
  let message = 'Có lỗi xảy ra, vui lòng thử lại.';
  const details = [];

  if (data && typeof data === 'object') {
    message = data.message || error?.message || message;

    if (includeDetails) {
      Object.entries(getErrorDetails(error)).forEach(([field, detail]) => {
        details.push(`${field}: ${detail}`);
      });

      const status = data.status || error?.status || error?.response?.status;
      const code = data.code || error?.code;
      const title = translateHttpTitle(data.title || error?.response?.statusText);

      if (status && !message.includes('Không tìm thấy thông tin bàn')) {
        details.push(`Mã lỗi: ${code || status}${title ? ` - ${title}` : ''}`);
      }
    }
  } else if (typeof data === 'string' && data.trim()) {
    message = data;
  } else if (error?.message) {
    message = error.message;
  }

  message = translateErrorMessage(message);
  return details.length > 0 ? `${message}\n\n${details.join('\n')}` : message;
};
