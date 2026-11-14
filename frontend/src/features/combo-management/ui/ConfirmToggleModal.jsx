import { Power, X } from 'lucide-react';
import SharedModal from '@shared/ui/SharedModal.jsx';

const ConfirmToggleModal = ({ isOpen, onConfirm, onCancel, currentActive }) => {
  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onCancel}
      className="relative max-w-sm p-8"
      ariaLabel={currentActive ? 'Xác nhận ngưng bán combo' : 'Xác nhận mở bán combo'}
    >
      {/* Nút đóng nhanh */}
      <button
        onClick={onCancel}
        className="absolute right-4 top-4 rounded-full p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-200"
      >
        <X size={20} />
      </button>

      {/* Icon cảnh báo */}
      <div
        className={`mx-auto w-20 h-20 rounded-3xl flex items-center justify-center mb-6 rotate-12 group-hover:rotate-0 transition-transform ${
          currentActive
            ? 'bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-300'
            : 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300'
        }`}
      >
        <Power size={40} strokeWidth={2.5} />
      </div>

      <h3 className="mb-3 text-center text-xl font-black text-slate-900 dark:text-slate-100">
        {currentActive ? 'Xác nhận ngưng bán?' : 'Xác nhận mở bán?'}
      </h3>

      <p className="mb-8 text-center text-sm leading-relaxed text-slate-500 dark:text-slate-400">
        {currentActive
          ? 'Combo này sẽ ẩn khỏi thực đơn khách hàng ngay lập tức. Bạn có chắc chắn muốn thực hiện?'
          : 'Combo này sẽ xuất hiện lại trên thực đơn để khách hàng có thể đặt món.'}
      </p>

      <div className="flex flex-col gap-3">
        <button
          onClick={onConfirm}
          className={`w-full py-3.5 text-white rounded-2xl font-bold shadow-lg transition-all active:scale-95 ${
            currentActive
              ? 'bg-gradient-to-r from-rose-600 to-red-600 shadow-rose-600/25 hover:from-rose-500 hover:to-red-500'
              : 'bg-gradient-to-r from-emerald-600 to-teal-600 shadow-emerald-600/25 hover:from-emerald-500 hover:to-teal-500'
          }`}
        >
          Thay đổi
        </button>
        <button
          onClick={onCancel}
          className="w-full rounded-2xl bg-slate-50 py-3.5 font-bold text-slate-500 transition-all hover:bg-slate-100 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
        >
          Để sau
        </button>
      </div>
    </SharedModal>
  );
};

export default ConfirmToggleModal;
