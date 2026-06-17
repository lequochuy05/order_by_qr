import { fmtVND } from '@shared/lib/formatters.js';

const MenuItemPrice = ({ value = 0, className = '' }) => (
  <span className={`font-black text-orange-600 dark:text-orange-400 ${className}`}>
    {fmtVND(value)}
  </span>
);

export default MenuItemPrice;
