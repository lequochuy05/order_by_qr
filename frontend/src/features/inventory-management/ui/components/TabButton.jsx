import { createElement } from 'react';

const TabButton = ({ active, icon, label, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex min-h-10 min-w-0 items-center justify-center gap-2 rounded-xl px-3 text-sm font-black transition-colors sm:px-4 ${
      active
        ? 'bg-white text-orange-600 shadow-sm dark:bg-slate-900 dark:text-orange-300'
        : 'text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-100'
    }`}
  >
    {createElement(icon, { size: 16 })} {label}
  </button>
);

export default TabButton;
