import { createElement } from 'react';

const TabButton = ({ active, icon, label, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex min-h-10 min-w-0 items-center justify-center gap-2 rounded-xl px-3 text-sm font-black transition-colors sm:px-4 ${
      active ? 'bg-white text-orange-600 shadow-sm' : 'text-gray-500 hover:text-gray-800'
    }`}
  >
    {createElement(icon, { size: 16 })} {label}
  </button>
);

export default TabButton;
