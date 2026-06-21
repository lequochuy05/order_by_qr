import { AlertCircle } from 'lucide-react';

const FormError = ({ message, className = '', showIcon = true }) => {
  if (!message) return null;
  return (
    <p
      className={`mt-1.5 flex items-center gap-1.5 text-[11px] font-bold text-red-500 animate-in slide-in-from-top-1 ${className}`}
    >
      {showIcon && <AlertCircle size={12} className="shrink-0" />}
      {message}
    </p>
  );
};

export default FormError;
