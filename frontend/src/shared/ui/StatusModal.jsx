import { CheckCircle2, AlertCircle, X } from 'lucide-react';

const StatusModal = ({ isOpen, onClose, type = 'success', title, message }) => {
  if (!isOpen) return null;

  const isSuccess = type === 'success';

  return (
    <div className="fixed inset-0 bg-black/40 z-[70] flex items-center justify-center p-4 backdrop-blur-[2px] animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200 dark:bg-slate-900">
        {/* Header Icon */}
        <div
          className={`p-6 flex flex-col items-center justify-center text-center ${isSuccess ? 'bg-green-50 dark:bg-green-500/10' : 'bg-red-50 dark:bg-red-500/10'}`}
        >
          <div
            className={`p-3 rounded-full mb-3 ${isSuccess ? 'bg-green-100 text-green-600 dark:bg-green-500/15 dark:text-green-300' : 'bg-red-100 text-red-600 dark:bg-red-500/15 dark:text-red-300'}`}
          >
            {isSuccess ? <CheckCircle2 size={48} /> : <AlertCircle size={48} />}
          </div>
          <h3
            className={`text-xl font-bold ${isSuccess ? 'text-green-700 dark:text-green-300' : 'text-red-700 dark:text-red-300'}`}
          >
            {title || (isSuccess ? 'Thành công' : 'Thất bại')}
          </h3>
        </div>

        {/* Content */}
        <div className="p-6 text-center">
          <p className="text-gray-600 text-sm mb-6 leading-relaxed whitespace-pre-line dark:text-slate-300">
            {message}
          </p>

          <button
            onClick={onClose}
            className={`w-full py-3 rounded-xl font-bold text-white transition-all active:scale-95 shadow-md
              ${
                isSuccess
                  ? 'bg-green-500 hover:bg-green-600 shadow-green-200'
                  : 'bg-red-500 hover:bg-red-600 shadow-red-200'
              }`}
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};

export default StatusModal;
