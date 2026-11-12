import { createElement } from 'react';

const TabButton = ({ active, icon, label, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex min-h-10 min-w-0 items-center justify-center gap-2 rounded-xl px-3 text-sm font-black transition-colors sm:px-4 ${
      active
        ? 'bg-white text-[#56372B] shadow-sm dark:bg-[#1F1A17] dark:text-[#D8986A]'
        : 'text-[#6B5D55] hover:text-[#2D1D16] dark:text-[#BDB0A4] dark:hover:text-[#F7F0E7]'
    }`}
  >
    {createElement(icon, { size: 16 })} {label}
  </button>
);

export default TabButton;
