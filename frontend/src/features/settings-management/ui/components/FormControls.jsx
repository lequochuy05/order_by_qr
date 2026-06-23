import { createElement } from 'react';

import FormError from '@shared/ui/FormError.jsx';

export const SectionHeader = ({ icon, title, subtitle }) => (
  <div className="mb-6 flex min-w-0 items-start gap-3">
    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-orange-50 text-orange-500 dark:bg-orange-500/10 dark:text-orange-300">
      {createElement(icon, { size: 20 })}
    </div>
    <div className="min-w-0">
      <h2 className="text-lg font-black text-slate-900 dark:text-slate-100">{title}</h2>
      <p className="text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>
    </div>
  </div>
);

export const SettingsGroup = ({ title, description, children, className = '' }) => (
  <div
    className={`min-w-0 rounded-lg border border-slate-200 p-4 dark:border-slate-800 ${className}`}
  >
    {(title || description) && (
      <div className="mb-4">
        {title && (
          <h3 className="text-sm font-black text-slate-900 dark:text-slate-100">{title}</h3>
        )}
        {description && (
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{description}</p>
        )}
      </div>
    )}
    {children}
  </div>
);

export const TextField = ({
  label,
  description,
  value,
  onChange,
  error,
  className = '',
  ...props
}) => (
  <label className={`min-w-0 space-y-2 ${className}`}>
    <span className="block text-sm font-bold text-slate-700 dark:text-slate-200">{label}</span>
    {description && (
      <span className="block text-xs text-slate-500 dark:text-slate-400">{description}</span>
    )}
    <input
      value={value ?? ''}
      onChange={(event) => onChange(event.target.value)}
      className={`h-11 w-full rounded-lg border bg-white px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 dark:bg-slate-950 dark:text-slate-100 ${
        error
          ? 'border-red-500 focus:ring-2 focus:ring-red-100 dark:border-red-500 dark:focus:ring-red-500/20'
          : 'border-slate-200 focus:border-orange-400 focus:ring-2 focus:ring-orange-100 dark:border-slate-700 dark:focus:ring-orange-500/20'
      }`}
      {...props}
    />
    <FormError message={error} />
  </label>
);

export const ToggleRow = ({ icon, title, description, checked, onChange }) => (
  <label className="flex min-w-0 cursor-pointer items-center justify-between gap-3 rounded-lg border border-slate-200 p-3 transition hover:bg-slate-50 sm:gap-4 sm:p-4 dark:border-slate-800 dark:hover:bg-slate-800/70">
    <span className="flex min-w-0 items-start gap-3">
      <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300">
        {createElement(icon, { size: 17 })}
      </span>
      <span className="min-w-0">
        <span className="block text-sm font-black text-slate-900 dark:text-slate-100">{title}</span>
        <span className="text-sm text-slate-500 dark:text-slate-400">{description}</span>
      </span>
    </span>
    <input
      type="checkbox"
      checked={Boolean(checked)}
      onChange={(event) => onChange(event.target.checked)}
      className="h-5 w-5 shrink-0 accent-orange-500"
    />
  </label>
);
