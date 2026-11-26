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
    className={`inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 text-sm font-bold text-white shadow-md shadow-indigo-600/20 transition-all hover:from-indigo-500 hover:to-violet-500 hover:shadow-lg hover:shadow-indigo-600/30 active:scale-[0.98] disabled:cursor-not-allowed disabled:from-slate-200 disabled:to-slate-200 disabled:text-slate-400 disabled:shadow-none disabled:active:scale-100 dark:disabled:from-slate-800 dark:disabled:to-slate-800 dark:disabled:text-slate-600 ${className}`}
  >
    {saving ? <Loader2 className="animate-spin" size={17} /> : createElement(icon, { size: 17 })}
    {saving ? savingLabel : label}
  </button>
);

export default SaveButton;
