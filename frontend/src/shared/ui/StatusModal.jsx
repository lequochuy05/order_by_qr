import { CheckCircle2, AlertCircle } from 'lucide-react';
import SharedModal from './SharedModal.jsx';

const StatusModal = ({ isOpen, onClose, type = 'success', title, message }) => {
  const isSuccess = type === 'success';

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      ariaLabel={title || (isSuccess ? 'Thành công' : 'Thất bại')}
      className="max-w-sm overflow-hidden p-0 sm:p-0 dark:bg-slate-900"
      backdropClassName="bg-black/40 backdrop-blur-[2px]"
    >
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
    </SharedModal>
  );
};

export default StatusModal;
