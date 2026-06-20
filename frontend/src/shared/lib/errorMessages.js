const tryParseJson = (value) => {
  if (typeof value !== 'string') return value;

  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
};

const ERROR_CODE_MESSAGES = {
  APP_ERROR: 'Hệ thống đang gặp sự cố. Vui lòng thử lại sau.',
  BUSINESS_ERROR: 'Thao tác không phù hợp với quy tắc nghiệp vụ.',
  INVALID_REQUEST: 'Yêu cầu không hợp lệ.',
  VALIDATION_ERROR: 'Dữ liệu đầu vào không hợp lệ.',
  UNAUTHORIZED: 'Vui lòng đăng nhập để tiếp tục.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  RESOURCE_NOT_FOUND: 'Không tìm thấy tài nguyên yêu cầu.',
  CONFLICT: 'Dữ liệu đang bị xung đột.',
  RATE_LIMIT_EXCEEDED: 'Bạn thao tác quá nhanh. Vui lòng chờ một chút rồi thử lại.',

  USER_NOT_FOUND: 'Không tìm thấy người dùng.',
  EMAIL_NOT_FOUND: 'Không tìm thấy địa chỉ email.',
  PHONE_NOT_FOUND: 'Không tìm thấy số điện thoại.',
  EMAIL_EXISTS: 'Email đã tồn tại trong hệ thống.',
  PHONE_EXISTS: 'Số điện thoại đã tồn tại trong hệ thống.',
  ACCOUNT_INACTIVE: 'Tài khoản đã bị khóa hoặc chưa được kích hoạt.',
  INVALID_CREDENTIALS: 'Email hoặc mật khẩu không chính xác.',
  INVALID_REFRESH_TOKEN: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn.',
  PASSWORD_INVALID: 'Mật khẩu hiện tại không chính xác.',
  PASSWORD_RESET_TOKEN_INVALID: 'Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.',

  CATEGORY_NOT_FOUND: 'Không tìm thấy danh mục.',
  CATEGORY_NAME_EXISTS: 'Tên danh mục đã tồn tại.',
  MENU_ITEM_NOT_FOUND: 'Không tìm thấy món ăn.',
  MENU_ITEM_NAME_EXISTS: 'Tên món ăn đã tồn tại.',
  COMBO_NOT_FOUND: 'Không tìm thấy combo.',
  COMBO_NAME_EXISTS: 'Tên combo đã tồn tại.',

  TABLE_NOT_FOUND: 'Không tìm thấy bàn.',
  TABLE_CODE_INVALID: 'Mã bàn không hợp lệ.',
  TABLE_NUMBER_EXISTS: 'Số bàn đã tồn tại.',
  TABLE_QR_GENERATION_FAILED: 'Không thể tạo mã QR cho bàn.',
  TABLE_SESSION_NOT_FOUND: 'Không tìm thấy phiên sử dụng bàn.',
  TABLE_SESSION_INVALID: 'Phiên sử dụng bàn không hợp lệ.',
  TABLE_SESSION_EXPIRED: 'Phiên sử dụng bàn đã hết hạn.',

  ORDER_NOT_FOUND: 'Không tìm thấy đơn hàng.',
  ORDER_ITEM_NOT_FOUND: 'Không tìm thấy món trong đơn hàng.',
  ORDER_CONTENT_EMPTY: 'Giỏ hàng đang trống. Hãy chọn món trước khi đặt.',
  ORDER_INVALID_STATUS: 'Trạng thái đơn hàng không hợp lệ.',
  ORDER_INVALID_ITEM_STATUS: 'Trạng thái món không hợp lệ.',
  ORDER_INVALID_STATE: 'Đơn hàng không ở trạng thái phù hợp để thực hiện thao tác này.',
  ORDER_ALREADY_PAID: 'Đơn hàng đã được thanh toán.',
  ORDER_PAYMENT_INVALID: 'Thông tin thanh toán của đơn hàng không hợp lệ.',
  ORDER_PAYMENT_IN_PROGRESS: 'Đơn hàng đang trong quá trình thanh toán.',
  ORDER_IDEMPOTENCY_CONFLICT: 'Yêu cầu đặt món này đã được gửi trước đó.',

  VOUCHER_NOT_FOUND: 'Không tìm thấy voucher.',
  VOUCHER_CODE_EXISTS: 'Mã voucher đã tồn tại.',
  VOUCHER_INACTIVE: 'Voucher đang bị vô hiệu hóa.',
  VOUCHER_NOT_YET_ACTIVE: 'Voucher chưa đến thời gian sử dụng.',
  VOUCHER_EXPIRED: 'Voucher đã hết hạn.',
  VOUCHER_USAGE_LIMIT_REACHED: 'Voucher đã hết lượt sử dụng.',
  VOUCHER_INVALID: 'Voucher không hợp lệ.',

  PROMOTION_NOT_FOUND: 'Không tìm thấy chương trình khuyến mãi.',
  PROMOTION_NAME_EXISTS: 'Tên chương trình khuyến mãi đã tồn tại.',
  PROMOTION_INACTIVE: 'Chương trình khuyến mãi đang bị vô hiệu hóa.',
  PROMOTION_INVALID: 'Chương trình khuyến mãi không hợp lệ.',

  INVENTORY_ITEM_NOT_FOUND: 'Không tìm thấy nguyên liệu.',
  INVENTORY_ITEM_NAME_EXISTS: 'Tên nguyên liệu đã tồn tại.',
  INVENTORY_ITEM_IN_USE: 'Nguyên liệu đang được sử dụng trong công thức.',
  INVENTORY_QUANTITY_INVALID: 'Số lượng tồn kho không hợp lệ.',
  INVENTORY_QUANTITY_BELOW_RESERVED: 'Tồn kho thực tế không được thấp hơn số lượng đã giữ chỗ.',
  INVENTORY_INSUFFICIENT_STOCK: 'Không đủ nguyên liệu trong kho.',
  RECIPE_ITEM_DUPLICATED: 'Nguyên liệu bị trùng trong công thức.',
  INVALID_DATE_RANGE: 'Ngày bắt đầu không được sau ngày kết thúc.',

  PAYMENT_TRANSACTION_NOT_FOUND: 'Không tìm thấy giao dịch thanh toán.',
  PAYMENT_GATEWAY_ERROR: 'Cổng thanh toán đang gặp sự cố.',
  PAYMENT_CANCELLATION_FAILED: 'Không thể hủy giao dịch thanh toán.',
  PAYMENT_TRANSACTION_INVALID_STATE: 'Trạng thái giao dịch thanh toán không hợp lệ.',
  PAYMENT_IDEMPOTENCY_CONFLICT: 'Yêu cầu thanh toán này đã được xử lý trước đó.',
  PAYMENT_WEBHOOK_INVALID: 'Dữ liệu xác nhận thanh toán không hợp lệ.',

  FILE_INVALID: 'Tệp tải lên không hợp lệ.',
  FILE_UPLOAD_FAILED: 'Không thể tải tệp lên.',
};

const EXACT_MESSAGE_TRANSLATIONS = {
  'Application error': 'Hệ thống đang gặp sự cố. Vui lòng thử lại sau.',
  'Business rule violation': 'Thao tác không phù hợp với quy tắc nghiệp vụ.',
  'Invalid request': 'Yêu cầu không hợp lệ.',
  'Invalid input data': 'Dữ liệu đầu vào không hợp lệ.',
  'Authentication is required': 'Vui lòng đăng nhập để tiếp tục.',
  'Permission denied': 'Bạn không có quyền thực hiện thao tác này.',
  'Resource not found': 'Không tìm thấy tài nguyên yêu cầu.',
  'Requested resource not found': 'Không tìm thấy tài nguyên yêu cầu.',
  'You do not have permission to perform this action': 'Bạn không có quyền thực hiện thao tác này.',
  'Internal server error. Please try again later.':
    'Hệ thống đang gặp sự cố. Vui lòng thử lại sau.',
  'Network Error': 'Không thể kết nối đến máy chủ!',

  'Secure Table Code invalid':
    'Không tìm thấy thông tin bàn. Mã QR này có thể đã được tạo lại hoặc không còn hiệu lực.',
  'Table ID invalid': 'Không tìm thấy thông tin bàn.',
  'Table identification required for order creation': 'Vui lòng quét mã QR trên bàn để đặt món.',
  'Order content cannot be empty': 'Giỏ hàng đang trống. Hãy chọn món trước khi đặt.',
  'Menu item not found': 'Không tìm thấy món ăn.',
  'Combo not found': 'Không tìm thấy combo.',

  'Option must contain at least one value': 'Nhóm tùy chọn phải có ít nhất một giá trị.',
  'Max selection cannot exceed option value count':
    'Số lựa chọn tối đa không được lớn hơn số giá trị hiện có.',
  'Combo must contain at least one item': 'Combo phải có ít nhất một món.',
  'Menu item ID is required': 'Vui lòng chọn món cho combo.',
  'Combo item quantity must be at least 1': 'Số lượng món trong combo phải ít nhất là 1.',
  'Cannot delete category that still contains active menu items':
    'Không thể xóa danh mục đang chứa món ăn hoạt động.',
  'Cannot delete menu item that is part of an active combo':
    'Không thể xóa món ăn đang thuộc một combo hoạt động.',
  'Selected option value does not exist.': 'Giá trị tùy chọn đã chọn không tồn tại.',
  'Ordering is currently disabled': 'Nhà hàng hiện đang tạm ngừng nhận đơn.',
  'Table is inactive and cannot accept orders': 'Bàn đang ngừng hoạt động và không thể nhận đơn.',
  'Combo is not available for ordering': 'Combo hiện không khả dụng để đặt.',
  'This order is already settled': 'Đơn hàng đã được thanh toán.',
  'Order has no payable amount': 'Đơn hàng không có số tiền cần thanh toán.',
  'Refresh token has expired or was revoked': 'Phiên đăng nhập đã hết hạn hoặc bị thu hồi.',
  'Refresh token missing': 'Không tìm thấy thông tin phiên đăng nhập.',
  'Token invalid or expired': 'Mã xác thực không hợp lệ hoặc đã hết hạn.',
  'Token expired or already used': 'Mã xác thực đã hết hạn hoặc đã được sử dụng.',
  'OTP invalid or expired': 'Mã OTP không hợp lệ hoặc đã hết hạn.',
  'This order request has already been submitted. Please wait for processing.':
    'Yêu cầu đặt món này đã được gửi. Vui lòng chờ hệ thống xử lý.',
  'The table is currently being paid. Please contact a staff member.':
    'Bàn đang trong quá trình thanh toán. Vui lòng liên hệ nhân viên.',
  'Voucher applied successfully': 'Áp dụng voucher thành công.',
  'Voucher is invalid or expired': 'Voucher không hợp lệ hoặc đã hết hạn.',
  'Order placed successfully': 'Đặt món thành công.',
};

const MESSAGE_PATTERNS = [
  [/^Duplicate option name:\s*(.+)$/i, 'Tên nhóm tùy chọn bị trùng: $1'],
  [/^Duplicate option value:\s*(.+)$/i, 'Giá trị tùy chọn bị trùng: $1'],
  [/^Invalid item option id:\s*(.+)$/i, 'Mã nhóm tùy chọn không hợp lệ: $1'],
  [/^Invalid item option value id:\s*(.+)$/i, 'Mã giá trị tùy chọn không hợp lệ: $1'],
  [/^Duplicate menu item in combo:\s*(.+)$/i, 'Món ăn bị trùng trong combo: $1'],
  [/^Menu item not found:\s*(.+)$/i, 'Không tìm thấy món ăn: $1'],
  [/^Table number not found:\s*(.+)$/i, 'Không tìm thấy bàn số $1'],
  [/^Unsupported payment method:\s*(.+)$/i, 'Phương thức thanh toán không được hỗ trợ: $1'],
];

const FIELD_LABELS = {
  name: 'Tên',
  description: 'Mô tả',
  img: 'Đường dẫn ảnh',
  price: 'Giá',
  categoryId: 'Danh mục',
  itemOptions: 'Nhóm tùy chọn',
  optionValues: 'Giá trị tùy chọn',
  maxSelection: 'Số lựa chọn tối đa',
  extraPrice: 'Giá cộng thêm',
  items: 'Danh sách món',
  menuItemId: 'Món ăn',
  quantity: 'Số lượng',
  email: 'Email',
  password: 'Mật khẩu',
  fullName: 'Họ và tên',
  phone: 'Số điện thoại',
  currentPassword: 'Mật khẩu hiện tại',
  newPassword: 'Mật khẩu mới',
  tableNumber: 'Số bàn',
  capacity: 'Sức chứa',
  status: 'Trạng thái',
  code: 'Mã',
  validFrom: 'Thời gian bắt đầu',
  validTo: 'Thời gian kết thúc',
  usageLimit: 'Giới hạn lượt dùng',
  restaurantName: 'Tên nhà hàng',
  restaurantPhone: 'Số điện thoại nhà hàng',
  restaurantEmail: 'Email nhà hàng',
  restaurantAddress: 'Địa chỉ nhà hàng',
  logoUrl: 'Đường dẫn logo',
  wifiName: 'Tên Wi-Fi',
  wifiPassword: 'Mật khẩu Wi-Fi',
  currency: 'Đơn vị tiền tệ',
  taxPercent: 'Thuế',
  serviceChargePercent: 'Phí dịch vụ',
  inventoryItemId: 'Nguyên liệu',
  quantityRequired: 'Định lượng nguyên liệu',
  quantityDelta: 'Số lượng điều chỉnh',
  lowStockThreshold: 'Ngưỡng tồn kho thấp',
  note: 'Ghi chú',
  orderId: 'Đơn hàng',
  paymentMethod: 'Phương thức thanh toán',
  voucherCode: 'Mã voucher',
  idempotencyKey: 'Mã chống gửi trùng',
  message: 'Nội dung',
  history: 'Lịch sử hội thoại',
  role: 'Vai trò',
  content: 'Nội dung',
  token: 'Mã xác thực',
  otp: 'Mã OTP',
};

const containsVietnamese = (value) =>
  /[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i.test(value);

export const translateErrorMessage = (message, code) => {
  if (!message) return ERROR_CODE_MESSAGES[code] || message;
  if (EXACT_MESSAGE_TRANSLATIONS[message]) return EXACT_MESSAGE_TRANSLATIONS[message];

  for (const [pattern, replacement] of MESSAGE_PATTERNS) {
    if (pattern.test(message)) return message.replace(pattern, replacement);
  }

  if (containsVietnamese(message)) return message;
  return ERROR_CODE_MESSAGES[code] || 'Có lỗi xảy ra, vui lòng thử lại.';
};

const translateValidationDetail = (field, detail) => {
  const message = String(detail);
  if (containsVietnamese(message)) return message;
  if (EXACT_MESSAGE_TRANSLATIONS[message]) return EXACT_MESSAGE_TRANSLATIONS[message];

  const label = FIELD_LABELS[field] || 'Giá trị';
  const rules = [
    [/^(?:.+) (?:cannot be empty|is required)$/i, `${label} không được để trống.`],
    [/^(?:.+) is invalid$/i, `${label} không hợp lệ.`],
    [/^Invalid (?:.+) format$/i, `${label} không đúng định dạng.`],
    [/^(?:.+) cannot be negative$/i, `${label} không được âm.`],
    [/^(?:.+) must be positive$/i, `${label} phải lớn hơn 0.`],
    [/^(?:.+) must be greater than 0$/i, `${label} phải lớn hơn 0.`],
    [/^(?:.+) must be at least (\d+)$/i, `${label} phải ít nhất là $1.`],
    [
      /^(?:.+) must be at least (\d+) characters?(?: long)?$/i,
      `${label} phải có ít nhất $1 ký tự.`,
    ],
    [/^(?:.+) cannot exceed (\d+) characters?$/i, `${label} không được vượt quá $1 ký tự.`],
    [/^(?:.+) cannot exceed (\d+)$/i, `${label} không được vượt quá $1.`],
    [
      /^(?:.+) must be between (\d+) and (\d+) characters?$/i,
      `${label} phải có từ $1 đến $2 ký tự.`,
    ],
  ];

  for (const [pattern, replacement] of rules) {
    if (pattern.test(message)) return message.replace(pattern, replacement);
  }

  return 'Giá trị không hợp lệ.';
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
  const details =
    payload && typeof payload === 'object' && payload.details && typeof payload.details === 'object'
      ? payload.details
      : {};

  return Object.fromEntries(
    Object.entries(details).map(([field, detail]) => [
      field,
      translateValidationDetail(field, detail),
    ]),
  );
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

export const buildErrorMessage = (error, { includeDetails = true } = {}) => {
  const data = getErrorPayload(error);
  let message = 'Có lỗi xảy ra, vui lòng thử lại.';
  const details = [];

  if (data && typeof data === 'object') {
    const code = data.code || error?.code;
    message = translateErrorMessage(data.message || error?.message || message, code);

    if (includeDetails) {
      Object.entries(getErrorDetails(error)).forEach(([field, detail]) => {
        const label = FIELD_LABELS[field] || field;
        details.push(`${label}: ${detail}`);
      });

      const status = data.status || error?.status || error?.response?.status;
      const title = translateHttpTitle(data.title || error?.response?.statusText);

      if (status && !message.includes('Không tìm thấy thông tin bàn')) {
        details.push(`Mã lỗi: ${code || status}${title ? ` - ${title}` : ''}`);
      }
    }
  } else if (typeof data === 'string' && data.trim()) {
    message = translateErrorMessage(data);
  } else if (error?.message) {
    message = translateErrorMessage(error.message, error?.code);
  }

  return details.length > 0 ? `${message}\n\n${details.join('\n')}` : message;
};
