import React, { useEffect, useRef } from 'react';

const SharedModal = ({
  isOpen,
  onClose,
  children,
  className = 'max-w-lg',
  backdropClassName = 'bg-black/60 backdrop-blur-sm',
  closeOnBackdrop = false,
  ariaLabel = 'Hộp thoại',
}) => {
  const dialogRef = useRef(null);
  const previouslyFocusedRef = useRef(null);

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
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose?.();
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
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className={`fixed inset-0 z-[60] flex items-center justify-center p-2 animate-in fade-in duration-200 sm:p-4 ${backdropClassName}`}
      onMouseDown={() => {
        if (closeOnBackdrop) onClose?.();
      }}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel}
        tabIndex={-1}
        className={`flex max-h-[94dvh] w-[calc(100vw-1rem)] min-w-0 flex-col rounded-[1.5rem] bg-white p-4 shadow-2xl animate-in zoom-in duration-300 sm:max-h-[90vh] sm:w-[calc(100vw-2rem)] sm:rounded-[2rem] sm:p-6 ${className}`}
        onMouseDown={(event) => event.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
};

export default SharedModal;
