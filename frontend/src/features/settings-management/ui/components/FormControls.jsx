import { createElement } from 'react';

import FormError from '@shared/ui/FormError.jsx';

export const SectionHeader = ({ icon, title, subtitle }) => (
  <div className="mb-6 flex min-w-0 items-start gap-3.5">
    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
      {createElement(icon, { size: 22 })}
    </div>
    <div className="min-w-0">
      <h2 className="text-xl font-black tracking-tight text-slate-900 dark:text-white">{title}</h2>
      <p className="mt-0.5 text-sm font-medium text-slate-500 dark:text-slate-400">{subtitle}</p>
    </div>
  </div>
);

export const SettingsGroup = ({ title, description, children, className = '' }) => (
  <div
    className={`min-w-0 rounded-2xl border border-slate-200/80 bg-slate-50/50 p-5 dark:border-slate-800/80 dark:bg-slate-950/40 ${className}`}
  >
    {(title || description) && (
      <div className="mb-4">
        {title && (
          <h3 className="text-sm font-black tracking-tight text-slate-900 dark:text-white">{title}</h3>
        )}
        {description && (
          <p className="mt-1 text-xs font-medium text-slate-500 dark:text-slate-400">{description}</p>
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
      <span className="block text-xs font-medium text-slate-500 dark:text-slate-400">{description}</span>
    )}
    <input
      value={value ?? ''}
      onChange={(event) => onChange(event.target.value)}
      className={`h-11 w-full rounded-2xl border bg-white px-3.5 text-sm font-medium text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:ring-2 dark:bg-slate-900 dark:text-white ${
        error
          ? 'border-rose-300 focus:border-rose-500 focus:ring-rose-500/20 dark:border-rose-500/50'
          : 'border-slate-200/80 focus:border-indigo-500 focus:ring-indigo-500/20 dark:border-slate-700/80 dark:focus:border-indigo-400'
      }`}
      {...props}
    />
    <FormError message={error} />
  </label>
);

export const ToggleRow = ({ icon, title, description, checked, onChange }) => (
  <label className="flex min-w-0 cursor-pointer items-center justify-between gap-4 rounded-2xl border border-slate-200/80 bg-white p-4 transition-all duration-200 hover:border-indigo-200 hover:bg-indigo-50/20 dark:border-slate-800/80 dark:bg-slate-900/60 dark:hover:border-indigo-500/30 dark:hover:bg-slate-800/40">
    <span className="flex min-w-0 items-start gap-3.5">
      <span className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400">
        {createElement(icon, { size: 18 })}
      </span>
      <span className="min-w-0">
        <span className="block text-sm font-bold text-slate-900 dark:text-white">{title}</span>
        <span className="text-xs font-medium text-slate-500 dark:text-slate-400 mt-0.5 block">{description}</span>
      </span>
    </span>
    <input
      type="checkbox"
      checked={Boolean(checked)}
      onChange={(event) => onChange(event.target.checked)}
      className="h-5 w-5 shrink-0 accent-indigo-600 cursor-pointer"
    />
  </label>
);
