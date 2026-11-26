import SaveButton from '../SaveButton.jsx';

const ModalActions = ({
  onClose,
  formId,
  submitLabel = 'Lưu',
  submittingLabel = 'Đang lưu...',
  cancelLabel = 'Hủy bỏ',
  isSubmitting = false,
  disabled = false,
  className = '',
}) => (
  <div
    className={`flex shrink-0 items-center justify-end gap-3 border-t border-slate-200/70 bg-slate-50/85 px-6 py-4 backdrop-blur-sm transition-colors dark:border-slate-800/80 dark:bg-slate-900/95 sm:px-8 ${className}`}
  >
    <button
      type="button"
      onClick={onClose}
      disabled={isSubmitting}
      className="inline-flex h-11 flex-1 items-center justify-center rounded-xl border border-slate-200/90 bg-white px-5 text-sm font-bold text-slate-600 shadow-xs transition-all hover:border-slate-300 hover:bg-slate-100/80 hover:text-slate-900 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700/80 dark:bg-slate-800/80 dark:text-slate-300 dark:hover:border-slate-600 dark:hover:bg-slate-800 dark:hover:text-white sm:flex-none sm:min-w-[120px]"
    >
      {cancelLabel}
    </button>
    <SaveButton
      form={formId}
      type="submit"
      saving={isSubmitting}
      disabled={disabled}
      label={submitLabel}
      savingLabel={submittingLabel}
      className="flex-[1.5] px-6 sm:flex-none sm:min-w-[156px]"
    />
  </div>
);

export default ModalActions;
