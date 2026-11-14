import { ShoppingBasket } from 'lucide-react';

const AddToCartButton = ({ onClick, disabled = false, label = 'Thêm vào giỏ' }) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled}
    className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-600/25 transition-all active:scale-95 disabled:cursor-not-allowed disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:shadow-none"
  >
    <ShoppingBasket size={16} />
    <span>{label}</span>
  </button>
);

export default AddToCartButton;
