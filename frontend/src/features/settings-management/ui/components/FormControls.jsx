import { createElement } from 'react';

import FormError from '@shared/ui/FormError.jsx';

export const SectionHeader = ({ icon, title, subtitle }) => (
  <div className="mb-6 flex min-w-0 items-start gap-3">
    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#F7F0E7] text-[#C17C4A] dark:bg-[#2A221E] dark:text-[#D8986A]">
      {createElement(icon, { size: 20 })}
    </div>
    <div className="min-w-0">
      <h2 className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7]">{title}</h2>
      <p className="text-sm text-[#6B5D55] dark:text-[#BDB0A4]">{subtitle}</p>
    </div>
  </div>
);

export const SettingsGroup = ({ title, description, children, className = '' }) => (
  <div
    className={`min-w-0 rounded-2xl border border-[#E8DCD1] p-5 dark:border-[#352C26] ${className}`}
  >
    {(title || description) && (
      <div className="mb-4">
        {title && (
          <h3 className="text-sm font-black text-[#2D1D16] dark:text-[#F7F0E7]">{title}</h3>
        )}
        {description && (
          <p className="mt-1 text-xs text-[#6B5D55] dark:text-[#BDB0A4]">{description}</p>
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
    <span className="block text-sm font-bold text-[#2D1D16] dark:text-[#F7F0E7]">{label}</span>
    {description && (
      <span className="block text-xs text-[#6B5D55] dark:text-[#BDB0A4]">{description}</span>
    )}
    <input
      value={value ?? ''}
      onChange={(event) => onChange(event.target.value)}
      className={`h-11 w-full rounded-xl border bg-white px-3 text-sm text-[#2D1D16] outline-none transition placeholder:text-[#9C8E85] dark:bg-[#150E0B] dark:text-[#F7F0E7] ${
        error
          ? 'border-red-500 focus:ring-2 focus:ring-red-100 dark:border-red-500 dark:focus:ring-red-500/20'
          : 'border-[#E8DCD1] focus:border-[#C17C4A] focus:ring-2 focus:ring-[#C17C4A]/20 dark:border-[#352C26] dark:focus:ring-[#C17C4A]/20'
      }`}
      {...props}
    />
    <FormError message={error} />
  </label>
);

export const ToggleRow = ({ icon, title, description, checked, onChange }) => (
  <label className="flex min-w-0 cursor-pointer items-center justify-between gap-3 rounded-2xl border border-[#E8DCD1] p-4 transition hover:bg-[#F7F0E7]/40 sm:gap-4 dark:border-[#352C26] dark:hover:bg-[#2A221E]/40">
    <span className="flex min-w-0 items-start gap-3">
      <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[#F7F0E7] text-[#56372B] dark:bg-[#2A221E] dark:text-[#D8986A]">
        {createElement(icon, { size: 17 })}
      </span>
      <span className="min-w-0">
        <span className="block text-sm font-black text-[#2D1D16] dark:text-[#F7F0E7]">{title}</span>
        <span className="text-sm text-[#6B5D55] dark:text-[#BDB0A4]">{description}</span>
      </span>
    </span>
    <input
      type="checkbox"
      checked={Boolean(checked)}
      onChange={(event) => onChange(event.target.checked)}
      className="h-5 w-5 shrink-0 accent-[#56372B] dark:accent-[#C17C4A]"
    />
  </label>
);
