import { createElement } from 'react';

const TabButton = ({ active, icon, label, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex min-h-10 min-w-0 items-center justify-center gap-2 rounded-xl px-3.5 text-xs font-bold transition-all active:scale-95 sm:px-4 ${
      active
        ? 'bg-white text-indigo-600 shadow-sm dark:bg-slate-900 dark:text-indigo-400'
        : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
    }`}
  >
    {createElement(icon, { size: 16 })} {label}
  </button>
);

export default TabButton;
