import React from 'react';

const FormError = ({ message, className = '' }) => {
  if (!message) return null;
  return (
    <p
      className={`text-red-500 text-[11px] mt-1.5 flex items-center gap-1.5 font-bold animate-in slide-in-from-top-1 ${className}`}
    >
      {message}
    </p>
  );
};

export default FormError;
