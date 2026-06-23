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
    className={`flex shrink-0 gap-4 rounded-b-[2rem] border-t bg-gray-50/50 px-6 py-5 sm:px-8 sm:py-6 ${className}`}
  >
    <button
      type="button"
      onClick={onClose}
      disabled={isSubmitting}
      className="flex-1 rounded-2xl border border-gray-200 bg-white py-4 text-[11px] font-black uppercase tracking-[0.1em] text-gray-600 shadow-sm transition-all hover:bg-gray-100 active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
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
      className="flex-[2] py-4"
    />
  </div>
);

export default ModalActions;
