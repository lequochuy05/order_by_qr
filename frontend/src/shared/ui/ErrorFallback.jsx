import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import SystemErrorPage from './SystemErrorPage.jsx';

const ErrorFallback = ({ error, resetErrorBoundary, fullScreen = false }) => {
  const message = error
    ? buildErrorMessage(error, { includeDetails: false })
    : 'Ứng dụng gặp sự cố không mong muốn. Vui lòng thử lại.';

  return (
    <SystemErrorPage
      statusCode="runtime"
      message={message}
      primaryAction={{
        type: 'button',
        label: 'Thử lại',
        icon: 'refresh',
        onClick: resetErrorBoundary,
      }}
      fullScreen={fullScreen}
    />
  );
};

export default ErrorFallback;
