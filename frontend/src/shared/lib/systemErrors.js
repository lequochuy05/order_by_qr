const SYSTEM_ERROR_CONTENT = {
  400: {
    statusCode: '400',
    title: 'Yêu cầu không hợp lệ',
    message: 'Thông tin gửi lên chưa đúng định dạng. Vui lòng kiểm tra lại và thử lần nữa.',
    icon: 'circleAlert',
    tone: 'amber',
    primaryAction: { type: 'back', label: 'Quay lại', icon: 'back' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
  401: {
    statusCode: '401',
    title: 'Cần đăng nhập',
    message: 'Phiên làm việc chưa sẵn sàng hoặc đã hết hạn. Vui lòng đăng nhập để tiếp tục.',
    icon: 'lock',
    tone: 'orange',
    primaryAction: { type: 'link', label: 'Đăng nhập', icon: 'login', to: '/login' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
  403: {
    statusCode: '403',
    title: 'Không có quyền truy cập',
    message: 'Tài khoản của bạn chưa được cấp quyền để mở khu vực này.',
    icon: 'shield',
    tone: 'red',
    primaryAction: { type: 'link', label: 'Về trang quản trị', icon: 'home', to: '/admin/dashboard' },
    secondaryAction: { type: 'back', label: 'Quay lại', icon: 'back' },
  },
  404: {
    statusCode: '404',
    title: 'Không tìm thấy trang',
    message: 'Đường dẫn này không tồn tại hoặc đã được thay đổi.',
    icon: 'search',
    tone: 'slate',
    primaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
    secondaryAction: { type: 'back', label: 'Quay lại', icon: 'back' },
  },
  408: {
    statusCode: '408',
    title: 'Yêu cầu quá thời gian',
    message: 'Máy chủ mất quá nhiều thời gian để phản hồi yêu cầu này.',
    icon: 'clock',
    tone: 'amber',
    primaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
    secondaryAction: { type: 'back', label: 'Quay lại', icon: 'back' },
  },
  409: {
    statusCode: '409',
    title: 'Dữ liệu đang xung đột',
    message: 'Thông tin đã thay đổi trong lúc bạn thao tác. Vui lòng tải lại trước khi tiếp tục.',
    icon: 'fileWarning',
    tone: 'amber',
    primaryAction: { type: 'reload', label: 'Tải lại', icon: 'refresh' },
    secondaryAction: { type: 'back', label: 'Quay lại', icon: 'back' },
  },
  410: {
    statusCode: '410',
    title: 'Liên kết đã hết hạn',
    message: 'Liên kết hoặc phiên truy cập này không còn hiệu lực.',
    icon: 'timerReset',
    tone: 'orange',
    primaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
    secondaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
  },
  422: {
    statusCode: '422',
    title: 'Dữ liệu chưa hợp lệ',
    message: 'Một số thông tin chưa đáp ứng điều kiện xử lý. Vui lòng kiểm tra lại biểu mẫu.',
    icon: 'circleAlert',
    tone: 'amber',
    primaryAction: { type: 'back', label: 'Quay lại chỉnh sửa', icon: 'back' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
  429: {
    statusCode: '429',
    title: 'Thao tác quá nhanh',
    message: 'Hệ thống đang giới hạn tần suất yêu cầu. Vui lòng chờ một chút rồi thử lại.',
    icon: 'clock',
    tone: 'orange',
    primaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
    secondaryAction: { type: 'back', label: 'Quay lại', icon: 'back' },
  },
  500: {
    statusCode: '500',
    title: 'Hệ thống đang gặp sự cố',
    message: 'Đã có lỗi ngoài dự kiến. Vui lòng thử lại sau ít phút.',
    icon: 'serverCrash',
    tone: 'red',
    primaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
  502: {
    statusCode: '502',
    title: 'Máy chủ phản hồi lỗi',
    message: 'Dịch vụ trung gian chưa nhận được phản hồi hợp lệ từ máy chủ.',
    icon: 'serverCrash',
    tone: 'red',
    primaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
  503: {
    statusCode: '503',
    title: 'Hệ thống tạm thời chưa sẵn sàng',
    message: 'Dịch vụ có thể đang bảo trì hoặc quá tải. Vui lòng quay lại sau.',
    icon: 'serverCrash',
    tone: 'orange',
    primaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
  504: {
    statusCode: '504',
    title: 'Máy chủ phản hồi quá lâu',
    message: 'Kết nối đến dịch vụ xử lý bị quá thời gian chờ.',
    icon: 'clock',
    tone: 'amber',
    primaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
  offline: {
    statusCode: 'OFFLINE',
    title: 'Không có kết nối',
    message: 'Thiết bị chưa kết nối được đến máy chủ. Vui lòng kiểm tra mạng và thử lại.',
    icon: 'wifiOff',
    tone: 'slate',
    primaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
  runtime: {
    statusCode: 'LỖI',
    title: 'Đã xảy ra lỗi',
    message: 'Ứng dụng gặp sự cố không mong muốn. Vui lòng thử lại.',
    icon: 'alertTriangle',
    tone: 'red',
    primaryAction: { type: 'reload', label: 'Thử lại', icon: 'refresh' },
    secondaryAction: { type: 'link', label: 'Về trang chủ', icon: 'home', to: '/' },
  },
};

const DEFAULT_SYSTEM_ERROR = SYSTEM_ERROR_CONTENT[500];

export const normalizeSystemErrorKey = (statusCode) => {
  const key = String(statusCode || '').trim().toLowerCase();
  if (key === 'network') return 'offline';
  if (key === 'error') return 'runtime';
  return key;
};

export const getSystemErrorContent = (statusCode = 500) => {
  const key = normalizeSystemErrorKey(statusCode);
  const content = SYSTEM_ERROR_CONTENT[key];

  if (content) return content;

  return {
    ...DEFAULT_SYSTEM_ERROR,
    statusCode: key ? String(statusCode).trim().toUpperCase() : DEFAULT_SYSTEM_ERROR.statusCode,
    title: 'Không thể xử lý yêu cầu',
    message: 'Hệ thống chưa có thông báo riêng cho lỗi này. Vui lòng thử lại sau.',
  };
};

export const getSystemErrorStatusCodes = () => Object.keys(SYSTEM_ERROR_CONTENT);

export default SYSTEM_ERROR_CONTENT;
