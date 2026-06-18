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
