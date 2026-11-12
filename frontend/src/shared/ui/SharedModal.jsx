import React, { useEffect, useRef } from 'react';

const SharedModal = ({
  isOpen,
  onClose,
  children,
  className = '',
  backdropClassName = 'bg-black/60 backdrop-blur-sm',
  closeOnBackdrop = false,
  ariaLabel = 'Hộp thoại',
  placement = 'center',
}) => {
  const dialogRef = useRef(null);
  const previouslyFocusedRef = useRef(null);
  const onCloseRef = useRef(onClose);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    if (!isOpen) return undefined;

    previouslyFocusedRef.current = document.activeElement;
    const dialog = dialogRef.current;
    const focusableSelector =
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
    const focusableElements = () => Array.from(dialog?.querySelectorAll(focusableSelector) || []);
    const firstFocusable = focusableElements()[0];
    (firstFocusable || dialog)?.focus();

    const handleKeyDown = (event) => {
      const activeInsideDialog = dialog?.contains(document.activeElement);
      if (!activeInsideDialog) return;

      if (event.key === 'Escape') {
        event.preventDefault();
        onCloseRef.current?.();
        return;
      }

      if (event.key !== 'Tab') return;
      const elements = focusableElements();
      if (elements.length === 0) {
        event.preventDefault();
        dialog?.focus();
        return;
      }

      const first = elements[0];
      const last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      previouslyFocusedRef.current?.focus?.();
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const placementStyles = {
    center: {
      backdrop: 'items-center justify-center p-2 sm:p-4',
      panel:
        'max-h-[94dvh] w-[calc(100vw-1rem)] rounded-[1.5rem] p-4 animate-in zoom-in duration-300 sm:max-h-[90vh] sm:w-[calc(100vw-2rem)] sm:rounded-[2rem] sm:p-6',
      defaultWidth: 'max-w-lg',
    },
    right: {
      backdrop: 'items-stretch justify-end p-0',
      panel:
        'h-full w-full rounded-none border-l border-slate-200 p-0 animate-in slide-in-from-right duration-300 dark:border-slate-800 sm:rounded-l-2xl',
      defaultWidth: 'max-w-md',
    },
    bottom: {
      backdrop: 'items-end justify-center p-0 sm:p-4',
      panel:
        'max-h-[88dvh] w-full rounded-t-[2rem] p-0 animate-in slide-in-from-bottom duration-300 sm:rounded-[2rem]',
      defaultWidth: 'max-w-md',
    },
  };
  const styles = placementStyles[placement] || placementStyles.center;
  const resolvedMaxWidth = className.includes('max-w-') ? '' : styles.defaultWidth;

  return (
    <div
      className={`fixed inset-0 z-[60] flex animate-in fade-in duration-200 ${styles.backdrop} ${backdropClassName}`}
      onMouseDown={() => {
        if (closeOnBackdrop) onCloseRef.current?.();
      }}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel}
        tabIndex={-1}
        className={`flex min-w-0 flex-col bg-white text-slate-900 shadow-2xl transition-colors dark:bg-slate-900 dark:text-slate-100 ${resolvedMaxWidth} ${styles.panel} ${className}`}
        onMouseDown={(event) => event.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
};

export default SharedModal;
