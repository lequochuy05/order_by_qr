import { createElement } from 'react';
import { PencilLine, PlusCircle, X } from 'lucide-react';

const EDIT_TITLE_PATTERN = /(cập nhật|chỉnh sửa|sửa|thiết lập|định lượng|gộp|nhập kho|kiểm kê|edit|update)/i;

const ModalHeader = ({
  title,
  subtitle,
  badge,
  icon,
  onClose,
  disabled = false,
  className = '',
}) => {
  const isEditLike = EDIT_TITLE_PATTERN.test(title || '');
  const ResolvedIcon = icon || (isEditLike ? PencilLine : PlusCircle);

  return (
    <div className={`shrink-0 ${className}`}>
      <div className="h-1 w-full bg-gradient-to-r from-indigo-500 via-violet-500 to-purple-500" />
      <div className="flex items-center justify-between gap-4 border-b border-slate-200/80 bg-gradient-to-b from-slate-50/90 to-white px-6 py-4 transition-colors dark:border-slate-800/80 dark:from-slate-900 dark:to-slate-900/95 sm:px-8 sm:py-5">
        <div className="flex min-w-0 items-center gap-3.5">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500/12 via-violet-500/12 to-purple-500/12 text-indigo-600 ring-1 ring-indigo-500/20 dark:from-indigo-500/20 dark:to-violet-500/20 dark:text-indigo-400 dark:ring-indigo-400/25">
            {createElement(ResolvedIcon, { size: 20, strokeWidth: 2.2 })}
          </div>
          <div className="min-w-0">
            {subtitle && (
              <p className="mb-0.5 truncate text-[10px] font-extrabold uppercase tracking-[0.18em] text-indigo-600/85 dark:text-indigo-400/90">
                {subtitle}
              </p>
            )}
            <div className="flex min-w-0 items-center gap-2">
              <h2 className="truncate text-lg font-black tracking-tight text-slate-900 dark:text-white sm:text-xl">
                {title}
              </h2>
              {badge}
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          disabled={disabled}
          aria-label="Đóng"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-slate-200/80 bg-white text-slate-400 shadow-xs transition-all hover:border-rose-200 hover:bg-rose-50 hover:text-rose-600 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700/80 dark:bg-slate-800/80 dark:text-slate-400 dark:hover:border-rose-500/30 dark:hover:bg-rose-500/15 dark:hover:text-rose-400"
        >
          <X size={18} />
        </button>
      </div>
    </div>
  );
};

export default ModalHeader;
