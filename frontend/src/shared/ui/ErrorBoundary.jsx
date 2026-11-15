import { Component } from 'react';
import ErrorFallback from './ErrorFallback.jsx';

class ErrorBoundary extends Component {
  state = {
    error: null,
    hasError: false,
  };

  static getDerivedStateFromError(error) {
    return {
      error,
      hasError: true,
    };
  }

  componentDidUpdate(prevProps) {
    // Auto-reset khi resetKeys thay đổi — ví dụ location.pathname đổi khi user navigate
    // Giúp tránh bị kẹt màn hình lỗi khi người dùng bấm Back/Forward trình duyệt
    if (this.state.hasError && this.props.resetKeys) {
      const hasKeysChanged = this.props.resetKeys.some(
        (key, idx) => key !== prevProps.resetKeys?.[idx],
      );
      if (hasKeysChanged) {
        this.resetErrorBoundary();
      }
    }
  }

  componentDidCatch(error, errorInfo) {
    if (import.meta.env.DEV) {
      console.error('[ErrorBoundary] Render error:', error, errorInfo);
    }

    this.props.onError?.(error, errorInfo);
  }

  resetErrorBoundary = () => {
    this.setState(
      {
        error: null,
        hasError: false,
      },
      () => this.props.onReset?.(),
    );
  };

  render() {
    if (this.state.hasError) {
      const FallbackComponent = this.props.fallbackComponent || ErrorFallback;

      return (
        <FallbackComponent
          error={this.state.error}
          resetErrorBoundary={this.resetErrorBoundary}
          fullScreen={this.props.fullScreen}
        />
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
