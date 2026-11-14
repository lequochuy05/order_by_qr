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
    className={`inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-3 text-sm font-bold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 disabled:active:scale-100 ${className}`}
  >
    {saving ? <Loader2 className="animate-spin" size={17} /> : createElement(icon, { size: 17 })}
    {saving ? savingLabel : label}
  </button>
);

export default SaveButton;
