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
      className={`fixed inset-0 z-[60] flex items-center justify-center p-4 animate-in fade-in duration-200 ${backdropClassName}`}
      onMouseDown={() => {
        if (closeOnBackdrop) onClose?.();
      }}
    >
      <div
        className={`bg-white rounded-[2rem] w-full p-6 shadow-2xl animate-in zoom-in duration-300 flex flex-col max-h-[90vh] ${className}`}
        onMouseDown={(event) => event.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
};

export default SharedModal;
