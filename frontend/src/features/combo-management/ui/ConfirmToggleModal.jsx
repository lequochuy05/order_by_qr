import { Loader2, Power, X } from 'lucide-react';
import SharedModal from '@shared/ui/SharedModal.jsx';

const ConfirmToggleModal = ({ isOpen, onConfirm, onCancel, currentActive, isSubmitting = false }) => {
  return (
    <SharedModal
      isOpen={isOpen}
      onClose={isSubmitting ? undefined : onCancel}
      className="max-w-md !p-0"
      ariaLabel={currentActive ? 'Xác nhận ngưng bán combo' : 'Xác nhận mở bán combo'}
    >
      <div
        className={`h-1.5 w-full ${
          currentActive
            ? 'bg-gradient-to-r from-rose-500 via-red-500 to-orange-500'
            : 'bg-gradient-to-r from-emerald-500 via-teal-500 to-cyan-500'
        }`}
      />

      <div className="relative px-6 pb-6 pt-7 sm:px-8 sm:pb-7">
        <button
          type="button"
          onClick={onCancel}
          disabled={isSubmitting}
          aria-label="Đóng"
          className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200/80 bg-slate-50/80 text-slate-400 transition-all hover:border-slate-300 hover:bg-slate-100 hover:text-slate-700 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-800 dark:bg-slate-800/80 dark:text-slate-400 dark:hover:bg-slate-700 dark:hover:text-slate-200"
        >
          <X size={16} />
        </button>

        <div className="flex flex-col items-center text-center">
          <div
            className={`mb-4 flex h-16 w-16 items-center justify-center rounded-2xl border ring-8 transition-colors ${
              currentActive
                ? 'border-rose-200/70 bg-rose-50 text-rose-600 ring-rose-500/5 dark:border-rose-500/20 dark:bg-rose-500/10 dark:text-rose-400 dark:ring-rose-500/5'
                : 'border-emerald-200/70 bg-emerald-50 text-emerald-600 ring-emerald-500/5 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-400 dark:ring-emerald-500/5'
            }`}
          >
            <Power size={28} strokeWidth={2.3} />
          </div>

          <span
            className={`mb-2 inline-flex items-center rounded-full px-3 py-0.5 text-[10px] font-extrabold uppercase tracking-[0.16em] ${
              currentActive
                ? 'bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-400'
                : 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-400'
            }`}
          >
            {currentActive ? 'Trạng thái kinh doanh' : 'Kích hoạt thực đơn'}
          </span>

          <h3 className="text-xl font-black tracking-tight text-slate-900 dark:text-white">
            {currentActive ? 'Xác nhận ngưng bán?' : 'Xác nhận mở bán?'}
          </h3>

          <div className="mt-4 w-full rounded-2xl border border-slate-200/70 bg-slate-50/75 px-4 py-3.5 text-sm leading-relaxed text-slate-600 dark:border-slate-800 dark:bg-slate-800/50 dark:text-slate-300">
            {currentActive
              ? 'Combo này sẽ ẩn khỏi thực đơn khách hàng ngay lập tức. Bạn có chắc chắn muốn thực hiện?'
              : 'Combo này sẽ xuất hiện lại trên thực đơn để khách hàng có thể đặt món.'}
          </div>
        </div>

        <div className="mt-6 flex items-center gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={isSubmitting}
            className="inline-flex h-11 flex-1 items-center justify-center rounded-xl border border-slate-200/90 bg-white px-4 text-sm font-bold text-slate-600 shadow-xs transition-all hover:border-slate-300 hover:bg-slate-100/80 hover:text-slate-900 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700/80 dark:bg-slate-800/80 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isSubmitting}
            className={`inline-flex h-11 flex-1 items-center justify-center gap-2 rounded-xl px-4 text-sm font-bold text-white shadow-md transition-all active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 ${
              currentActive
                ? 'bg-gradient-to-r from-rose-600 to-red-600 shadow-rose-600/25 hover:from-rose-500 hover:to-red-500 hover:shadow-lg hover:shadow-rose-600/30'
                : 'bg-gradient-to-r from-emerald-600 to-teal-600 shadow-emerald-600/25 hover:from-emerald-500 hover:to-teal-500 hover:shadow-lg hover:shadow-emerald-600/30'
            }`}
          >
            {isSubmitting ? <Loader2 size={16} className="animate-spin" /> : <Power size={16} />}
            <span>
              {isSubmitting
                ? 'Đang cập nhật...'
                : currentActive
                  ? 'Tạm ngưng ngay'
                  : 'Kích hoạt ngay'}
            </span>
          </button>
        </div>
      </div>
    </SharedModal>
  );
};

export default ConfirmToggleModal;
