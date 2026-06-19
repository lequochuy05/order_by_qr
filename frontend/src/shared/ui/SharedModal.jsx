import React from 'react';

const SharedModal = ({
  isOpen,
  onClose,
  children,
  className = 'max-w-lg',
  backdropClassName = 'bg-black/60 backdrop-blur-sm',
  closeOnBackdrop = false,
}) => {
  if (!isOpen) return null;

  return (
    <div
      className={`fixed inset-0 z-[60] flex items-center justify-center p-2 animate-in fade-in duration-200 sm:p-4 ${backdropClassName}`}
      onMouseDown={() => {
        if (closeOnBackdrop) onClose?.();
      }}
    >
      <div
        className={`flex max-h-[94dvh] w-[calc(100vw-1rem)] min-w-0 flex-col rounded-[1.5rem] bg-white p-4 shadow-2xl animate-in zoom-in duration-300 sm:max-h-[90vh] sm:w-[calc(100vw-2rem)] sm:rounded-[2rem] sm:p-6 ${className}`}
        onMouseDown={(event) => event.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
};

export default SharedModal;
