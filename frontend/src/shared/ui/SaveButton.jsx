import { createElement } from 'react';
import { Loader2, Save } from 'lucide-react';

const SaveButton = ({
  type = 'button',
  form,
  onClick,
  saving = false,
  disabled = false,
  label = 'Lưu thay đổi',
  savingLabel = 'Đang lưu...',
  icon = Save,
  className = '',
}) => (
  <button
    type={type}
    form={form}
    onClick={onClick}
    disabled={saving || disabled}
    className={`inline-flex items-center justify-center gap-2 rounded-lg bg-orange-500 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-orange-600 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 disabled:active:scale-100 ${className}`}
  >
    {saving ? <Loader2 className="animate-spin" size={17} /> : createElement(icon, { size: 17 })}
    {saving ? savingLabel : label}
  </button>
);

export default SaveButton;
